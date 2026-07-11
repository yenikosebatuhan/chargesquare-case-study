import { useState } from "react";
import { api, auth } from "../api.js";

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

  return (
    <div className="login-screen">
      <form className="login-card" onSubmit={submit}>
        <div className="brand big">⚡ ChargeSquare</div>
        <p className="subtitle">Operations panel</p>

        <label>
          Username
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>

        {error && <div className="error-banner">{error}</div>}

        <button type="submit" disabled={loading || !username || !password}>
          {loading ? "Signing in…" : "Sign in"}
        </button>

        <p className="hint">Demo users: <code>admin / admin123</code> or <code>viewer / viewer123</code></p>
      </form>
    </div>
  );
}
