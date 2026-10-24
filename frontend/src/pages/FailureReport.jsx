import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { usePolling } from '../hooks/usePolling.js';
import { FAILURE_LABELS, formatDuration, formatTime } from '../format.js';
import { ErrorBox, Loading, ProviderStatus } from '../components/Status.jsx';

const VERDICT_STYLE = {
  THEM: { className: 'verdict-them', icon: '⚡', label: 'Provider problem' },
  YOU: { className: 'verdict-you', icon: '🔧', label: 'Your setup' },
  UNCLEAR: { className: 'verdict-unclear', icon: '?', label: 'Still investigating' },
  NONE: { className: 'verdict-ok', icon: '✓', label: 'Healthy' },
};

function prettyJson(text) {
  if (!text) return '(empty response)';
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
}

/** Plain-text version of the report, to paste into Slack, a ticket or a support email. */
function reportAsText(r) {
  const lines = [
    `KingaAPI Dependency Failure Report: ${r.monitorName} (${r.providerName})`,
    `Verdict: ${r.verdictHeadline} ${r.verdictDetail}`,
    `Failure: ${r.title}${r.failingSince ? `, since ${formatTime(r.failingSince)}` : ''}`,
    `Request: ${r.expected.method} ${r.expected.url}`,
    `Expected: HTTP ${r.expected.status}${r.expected.fields.length ? `, fields ${r.expected.fields.join(', ')}` : ''}`,
    `Got: HTTP ${r.actual.status ?? 'no response'} in ${r.actual.responseTimeMs} ms. ${r.actual.error || ''}`,
    `Community: ${r.community.failingMonitors}/${r.community.totalMonitors} monitors failing across ${r.community.developers} developers`,
    '',
    'What to check next:',
    ...r.suggestions.map((s, i) => `${i + 1}. ${s}`),
  ];
  return lines.join('\n');
}

