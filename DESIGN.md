# DESIGN — key decisions & reasoning

A short tour of the choices behind the slice and the two hard problems the brief asks me to
*discuss rather than build*.

## Shape of the system

Two services over a shared Postgres, talking synchronous REST.

- **Station Service** owns physical infra + pricing. Deliberately dumb: reads plus one
  `occupy`/`release` status flip. It never knows about sessions or money.
- **Session Service** owns the lifecycle and the wallet. It is where correctness matters, so the
  guards and the money maths live here. The `ChargingSession` aggregate owns its own
  `start()`/`complete()` transitions, so the guard ("can't stop a non-ACTIVE session") is enforced
  by the domain object, not scattered across the controller.

## Open design decisions (each with a one-line "why")

- **Language — Java 21 + Spring Boot.** House stack; lets me write idiomatic REST + JPA + tests fast.
- **Database — one shared Postgres.** Simplest thing that works. Each service keeps its own tables
  and its own Flyway history table (`flyway_history_station` / `_session`) so migrating the shared
  DB never clashes. Schema is created from scratch by Flyway and survives a restart (named volume).
- **Service comms — sync REST.** The required Session→Station call is a real network hop via
  `RestClient`. Wallet settlement is an in-process module call, not a hop — no broker, no saga.
  (How I'd do the event alternative: publish one `SessionCompleted` and debit in a listener; I
  kept it in-process to avoid a second moving part for a single consumer.)
- **Tariff — snapshot at start.** Stored on the session row. If pricing changes mid-session the
  driver still pays the price they saw when they plugged in. Re-look-up-at-stop would leak later
  price changes into an already-running session.
- **Wallet — folded into Session Service.** Two services keeps the essential slice small. A third
  Wallet Service would demonstrate another boundary but adds a network hop and failure mode for no
  correctness gain here; I'd split it out once a second consumer of balances exists.
- **Insufficient balance — allow the stop, go negative.** A session that has physically ended must
  always be closeable and the connector freed; blocking the stop on billing would strand the
  connector as OCCUPIED. The debt is recoverable later (top-up / dunning). Rejecting the stop is
  equally defensible — I chose availability of the physical asset over strict prepay.
- **Dependency-down — fail fast.** If Station Service is unreachable at start/stop, the client
  maps it to a clean **502 `DEPENDENCY_UNAVAILABLE`** rather than retrying or falling back. Stop
  runs in one transaction, so a failure to `release` rolls the whole stop back — nothing is half
  committed. Retries/backoff are deliberately not built.
- **Repo — monorepo.** One clone, one compose up. Trade-off: services version together; fine here.
- **Money — `BigDecimal`, `HALF_UP`, 2 dp**, stored as `NUMERIC(12,2)`. All arithmetic in
  `CostCalculator.cost()`; no float ever touches a price.

## Reasoning paragraph 1 — idempotent retries (NOT built)

The app resends `POST /sessions/{id}/stop` because the first response was lost, so the same stop
arrives twice. Today the second call is already safe *for the wrong reason*: the session is no
longer ACTIVE, so it returns `409 SESSION_NOT_ACTIVE` and does **not** double-charge — the state
guard covers the common case. What it does *not* do is return the original success receipt, so a
retrying client sees an error for a stop that actually succeeded. The clean fix is an
**idempotency key**: the client sends `Idempotency-Key: <uuid>` on the stop; we persist the key
alongside the session outcome the first time, and on a replay with the same key we short-circuit
and return the stored receipt (same 200 body) instead of re-running billing. That makes the
operation genuinely idempotent — safe *and* transparent to retries — without brokers or
exactly-once delivery. I'd scope the key per session so an unrelated stop can't collide.

## Reasoning paragraph 2 — stuck connectors / partial failure (NOT built)

The dangerous window is: `occupy` succeeds on Station Service, then `create-session` fails (DB
blip, or Session Service crashes before committing). Now the connector is OCCUPIED with no ACTIVE
session behind it — stuck, un-startable, and no stop will ever free it. Three complementary fixes,
cheapest first: (1) **order + compensate** — if session creation throws after a successful occupy,
best-effort call `release` in a catch/finally so the common transient failure self-heals; (2) a
**reaper job** — periodically find connectors OCCUPIED with no ACTIVE session (or whose session is
older than a max charge duration) and release them, which also recovers crashes the in-process
compensation can't; (3) a short **occupy TTL / lease** so an occupy that isn't confirmed by a
session within N seconds auto-expires. Trade-offs: the reaper needs Session→Station to agree on
"orphaned", so it must read session state; and the TTL risks releasing a legitimately slow start,
so N must exceed the worst-case start latency. For this slice I documented it rather than building
it, since it drags in timeouts and a scheduler the brief explicitly says to keep out.

## What's unfinished / cut deliberately

- No cross-service integration test with a real Postgres (would use Testcontainers next).
- No OpenAPI/Swagger (endpoints are documented in the README instead).
- No reservations, time-of-use tariff, or real event broker (stretch goals, not attempted).
- Duplicated error-handling + JWT/security classes across both services — acceptable for a
  two-service take-home; in a larger repo I'd extract a shared `common` module.
