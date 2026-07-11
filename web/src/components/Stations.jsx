import { useEffect, useState } from "react";
import { api } from "../api.js";

export default function Stations({ stationId }) {
  const [connectors, setConnectors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    api
      .listConnectors(stationId)
      .then((data) => alive && setConnectors(data))
      .catch((err) => alive && setError(err.message))
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [stationId]);

  if (loading) return <p className="muted">Loading connectors…</p>;
  if (error) return <div className="error-banner">{error}</div>;

  return (
    <section>
      <h2>Station #{stationId} — Connectors</h2>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Power</th>
            <th>Status</th>
            <th>Tariff</th>
          </tr>
        </thead>
        <tbody>
          {connectors.map((c) => (
            <tr key={c.connectorId}>
              <td>{c.connectorId}</td>
              <td>{c.type}</td>
              <td>{c.powerKw} kW</td>
              <td>
                <span className={`status ${c.status.toLowerCase()}`}>{c.status}</span>
              </td>
              <td>
                {c.tariff.pricePerKwh} {c.tariff.currency}/kWh
                {Number(c.tariff.startFee) > 0 && ` + ${c.tariff.startFee} start`}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
