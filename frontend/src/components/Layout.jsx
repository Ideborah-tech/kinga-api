import { useEffect, useRef, useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../OwnerContext.jsx';
import { usePolling } from '../hooks/usePolling.js';

const ICONS = {
  dashboard: 'M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z',
  providers: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM2 12h20M12 2c3 3 4 6.5 4 10s-1 7-4 10c-3-3-4-6.5-4-10s1-7 4-10z',
  templates: 'M4 4h16v4H4zM4 10h10v10H4zM16 10h4v10h-4z',
  demo: 'M13 2 4 14h7l-1 8 9-12h-7z',
};

const NAV = [
  { to: '/', label: 'Overview', icon: 'dashboard', end: true },
  { to: '/providers', label: 'Providers', icon: 'providers' },
  { to: '/templates', label: 'Shared setups', icon: 'templates' },
  { to: '/demo', label: 'Demo', icon: 'demo' },
];

function Icon({ name }) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" aria-hidden="true">
      <path d={ICONS[name]} />
    </svg>
  );
}

function initials(name = '') {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('') || '?';
}

/** Small chip in the header: are all providers working? */
function NetworkChip({ providers }) {
  if (!providers) return null;
  const down = providers.filter((p) => p.health.status === 'OUTAGE');
  const issues = providers.filter((p) => p.health.status === 'DEGRADED');
  const tone = down.length ? 'bad' : issues.length ? 'warn' : 'ok';
  const text = down.length ? `${down.length} provider${down.length > 1 ? 's' : ''} down` : issues.length ? 'Some issues' : 'All providers working';
  return (
    <Link to="/providers" className={`net-chip net-chip-${tone}`}>
      <span className="net-dot" />
      {text}
    </Link>
  );
}

function UserMenu() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  return (
    <div className="user-menu" ref={ref}>
      <button className="avatar-btn" onClick={() => setOpen((o) => !o)} aria-expanded={open} aria-label="Account menu">
        <span className="avatar">{initials(user.name)}</span>
        <span className="avatar-name hide-sm">{user.name.split(' ')[0]}</span>
      </button>
      {open && (
        <div className="menu" role="menu">
          <div className="menu-head">
            <span className="avatar avatar-lg">{initials(user.name)}</span>
            <span>
              <b>{user.name}</b>
              <span className="small muted">{user.email}</span>
            </span>
          </div>
          <div className="menu-meta small muted">Handle: <code>{user.handle}</code></div>
          <button className="menu-item" onClick={logout} role="menuitem">Sign out</button>
        </div>
      )}
    </div>
  );
}

/** Full-width message under the header, only while a provider is down. */
function OutageNotice({ providers }) {
  const down = (providers || []).filter((p) => p.health.status === 'OUTAGE');
  if (!down.length) return null;
  return (
    <div className="outage-notice">
      <div className="outage-inner">
        <span className="net-dot" />
        <span><b>{down.map((p) => p.info.name).join(', ')}</b> {down.length === 1 ? 'is' : 'are'} down for every developer. If your payments fail right now, it’s not your code.</span>
        <Link to="/providers">Details</Link>
      </div>
    </div>
  );
}

export default function Layout({ children }) {
  const { data: providers } = usePolling(() => api.providers(), [], 6000);

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            <img src="/shield.svg" alt="" width="30" height="30" />
            <span className="wordmark">Kinga<b>API</b></span>
          </NavLink>
          <nav className="tabs">
            {NAV.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => (isActive ? 'active' : '')}>
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="topbar-right">
            <NetworkChip providers={providers} />
            <UserMenu />
          </div>
        </div>
      </header>
      <OutageNotice providers={providers} />

      <main className="container">{children}</main>

      <footer className="footer">
        <svg className="footer-wave" viewBox="0 0 1440 56" preserveAspectRatio="none" aria-hidden="true">
          <path className="fill" d="M0 56 L0 30 C240 -6 480 -6 720 24 S1200 54 1440 18 L1440 56 Z" />
          <path className="line" d="M0 22 C240 -14 480 -14 720 16 S1200 46 1440 10" vectorEffect="non-scaling-stroke" />
        </svg>
        <div className="footer-inner">
          <div className="footer-top">
            <Link to="/" className="footer-brand">
              <img src="/shield.svg" alt="" width="34" height="34" />
              <span className="wordmark">Kinga<b>API</b></span>
            </Link>
            <p className="footer-tagline">It’s them, <span>not you.</span></p>
            <nav className="footer-links">
              {NAV.map((item) => <Link key={item.to} to={item.to}>{item.label}</Link>)}
            </nav>
          </div>
          <div className="footer-bottom">
            <span>© {new Date().getFullYear()} KingaAPI</span>
            <span><i>Kinga</i> means “protection” in Swahili</span>
            <span>Built by Deborah, Sifa &amp; Gisele</span>
          </div>
        </div>
      </footer>

      <nav className="tabbar">
        {NAV.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => (isActive ? 'active' : '')}>
            <Icon name={item.icon} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
