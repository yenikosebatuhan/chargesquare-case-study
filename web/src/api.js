// Thin API client. Base URLs come from build-time env (VITE_*), defaulting to localhost.
// The token is kept in localStorage for demo simplicity — see SECURITY.md for the
// XSS-vs-CSRF trade-off and what we'd use in production (httpOnly cookie / in-memory).

const SESSION_API = import.meta.env.VITE_SESSION_API || "http://localhost:8082";
const STATION_API = import.meta.env.VITE_STATION_API || "http://localhost:8081";
const WALLET_API = import.meta.env.VITE_WALLET_API || "http://localhost:8083";

const TOKEN_KEY = "cs_token";
const USER_KEY = "cs_user";

export const auth = {
  save(session) {
    localStorage.setItem(TOKEN_KEY, session.token);
    localStorage.setItem(USER_KEY, JSON.stringify({ username: session.username, role: session.role }));
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
  token: () => localStorage.getItem(TOKEN_KEY),
  user() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  isAdmin() {
    return this.user()?.role === "ADMIN";
  },
};

async function request(base, path, { method = "GET", body } = {}) {
  const headers = { "Content-Type": "application/json" };
  const token = auth.token();
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${base}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return null;

  let payload = null;
  try {
    payload = await res.json();
  } catch {
    payload = null;
  }

  if (!res.ok) {
    const err = new Error(payload?.message || `Request failed (${res.status})`);
    err.status = res.status;
    err.code = payload?.error;
    throw err;
  }
  return payload;
}

export const api = {
  login: (username, password) =>
    request(SESSION_API, "/auth/login", { method: "POST", body: { username, password } }),

  listConnectors: (stationId) => request(STATION_API, `/stations/${stationId}/connectors`),

  listUserSessions: (userId) => request(SESSION_API, `/users/${userId}/sessions`),

  // Wallet lives in its own service (Wallet Service).
  getWallet: (userId) => request(WALLET_API, `/wallets/${userId}`),

  topUp: (userId, amount) =>
    request(WALLET_API, `/wallets/${userId}/topup`, { method: "POST", body: { amount } }),

  startSession: (userId, connectorId) =>
    request(SESSION_API, "/sessions", { method: "POST", body: { userId, connectorId } }),

  stopSession: (sessionId, energyKwh) =>
    request(SESSION_API, `/sessions/${sessionId}/stop`, { method: "POST", body: { energyKwh } }),

  // Reserve a connector before starting (Session orchestrates the Station reservation).
  reserve: (userId, connectorId) =>
    request(SESSION_API, "/reservations", { method: "POST", body: { userId, connectorId } }),
};
