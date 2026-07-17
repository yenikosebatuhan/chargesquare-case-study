# SECURITY — auth & access control (Stage 2)

Graded on design sense, not on a hardened auth system. This is a working-but-simple JWT login
plus server-enforced RBAC, with the trade-offs called out.

## Authentication — how the token is issued, carried, verified

- **Issued:** `POST /auth/login {username, password}` on Session Service. Credentials are checked
  against a **BCrypt** hash (`AuthService` + `PasswordEncoder`); on success we mint an **HS256 JWT**
  with the username as `sub`, a `role` claim, and an expiry (`JWT_EXPIRY_MINUTES`, default 120).
- **Carried:** the SPA stores the token and sends it on every request as
  `Authorization: Bearer <token>`.
- **Verified:** a `JwtAuthFilter` in **each of the three** services validates the signature + expiry
  with the shared secret and puts the role into the Spring `SecurityContext` as `ROLE_<role>`. No DB
  lookup on the hot path — the role travels in the token.

All three services share one signing secret (`JWT_SECRET`), so each can verify a token independently.
Trade-off vs. per-service keys: a shared secret is simple and stateless but means a leak affects every
service and rotation must be coordinated; as the number of services grows I'd move to asymmetric keys
(services verify with a public key, only the auth service signs) or a small introspection endpoint.

## Authorization — two roles, enforced on the server

Roles live in the token claim. Enforcement is in `SecurityConfig` on **every** service — never by
hiding buttons. The panel disables/hides controls as a courtesy, but a VIEWER token calling a write
endpoint directly still gets a **403**.

| Capability | VIEWER | ADMIN |
| --- | :---: | :---: |
| Log in | ✅ | ✅ |
| List stations & connectors | ✅ | ✅ |
| List / view sessions & receipts | ✅ | ✅ |
| Reserve / start / stop a session | ❌ | ✅ |
| Top up a wallet | ❌ | ✅ |
| Internal occupy / release / reserve connector | ❌ | ✅ (service-to-service) |
| Internal wallet **debit** | ❌ | ✅ (service-to-service) |

- Reads require any valid token → unauthenticated = **401**, under-privileged = **403** (both use
  the consistent `{ "error", "message" }` body).
- Write/management endpoints (`POST /sessions`, `/sessions/*/stop`, `/reservations`, `/wallets/*/topup`)
  require **ADMIN**.
- The internal connector `occupy|release|reserve|cancel-reservation` (Station) and the wallet `debit`
  (Wallet) also require **ADMIN**. Session Service reaches them **service-to-service** by minting a
  short-lived ADMIN service token (`ServiceTokenProvider`, subject `service:session-service`) signed
  with the shared secret — so even the internal paths are authenticated, and anonymous/viewer calls
  to them are rejected.

## Roles in the token vs. looked up server-side

I put the role **in the token**: stateless, no per-request DB hit, and any service can authorize
without talking to a user store. The trade-off is staleness — revoking or downgrading a role only
takes effect when the (short-lived) token expires. With only two demo users and 2-hour tokens
that's acceptable; for real use I'd pair short access tokens with a revocation list or server-side
lookup for sensitive actions.

## Secrets management

- `JWT_SECRET`, DB credentials, and demo passwords come from **env / config only** — never
  committed. `.env.example` ships placeholders; the k8s `Secret` (`k8s/secret.yaml`) holds
  placeholder values and is clearly marked as demo-only.
- Demo panel users are seeded at **startup** with BCrypt-hashed passwords from env (`DataSeeder`),
  so no credential — not even a hash — lives in a committed SQL file.
- **In a real deployment** I'd never store secrets in a ConfigMap or a checked-in Secret: I'd use a
  managed store (AWS/GCP Secrets Manager, Vault) via an external-secrets operator or CSI driver,
  with per-environment values and rotation, and enable encryption-at-rest for etcd.

## Input validation — never trust the client

Every request body is validated on the **backend** with Bean Validation (`@NotNull`, `@Positive`,
`@PositiveOrZero`, `@NotBlank`) and returns `400 VALIDATION_ERROR` on failure. The panel also
validates for UX, but that is *only* UX: anyone can bypass the UI and hit the API directly with
curl, so server-side validation is the real gate.

## CORS

The SPA and services are different origins, so each service exposes a **CORS policy** allowing the
panel origin(s) (`security.cors.allowed-origins`, default `localhost:5173`/`:8080`) for the needed
methods and the `Authorization`/`Content-Type` headers. In production I'd pin this to the exact
panel domain(s) over HTTPS rather than a wildcard.

## Token storage in the browser — the trade-off I'm aware of

The demo keeps the JWT in **`localStorage`** for simplicity. Trade-off: `localStorage` is readable
by any JavaScript, so it's exposed to **XSS**; an **httpOnly cookie** removes the XSS-read risk but
introduces **CSRF** (mitigated with `SameSite`/CSRF tokens). For a real ops panel I'd prefer a
short-lived token in memory + an httpOnly refresh cookie, and a strict CSP to shrink the XSS surface.

## Audit logging (plan)

Security-relevant actions should be recorded with **who, what, when**: session started/stopped (by
which actor), wallet top-ups, and **failed logins**. The services already log the domain events;
`AuthService` logs failed logins (`failed login attempt for username '…'`). A real ops panel needs
this for accountability and incident review — "who stopped that session at 02:14?" must be
answerable. Example line I'd emit to a structured audit sink:

```
audit actor=admin action=SESSION_STOP sessionId=100 cost=108.25 at=2026-07-11T09:00:00Z result=OK
```

## Not included (would add later)

Refresh tokens, OAuth/OIDC, password reset, rate limiting on login, and a real user store — all
out of scope per the brief. Each is a small, well-understood addition on top of this foundation.
