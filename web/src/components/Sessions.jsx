import { useCallback, useEffect, useState } from "react";
import { api } from "../api.js";
import { StatusPill } from "./Stations.jsx";
import { Activity, Wallet, Receipt, Stop, Plus, Close, Warning } from "../icons.jsx";

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

  useEffect(() => { load(); }, [load]);

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

  const stop = (session, e) => {
    e.stopPropagation();
    const input = window.prompt(`Energy delivered (kWh) for session ${session.sessionId}?`, "12.5");
    if (input == null) return;
    action(() => api.stopSession(session.sessionId, Number(input)));
  };

  const topUp = () => {
    const input = window.prompt(`Top-up amount for driver ${userId}?`, "100");
    if (input == null) return;
    action(() => api.topUp(userId, Number(input)));
  };

  const active = sessions.filter((s) => s.status === "ACTIVE").length;
  const completed = sessions.filter((s) => s.status === "COMPLETED").length;

  return (
    <>
      <div className="kpis">
        <Kpi label="Active" value={active} tone="amber" icon={<Activity />} />
        <Kpi label="Completed" value={completed} tone="green" icon={<Receipt />} />
        <Kpi label="Wallet balance" tone="blue" icon={<Wallet />}
             value={wallet ? <>{wallet.balance}<small>{wallet.currency}</small></> : "—"} />
      </div>

      {error && <div className="error-banner"><Warning style={{ width: 16, height: 16 }} /> {error}</div>}

      <div className="card">
        <div className="card-head">
          <h3>Driver #{userId} — Sessions</h3>
          <button className="btn btn-ghost btn-sm" onClick={topUp} disabled={!isAdmin || busy}
                  title={isAdmin ? "Top up wallet" : "ADMIN only"}>
            <Plus style={{ width: 15, height: 15 }} /> Top up
          </button>
        </div>
        <div className="card-body">
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Session</th><th>Status</th><th>Energy</th><th>Cost</th><th>Started</th><th></th></tr>
              </thead>
              <tbody>
                {loading && [0, 1, 2].map((i) => (
                  <tr key={i} className="skeleton-row"><td><span /></td><td><span /></td><td><span /></td><td><span /></td><td><span /></td><td><span /></td></tr>
                ))}
                {!loading && sessions.length === 0 && (
                  <tr><td colSpan="6" className="state">
                    <div className="spinner" style={{ display: "none" }} />
                    No sessions yet. Start one from the API or panel.
                  </td></tr>
                )}
                {!loading && sessions.map((s) => (
                  <tr key={s.sessionId} className="clickable" onClick={() => setSelected(s)}>
                    <td className="mono cell-strong">#{s.sessionId}</td>
                    <td><StatusPill status={s.status} /></td>
                    <td className="mono">{s.energyKwh != null ? `${s.energyKwh} kWh` : "—"}</td>
                    <td className="mono cell-strong">{s.cost != null ? `${s.cost} ${s.currency}` : "—"}</td>
                    <td className="cell-sub">{new Date(s.startedAt).toLocaleString()}</td>
                    <td style={{ textAlign: "right" }}>
                      {s.status === "ACTIVE" && (
                        <button className="btn btn-danger btn-sm" disabled={!isAdmin || busy} onClick={(e) => stop(s, e)}
                                title={isAdmin ? "Stop session" : "ADMIN only"}>
                          <Stop style={{ width: 14, height: 14 }} /> Stop
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {selected && <ReceiptModal session={selected} onClose={() => setSelected(null)} />}
    </>
  );
}

function Kpi({ label, value, tone, icon }) {
  return (
    <div className="kpi">
      <div className="kpi-top">
        <span className="kpi-label">{label}</span>
        <span className={`kpi-icon ${tone}`}>{icon}</span>
      </div>
      <div className="kpi-value mono">{value}</div>
    </div>
  );
}

function ReceiptModal({ session, onClose }) {
  const t = session.tariffSnapshot;
  const row = (k, v) => (
    <div className="receipt-row"><span className="k">{k}</span><span className="v">{v}</span></div>
  );
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>Session #{session.sessionId}</h3>
          <button className="icon-btn" onClick={onClose} style={{ color: "var(--muted)" }}><Close /></button>
        </div>
        <div className="receipt-rows">
          {row("Status", <StatusPill status={session.status} />)}
          {row("Connector", `#${session.connectorId}`)}
          {row("Energy", session.energyKwh != null ? `${session.energyKwh} kWh` : "—")}
          {t && row("Tariff snapshot", `${t.pricePerKwh} ${t.currency}/kWh + ${t.startFee} start`)}
          {row("Started", new Date(session.startedAt).toLocaleString())}
          {row("Ended", session.endedAt ? new Date(session.endedAt).toLocaleString() : "—")}
        </div>
        <div className="receipt-total">
          <span className="k">Total charged</span>
          <span className="v mono">{session.cost != null ? `${session.cost} ${session.currency}` : "—"}</span>
        </div>
      </div>
    </div>
  );
}
