import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks/usePolling.js';
import { useOwner } from '../OwnerContext.jsx';
import { FAILURE_LABELS, formatDuration, timeAgo } from '../format.js';
import Sparkline from '../components/Sparkline.jsx';
import HealthRing from '../components/HealthRing.jsx';
import IncidentBanner from '../components/IncidentBanner.jsx';
import { ErrorBox, Loading, MonitorStatus, ProviderStatus, ProviderTag } from '../components/Status.jsx';

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

function summarySentence(d) {
  if (d.monitors === 0) return 'Add your first monitor to start watching your payment APIs.';
  if (d.incidentsAffectingYou.length) return 'A provider you depend on is having an outage. Your code is probably fine.';
  if (d.down) return `${d.down} of your endpoints ${d.down === 1 ? 'is' : 'are'} failing. Open the report to see why.`;
  if (d.degraded) return `Everything answers, but ${d.degraded} ${d.degraded === 1 ? 'endpoint is' : 'endpoints are'} slow.`;
  return 'All your payment APIs are healthy.';
}

export default function Dashboard() {
  const { owner } = useOwner();
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
  const allGood = d.monitors > 0 && d.down === 0 && d.degraded === 0;

  return (
    <>
      <ErrorBox error={error} />

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

      <section className="bento">
        <div className="card bento-hero">
          <div className="bento-hero-text">
            <div className="eyebrow">{greeting()}, {owner}</div>
            <h1>{allGood ? 'All systems protected.' : 'Your API dependencies'}</h1>
            <p className="muted">{summarySentence(d)}</p>
            <div className="row gap wrap bento-actions">
              <Link className="btn btn-primary" to="/monitors/new">+ Add monitor</Link>
              <button className="btn btn-ghost" onClick={reload}>Refresh</button>
            </div>
          </div>
          <HealthRing value={d.avgUptime24h} label="uptime 24 h" />
        </div>

        <div className="bento-tile tile-up">
          <span className="tile-label">Up</span>
          <span className="tile-value">{d.up}</span>
          <span className="tile-hint">of {d.monitors} monitors</span>
        </div>
        <div className={`bento-tile ${d.down ? 'tile-down' : ''}`}>
          <span className="tile-label">Down</span>
          <span className="tile-value">{d.down}</span>
          <span className="tile-hint">{d.down ? 'needs attention' : 'nothing failing'}</span>
        </div>
        <div className={`bento-tile ${d.degraded ? 'tile-degraded' : ''}`}>
          <span className="tile-label">Slow</span>
          <span className="tile-value">{d.degraded}</span>
          <span className="tile-hint">above threshold</span>
        </div>
        <div className="bento-tile">
          <span className="tile-label">Checks · 24 h</span>
          <span className="tile-value">{d.checks24h}</span>
          <span className="tile-hint">{d.failures24h} failed</span>
        </div>
      </section>

      <div className="dash-columns">
        <section className="card">
          <div className="card-head">
            <h2>My monitors</h2>
            <span className="small muted">{monitors.length} endpoints · checked every minute</span>
          </div>
          {monitors.length === 0 ? (
            <div className="empty">
              <p>No monitors yet for <b>{owner}</b>.</p>
              <Link className="btn btn-primary" to="/monitors/new">Add your first monitor</Link>
            </div>
          ) : (
            <div className="monitor-list">
              {monitors.map(({ monitor: m, providerName, uptime24h, recentResponseTimes }) => (
                <Link key={m.id} to={`/monitors/${m.id}`} className={`monitor-card mc-${m.lastStatus.toLowerCase()}`}>
                  <div className="mc-main">
                    <div className="row gap wrap">
                      <span className="strong">{m.name}</span>
                      <MonitorStatus status={m.lastStatus} failureType={m.lastFailureType} />
                    </div>
                    <div className="mc-meta">
                      <ProviderTag code={m.provider} name={providerName} color={providerByCode[m.provider]?.info.color} />
                      <span className="mono small muted">{m.method}</span>
                      <span className="small muted">checked {timeAgo(m.lastCheckedAt)}</span>
                    </div>
                  </div>
                  <div className="mc-spark hide-sm"><Sparkline values={recentResponseTimes} width={110} /></div>
                  <div className="mc-numbers">
                    <div><b>{m.lastResponseTimeMs ?? '—'}</b><span>ms</span></div>
                    <div><b>{uptime24h}</b><span>% up</span></div>
                  </div>
                  {m.lastStatus === 'DOWN' && <span className="mc-report">Report →</span>}
                </Link>
              ))}
            </div>
          )}
        </section>

        <aside className="dash-aside">
          <section className="card">
            <div className="card-head">
              <h2>Community pulse</h2>
              <Link to="/providers" className="small">All →</Link>
            </div>
            <div className="pulse-list">
              {providers.map(({ info, health }) => (
                <Link to="/providers" key={info.code} className={`pulse-row status-${health.status.toLowerCase()}`}>
                  <ProviderTag code={info.code} name={info.name} color={info.color} />
                  <span className="pulse-bar" title={`${health.failingMonitors}/${health.totalMonitors} failing`}>
                    <span style={{ width: `${health.totalMonitors ? 100 - (health.failingMonitors / health.totalMonitors) * 100 : 0}%` }} />
                  </span>
                  <ProviderStatus status={health.status} />
                </Link>
              ))}
            </div>
            <p className="small muted">Built from every developer’s monitors, so you can see whether a failure is only yours.</p>
          </section>

          <section className="card">
            <h2>Recent incidents</h2>
            {incidents.length === 0 ? (
              <div className="muted small">No provider-wide incidents.</div>
            ) : (
              <ul className="incident-feed">
                {incidents.slice(0, 4).map((i) => (
                  <li key={i.id} className={i.active ? 'active' : ''}>
                    <div className="strong">{providerByCode[i.provider]?.info.name || i.provider}</div>
                    <div className="small muted">
                      {FAILURE_LABELS[i.dominantFailure]} · {i.affectedDevelopers} developers ·{' '}
                      {i.active ? `ongoing ${formatDuration(i.startedAt)}` : `${timeAgo(i.startedAt)}, lasted ${formatDuration(i.startedAt, i.resolvedAt)}`}
                    </div>
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
