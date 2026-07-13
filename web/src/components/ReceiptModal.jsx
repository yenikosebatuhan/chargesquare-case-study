import Modal from "./Modal.jsx";
import { StatusPill, money } from "../ui.jsx";
import { Receipt } from "../icons.jsx";

// Session receipt — used both when a stop completes and when a history row is opened.
export default function ReceiptModal({ session, onClose }) {
  const t = session.tariffSnapshot;
  const row = (k, v) => (
    <div className="receipt-row"><span className="k">{k}</span><span className="v">{v}</span></div>
  );
  return (
    <Modal
      title={`Session #${session.sessionId}`}
      subtitle="Charging receipt"
      icon={<Receipt style={{ width: 17, height: 17 }} />}
      onClose={onClose}
      footer={<button className="btn btn-ghost" onClick={onClose}>Close</button>}
    >
      <div className="receipt-rows">
        {row("Status", <StatusPill status={session.status} />)}
        {row("Connector", `#${session.connectorId}`)}
        {row("Driver", `#${session.userId}`)}
        {row("Energy", session.energyKwh != null ? `${session.energyKwh} kWh` : "—")}
        {t && row("Tariff snapshot", `${t.pricePerKwh} ${t.currency}/kWh + ${t.startFee} start`)}
        {row("Started", new Date(session.startedAt).toLocaleString())}
        {row("Ended", session.endedAt ? new Date(session.endedAt).toLocaleString() : "—")}
        {session.walletBalanceAfter != null && row("Wallet after", money(session.walletBalanceAfter, session.currency))}
      </div>
      <div className="receipt-total">
        <span className="k">Total charged</span>
        <span className="v mono">{money(session.cost, session.currency)}</span>
      </div>
    </Modal>
  );
}
