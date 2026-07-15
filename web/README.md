# ChargeSquare Ops Panel (Stage 2)

A React + Vite operations panel for the charging backend. It makes the whole product flow
visible and driveable: **start a session → watch it charge live → stop & bill → receipt**,
with a driver wallet and JWT/RBAC. Auth is JWT; roles are enforced on the **server** (the
panel only disables buttons as a hint). The brand mark is the ChargeSquare bolt, traced to SVG.

## Screens

| Screen | What it does |
| --- | --- |
| **Login** | Username/password → JWT. One-click demo credential chips. |
| **Charging** | KPI cards (charging now / available / wallet); **live "Charging now"** cards with an animated bolt + elapsed timer + **Stop & bill**; connector cards with **Start charging**. |
| **Start / Stop / Top-up modals** | Start confirms connector + tariff; Stop takes metered energy with a **live cost + wallet-after preview**; Top-up adds funds. |
| **Sessions** | History table with status/energy/cost, click a row for the **receipt**, plus wallet + top-up. |

Screenshots live in [`../docs/screenshots/`](../docs/screenshots).

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

| User | Password | Role | Can start / stop / top-up? |
| --- | --- | --- | --- |
| `admin` | `admin123` | ADMIN | ✅ |
| `viewer` | `viewer123` | VIEWER | ❌ (buttons disabled; API returns 403) |

## Config

Base URLs are read at build time from `VITE_SESSION_API`, `VITE_STATION_API` and `VITE_WALLET_API`
(default `:8082` / `:8081` / `:8083`) — the wallet now lives in its own service. The Charging page
also exposes a **Reserve** action and shows **RESERVED** / **PEAK** badges. See
[`../SECURITY.md`](../SECURITY.md) for the auth design and the token-storage trade-off.
