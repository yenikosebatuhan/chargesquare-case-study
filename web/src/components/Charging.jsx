import { useCallback, useEffect, useState } from "react";
import { api } from "../api.js";
import { Kpi, money, useElapsed } from "../ui.jsx";
import { Plug, Zap, Wallet, Activity, Clock, Car, Warning, Stop } from "../icons.jsx";
import StartModal from "./StartModal.jsx";
import StopModal from "./StopModal.jsx";
import ReceiptModal from "./ReceiptModal.jsx";

export default function Charging({ stationId, driverId, isAdmin, onAuthError }) {
  const [connectors, setConnectors] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [startTarget, setStartTarget] = useState(null);
  const [stopTarget, setStopTarget] = useState(null);
  const [receipt, setReceipt] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [c, s, w] = await Promise.all([
        api.listConnectors(stationId),
        api.listUserSessions(driverId),
        api.getWallet(driverId),
      ]);
      setConnectors(c);
      setSessions(s);
      setWallet(w);
    } catch (err) {
      if (err.status === 401) return onAuthError();
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [stationId, driverId, onAuthError]);

  useEffect(() => { load(); }, [load]);

  const reserve = async (connector) => {
    setError(null);
    try {
      await api.reserve(driverId, connector.connectorId);
      await load();
    } catch (err) {
      if (err.status === 401) return onAuthError();
      setError(err.code === "FORBIDDEN" ? "Your role cannot reserve." : err.message);
    }
  };

  const active = sessions.filter((s) => s.status === "ACTIVE");
  const available = connectors.filter((c) => c.status === "AVAILABLE");
  const occupied = connectors.filter((c) => c.status === "OCCUPIED");

  if (loading) return <div className="state"><div className="spinner" />Loading charging network…</div>;

  return (
    <>
      <div className="kpis">
        <Kpi label="Charging now" value={active.length} tone="live" icon={<Zap style={{ width: 18, height: 18 }} />} />
        <Kpi label="Available" value={available.length} tone="green" icon={<Plug />} />
        <Kpi label="Wallet" tone="blue" icon={<Wallet />}
             value={wallet ? <>{Number(wallet.balance).toFixed(2)}<small>{wallet.currency}</small></> : "—"} />
      </div>

      {error && <div className="error-banner"><Warning style={{ width: 16, height: 16 }} /> {error}</div>}

      {/* Charging now */}
      <section>
        <div className="section-head">
          <h2><Activity style={{ width: 17, height: 17 }} /> Charging now</h2>
          <span className="section-count">{active.length} active</span>
        </div>
        {active.length === 0 ? (
          <div className="empty-panel">
            <Zap style={{ width: 26, height: 26, opacity: 0.5 }} />
            <p>No active sessions. Start charging on an available connector below.</p>
          </div>
        ) : (
          <div className="grid-cards">
            {active.map((s) => (
              <ActiveCard key={s.sessionId} session={s} isAdmin={isAdmin} onStop={() => setStopTarget(s)} />
            ))}
          </div>
        )}
      </section>

      {/* Connectors */}
      <section>
        <div className="section-head">
          <h2><Plug style={{ width: 17, height: 17 }} /> Connectors · Station #{stationId}</h2>
          <span className="section-count">{available.length} available · {occupied.length} in use</span>
        </div>
        <div className="grid-cards">
          {connectors.map((c) => (
            <ConnectorCard key={c.connectorId} connector={c} isAdmin={isAdmin}
                           onStart={() => setStartTarget(c)} onReserve={() => reserve(c)} />
          ))}
        </div>
      </section>

      {startTarget && (
        <StartModal connector={startTarget} driverId={driverId}
          onClose={() => setStartTarget(null)}
          onStarted={() => { setStartTarget(null); load(); }} />
      )}
      {stopTarget && (
        <StopModal session={stopTarget} walletBalance={wallet ? Number(wallet.balance) : null}
          onClose={() => setStopTarget(null)}
          onStopped={(r) => { setStopTarget(null); setReceipt(r); load(); }} />
      )}
      {receipt && <ReceiptModal session={receipt} onClose={() => setReceipt(null)} />}
    </>
  );
}

function ActiveCard({ session, isAdmin, onStop }) {
  const elapsed = useElapsed(session.startedAt);
  const t = session.tariffSnapshot;
  return (
    <div className="charge-card live">
      <div className="charge-top">
        <div className="charging-badge"><span className="dot" /> Charging</div>
        <span className="charge-conn">Connector #{session.connectorId}</span>
      </div>
      <div className="charge-visual">
        <div className="ev"><Car style={{ width: 26, height: 26 }} /></div>
        <div className="flow"><span /><span /><span /></div>
        <div className="bolt"><Zap style={{ width: 22, height: 22 }} /></div>
      </div>
      <div className="charge-meta">
        <div className="meta-item"><Clock style={{ width: 14, height: 14 }} /><span className="mono">{elapsed}</span><small>elapsed</small></div>
        <div className="meta-item"><span className="mono">{t.pricePerKwh} {t.currency}</span><small>/kWh</small></div>
        <div className="meta-item"><span className="mono">#{session.userId}</span><small>driver</small></div>
      </div>
      <button className="btn btn-danger btn-block" disabled={!isAdmin} onClick={onStop}
              title={isAdmin ? "Stop & bill" : "ADMIN only"}>
        <Stop style={{ width: 15, height: 15 }} /> Stop &amp; bill
      </button>
    </div>
  );
}

function ConnectorCard({ connector, isAdmin, onStart, onReserve }) {
  const c = connector;
  const pill = { AVAILABLE: "green", RESERVED: "reserved", OCCUPIED: "amber" }[c.status] || "amber";
  const isOccupied = c.status === "OCCUPIED";
  const canStart = c.status === "AVAILABLE" || c.status === "RESERVED"; // reserved is held by our demo driver
  return (
    <div className={`connector-card ${isOccupied ? "busy" : ""}`}>
      <div className="connector-head">
        <span className="plug-lg"><Plug style={{ width: 20, height: 20 }} /></span>
        <div>
          <div className="connector-name">{c.type}</div>
          <div className="connector-id">Connector #{c.connectorId} · {c.powerKw} kW</div>
        </div>
        <span className={`pill ${pill}`}>{c.status}</span>
      </div>
      <div className="connector-tariff">
        <span>{money(c.tariff.pricePerKwh, c.tariff.currency)}<small>/kWh</small></span>
        {c.tariff.peakNow === true && <span className="peak-badge">PEAK</span>}
        {Number(c.tariff.startFee) > 0 && <span className="fee">+{c.tariff.startFee} start</span>}
      </div>
      <div className="connector-actions">
        <button className="btn btn-primary btn-block" disabled={!canStart || !isAdmin} onClick={onStart}
                title={isOccupied ? "In use" : isAdmin ? "Start charging" : "ADMIN only"}>
          <Zap style={{ width: 15, height: 15 }} /> {isOccupied ? "In use" : "Start charging"}
        </button>
        {c.status === "AVAILABLE" && (
          <button className="btn btn-ghost btn-sm" disabled={!isAdmin} onClick={onReserve}
                  title={isAdmin ? "Reserve for 5 min" : "ADMIN only"}>
            <Clock style={{ width: 14, height: 14 }} /> Reserve
          </button>
        )}
      </div>
    </div>
  );
}
