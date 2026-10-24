import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks/usePolling.js';
import { timeAgo } from '../format.js';
import { ErrorBox, Loading, ProviderTag } from '../components/Status.jsx';

/** Community library of ready-made monitoring setups. */
export default function Templates() {
  const [provider, setProvider] = useState('');
  const { data, error, loading } = usePolling(async () => {
    const [templates, providers] = await Promise.all([api.templates(provider), api.providers()]);
    return { templates, providers };
  }, [provider], 0);

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorBox error={error} />;

  const info = Object.fromEntries(data.providers.map((p) => [p.info.code, p.info]));

  return (
    <>
      <section className="hero">
        <div>
          <h1>Shared monitoring setups</h1>
          <p className="muted">
            Ready-made checks for the APIs developers use across Africa. Use one in a click, or share your own from any monitor page. Secrets are never shared.
          </p>
        </div>
      </section>

      <div className="filters">
        <button className={`chip-btn ${provider === '' ? 'active' : ''}`} onClick={() => setProvider('')}>All</button>
        {data.providers.map((p) => (
          <button key={p.info.code} className={`chip-btn ${provider === p.info.code ? 'active' : ''}`} onClick={() => setProvider(p.info.code)}>
            {p.info.name}
          </button>
        ))}
      </div>

      <ErrorBox error={error} />
      <div className="template-cards">
        {data.templates.map((t) => (
          <article key={t.id} className="card template-card">
            <div className="row between">
              <ProviderTag code={t.provider} name={info[t.provider]?.name} color={info[t.provider]?.color} />
              <span className="small muted">used {t.uses}×</span>
            </div>
            <h3>{t.name}</h3>
            <p className="small muted">{t.description}</p>
            <div className="mono small truncate">{t.method} {t.liveUrl || t.mockUrl}</div>
            <div className="chips">
              <code className="chip">HTTP {t.expectedStatus}</code>
              {(t.expectedFields || '').split(',').filter(Boolean).map((f) => <code key={f} className="chip">{f}</code>)}
            </div>
            <div className="row between">
              <span className="small muted">by {t.author} · {timeAgo(t.createdAt)}</span>
              <Link className="btn btn-primary btn-sm" to={`/monitors/new?template=${t.id}`}>Use</Link>
            </div>
          </article>
        ))}
        {data.templates.length === 0 && <div className="card empty muted">No templates for this provider yet.</div>}
      </div>
    </>
  );
}
