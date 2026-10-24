export function timeAgo(iso) {
  if (!iso) return 'never';
  const seconds = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  if (seconds < 5) return 'just now';
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  return `${Math.round(hours / 24)} d ago`;
}

export function formatTime(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', day: 'numeric', month: 'short' });
}

export function formatDuration(fromIso, toIso) {
  if (!fromIso) return '—';
  const ms = (toIso ? new Date(toIso) : new Date()) - new Date(fromIso);
  const minutes = Math.round(ms / 60000);
  if (minutes < 1) return 'under a minute';
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  return `${h} h ${minutes % 60} min`;
}

export const FAILURE_LABELS = {
  NONE: 'OK',
  SLOW: 'Slow',
  AUTH: 'Auth failed',
  RATE_LIMIT: 'Rate limited',
  SERVER_ERROR: 'Server error',
  TIMEOUT: 'Timeout',
  CONNECTION: 'Connection failed',
  BAD_FORMAT: 'Bad format',
  UNEXPECTED_STATUS: 'Unexpected status',
};

export const PROVIDER_STATUS_LABELS = {
  OPERATIONAL: 'Operational',
  DEGRADED: 'Degraded',
  OUTAGE: 'Outage',
  UNKNOWN: 'No data',
};
