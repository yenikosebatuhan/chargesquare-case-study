# ChargeSquare Ops Panel (Stage 2)

A small React + Vite admin panel: login, stations/connectors list, sessions list, and a
role-gated **stop session** / **top-up wallet** action. Auth is JWT; roles are enforced on the
**server** (the panel only disables buttons as a hint).

## Run

With the backend already up (`docker compose up` from the repo root):

```bash
npm install
npm run dev        # http://localhost:5173
```

Or run everything (backend + panel) with Docker from the repo root:

```bash
docker compose --profile panel up --build   # panel on http://localhost:8080
```

Log in with a demo user:

| User | Password | Role | Can stop/top-up? |
| --- | --- | --- | --- |
| `admin` | `admin123` | ADMIN | ✅ |
| `viewer` | `viewer123` | VIEWER | ❌ (buttons disabled; API returns 403) |

## Config

Base URLs are read at build time from `VITE_SESSION_API` and `VITE_STATION_API`
(default `http://localhost:8082` / `:8081`). See `../SECURITY.md` for the auth design and the
token-storage trade-off.
