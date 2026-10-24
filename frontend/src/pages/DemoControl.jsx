import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks/usePolling.js';
import { useOwner } from '../OwnerContext.jsx';
import { ErrorBox, Loading, ProviderStatus, ProviderTag } from '../components/Status.jsx';

const MODES = [
  { value: 'HEALTHY', label: 'Healthy' },
  { value: 'SLOW', label: 'Slow' },
  { value: 'FLAKY', label: 'Flaky' },
  { value: 'SERVER_ERROR', label: '503 error' },
  { value: 'TIMEOUT', label: 'Timeout' },
  { value: 'AUTH_ERROR', label: '401 for all' },
  { value: 'RATE_LIMITED', label: '429 limit' },
  { value: 'BAD_FORMAT', label: 'Schema change' },
];

/** Stage outages on the built-in mock providers for the live demo. */
export default function DemoControl() {
  const { owner } = useOwner();
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const { data, error, loading, reload } = usePolling(async () => {
    const [modes, providers, mine] = await Promise.all([api.demoModes(), api.providers(), api.monitors(owner)]);
    return { modes, providers, mine };
  }, [owner], 4000);

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorBox error={error} />;

  const act = async (fn, text) => {
    setBusy(true);
    try {
      await fn();
      setMessage(text);
      await reload();
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  const myProviders = [...new Set(data.mine.map((s) => s.monitor.provider))];
  const isKeyBroken = (code) =>
    data.mine.some((s) => s.monitor.provider === code && (s.monitor.headers || '').includes('INVALID_KEY'));

  return (
    <>
      <section className="hero">
        <div>
          <h1>Demo control</h1>
          <p className="muted">
            Break the built-in mock providers on cue. Monitors are re-checked straight away, so the dashboard updates within seconds.
          </p>
        </div>
        <button className="btn btn-ghost" disabled={busy} onClick={() => act(() => api.resetDemo(owner), 'Everything is healthy again.')}>
          Reset everything
        </button>
      </section>

      {message && <div className="alert alert-success">{message}</div>}
      <ErrorBox error={error} />

      <section className="card script">
        <h2>Demo script (2 minutes)</h2>
        <ol>
          <li>Open the <Link to="/">dashboard</Link>: all your payment APIs are green, with 24 h of history.</li>
          <li><b>Story 1, “it’s them”:</b> set M-Pesa to <i>503 error</i>. Within seconds every developer’s M-Pesa monitors fail, an incident opens, and your dashboard shows “It’s them, not you”. Open the failure report.</li>
          <li><b>Story 2, “it’s you”:</b> reset, then click <i>Break my key</i> for MTN MoMo. Only your monitor fails; the report says other developers are fine and points to your credentials.</li>
          <li><b>Story 3, silent breakage:</b> set Paystack to <i>Schema change</i>. It still returns 200, but KingaAPI lists exactly which fields disappeared.</li>
        </ol>
      </section>

      <section className="card">
        <h2>Provider-wide problems (affect every developer)</h2>
        <div className="demo-rows">
          {data.providers.map(({ info, health }) => (
            <div key={info.code} className="demo-row">
              <div className="demo-row-head">
                <ProviderTag code={info.code} name={info.name} color={info.color} />
                <ProviderStatus status={health.status} />
              </div>
              <div className="segmented wrap">
                {MODES.map((m) => (
                  <button
                    key={m.value}
                    disabled={busy}
                    className={data.modes[info.code] === m.value ? `active ${m.value === 'HEALTHY' ? '' : 'danger'}` : ''}
                    onClick={() => act(() => api.setDemoMode(info.code, m.value), `${info.name} is now: ${m.label}.`)}
                  >
                    {m.label}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>Only your problem: break {owner}’s API key</h2>
        <p className="small muted">Swaps the Authorization header on {owner}’s monitors for an invalid key. Other developers stay healthy.</p>
        {myProviders.length === 0 && <div className="muted">{owner} has no monitors.</div>}
        <div className="demo-rows">
          {myProviders.map((code) => {
            const p = data.providers.find((x) => x.info.code === code)?.info;
            const broken = isKeyBroken(code);
            return (
              <div key={code} className="demo-row">
                <ProviderTag code={code} name={p?.name} color={p?.color} />
                <button
                  className={`btn btn-sm ${broken ? 'btn-primary' : 'btn-danger'}`}
                  disabled={busy}
                  onClick={() => act(() => api.setMyKey(owner, code, !broken), broken ? `${p?.name} key restored.` : `${p?.name} key broken for ${owner}.`)}
                >
                  {broken ? 'Fix my key' : 'Break my key'}
                </button>
              </div>
            );
          })}
        </div>
      </section>
    </>
  );
}
