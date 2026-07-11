# ChargeSquare — Charging Session Backend (Case Study)

One clean, correct slice of an EV-charging backend: **start a session → stop it → price it
from a tariff → settle the wallet**, built as two cooperating Spring Boot services behind a
shared Postgres database, plus an optional React ops panel with JWT/RBAC (Stage 2).

```
 React Ops Panel (Stage 2)          POST /auth/login
        │  Bearer <jwt>                     │
        ▼                                   ▼
┌──────────────────┐   REST (occupy/    ┌──────────────────┐
│  Session Service │───release/read)──▶ │  Station Service │
│  8082            │   over the network │  8081            │
│  sessions + wallet│                    │ stations/        │
│  + auth/JWT       │                    │ connectors/tariffs│
└────────┬─────────┘                    └─────────┬────────┘
         └───────────────┬──────────────────────┘
                         ▼
                   PostgreSQL (shared)
```

- **Station Service** (`:8081`) — source of truth for stations, connectors and tariffs.
  Reads plus an internal `occupy`/`release` status flip.
- **Session Service** (`:8082`) — the heart of the exercise: the guarded start/stop lifecycle,
  cost calculation, and wallet settlement (wallet folded in, the recommended two-service
  default). Also hosts login/JWT issuance for Stage 2.
- **Web panel** (`:8080`, optional) — login, stations list, sessions list, and a role-gated
  stop / top-up action.

## Stack & why

| Choice | Pick | Why (one sentence) |
| --- | --- | --- |
| Language / framework | **Java 21 + Spring Boot 3.3** | Your house stack, and the fastest way for me to write idiomatic, well-tested REST + JPA. |
| Database | **PostgreSQL, single shared DB** | Simplest thing that clearly works; each service owns its own tables + Flyway history table. |
| Service comms | **Synchronous REST** (`RestClient`) | Expected default; the required Session→Station call goes over the network, wallet settled in-process. |
| Tariff on a session | **Snapshot at start** | A mid-session price change must never alter what the driver is charged. |
| Wallet placement | **Folded into Session Service** | Keeps the essential slice small; a third service would add a network hop for little gain here. |
| Repo layout | **Monorepo** | One clone, one `docker compose up`; trade-off is coupled versioning, fine for a take-home. |
| Money | **`BigDecimal`, `HALF_UP` to 2 dp** | Decimal-safe; never a float where money is involved (see `CostCalculator`). |

## Run it — one command

```bash
cp .env.example .env          # local dev placeholders; never commit a real .env
docker compose up --build     # Postgres + Station + Session, schema created from scratch
```

