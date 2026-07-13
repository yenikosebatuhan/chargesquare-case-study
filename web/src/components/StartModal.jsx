import { useState } from "react";
import { api } from "../api.js";
import Modal from "./Modal.jsx";
import { money } from "../ui.jsx";
import { Zap, Warning, Plug } from "../icons.jsx";

// Confirm-and-start a charging session on an available connector.
export default function StartModal({ connector, driverId, onClose, onStarted }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const t = connector.tariff;

  const start = async () => {
    setBusy(true);
    setError(null);
    try {
      const session = await api.startSession(driverId, connector.connectorId);
      onStarted(session);
    } catch (err) {
      setError(err.code === "FORBIDDEN" ? "Your role cannot start a session." : err.message);
      setBusy(false);
    }
  };

  return (
    <Modal
      title="Start charging"
      subtitle={`Connector #${connector.connectorId} · ${connector.type}`}
      icon={<Zap style={{ width: 18, height: 18 }} />}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose} disabled={busy}>Cancel</button>
          <button className="btn btn-primary" onClick={start} disabled={busy}>
            <Zap style={{ width: 15, height: 15 }} /> {busy ? "Starting…" : "Start charging"}
          </button>
        </>
      }
    >
      {error && <div className="error-banner"><Warning style={{ width: 16, height: 16 }} /> {error}</div>}
      <div className="summary">
        <div className="summary-row"><span className="k"><Plug style={{ width: 15, height: 15 }} /> Connector</span>
          <span className="v">#{connector.connectorId} · {connector.type} · {connector.powerKw} kW</span></div>
        <div className="summary-row"><span className="k">Driver</span><span className="v">#{driverId}</span></div>
        <div className="summary-row"><span className="k">Price</span><span className="v">{money(t.pricePerKwh, t.currency)}/kWh</span></div>
        <div className="summary-row"><span className="k">Start fee</span><span className="v">{money(t.startFee, t.currency)}</span></div>
      </div>
      <p className="hint-text">
        The connector will be marked <b>OCCUPIED</b> and the tariff snapshotted. Energy is metered
        when you stop — duration doesn’t affect the price in this slice.
      </p>
    </Modal>
  );
}
