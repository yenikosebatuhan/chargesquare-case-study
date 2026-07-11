import { useState } from "react";
import { auth } from "./api.js";
import Login from "./components/Login.jsx";
import Stations from "./components/Stations.jsx";
import Sessions from "./components/Sessions.jsx";

// Demo scope: one station and one driver, matching the seed data.
const STATION_ID = 1;
const USER_ID = 7;

export default function App() {
  const [user, setUser] = useState(auth.user());
  const [tab, setTab] = useState("stations");

  if (!user) {
    return <Login onLoggedIn={setUser} />;
  }

  const logout = () => {
    auth.clear();
    setUser(null);
  };

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">⚡ ChargeSquare <span>Ops Panel</span></div>
        <div className="session-info">
          <span className={`role-badge ${user.role.toLowerCase()}`}>{user.role}</span>
          <span className="username">{user.username}</span>
          <button className="link-btn" onClick={logout}>Log out</button>
        </div>
      </header>

      <nav className="tabs">
        <button className={tab === "stations" ? "active" : ""} onClick={() => setTab("stations")}>
          Stations &amp; Connectors
        </button>
        <button className={tab === "sessions" ? "active" : ""} onClick={() => setTab("sessions")}>
          Sessions
        </button>
      </nav>

      <main className="content">
        {tab === "stations" && <Stations stationId={STATION_ID} />}
        {tab === "sessions" && <Sessions userId={USER_ID} isAdmin={user.role === "ADMIN"} onAuthError={logout} />}
      </main>

      <footer className="footnote">
        Role enforcement lives on the server — the disabled buttons are only a hint. A VIEWER
        token calling a write endpoint directly still gets a 403.
      </footer>
    </div>
  );
}
