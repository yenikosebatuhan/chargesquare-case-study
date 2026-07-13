import { useCallback, useEffect, useState } from "react";
import { api } from "../api.js";
import { Kpi, StatusPill, money } from "../ui.jsx";
import { Activity, Wallet, Receipt, Stop, Plus, Warning } from "../icons.jsx";
import StopModal from "./StopModal.jsx";
import TopUpModal from "./TopUpModal.jsx";
import ReceiptModal from "./ReceiptModal.jsx";

export default function Sessions({ userId, isAdmin, onAuthError }) {
  const [sessions, setSessions] = useState([]);
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [receipt, setReceipt] = useState(null);
  const [stopTarget, setStopTarget] = useState(null);
  const [topUp, setTopUp] = useState(false);

  const load = useCallback(async () => {
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

  const active = sessions.filter((s) => s.status === "ACTIVE").length;
  const completed = sessions.filter((s) => s.status === "COMPLETED").length;

  return (
    <>
      <div className="kpis">
        <Kpi label="Active" value={active} tone="live" icon={<Activity />} />
        <Kpi label="Completed" value={completed} tone="green" icon={<Receipt />} />
        <Kpi label="Wallet balance" tone="blue" icon={<Wallet />}
             value={wallet ? <>{Number(wallet.balance).toFixed(2)}<small>{wallet.currency}</small></> : "—"} />
      </div>

      {error && <div className="error-banner"><Warning style={{ width: 16, height: 16 }} /> {error}</div>}

      <div className="card">
        <div className="card-head">
          <h3>Driver #{userId} — Sessions</h3>
          <button className="btn btn-ghost btn-sm" onClick={() => setTopUp(true)} disabled={!isAdmin}
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
                  <tr><td colSpan="6" className="state">No sessions yet.</td></tr>
                )}
                {!loading && sessions.map((s) => (
                  <tr key={s.sessionId} className="clickable" onClick={() => setReceipt(s)}>
                    <td className="mono cell-strong">#{s.sessionId}</td>
                    <td><StatusPill status={s.status} /></td>
                    <td className="mono">{s.energyKwh != null ? `${s.energyKwh} kWh` : "—"}</td>
                    <td className="mono cell-strong">{money(s.cost, s.currency)}</td>
                    <td className="cell-sub">{new Date(s.startedAt).toLocaleString()}</td>
                    <td style={{ textAlign: "right" }} onClick={(e) => e.stopPropagation()}>
                      {s.status === "ACTIVE" && (
                        <button className="btn btn-danger btn-sm" disabled={!isAdmin} onClick={() => setStopTarget(s)}
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

      {receipt && <ReceiptModal session={receipt} onClose={() => setReceipt(null)} />}
      {stopTarget && (
        <StopModal session={stopTarget} walletBalance={wallet ? Number(wallet.balance) : null}
          onClose={() => setStopTarget(null)}
          onStopped={(r) => { setStopTarget(null); setReceipt(r); load(); }} />
      )}
      {topUp && (
        <TopUpModal userId={userId} wallet={wallet}
          onClose={() => setTopUp(false)}
          onDone={() => { setTopUp(false); load(); }} />
      )}
    </>
  );
}
