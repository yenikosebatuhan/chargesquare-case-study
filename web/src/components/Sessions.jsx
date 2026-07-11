import { useCallback, useEffect, useState } from "react";
import { api } from "../api.js";

export default function Sessions({ userId, isAdmin, onAuthError }) {
  const [sessions, setSessions] = useState([]);
  const [wallet, setWallet] = useState(null);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [s, w] = await Promise.all([api.listUserSessions(userId), api.getWallet(userId)]);
      setSessions(s);
      setWallet(w);
    } catch (err) {
      if (err.status === 401) return onAuthError();
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [userId, onAuthError]);

  useEffect(() => {
    load();
  }, [load]);

  const action = async (fn) => {
    setBusy(true);
    setError(null);
    try {
      await fn();
      await load();
    } catch (err) {
      if (err.status === 401) return onAuthError();
      setError(err.code === "FORBIDDEN" ? "Your role cannot perform that action." : err.message);
    } finally {
      setBusy(false);
    }
  };

  const stop = (session) => {
    const input = window.prompt(`Energy delivered (kWh) for session ${session.sessionId}?`, "12.5");
    if (input == null) return;
    action(() => api.stopSession(session.sessionId, Number(input)));
  };

  const topUp = () => {
    const input = window.prompt(`Top-up amount for user ${userId}?`, "100");
    if (input == null) return;
    action(() => api.topUp(userId, Number(input)));
  };

  if (loading) return <p className="muted">Loading sessions…</p>;

  return (
    <section>
      <div className="row-between">
        <h2>Driver #{userId} — Sessions</h2>
        {wallet && (
          <div className="wallet-card">
            <span className="wallet-label">Wallet</span>
            <span className="wallet-balance">
              {wallet.balance} {wallet.currency}
            </span>
            <button className="small-btn" disabled={!isAdmin || busy} onClick={topUp}
                    title={isAdmin ? "Top up wallet" : "ADMIN only"}>
              Top up
            </button>
          </div>
        )}
      </div>

      {error && <div className="error-banner">{error}</div>}

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Status</th>
            <th>Energy</th>
            <th>Cost</th>
            <th>Started</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {sessions.length === 0 && (
            <tr><td colSpan="6" className="muted">No sessions yet.</td></tr>
          )}
          {sessions.map((s) => (
            <tr key={s.sessionId} className="clickable" onClick={() => setSelected(s)}>
              <td>{s.sessionId}</td>
              <td><span className={`status ${s.status.toLowerCase()}`}>{s.status}</span></td>
              <td>{s.energyKwh != null ? `${s.energyKwh} kWh` : "—"}</td>
              <td>{s.cost != null ? `${s.cost} ${s.currency}` : "—"}</td>
              <td>{new Date(s.startedAt).toLocaleString()}</td>
              <td onClick={(e) => e.stopPropagation()}>
                {s.status === "ACTIVE" && (
                  <button className="small-btn danger" disabled={!isAdmin || busy} onClick={() => stop(s)}
                          title={isAdmin ? "Stop session" : "ADMIN only"}>
                    Stop
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selected && <Receipt session={selected} onClose={() => setSelected(null)} />}
    </section>
  );
}

function Receipt({ session, onClose }) {
  const t = session.tariffSnapshot;
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Session {session.sessionId} receipt</h3>
        <dl>
          <dt>Status</dt><dd>{session.status}</dd>
          <dt>Connector</dt><dd>{session.connectorId}</dd>
          <dt>Energy</dt><dd>{session.energyKwh != null ? `${session.energyKwh} kWh` : "—"}</dd>
          {t && <><dt>Tariff snapshot</dt><dd>{t.pricePerKwh} {t.currency}/kWh + {t.startFee} start</dd></>}
          <dt>Cost</dt><dd>{session.cost != null ? `${session.cost} ${session.currency}` : "—"}</dd>
          <dt>Started</dt><dd>{new Date(session.startedAt).toLocaleString()}</dd>
          <dt>Ended</dt><dd>{session.endedAt ? new Date(session.endedAt).toLocaleString() : "—"}</dd>
        </dl>
        <button className="small-btn" onClick={onClose}>Close</button>
      </div>
    </div>
  );
}
