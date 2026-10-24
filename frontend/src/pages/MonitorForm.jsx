import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { useOwner } from '../OwnerContext.jsx';
import { ErrorBox, Loading, ProviderTag } from '../components/Status.jsx';

const EMPTY = {
  name: '',
  provider: 'MPESA',
  url: '',
  method: 'GET',
  headers: '',
  body: '',
  expectedStatus: 200,
  expectedFields: '',
  intervalSeconds: 60,
  timeoutMs: 8000,
  slowThresholdMs: 2000,
  active: true,
  templateId: null,
};

function fromTemplate(t, target) {
  return {
    ...EMPTY,
    name: t.name,
    provider: t.provider,
    url: (target === 'live' ? t.liveUrl : t.mockUrl) || t.mockUrl || t.liveUrl || '',
    method: t.method,
    headers: t.headersHint || '',
    body: t.body || '',
    expectedStatus: t.expectedStatus,
    expectedFields: t.expectedFields || '',
    templateId: t.id,
  };
}

export default function MonitorForm() {
  const { id } = useParams();
  const editing = Boolean(id);
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { owner } = useOwner();

  const [form, setForm] = useState(EMPTY);
  const [providers, setProviders] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [selected, setSelected] = useState(null);
  const [target, setTarget] = useState('mock');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([api.providers(), api.templates(), editing ? api.monitor(id) : null])
      .then(([p, t, existing]) => {
        setProviders(p);
        setTemplates(t);
        if (existing) {
          setForm({ ...EMPTY, ...existing.monitor });
        } else if (params.get('template')) {
          const template = t.find((x) => String(x.id) === params.get('template'));
          if (template) {
            setSelected(template);
            setForm(fromTemplate(template, 'mock'));
          }
        }
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  const set = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setForm((f) => ({ ...f, [field]: value }));
  };

  const pick = (template) => {
    setSelected(template);
    setForm(fromTemplate(template, target));
  };

  const switchTarget = (value) => {
    setTarget(value);
    if (selected) setForm((f) => ({ ...f, url: (value === 'live' ? selected.liveUrl : selected.mockUrl) || f.url }));
  };

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const payload = {
      ...form,
      owner: editing ? form.owner : owner,
      expectedStatus: Number(form.expectedStatus),
      intervalSeconds: Number(form.intervalSeconds),
      timeoutMs: Number(form.timeoutMs),
      slowThresholdMs: Number(form.slowThresholdMs),
    };
    try {
      const saved = editing ? await api.updateMonitor(id, payload) : await api.createMonitor(payload);
      if (!editing) await api.checkNow(saved.id).catch(() => {});
      navigate(`/monitors/${saved.id}`);
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  if (loading) return <Loading />;

  const grouped = providers.map((p) => ({ provider: p.info, items: templates.filter((t) => t.provider === p.info.code) }));

  return (
    <>
      <div className="breadcrumb"><Link to="/">Dashboard</Link> / {editing ? `Edit ${form.name}` : 'Add monitor'}</div>
      <section className="hero">
        <div>
          <h1>{editing ? 'Edit monitor' : 'Add a monitor'}</h1>
          <p className="muted">
            {editing ? 'Changes are checked straight away.' : 'Start from a setup shared by other developers, or fill in your own endpoint.'}
          </p>
        </div>
      </section>

      {!editing && (
        <section className="card">
          <div className="card-head">
            <h2>1. Pick a ready-made setup</h2>
            <div className="segmented">
              <button type="button" className={target === 'mock' ? 'active' : ''} onClick={() => switchTarget('mock')}>Demo mock</button>
              <button type="button" className={target === 'live' ? 'active' : ''} onClick={() => switchTarget('live')}>Live sandbox</button>
            </div>
          </div>
          <p className="small muted">
            “Demo mock” points at KingaAPI’s built-in fake providers, so it works without real keys. “Live sandbox” uses the provider’s real sandbox URL; put your own keys in the headers.
          </p>
          {grouped.map(({ provider, items }) => items.length > 0 && (
            <div key={provider.code} className="template-group">
              <ProviderTag code={provider.code} name={provider.name} color={provider.color} />
              <div className="template-list">
                {items.map((t) => (
                  <button type="button" key={t.id} className={`template-option ${selected?.id === t.id ? 'selected' : ''}`} onClick={() => pick(t)}>
                    <span className="strong">{t.name}</span>
                    <span className="small muted">{t.method} · used by {t.uses} · by {t.author}</span>
                  </button>
                ))}
              </div>
            </div>
          ))}
        </section>
      )}

      <form className="card form" onSubmit={submit}>
        <h2>{editing ? 'Monitor settings' : '2. Check the details'}</h2>
        <ErrorBox error={error} />

        <div className="form-grid">
          <label className="span-2">Name
            <input value={form.name} onChange={set('name')} required placeholder="M-Pesa STK Push" />
          </label>
          <label>Provider
            <select value={form.provider} onChange={set('provider')}>
              {providers.map((p) => <option key={p.info.code} value={p.info.code}>{p.info.name}</option>)}
              <option value="OTHER">Other</option>
            </select>
          </label>
          <label>Method
            <select value={form.method} onChange={set('method')}>
              {['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD'].map((m) => <option key={m}>{m}</option>)}
            </select>
          </label>
          <label className="span-2">URL
            <input value={form.url} onChange={set('url')} required placeholder="https://sandbox.safaricom.co.ke/..." className="mono" />
          </label>
          <label className="span-2">Headers <span className="muted small">one per line, e.g. Authorization: Bearer …</span>
            <textarea rows={3} value={form.headers || ''} onChange={set('headers')} className="mono" />
          </label>
          {form.method !== 'GET' && form.method !== 'HEAD' && (
            <label className="span-2">Request body
              <textarea rows={4} value={form.body || ''} onChange={set('body')} className="mono" />
            </label>
          )}
          <label>Expected status
            <input type="number" value={form.expectedStatus} onChange={set('expectedStatus')} min={100} max={599} />
          </label>
          <label>Required JSON fields <span className="muted small">comma separated, e.g. data.0.code</span>
            <input value={form.expectedFields || ''} onChange={set('expectedFields')} className="mono" placeholder="access_token,expires_in" />
          </label>
          <label>Check every (seconds)
            <input type="number" value={form.intervalSeconds} onChange={set('intervalSeconds')} min={15} max={3600} />
          </label>
          <label>Timeout (ms)
            <input type="number" value={form.timeoutMs} onChange={set('timeoutMs')} min={1000} max={30000} />
          </label>
          <label>Slow above (ms)
            <input type="number" value={form.slowThresholdMs} onChange={set('slowThresholdMs')} min={100} max={30000} />
          </label>
          <label className="checkbox">
            <input type="checkbox" checked={form.active} onChange={set('active')} /> Active
          </label>
        </div>

        <div className="row gap">
          <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Start monitoring'}</button>
          <Link className="btn btn-ghost" to={editing ? `/monitors/${id}` : '/'}>Cancel</Link>
        </div>
      </form>
    </>
  );
}
