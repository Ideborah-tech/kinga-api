import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks/usePolling.js';
import { FAILURE_LABELS, formatTime, timeAgo } from '../format.js';
import ResponseChart from '../components/ResponseChart.jsx';
import StatCard from '../components/StatCard.jsx';
import { ErrorBox, Loading, MonitorStatus, ProviderStatus } from '../components/Status.jsx';

/** Hides header values that look like secrets. */
function maskHeaders(headers) {
  if (!headers) return '—';
  return headers
    .split('\n')
    .filter((line) => !line.toLowerCase().startsWith('x-kinga-original-auth'))
    .map((line) => {
      const i = line.indexOf(':');
      if (i < 0) return line;
      const name = line.slice(0, i);
      const value = line.slice(i + 1).trim();
      const secret = /auth|key|secret|token/i.test(name);
      return `${name}: ${secret ? value.slice(0, 10) + '••••' : value}`;
    })
    .join('\n');
}

export default function MonitorDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);

  const { data, error, loading, reload } = usePolling(async () => {
    const [summary, checks] = await Promise.all([api.monitor(id), api.checks(id, 60)]);
    return { summary, checks };
  }, [id], 5000);

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorBox error={error} />;

  const { summary, checks } = data;
  const m = summary.monitor;

  const run = async (action, done) => {
    setBusy(true);
    setMessage(null);
    try {
      const result = await action();
      if (done) setMessage(done(result));
      await reload();
    } catch (e) {
      setMessage({ type: 'error', text: e.message });
    } finally {
      setBusy(false);
    }
  };

  const checkNow = () => run(() => api.checkNow(id), (r) => ({
    type: r.success ? 'success' : 'error',
    text: r.success ? `Check passed in ${r.responseTimeMs} ms.` : `Check failed: ${r.errorMessage}`,
  }));

  const share = () => run(() => api.shareMonitor(id), () => ({
    type: 'success',
    text: 'Shared as a community template. Secrets in headers were replaced with placeholders.',
  }));

  const remove = async () => {
    if (!window.confirm(`Delete monitor "${m.name}" and its history?`)) return;
    try {
      await api.deleteMonitor(id);
      navigate('/');
    } catch (e) {
      setMessage({ type: 'error', text: e.message });
    }
  };

  return (
    <>
      <div className="breadcrumb"><Link to="/">Dashboard</Link> / {m.name}</div>
      <ErrorBox error={error} />

      <section className="hero">
        <div>
          <div className="row gap">
            <h1>{m.name}</h1>
            <MonitorStatus status={m.lastStatus} failureType={m.lastFailureType} />
          </div>
          <div className="mono small muted">{m.method} {m.url}</div>
          <div className="small muted">
            {summary.providerName} · community status <ProviderStatus status={summary.providerStatus} /> · checked every {m.intervalSeconds}s · owner {m.owner}
          </div>
        </div>
        <div className="row gap wrap">
          <button className="btn btn-primary" onClick={checkNow} disabled={busy}>{busy ? 'Checking…' : 'Check now'}</button>
          <Link className={`btn ${m.lastStatus === 'DOWN' ? 'btn-danger' : 'btn-ghost'}`} to={`/monitors/${id}/report`}>Failure report</Link>
          <Link className="btn btn-ghost" to={`/monitors/${id}/edit`}>Edit</Link>
          <button className="btn btn-ghost" onClick={share} disabled={busy}>Share as template</button>
          <button className="btn btn-ghost text-down" onClick={remove} disabled={busy}>Delete</button>
        </div>
      </section>

      {message && <div className={`alert alert-${message.type}`}>{message.text}</div>}

      <section className="stats">
        <StatCard label="Uptime (24 h)" value={`${summary.uptime24h}%`} tone={summary.uptime24h < 99 ? 'degraded' : 'up'} />
        <StatCard label="Avg response (24 h)" value={`${summary.avgResponseMs24h} ms`} />
        <StatCard label="Checks (24 h)" value={summary.checks24h} hint={`${summary.failures24h} failed`} />
        <StatCard label="Last check" value={timeAgo(m.lastCheckedAt)} hint={m.lastStatusCode ? `HTTP ${m.lastStatusCode}` : null} />
      </section>

      <section className="card">
        <div className="card-head">
          <h2>Response time</h2>
          <span className="small muted">Last {checks.length} checks · red dots are failures</span>
        </div>
        <ResponseChart checks={checks} />
      </section>

      <div className="grid-2">
        <section className="card">
          <h2>Recent checks</h2>
          <div className="table-wrap">
            <table className="table compact">
              <thead>
                <tr><th>Time</th><th>Result</th><th className="num">HTTP</th><th className="num">Time</th></tr>
              </thead>
              <tbody>
                {checks.slice(0, 15).map((c) => (
                  <tr key={c.id} title={c.errorMessage || ''}>
                    <td className="small nowrap">{formatTime(c.checkedAt)}</td>
                    <td className={c.success ? (c.failureType === 'SLOW' ? 'text-degraded' : 'text-up') : 'text-down'}>
                      {FAILURE_LABELS[c.failureType]}
                    </td>
                    <td className="num">{c.statusCode ?? '—'}</td>
                    <td className="num">{c.responseTimeMs} ms</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="card">
          <h2>What KingaAPI expects</h2>
          <dl className="details">
            <dt>Status code</dt><dd>{m.expectedStatus}</dd>
            <dt>Required fields</dt>
            <dd>{m.expectedFields ? m.expectedFields.split(',').map((f) => <code key={f} className="chip">{f.trim()}</code>) : 'none'}</dd>
            <dt>Timeout</dt><dd>{m.timeoutMs} ms</dd>
            <dt>Slow above</dt><dd>{m.slowThresholdMs} ms</dd>
            <dt>Headers</dt><dd><pre className="code">{maskHeaders(m.headers)}</pre></dd>
            {m.body && (<><dt>Body</dt><dd><pre className="code">{m.body}</pre></dd></>)}
          </dl>
        </section>
      </div>
    </>
  );
}