export default function FailureReport() {
  const { id } = useParams();
  const [copied, setCopied] = useState(false);
  const { data: r, error, loading } = usePolling(() => api.report(id), [id], 5000);

  if (loading && !r) return <Loading text="Building the failure report…" />;
  if (!r) return <ErrorBox error={error} />;

  const verdict = VERDICT_STYLE[r.verdict] || VERDICT_STYLE.UNCLEAR;

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(reportAsText(r));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      window.prompt('Copy the report:', reportAsText(r));
    }
  };

  return (
    <>
      <div className="breadcrumb">
        <Link to="/">Dashboard</Link> / <Link to={`/monitors/${id}`}>{r.monitorName}</Link> / Failure report
      </div>
      <ErrorBox error={error} />

      <section className="hero">
        <div>
          <div className="eyebrow">Dependency Failure Report</div>
          <h1>{r.monitorName}</h1>
          <div className="small muted">
            {r.providerName} · generated {formatTime(r.generatedAt)}
            {r.hasFailure && r.resolved && <span className="pill pill-up" style={{ marginLeft: 8 }}><span className="dot" />Recovered</span>}
          </div>
        </div>
        {r.hasFailure && <button className="btn btn-ghost" onClick={copy}>{copied ? 'Copied ✓' : 'Copy report'}</button>}
      </section>

      <section className={`verdict ${verdict.className}`}>
        <div className="verdict-icon">{verdict.icon}</div>
        <div>
          <div className="verdict-label">{verdict.label}</div>
          <div className="verdict-headline">{r.verdictHeadline}</div>
          <p>{r.verdictDetail}</p>
        </div>
      </section>

      <div className="report-layout">
        <div className="report-main">
          {!r.hasFailure ? (
            <div className="card empty">
              <p>This monitor has not failed yet.</p>
              <Link className="btn btn-ghost" to={`/monitors/${id}`}>Back to monitor</Link>
            </div>
          ) : (
            <>
              <section className="card">
                <div className="card-head">
                  <h2>What failed</h2>
                  <span className="pill pill-down"><span className="dot" />{FAILURE_LABELS[r.failureType]}</span>
                </div>
                <div className="what-failed">
                  <div className="what-failed-title">{r.title}</div>
                  <div className="muted">{r.actual.error}</div>
                  <div className="small muted">
                    {r.resolved
                      ? `Last failure at ${formatTime(r.actual.checkedAt)}.`
                      : `Failing for ${formatDuration(r.failingSince)} · ${r.consecutiveFailures} failed checks in a row.`}
                  </div>
                </div>
              </section>

              <div className="grid-2">
                <section className="card compare compare-expected">
                  <h2>Expected</h2>
                  <dl className="details">
                    <dt>Request</dt><dd className="mono small">{r.expected.method} {r.expected.url}</dd>
                    <dt>Status</dt><dd><b>HTTP {r.expected.status}</b></dd>
                    <dt>Fields</dt>
                    <dd>{r.expected.fields.length ? r.expected.fields.map((f) => <code key={f} className="chip">{f}</code>) : 'any body'}</dd>
                    <dt>Within</dt><dd>{r.expected.timeoutMs} ms</dd>
                  </dl>
                </section>
                <section className="card compare compare-actual">
                  <h2>What came back</h2>
                  <dl className="details">
                    <dt>Checked</dt><dd>{formatTime(r.actual.checkedAt)}</dd>
                    <dt>Status</dt><dd><b className="text-down">{r.actual.status ? `HTTP ${r.actual.status}` : 'No response'}</b></dd>
                    <dt>Missing</dt>
                    <dd>{r.actual.missingFields.length ? r.actual.missingFields.map((f) => <code key={f} className="chip chip-bad">{f}</code>) : '—'}</dd>
                    <dt>Took</dt><dd>{r.actual.responseTimeMs} ms</dd>
                  </dl>
                </section>
              </div>

              <section className="card">
                <h2>What to check next</h2>
                <ol className="suggestions">
                  {r.suggestions.map((s) => <li key={s}>{s}</li>)}
                </ol>
              </section>

              <section className="card">
                <h2>Response body</h2>
                <pre className="code code-block">{prettyJson(r.actual.body)}</pre>
              </section>
            </>
          )}
        </div>

        <aside className="report-aside">
          <section className="card">
            <h2>Other developers on {r.providerName}</h2>
            <div className="community">
              <div>
                <div className="community-big">{r.community.failingMonitors}<span>/{r.community.totalMonitors}</span></div>
                <div className="small muted">monitors failing</div>
              </div>
              <div>
                <div className="community-big">{r.community.affectedDevelopers}<span>/{r.community.developers}</span></div>
                <div className="small muted">developers affected</div>
              </div>
            </div>
            <div className="row between aside-status">
              <ProviderStatus status={r.community.status} />
              <span className="small muted">avg {r.community.avgResponseMs} ms</span>
            </div>
          </section>

          <section className="card">
            <h2>Timeline</h2>
            <ul className="timeline">
              {r.recentChecks.slice(0, 8).map((c) => (
                <li key={c.id} className={c.success ? 'ok' : 'bad'}>
                  <span className="small nowrap">{formatTime(c.checkedAt)}</span>
                  <span>{FAILURE_LABELS[c.failureType]}</span>
                  <span className="small muted">{c.statusCode ?? '—'} · {c.responseTimeMs} ms</span>
                </li>
              ))}
            </ul>
          </section>

          <section className="card aside-links">
            <h2>Shortcuts</h2>
            {r.statusPageUrl && <a className="btn btn-ghost" href={r.statusPageUrl} target="_blank" rel="noreferrer">{r.providerName} status ↗</a>}
            {r.docsUrl && <a className="btn btn-ghost" href={r.docsUrl} target="_blank" rel="noreferrer">API docs ↗</a>}
            <Link className="btn btn-ghost" to={`/monitors/${id}/edit`}>Edit monitor</Link>
          </section>
        </aside>
      </div>
    </>
  );
}
