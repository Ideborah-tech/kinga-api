import { api } from '../api.js';
import { usePolling } from '../hooks/usePolling.js';
import { FAILURE_LABELS, formatDuration, formatTime } from '../format.js';
import { ErrorBox, Loading, ProviderStatus, ProviderTag } from '../components/Status.jsx';

export default function Providers() {
  const { data, error, loading } = usePolling(async () => {
    const [providers, incidents] = await Promise.all([api.providers(), api.incidents()]);
    return { providers, incidents };
  }, [], 5000);

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorBox error={error} />;

  const names = Object.fromEntries(data.providers.map((p) => [p.info.code, p.info.name]));

  return (
    <>
      <section className="hero">
        <div>
          <h1>Provider status</h1>
          <p className="muted">
            Built from every developer’s monitors. When at least half of a provider’s monitors fail at the same time, KingaAPI opens an incident and tells each developer: it’s them, not you.
          </p>
        </div>
      </section>
      <ErrorBox error={error} />

      <div className="provider-detail-grid">
        {data.providers.map(({ info, health }) => (
          <article key={info.code} className={`card provider-detail status-${health.status.toLowerCase()}`}>
            <div className="row between">
              <ProviderTag code={info.code} name={info.name} color={info.color} />
              <ProviderStatus status={health.status} />
            </div>
            <div className="small muted">{info.company} · {info.category}</div>
            <div className="small">{info.countries.join(', ')}</div>
            <div className="provider-numbers">
              <div><b>{health.totalMonitors}</b><span>monitors</span></div>
              <div><b>{health.developers}</b><span>developers</span></div>
              <div><b className={health.failingMonitors ? 'text-down' : ''}>{health.failingMonitors}</b><span>failing</span></div>
              <div><b>{health.avgResponseMs}</b><span>avg ms</span></div>
            </div>
            {health.activeIncident && <div className="alert alert-error small">{health.activeIncident.summary}</div>}
            <div className="row gap small">
              <a href={info.statusPageUrl} target="_blank" rel="noreferrer">Status page ↗</a>
              <a href={info.docsUrl} target="_blank" rel="noreferrer">Docs ↗</a>
            </div>
          </article>
        ))}
      </div>

      <section className="card">
        <h2>Incident history</h2>
        {data.incidents.length === 0 ? (
          <div className="empty muted">No provider-wide incidents yet.</div>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Provider</th><th>Started</th><th>Duration</th><th>Cause</th><th>Impact</th><th>Status</th></tr>
              </thead>
              <tbody>
                {data.incidents.map((i) => (
                  <tr key={i.id}>
                    <td className="strong">{names[i.provider] || i.provider}</td>
                    <td className="small nowrap">{formatTime(i.startedAt)}</td>
                    <td className="small">{formatDuration(i.startedAt, i.resolvedAt)}</td>
                    <td>{FAILURE_LABELS[i.dominantFailure] || i.dominantFailure}</td>
                    <td className="small">{i.affectedMonitors}/{i.totalMonitors} monitors · {i.affectedDevelopers} devs</td>
                    <td>{i.active ? <span className="pill pill-down"><span className="dot" />Ongoing</span> : <span className="pill pill-up"><span className="dot" />Resolved</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
