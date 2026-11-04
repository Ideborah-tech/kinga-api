import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks/usePolling.js';
import { useAuth } from '../OwnerContext.jsx';
import { FAILURE_LABELS, formatDuration, timeAgo } from '../format.js';
import Sparkline from '../components/Sparkline.jsx';
import IncidentBanner from '../components/IncidentBanner.jsx';
import { ErrorBox, Loading, MonitorStatus, ProviderStatus, ProviderTag, StatusIcon } from '../components/Status.jsx';

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

function today() {
  return new Date().toLocaleDateString([], { weekday: 'long', day: 'numeric', month: 'long' });
}

function summarySentence(d) {
  if (d.monitors === 0) return 'Add your first monitor and KingaAPI starts checking it every minute.';
  if (d.incidentsAffectingYou.length) return 'A provider you depend on is down for everyone. Your code is probably fine.';
  if (d.down) return `${d.down} of your ${d.monitors} endpoints ${d.down === 1 ? 'is' : 'are'} failing. Open the report to see why.`;
  if (d.degraded) return `Everything answers, but ${d.degraded} ${d.degraded === 1 ? 'endpoint is' : 'endpoints are'} slow.`;
  return `All ${d.monitors} endpoints are working. KingaAPI checks them every minute.`;
}

function Kpi({ label, value, hint, tone }) {
  return (
    <div className={`kpi ${tone ? `kpi-${tone}` : ''}`}>
      <span className="kpi-label">{label}</span>
      <span className="kpi-value">{value}</span>
      <span className="kpi-hint">{hint}</span>
    </div>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const owner = user.handle;
  const { data, error, loading, reload } = usePolling(async () => {
    const [dashboard, monitors, providers, incidents] = await Promise.all([
      api.dashboard(owner),
      api.monitors(owner),
      api.providers(),
      api.incidents(),
    ]);
    return { dashboard, monitors, providers, incidents };
  }, [owner], 5000);

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorBox error={error} />;

  const { dashboard: d, monitors, providers, incidents } = data;
  const providerByCode = Object.fromEntries(providers.map((p) => [p.info.code, p]));
  const firstDownFor = (code) => monitors.find((s) => s.monitor.provider === code && s.monitor.lastStatus === 'DOWN');
  const firstName = user.name.split(' ')[0];

  return (
    <>
      <ErrorBox error={error} />

      <section className="page-head">
        <div>
          <p className="eyebrow">{today()}</p>
          <h1>{greeting()}, <em>{firstName}</em>.</h1>
          <p className="lede">{summarySentence(d)}</p>
        </div>
        <div className="row gap wrap">
          <button className="btn btn-ghost" onClick={reload}>Refresh</button>
          <Link className="btn btn-primary" to="/monitors/new">Add monitor</Link>
        </div>
      </section>

      {d.incidentsAffectingYou.map((incident) => {
        const down = firstDownFor(incident.provider);
        return (
          <IncidentBanner
            key={incident.id}
            incident={incident}
            providerName={providerByCode[incident.provider]?.info.name}
            reportLink={down ? `/monitors/${down.monitor.id}/report` : null}
          />
        );
      })}

      <section className="kpis">
        <Kpi label="Uptime" value={d.monitors ? `${d.avgUptime24h}%` : '—'} hint="last 24 hours" />
        <Kpi label="Working" value={d.up} hint={`of ${d.monitors} endpoints`} tone={d.up ? 'ok' : null} />
        <Kpi label="Failing" value={d.down} hint={d.down ? 'needs attention' : 'nothing failing'} tone={d.down ? 'bad' : null} />
        <Kpi label="Slow" value={d.degraded} hint="over 2 seconds" tone={d.degraded ? 'warn' : null} />
        <Kpi label="Checks" value={d.checks24h.toLocaleString()} hint={`${d.failures24h} failed`} />
      </section>

      <div className="dash-grid">
        <section className="card card-flush">
          <div className="card-head card-head-pad">
            <div>
              <h2>Your endpoints</h2>
              <p className="small muted">Checked every minute. Select one for details.</p>
            </div>
            <span className="count">{monitors.length}</span>
          </div>
          {monitors.length === 0 ? (
            <div className="empty">
              <h3>No monitors yet</h3>
              <p className="muted">Start from a shared setup for M-Pesa, MTN MoMo, Flutterwave or Paystack.</p>
              <Link className="btn btn-primary" to="/monitors/new">Add your first monitor</Link>
            </div>
          ) : (
            <div className="endpoint-list">
              {monitors.map(({ monitor: m, providerName, uptime24h, recentResponseTimes }) => {
                const down = m.lastStatus === 'DOWN';
                return (
                  <Link
                    key={m.id}
                    to={down ? `/monitors/${m.id}/report` : `/monitors/${m.id}`}
                    className={`endpoint endpoint-${m.lastStatus.toLowerCase()}`}
                  >
                    <StatusIcon status={m.lastStatus} />
                    <span className="endpoint-main">
                      <span className="endpoint-name">{m.name}</span>
                      <span className="endpoint-meta">
                        <ProviderTag code={m.provider} name={providerName} />
                        <span className="method">{m.method}</span>
                        <span>{timeAgo(m.lastCheckedAt)}</span>
                      </span>
                    </span>
                    <span className="endpoint-trend hide-sm"><Sparkline values={recentResponseTimes} width={96} height={28} /></span>
                    <span className="endpoint-figure">
                      <b>{m.lastResponseTimeMs ?? '—'}<small> ms</small></b>
                      <span>{uptime24h}%</span>
                    </span>
                    <span className="endpoint-status">
                      <MonitorStatus status={m.lastStatus} failureType={m.lastFailureType} />
                      {down && <span className="endpoint-cta">View report →</span>}
                    </span>
                  </Link>
                );
              })}
            </div>
          )}
        </section>

        <aside className="dash-aside">
          <section className="card">
            <div className="card-head">
              <div>
                <h2>Provider network</h2>
                <p className="small muted">From every developer on KingaAPI.</p>
              </div>
              <Link to="/providers" className="small">All</Link>
            </div>
            <div className="provider-list">
              {providers.map(({ info, health }) => (
                <Link to="/providers" key={info.code} className={`provider-row provider-row-${health.status.toLowerCase()}`}>
                  <ProviderTag code={info.code} name={info.name} />
                  <ProviderStatus status={health.status} />
                </Link>
              ))}
            </div>
          </section>

          <section className="card">
            <div className="card-head">
              <div>
                <h2>Incidents</h2>
                <p className="small muted">Failures seen by many developers.</p>
              </div>
            </div>
            {incidents.length === 0 ? (
              <div className="muted small">No provider-wide incidents.</div>
            ) : (
              <ul className="incident-list">
                {incidents.slice(0, 4).map((i) => (
                  <li key={i.id} className={i.active ? 'incident-active' : ''}>
                    <span className="incident-name">
                      {providerByCode[i.provider]?.info.name || i.provider}
                      <span className={`tag ${i.active ? 'tag-bad' : ''}`}>{i.active ? 'Ongoing' : 'Resolved'}</span>
                    </span>
                    <span className="small muted">
                      {FAILURE_LABELS[i.dominantFailure]} · {i.affectedDevelopers} developers ·{' '}
                      {i.active ? `for ${formatDuration(i.startedAt)}` : `${timeAgo(i.startedAt)}, lasted ${formatDuration(i.startedAt, i.resolvedAt)}`}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </aside>
      </div>
    </>
  );
}
