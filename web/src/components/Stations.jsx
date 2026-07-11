import { useEffect, useState } from "react";
import { api } from "../api.js";
import { Plug, Layers, Warning } from "../icons.jsx";

export default function Stations({ stationId }) {
  const [connectors, setConnectors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    api.listConnectors(stationId)
      .then((data) => alive && setConnectors(data))
      .catch((err) => alive && setError(err.message))
      .finally(() => alive && setLoading(false));
    return () => { alive = false; };
  }, [stationId]);

  const available = connectors.filter((c) => c.status === "AVAILABLE").length;
  const occupied = connectors.filter((c) => c.status === "OCCUPIED").length;

  return (
    <>
      <div className="kpis">
        <Kpi label="Connectors" value={connectors.length} tone="blue" icon={<Layers />} />
        <Kpi label="Available" value={available} tone="green" icon={<Plug />} />
        <Kpi label="Occupied" value={occupied} tone="amber" icon={<Plug />} />
      </div>

      <div className="card">
        <div className="card-head"><h3>Station #{stationId} — Connectors</h3></div>
        <div className="card-body">
          {error && <div className="error-banner" style={{ margin: "0.6rem 0.8rem" }}><Warning style={{ width: 16, height: 16 }} /> {error}</div>}
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>ID</th><th>Type</th><th>Power</th><th>Status</th><th>Tariff</th></tr>
              </thead>
              <tbody>
                {loading && [0, 1].map((i) => (
                  <tr key={i} className="skeleton-row"><td><span /></td><td><span /></td><td><span /></td><td><span /></td><td><span /></td></tr>
                ))}
                {!loading && connectors.map((c) => (
                  <tr key={c.connectorId}>
                    <td className="mono cell-strong">#{c.connectorId}</td>
                    <td>
                      <div className="conn-type">
                        <span className="plug"><Plug /></span>
                        <span className="cell-strong">{c.type}</span>
                      </div>
                    </td>
                    <td className="mono">{c.powerKw} kW</td>
                    <td><StatusPill status={c.status} /></td>
                    <td className="mono">
                      <span className="cell-strong">{c.tariff.pricePerKwh} {c.tariff.currency}</span>
                      <span className="cell-sub">/kWh{Number(c.tariff.startFee) > 0 ? ` · +${c.tariff.startFee} start` : ""}</span>
                    </td>
                  </tr>
                ))}
                {!loading && connectors.length === 0 && (
                  <tr><td colSpan="5" className="state">No connectors found.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
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

export function StatusPill({ status }) {
  const map = { AVAILABLE: "green", COMPLETED: "green", OCCUPIED: "amber", ACTIVE: "amber" };
  return <span className={`pill ${map[status] || "green"}`}>{status}</span>;
}
