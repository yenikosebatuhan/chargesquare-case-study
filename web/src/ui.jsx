import { useEffect, useState } from "react";

// ---- Shared presentational helpers used across the panel ----

export function StatusPill({ status }) {
  const map = { AVAILABLE: "green", COMPLETED: "green", OCCUPIED: "amber", ACTIVE: "live", RESERVED: "reserved" };
  const cls = map[status] || "green";
  return <span className={`pill ${cls}`}>{status}</span>;
}

export function Kpi({ label, value, tone, icon }) {
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

// Money helpers. The preview cost mirrors the backend formula (energy*price + startFee);
// the authoritative figure always comes back from the API.
export function previewCost(energyKwh, pricePerKwh, startFee) {
  const raw = Number(energyKwh) * Number(pricePerKwh) + Number(startFee);
  return Math.round(raw * 100) / 100;
}

export function money(amount, currency) {
  if (amount == null) return "—";
  return `${Number(amount).toFixed(2)} ${currency}`;
}

// Live-updating elapsed time since an ISO timestamp, formatted h:mm:ss / m:ss.
export function useElapsed(startIso) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);
  const secs = Math.max(0, Math.floor((now - new Date(startIso).getTime()) / 1000));
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  const pad = (n) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}
