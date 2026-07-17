# ChargeSquare — EV Charging Backend (Case Study)

One clean, correct slice of an EV-charging backend: **start a session → stop it → price it
from a tariff → settle the wallet**, built as **three cooperating Spring Boot services** behind a
shared Postgres database, plus a React ops panel with JWT/RBAC (Stage 2).

Stage 1 (essential) and Stage 2 (panel + security) are complete, along with **every stretch goal**
from the brief — see [Advanced features](#advanced-features-stretch-goals).

```
 React Ops Panel (Stage 2)  ──login──▶ Session Service ──┐
        │  Bearer <jwt>                                   │
        ├─────────── reads ──────────▶ Station Service    │ real synchronous
        └─────────── wallet ─────────▶ Wallet Service     │ REST calls
                                                          ▼
┌──────────────────┐   occupy/release/reserve   ┌──────────────────┐
│  Session Service │──────── (network) ────────▶│  Station Service │
│  :8082           │                            │  :8081           │
│  session lifecycle│   idempotent debit         │ stations/        │
│  auth · idempotency│──────────┐                │ connectors/tariffs│
└──────────────────┘           ▼                └──────────────────┘
                     ┌──────────────────┐
                     │  Wallet Service  │        all three own their own tables
                     │  :8083           │        + their own Flyway history in
                     │ balances · debit │        one shared PostgreSQL
                     └──────────────────┘
```

- **Station Service** (`:8081`) — source of truth for stations, connectors and tariffs. Reads,
  the internal `occupy`/`release` flip, **reservations**, and **time-of-use** pricing.
- **Session Service** (`:8082`) — the heart of the exercise: the guarded start/stop lifecycle,
  cost calculation, settlement orchestration, **client idempotency**, auth/JWT, and a
  **stuck-connector reaper**.
- **Wallet Service** (`:8083`) — per-user balances with an **idempotent debit** at stop time and a
  top-up endpoint.
- **Web panel** (`:8080`, optional) — a JWT-authenticated ops panel: a **Charging** page (start,
  reserve, watch live charging, stop & bill) and a **Sessions** page (history, receipts, wallet).
  Screenshots in [docs/screenshots/](docs/screenshots).

## Stack & why

| Choice | Pick | Why (one sentence) |
| --- | --- | --- |
| Language / framework | **Java 21 + Spring Boot 3.3** | House stack; fastest path to idiomatic, well-tested REST + JPA. |
| Database | **PostgreSQL, single shared DB** | Simplest thing that works; each service owns its tables + its own Flyway history table. |
| Service comms | **Synchronous REST** (`RestClient`) | Two real network hops (Session→Station, Session→Wallet); no broker/saga. |
| Tariff on a session | **Snapshot at start** (peak/off-peak resolved then) | A mid-session price change never alters what the driver is charged. |
| Wallet placement | **Separate Wallet Service** | Demonstrates a second clean boundary + idempotent settlement (a stretch beyond the 2-service default). |
| Repo layout | **Monorepo** | One clone, one `docker compose up`; trade-off is coupled versioning. |
| Money | **`BigDecimal`, `HALF_UP` to 2 dp** | Decimal-safe; never a float where money is involved (`CostCalculator`). |
| Idempotency | **Two layers** | Client `Idempotency-Key` on stop + wallet debit keyed by session id → a retried stop never double-charges. |

## Run it — one command

```bash
cp .env.example .env          # local dev placeholders; never commit a real .env
docker compose up --build     # Postgres + Station + Wallet + Session, schema created from scratch
```

Wait for the services to report healthy, then drive the flow below (curl) or use the UI.

### Open the admin panel (UI)

```bash
docker compose --profile panel up --build     # brings up everything + the panel
```

Then open **http://localhost:8080** and log in:

| User | Password | Role | Can start / stop / reserve / top-up? |
| --- | --- | --- | --- |
| `admin` | `admin123` | ADMIN | ✅ yes |
| `viewer` | `viewer123` | VIEWER | ❌ read-only (buttons disabled; the API also returns 403) |

From the **Charging** page you can start a session, reserve a connector, watch it charge live and
**Stop & bill**; the **Sessions** page shows the history, receipts and wallet. (Screenshots are in
[docs/screenshots/](docs/screenshots).) API docs are at **http://localhost:8082/swagger-ui.html**.

> **Security note:** the compose file runs with `SECURITY_ENABLED=true`. The curl walkthrough logs
> in first. Set `SECURITY_ENABLED=false` in `.env` for an open, token-free backend.

## End-to-end walkthrough (curl)

Seed: connector `10` AVAILABLE, tariff `8.50/kWh + 2.00` (peak `10.50`), connector `11` at `5.00`,
user `7` wallet `500.00`.

```bash
# 1) Log in as ADMIN
TOKEN=$(curl -s -X POST http://localhost:8082/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
A="Authorization: Bearer $TOKEN"; J="Content-Type: application/json"

# 2) START on connector 10 -> 201, connector OCCUPIED, tariff snapshotted
curl -s -X POST http://localhost:8082/sessions -H "$A" -H "$J" -d '{"userId":7,"connectorId":10}'

# 3) STOP with 12.5 kWh (+ Idempotency-Key) -> priced, settled via Wallet Service
curl -s -X POST http://localhost:8082/sessions/100/stop -H "$A" -H "$J" \
  -H 'Idempotency-Key: abc-1' -d '{"energyKwh":12.5}'
# { "status":"COMPLETED", "cost":108.25, "walletBalanceAfter":391.75, ... }
# (peak hours: pricePerKwh snapshot is 10.50 and cost is 133.25)

# 4) Replaying the same stop with the same Idempotency-Key -> the SAME receipt, no double charge
curl -s -X POST http://localhost:8082/sessions/100/stop -H "$A" -H "$J" \
  -H 'Idempotency-Key: abc-1' -d '{"energyKwh":12.5}'

# 5) Reserve then start on your own reservation
curl -s -X POST http://localhost:8082/reservations -H "$A" -H "$J" -d '{"userId":7,"connectorId":11}'
curl -s -X POST http://localhost:8082/sessions      -H "$A" -H "$J" -d '{"userId":7,"connectorId":11}'

# 6) Reads
curl -s -H "$A" http://localhost:8082/sessions/100
curl -s -H "$A" http://localhost:8081/stations/1/connectors
curl -s -H "$A" http://localhost:8083/wallets/7
```

Guards (each returns the small `{ "error", "message" }` body): unknown connector → 404, occupied →
409, stop-twice (no key) → 409, missing/negative field → 400, VIEWER on a write → 403.

## Main endpoints

**Station Service (`:8081`)** — `GET /connectors/{id}` · `GET /stations/{id}/connectors` ·
`POST /connectors/{id}/occupy|release` · `POST /connectors/{id}/reserve|cancel-reservation` (ADMIN).

**Session Service (`:8082`)** — `POST /auth/login` · `POST /sessions` (ADMIN) ·
`POST /sessions/{id}/stop` (ADMIN, optional `Idempotency-Key`) · `POST /reservations` (ADMIN) ·
`GET /sessions/{id}` · `GET /users/{userId}/sessions`.

**Wallet Service (`:8083`)** — `GET /wallets/{userId}` · `POST /wallets/{userId}/topup` (ADMIN) ·
`POST /wallets/{userId}/debit` (ADMIN, service-to-service, idempotent).

Per service: `GET /actuator/health` (+ readiness/liveness), `GET /actuator/prometheus` (metrics),
and `GET /swagger-ui.html` (OpenAPI).

## Advanced features (stretch goals)

Every stretch goal from the brief is implemented and tested:

| Stretch goal | Where |
| --- | --- |
| **Wallet top-up** | `POST /wallets/{id}/topup` |
| **Third Wallet Service** | `wallet-service/` — second clean boundary, real Session→Wallet REST call |
| **Reservation** (RESERVED, expiry) | Station `reserve/cancel` + `ReservationReaper`; Session `POST /reservations` |
| **Time-of-use tariff** | `PeakSchedule` + peak price; effective price snapshotted at start (see `PeakScheduleTest`) |
| **Real idempotent stop** | client `Idempotency-Key` (Session) + idempotent debit keyed by session id (Wallet) |
| **Stuck-connector recovery** | `StuckConnectorReaper` reconciles OCCUPIED-without-ACTIVE and releases |
| **SessionCompleted event** | published after commit, logged stub subscriber (`SessionCompletedListener`) |
| **Observability** | Prometheus metrics, OpenAPI/Swagger, structured JSON logs (`json` profile), k8s probes |
| **Integration test** | `StartStopIntegrationTest` — Testcontainers Postgres + WireMock over real HTTP |

## Tests

```bash
cd station-service && mvn test   # 10: peak schedule, connector reads, reservation lifecycle
cd session-service && mvn test   # 9: cost calc, start/stop lifecycle, idempotency (+ 1 integration)
cd wallet-service  && mvn test   # 4: idempotent debit (no double-charge), top-up, 404
```

23 tests total. The Testcontainers integration test runs in CI and **skips automatically** where
Docker isn't reachable (it's marked `disabledWithoutDocker`).

