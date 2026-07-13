import { useState } from "react";
import { api } from "../api.js";
import Modal from "./Modal.jsx";
import { money, previewCost } from "../ui.jsx";
import { Stop, Warning } from "../icons.jsx";

// Stop + bill: enter the metered energy, preview the cost live, then settle.
export default function StopModal({ session, walletBalance, onClose, onStopped }) {
  const [energy, setEnergy] = useState("12.5");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const t = session.tariffSnapshot;

  const cost = previewCost(energy || 0, t.pricePerKwh, t.startFee);
  const after = walletBalance != null ? Math.round((walletBalance - cost) * 100) / 100 : null;

  const stop = async () => {
    setBusy(true);
    setError(null);
    try {
      const receipt = await api.stopSession(session.sessionId, Number(energy));
      onStopped(receipt);
    } catch (err) {
      setError(err.code === "FORBIDDEN" ? "Your role cannot stop a session." : err.message);
      setBusy(false);
    }
  };

  return (
    <Modal
      title="Stop & bill"
      subtitle={`Session #${session.sessionId} · connector #${session.connectorId}`}
      icon={<Stop style={{ width: 16, height: 16 }} />}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose} disabled={busy}>Cancel</button>
          <button className="btn btn-primary" onClick={stop} disabled={busy || energy === "" || Number(energy) < 0}>
            <Stop style={{ width: 14, height: 14 }} /> {busy ? "Settling…" : "Stop & bill"}
          </button>
        </>
      }
    >
      {error && <div className="error-banner"><Warning style={{ width: 16, height: 16 }} /> {error}</div>}

      <label className="field">
        <span>Energy delivered (kWh)</span>
        <input type="number" min="0" step="0.1" value={energy}
               onChange={(e) => setEnergy(e.target.value)} autoFocus />
      </label>
      <input className="range" type="range" min="0" max="80" step="0.5"
             value={Number(energy) || 0} onChange={(e) => setEnergy(e.target.value)} />

      <div className="cost-preview">
        <div className="cost-line"><span>{energy || 0} kWh × {money(t.pricePerKwh, t.currency)}</span>
          <span className="mono">{money(Number(energy || 0) * Number(t.pricePerKwh), t.currency)}</span></div>
        <div className="cost-line"><span>Start fee</span><span className="mono">{money(t.startFee, t.currency)}</span></div>
        <div className="cost-line total"><span>Total</span><span className="mono">{money(cost, t.currency)}</span></div>
        {after != null && (
          <div className="cost-line wallet-line">
            <span>Wallet after</span>
            <span className={`mono ${after < 0 ? "neg" : ""}`}>{money(after, t.currency)}</span>
          </div>
        )}
      </div>
    </Modal>
  );
}
