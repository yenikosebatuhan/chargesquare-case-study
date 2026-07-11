import { useState } from "react";
import { auth } from "./api.js";
import Login from "./components/Login.jsx";
import Stations from "./components/Stations.jsx";
import Sessions from "./components/Sessions.jsx";
import { Bolt, Layers, Activity, Logout } from "./icons.jsx";

// Demo scope: one station and one driver, matching the seed data.
const STATION_ID = 1;
const USER_ID = 7;

const PAGES = {
  stations: {
    title: "Stations & Connectors",
    subtitle: "Live view of the charging infrastructure and its tariffs.",
  },
  sessions: {
    title: "Sessions & Wallet",
    subtitle: "Charging sessions, receipts, and driver balance.",
  },
};

export default function App() {
  const [user, setUser] = useState(auth.user());
  const [page, setPage] = useState("stations");

  if (!user) return <Login onLoggedIn={setUser} />;

  const logout = () => { auth.clear(); setUser(null); };
  const initials = user.username.slice(0, 2).toUpperCase();

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="logo"><Bolt style={{ width: 20, height: 20, color: "#052e16" }} /></div>
          <div>
            <div className="brand-name">ChargeSquare</div>
            <div className="brand-sub">Ops Panel</div>
          </div>
        </div>

        <nav className="nav">
          <div className="nav-label">Manage</div>
          <button className={`nav-item ${page === "stations" ? "active" : ""}`} onClick={() => setPage("stations")}>
            <Layers /> Stations
          </button>
          <button className={`nav-item ${page === "sessions" ? "active" : ""}`} onClick={() => setPage("sessions")}>
            <Activity /> Sessions
          </button>
        </nav>

        <div className="spacer" />

        <div className="user-card">
          <div className="avatar">{initials}</div>
          <div className="user-meta">
            <div className="user-name">{user.username}</div>
            <div className="user-role">
              <span className={`role-badge ${user.role.toLowerCase()}`}>{user.role}</span>
            </div>
          </div>
          <button className="icon-btn" onClick={logout} title="Log out"><Logout /></button>
        </div>
      </aside>

      <div className="main">
        <header className="page-header">
          <div>
            <h1>{PAGES[page].title}</h1>
            <p>{PAGES[page].subtitle}</p>
          </div>
        </header>

        <main className="content">
          {page === "stations" && <Stations stationId={STATION_ID} />}
          {page === "sessions" && (
            <Sessions userId={USER_ID} isAdmin={user.role === "ADMIN"} onAuthError={logout} />
          )}
          <p className="footnote">
            Roles are enforced on the server. The disabled buttons are only a hint — a VIEWER token
            calling a write endpoint directly still receives a 403.
          </p>
        </main>
      </div>
    </div>
  );
}