Wait for both services to report healthy, then drive the flow below. Add the admin panel with
`docker compose --profile panel up --build` (panel on http://localhost:8080).

> **Security note:** the compose file runs with `SECURITY_ENABLED=true` (full Stage 1 + Stage 2).
> The curl walkthrough logs in first. To exercise a pure, open **Stage-1-only** backend, set
> `SECURITY_ENABLED=false` in `.env` and skip the token steps.

## End-to-end walkthrough (curl)

Seed state: connector `10` is AVAILABLE, tariff `8.50/kWh + 2.00` start fee, user `7` wallet `500.00`.

```bash
# 1) Log in as ADMIN and capture the token
TOKEN=$(curl -s -X POST http://localhost:8082/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 2) START a session on connector 10  -> 201, connector becomes OCCUPIED, tariff snapshotted
curl -s -X POST http://localhost:8082/sessions \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"userId":7,"connectorId":10}'
# { "sessionId":100, "status":"ACTIVE", "startedAt":"...", "tariffSnapshot":{...} }

# 3) STOP it with 12.5 kWh  -> cost = 12.5*8.50 + 2.00 = 108.25; wallet 500.00 -> 391.75
curl -s -X POST http://localhost:8082/sessions/100/stop \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"energyKwh":12.5}'
# { "sessionId":100, "status":"COMPLETED", "cost":108.25, "walletBalanceAfter":391.75, ... }

# 4) Read it all back
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8082/sessions/100
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8082/users/7/sessions
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/stations/1/connectors
```

Try the guards (each returns the small `{ "error", "message" }` body):

```bash
# Start on an OCCUPIED connector -> 409 CONNECTOR_OCCUPIED
# Start on an unknown connector  -> 404 CONNECTOR_NOT_FOUND
# Stop the same session twice     -> 409 SESSION_NOT_ACTIVE (no double charge)
# Missing userId / negative energy -> 400 VALIDATION_ERROR
# A VIEWER token calling start/stop -> 403 FORBIDDEN
```

## Main endpoints

**Station Service (`:8081`)**
| Method | Path | Notes |
| --- | --- | --- |
| GET | `/connectors/{id}` | status + tariff (404 if unknown) |
| GET | `/stations/{id}/connectors` | list connectors for a station |
| POST | `/connectors/{id}/occupy` | internal; ADMIN when secured |
| POST | `/connectors/{id}/release` | internal; ADMIN when secured |

**Session Service (`:8082`)**
| Method | Path | Notes |
| --- | --- | --- |
| POST | `/auth/login` | issue JWT (public) |
| POST | `/sessions` | START (ADMIN) |
| POST | `/sessions/{id}/stop` | STOP + BILL + SETTLE (ADMIN) |
| GET | `/sessions/{id}` | one session receipt |
| GET | `/users/{userId}/sessions` | a user's sessions |
| GET | `/wallets/{userId}` | wallet balance |
| POST | `/wallets/{userId}/topup` | top up (ADMIN, stretch) |

Health: `GET /actuator/health` (and `/health/readiness`, `/health/liveness`) on each service.

## Tests

```bash
cd station-service && mvn test     # connector reads + occupy/release guards
cd session-service && mvn test     # cost calc (worked example) + start/stop lifecycle + invalid cases
```

Highlights: `CostCalculatorTest` proves `12.5 kWh → 108.25`; `SessionLifecycleTest` drives
start→stop end to end (Station Service mocked), asserts the wallet settles to `391.75`, and
checks the guards (occupied-start → 409, stop-twice → 409, missing field → 400).

## Configuration

Everything comes from env vars — nothing hardcoded. See [.env.example](.env.example). Key vars:
`DB_URL`, `POSTGRES_USER/PASSWORD`, `STATION_SERVICE_URL`, `SECURITY_ENABLED`, `JWT_SECRET`,
`JWT_EXPIRY_MINUTES`, `DEMO_ADMIN_PASSWORD`, `DEMO_VIEWER_PASSWORD`. No secrets are committed.

## Kubernetes & CI

- `k8s/` — plain YAML: Deployment + Service per service, Postgres, a **ConfigMap** (non-secret
  config incl. a default tariff price) and a **Secret** (placeholders only). Validate without a
  cluster: `kubectl apply --dry-run=client -f k8s/`.
- `.github/workflows/ci.yml` — on push: builds + tests both services (JDK 21), builds the Docker
  images, and builds the web panel.

## Assumptions & known gaps

- **Insufficient balance → allow the stop and go negative** (documented in `DESIGN.md`): a session
  that physically ended must always be closeable and the connector freed. Reject-the-stop is the
  equally-valid alternative.
- Energy is **reported in the stop request** (meter is simulated), exactly as the brief allows.
- Users/wallets are **pre-seeded** (driver `7`); there is no user-creation endpoint — out of scope.
- Panel users (`admin`, `viewer`) are **driver-independent ops accounts**, seeded at startup with
  BCrypt-hashed passwords from env.
- The **stuck-connector** and **idempotent-retry** hard problems are written up as prose in
  `DESIGN.md`, not implemented — per the brief.

## Optional parts attempted

- ✅ **Stage 2** admin panel (login, stations, sessions, role-gated stop) + full JWT/RBAC on both
  services, enforced server-side. See `SECURITY.md`.
- ✅ Wallet **top-up** endpoint (stretch) — balance goes up then down across a session.
- ✅ Readiness/liveness probes wired into k8s.
- ⬜ Not done: separate Wallet Service, reservations, time-of-use tariff, real event broker.

## Time spent / what I'd do next

Roughly a focused day on Stage 1 and a few hours on Stage 2. **Next**, in priority order: an
integration test that runs both services against a Testcontainers Postgres; an idempotency key on
`stop` (see `DESIGN.md`); OpenAPI/Swagger docs; and splitting the wallet into its own service once
a second consumer of balances appears.