## Configuration

Everything from env vars — nothing hardcoded. See [.env.example](.env.example). Key vars:
`DB_URL`, `POSTGRES_USER/PASSWORD`, `STATION_SERVICE_URL`, `WALLET_SERVICE_URL`, `SECURITY_ENABLED`,
`JWT_SECRET`, `PRICING_PEAK_START_HOUR/END_HOUR`, `RECONCILE_*`. No secrets committed.

## Kubernetes & CI

- `k8s/` — Deployment + Service per service (station, session, wallet, postgres), a **ConfigMap**
  and a **Secret** (placeholders). Validate with `kubectl apply --dry-run=client -f k8s/` (the
  manifests are also structurally valid offline; a reachable cluster is needed for full schema
  validation).
- `.github/workflows/ci.yml` — matrix build + test (JDK 21) + Docker build for all three services,
  plus the panel build.

## Assumptions & known gaps

- **Insufficient balance → the stop succeeds and the wallet may go negative** (implemented; see
  `Wallet.debit`). A physically-ended session must always be closeable and the connector freed; the
  negative balance is a recoverable debt. Rejecting the stop is the equally-valid alternative.
- **Settlement across services** uses one synchronous idempotent debit inside the stop transaction:
  if the debit fails the stop rolls back (fail-fast 502); if a later step fails and the client
  retries, the debit's idempotency key (session id) prevents a double charge. See `DESIGN.md`.
- Energy is **reported at stop** (meter simulated). Driver `7` is pre-seeded; ops users (`admin`,
  `viewer`) are seeded at startup with BCrypt-hashed passwords from env.

## Time spent / what I'd do next

~A day on Stage 1, a few hours on Stage 2, and a further focused block on the stretch goals + the
third service. **Next:** promote the Testcontainers test to also boot Station/Wallet as containers;
add asymmetric (RS256) JWT so only the auth service signs; and add an outbox for the
`SessionCompleted` event if async consumers appear.
