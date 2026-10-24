import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { FAILURE_LABELS } from '../format.js';

function FailureDot({ cx, cy, payload }) {
  if (cx == null || cy == null) return null;
  const color = !payload.success ? 'var(--down)' : payload.failureType === 'SLOW' ? 'var(--degraded)' : 'var(--brand)';
  const r = payload.success && payload.failureType !== 'SLOW' ? 2 : 4.5;
  return <circle cx={cx} cy={cy} r={r} fill={color} stroke="var(--surface)" strokeWidth="1" />;
}

function ChartTooltip({ active, payload }) {
  if (!active || !payload?.length) return null;
  const c = payload[0].payload;
  return (
    <div className="chart-tooltip">
      <div><b>{c.time}</b></div>
      <div>{c.responseTimeMs} ms · HTTP {c.statusCode ?? '—'}</div>
      <div className={c.success ? 'text-up' : 'text-down'}>{FAILURE_LABELS[c.failureType]}</div>
    </div>
  );
}

/** Response time of recent checks; failed checks are drawn as red dots. */
export default function ResponseChart({ checks }) {
  const data = [...checks].reverse().map((c) => ({
    ...c,
    time: new Date(c.checkedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  }));
  if (data.length === 0) return <div className="empty muted">No checks yet.</div>;
  return (
    <ResponsiveContainer width="100%" height={240}>
      <LineChart data={data} margin={{ top: 10, right: 12, bottom: 0, left: -10 }}>
        <CartesianGrid stroke="var(--border)" vertical={false} />
        <XAxis dataKey="time" tick={{ fontSize: 11, fill: 'var(--muted)' }} minTickGap={40} tickLine={false} axisLine={false} />
        <YAxis unit=" ms" tick={{ fontSize: 11, fill: 'var(--muted)' }} tickLine={false} axisLine={false} width={70} />
        <Tooltip content={<ChartTooltip />} />
        <Line type="monotone" dataKey="responseTimeMs" stroke="var(--brand)" strokeWidth={2} dot={<FailureDot />} isAnimationActive={false} />
      </LineChart>
    </ResponsiveContainer>
  );
}
