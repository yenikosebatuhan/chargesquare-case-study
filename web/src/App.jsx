import { useState } from "react";
import { auth } from "./api.js";
import Logo from "./Logo.jsx";
import Login from "./components/Login.jsx";
import Charging from "./components/Charging.jsx";
import Sessions from "./components/Sessions.jsx";
import { Zap, Receipt, Logout } from "./icons.jsx";

// Demo scope: one station and one driver, matching the seed data.
const STATION_ID = 1;
const USER_ID = 7;

const PAGES = {
  charging: { title: "Charging", subtitle: "Start sessions, watch live charging, stop and bill." },
  sessions: { title: "Sessions & Wallet", subtitle: "Session history, receipts, and driver balance." },
};

export default function App() {
  const [user, setUser] = useState(auth.user());
  const [page, setPage] = useState("charging");

  if (!user) return <Login onLoggedIn={setUser} />;

  const logout = () => { auth.clear(); setUser(null); };
  const initials = user.username.slice(0, 2).toUpperCase();

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="logo-badge"><Logo size={22} variant="mono" /></div>
          <div>
            <div className="brand-name">ChargeSquare</div>
            <div className="brand-sub">Ops Panel</div>
          </div>
        </div>

        <nav className="nav">
          <div className="nav-label">Operations</div>
          <button className={`nav-item ${page === "charging" ? "active" : ""}`} onClick={() => setPage("charging")}>
            <Zap style={{ width: 18, height: 18 }} /> Charging
          </button>
          <button className={`nav-item ${page === "sessions" ? "active" : ""}`} onClick={() => setPage("sessions")}>
            <Receipt style={{ width: 18, height: 18 }} /> Sessions
          </button>
        </nav>

        <div className="spacer" />

        <div className="user-card">
          <div className="avatar">{initials}</div>
          <div className="user-meta">
            <div className="user-name">{user.username}</div>
            <div className="user-role"><span className={`role-badge ${user.role.toLowerCase()}`}>{user.role}</span></div>
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
          <div className="header-station"><Logo size={16} /> <span>Station #{STATION_ID} · Kadıköy</span></div>
        </header>

        <main className="content">
          {page === "charging" && (
            <Charging stationId={STATION_ID} driverId={USER_ID} isAdmin={user.role === "ADMIN"} onAuthError={logout} />
          )}
          {page === "sessions" && (
            <Sessions userId={USER_ID} isAdmin={user.role === "ADMIN"} onAuthError={logout} />
          )}
          <p className="footnote">
            Roles are enforced on the server — disabled buttons are only a hint. A VIEWER token
            calling a write endpoint (start / stop / top-up) directly still receives a 403.
          </p>
        </main>
      </div>
    </div>
  );
}
