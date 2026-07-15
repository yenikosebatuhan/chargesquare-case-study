# DESIGN — key decisions & reasoning

A tour of the choices behind the slice. The two hard problems the brief asks me to *discuss rather
than build* are covered in the reasoning paragraphs — and, since the essentials are solid, both are
also **implemented** as stretch goals (noted inline).

## Shape of the system

Three services over a shared Postgres, talking synchronous REST.

- **Station Service** owns physical infra + pricing. Deliberately dumb: reads, one `occupy`/`release`
  flip, a short-lived reservation hold, and time-of-use pricing. It never knows about sessions or money.
- **Session Service** owns the lifecycle. Correctness lives here: the `ChargingSession` aggregate owns
  its own `start()`/`complete()` transitions, so the guard ("can't stop a non-ACTIVE session") is
  enforced by the domain object, not scattered across the controller. It orchestrates settlement.
- **Wallet Service** owns balances and the debit. The debit is idempotent (keyed by session id).

## Open design decisions (each with a one-line "why")

- **Language — Java 21 + Spring Boot.** House stack; idiomatic REST + JPA + tests fast.
- **Database — one shared Postgres.** Simplest thing that works. Each service keeps its own tables and
  its own Flyway history table (`flyway_history_station` / `_session` / `_wallet`) so migrating the
  shared DB never clashes (`baseline-on-migrate` + `baseline-version: 0` so the second/third service
  still runs its own V1). Schema created from scratch; survives restart (named volume).
- **Service comms — sync REST.** Two real network hops via `RestClient`: Session→Station (occupy/read)
  and Session→Wallet (debit). No broker, no saga.
- **Tariff — snapshot at start.** The *effective* price (peak or off-peak, resolved at start from the
  time-of-use window) is snapshotted on the session. A mid-session price change never alters this
  session; re-look-up-at-stop would leak later changes into a running session.
- **Wallet — its own service (updated decision).** The brief's default is to fold the wallet in, and
  Stage 1 shipped that way. With the essentials solid, I extracted it (a listed stretch) to show a
  second clean boundary and a real second inter-service call. The cost is a distributed-write concern
  at stop — handled by idempotency, below — which is exactly why the *default* is to fold in.
- **Insufficient balance — allow the stop, go negative.** A physically-ended session must always be
  closeable and the connector freed; blocking the stop on billing would strand the connector. The debt
  is recoverable (top-up / dunning). Rejecting the stop is equally defensible — I chose asset availability.
- **Dependency-down — fail fast.** If Station or Wallet is unreachable at stop, the client maps it to a
  clean **502 `DEPENDENCY_UNAVAILABLE`**. The stop runs in one transaction, so a failure rolls the whole
  thing back — nothing half-committed. No retry/backoff machinery.
- **Repo — monorepo.** One clone, one compose up. Trade-off: services version together.
- **Money — `BigDecimal`, `HALF_UP`, 2 dp**, stored `NUMERIC(12,2)`. All arithmetic in `CostCalculator`.

## Cross-service settlement correctness (the interesting part)

Extracting the wallet re-introduces the classic dual-write problem: Session marks a session COMPLETED
locally *and* debits a remote wallet — either can fail independently. I keep it correct without a saga:

1. `stop` runs in one transaction: guard ACTIVE → `complete()` (in memory) → **debit Wallet** (remote,
   idempotent, key = `session-<id>`) → record balance → **release** connector (remote).
2. If the **debit fails**, the transaction rolls back — session stays ACTIVE, nothing charged, client
   gets 502 and can retry safely.
3. If a step **after** the debit fails (e.g. release), the transaction rolls back the local completion —
   but the wallet was already debited. On retry, the debit re-issues with the **same key**, the Wallet
   Service sees the key already processed and returns the recorded balance **without charging again**.
   The flow converges; no double charge. (Verified end-to-end and in `WalletDebitTest`.)

## Reasoning paragraph 1 — idempotent retries (discussed *and* implemented)

The app resends `POST /sessions/{id}/stop` because the first response was lost. The state guard alone
makes the second call safe *for the wrong reason*: the session is no longer ACTIVE, so it returns
`409` — safe from double-charge, but a retrying client sees an error for a stop that actually succeeded.
The clean fix, now **built**: the client sends `Idempotency-Key: <uuid>`; on the first stop we persist
`key → session id`, and a replay with the same key short-circuits and returns the **original receipt**
(same 200 body). Combined with the wallet debit's own idempotency key, a retried stop is safe *and*
transparent — no brokers, no exactly-once delivery. Keys are scoped per session so an unrelated stop
can't collide (mismatch → `409 IDEMPOTENCY_KEY_REUSED`).

## Reasoning paragraph 2 — stuck connectors / partial failure (discussed *and* implemented)

The dangerous window: `occupy` succeeds, then `create-session` fails — the connector is OCCUPIED with
no ACTIVE session, stuck and un-startable. Three complementary fixes, cheapest first: (1) **order +
compensate** (best-effort release on failure); (2) a **reaper** that periodically finds OCCUPIED
connectors with no ACTIVE session and releases them — recovering crashes in-process compensation can't;
(3) a short **occupy TTL/lease**. I **built the reaper** (`StuckConnectorReaper`): it reconciles
Station's OCCUPIED connectors against Session's ACTIVE sessions on a schedule and releases orphans.
Trade-offs: the reaper must read session state to judge "orphaned", and runs best-effort so it never
crashes the app. The related reservation-expiry case is handled by Station's own `ReservationReaper`.

## What's deliberately not done

- The `SessionCompleted` event is emitted to a **logged stub subscriber**, not a real broker — one hop,
  no outbox/retry (I'd add an outbox only when a real async consumer appears).
- JWT is symmetric (HS256) with a shared secret — fine for three services; I'd move to RS256 (only the
  auth service signs) as the number of services grows.
- Error-handling + JWT/security classes are duplicated per service — acceptable here; a larger repo
  would extract a shared `common` module (deliberately avoided to keep services independently buildable).
