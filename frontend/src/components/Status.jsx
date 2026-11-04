import { FAILURE_LABELS, PROVIDER_STATUS_LABELS } from '../format.js';

const MONITOR_LABELS = { UP: 'Working', DOWN: 'Failing', DEGRADED: 'Slow', UNKNOWN: 'Checking' };
const MONITOR_CLASS = { UP: 'ok', DOWN: 'bad', DEGRADED: 'warn', UNKNOWN: 'neutral' };

export function MonitorStatus({ status, failureType }) {
  const label = status === 'DOWN' && failureType ? FAILURE_LABELS[failureType] : MONITOR_LABELS[status] || status;
  return (
    <span className={`pill pill-${MONITOR_CLASS[status] || 'neutral'}`}>
      <span className="dot" />
      {label}
    </span>
  );
}

const ICON_GLYPH = { UP: '✓', DOWN: '!', DEGRADED: '~', UNKNOWN: '…' };

/** Round status symbol at the start of a row. */
export function StatusIcon({ status }) {
  return (
    <span className={`status-icon status-icon-${MONITOR_CLASS[status] || 'neutral'}`} aria-label={MONITOR_LABELS[status]}>
      {ICON_GLYPH[status] || '…'}
    </span>
  );
}

const PROVIDER_CLASS = { OPERATIONAL: 'ok', DEGRADED: 'warn', OUTAGE: 'bad', UNKNOWN: 'neutral' };

export function ProviderStatus({ status }) {
  return (
    <span className={`pill pill-${PROVIDER_CLASS[status] || 'neutral'}`}>
      <span className="dot" />
      {PROVIDER_STATUS_LABELS[status] || status}
    </span>
  );
}

/** Two-letter badge, e.g. "M-Pesa (Daraja)" -> "MP", "Flutterwave" -> "FL". */
function initials(name) {
  const words = name.replace(/\(.*\)/, '').split(/[^A-Za-z]+/).filter(Boolean);
  if (words.length >= 2) return (words[0][0] + words[1][0]).toUpperCase();
  return (words[0] || '?').slice(0, 2).toUpperCase();
}

export function ProviderTag({ code, name }) {
  return (
    <span className="provider-tag" title={code}>
      <span className="provider-badge">{initials(name || code)}</span>
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
