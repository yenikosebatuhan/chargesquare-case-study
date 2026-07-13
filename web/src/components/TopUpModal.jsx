import { useState } from "react";
import { api } from "../api.js";
import Modal from "./Modal.jsx";
import { money } from "../ui.jsx";
import { Wallet, Plus, Warning } from "../icons.jsx";

export default function TopUpModal({ userId, wallet, onClose, onDone }) {
  const [amount, setAmount] = useState("100");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const after = wallet ? Math.round((Number(wallet.balance) + Number(amount || 0)) * 100) / 100 : null;

  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      const updated = await api.topUp(userId, Number(amount));
      onDone(updated);
    } catch (err) {
      setError(err.code === "FORBIDDEN" ? "Your role cannot top up wallets." : err.message);
      setBusy(false);
    }
  };

  return (
    <Modal
      title="Top up wallet"
      subtitle={`Driver #${userId}`}
      icon={<Wallet style={{ width: 17, height: 17 }} />}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose} disabled={busy}>Cancel</button>
          <button className="btn btn-primary" onClick={submit} disabled={busy || !(Number(amount) > 0)}>
            <Plus style={{ width: 15, height: 15 }} /> {busy ? "Adding…" : "Add funds"}
          </button>
        </>
      }
    >
      {error && <div className="error-banner"><Warning style={{ width: 16, height: 16 }} /> {error}</div>}
      <label className="field">
        <span>Amount ({wallet?.currency || "TRY"})</span>
        <input type="number" min="0" step="10" value={amount} onChange={(e) => setAmount(e.target.value)} autoFocus />
      </label>
      <div className="quick-amounts">
        {[50, 100, 250, 500].map((a) => (
          <button key={a} type="button" className="chip" onClick={() => setAmount(String(a))}>+{a}</button>
        ))}
      </div>
      {wallet && (
        <div className="cost-preview">
          <div className="cost-line"><span>Current balance</span><span className="mono">{money(wallet.balance, wallet.currency)}</span></div>
          <div className="cost-line total"><span>New balance</span><span className="mono">{money(after, wallet.currency)}</span></div>
        </div>
      )}
    </Modal>
  );
}
