import { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { api } from '../api.js';
import { useOwner } from '../OwnerContext.jsx';
import { usePolling } from '../hooks/usePolling.js';

const ICONS = {
  dashboard: 'M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z',
  providers: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM2 12h20M12 2c3 3 4 6.5 4 10s-1 7-4 10c-3-3-4-6.5-4-10s1-7 4-10z',
  templates: 'M4 4h16v4H4zM4 10h10v10H4zM16 10h4v10h-4z',
  demo: 'M13 2 4 14h7l-1 8 9-12h-7z',
};

const NAV = [
  { to: '/', label: 'Dashboard', icon: 'dashboard', end: true },
  { to: '/providers', label: 'Providers', icon: 'providers' },
  { to: '/templates', label: 'Setups', icon: 'templates' },
  { to: '/demo', label: 'Demo', icon: 'demo' },
];

function Icon({ name }) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" aria-hidden="true">
      <path d={ICONS[name]} />
    </svg>
  );
}

function NavLinks({ className }) {
  return (
    <nav className={className}>
      {NAV.map((item) => (
        <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => (isActive ? 'active' : '')}>
          <Icon name={item.icon} />
          <span>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}

/** Running strip of every provider's live status under the header. */
function SignalBar() {
  const { data } = usePolling(() => api.providers(), [], 8000);
  if (!data) return <div className="signal-bar" />;
  return (
    <div className="signal-bar" aria-label="Provider status">
      <span className="signal-live">Live</span>
      <div className="signal-items">
        {data.map(({ info, health }) => (
          <NavLink to="/providers" key={info.code} className={`signal-item signal-${health.status.toLowerCase()}`}>
            <span className="signal-dot" />
            {info.name}
            <span className="signal-ms">{health.status === 'OUTAGE' ? 'OUTAGE' : `${health.avgResponseMs}ms`}</span>
          </NavLink>
        ))}
      </div>
    </div>
  );
}

function initials(name) {
  return name.split(/[-_\s]/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
}

export default function Layout({ children }) {
  const { owner, setOwner } = useOwner();
  const [owners, setOwners] = useState(['you']);

  useEffect(() => {
    api.monitors()
      .then((list) => {
        const names = [...new Set(list.map((s) => s.monitor.owner))].sort((a, b) =>
          a === 'you' ? -1 : b === 'you' ? 1 : a.localeCompare(b));
        if (names.length) setOwners(names);
      })
      .catch(() => {});
  }, []);

  return (
    <div className="shell">
      <header className="topbar">
        <NavLink to="/" className="brand">
          <img src="/shield.svg" alt="" width="32" height="32" />
          <span className="brand-name">Kinga<b>API</b></span>
        </NavLink>

        <NavLinks className="nav" />

        <label className="owner-select" title="Developer">
          <span className="avatar">{initials(owner)}</span>
          <select value={owner} onChange={(e) => setOwner(e.target.value)} aria-label="Developer">
            {(owners.includes(owner) ? owners : [owner, ...owners]).map((o) => (
              <option key={o} value={o}>{o}</option>
            ))}
          </select>
        </label>
      </header>

      <SignalBar />
      <main className="container">{children}</main>
      <footer className="footer">
        KingaAPI · <i>kinga</i> means “protection” in Swahili · built by Deborah, Sifa &amp; Gisele
      </footer>

      {/* Phone navigation: an app-style dock at the bottom of the screen */}
      <NavLinks className="dock" />
    </div>
  );
}
