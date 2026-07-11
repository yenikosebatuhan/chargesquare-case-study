import { useState } from "react";
import { api, auth } from "../api.js";
import { Bolt, Warning } from "../icons.jsx";

export default function Login({ onLoggedIn }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const session = await api.login(username, password);
      auth.save(session);
      onLoggedIn(auth.user());
    } catch (err) {
      setError(err.status === 401 ? "Invalid username or password." : err.message);
    } finally {
      setLoading(false);
    }
  };

  const fill = (u, p) => { setUsername(u); setPassword(p); setError(null); };

  return (
    <div className="login-screen">
      <form className="login-card" onSubmit={submit}>
        <div className="login-brand">
          <div className="logo"><Bolt style={{ width: 22, height: 22, color: "#052e16" }} /></div>
          <div>
            <h1>ChargeSquare</h1>
            <p>Operations Panel</p>
          </div>
        </div>

        <div>
          <h2>Sign in</h2>
          <p className="lede">Use a demo account to explore the panel.</p>
        </div>

        <div className="field">
          <label htmlFor="u">Username</label>
          <input id="u" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus autoComplete="username" />
        </div>
        <div className="field">
          <label htmlFor="p">Password</label>
          <input id="p" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
        </div>

        {error && <div className="error-banner"><Warning style={{ width: 16, height: 16 }} /> {error}</div>}

        <button type="submit" className="btn btn-primary btn-block" disabled={loading || !username || !password}>
          {loading ? "Signing in…" : "Sign in"}
        </button>

        <div className="demo-chips">
          <button type="button" className="chip" onClick={() => fill("admin", "admin123")}>
            <b>admin</b> full access
          </button>
          <button type="button" className="chip" onClick={() => fill("viewer", "viewer123")}>
            <b>viewer</b> read-only
          </button>
        </div>
      </form>
    </div>
  );
}
