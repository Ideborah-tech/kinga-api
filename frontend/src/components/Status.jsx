import { FAILURE_LABELS, PROVIDER_STATUS_LABELS } from '../format.js';

const MONITOR_LABELS = { UP: 'Up', DOWN: 'Down', DEGRADED: 'Slow', UNKNOWN: 'Pending' };

export function MonitorStatus({ status, failureType }) {
  const label = status === 'DOWN' && failureType ? FAILURE_LABELS[failureType] : MONITOR_LABELS[status] || status;
  return (
    <span className={`pill pill-${(status || 'UNKNOWN').toLowerCase()}`}>
      <span className="dot" />
      {label}
    </span>
  );
}

const PROVIDER_CLASS = { OPERATIONAL: 'up', DEGRADED: 'degraded', OUTAGE: 'down', UNKNOWN: 'unknown' };

export function ProviderStatus({ status }) {
  return (
    <span className={`pill pill-${PROVIDER_CLASS[status] || 'unknown'}`}>
      <span className="dot" />
      {PROVIDER_STATUS_LABELS[status] || status}
    </span>
  );
}

export function ProviderTag({ code, name, color }) {
  return (
    <span className="provider-tag" title={code}>
      <span className="provider-swatch" style={{ background: color || 'var(--muted)' }} />
      {name || code}
    </span>
  );
}

export function Loading({ text = 'Loading…' }) {
  return <div className="card empty muted">{text}</div>;
}

export function ErrorBox({ error }) {
  if (!error) return null;
  return (
    <div className="alert alert-error">
      <b>Something went wrong.</b> {error}
      <div className="small">Is the backend running on the API URL? See the README.</div>
    </div>
  );
}
