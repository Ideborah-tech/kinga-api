/** Tiny inline response-time chart for table rows. */
export default function Sparkline({ values = [], width = 120, height = 28 }) {
  if (values.length < 2) return <span className="muted small">—</span>;
  const max = Math.max(...values);
  const min = Math.min(...values);
  const range = max - min || 1;
  const step = width / (values.length - 1);
  const points = values
    .map((v, i) => `${(i * step).toFixed(1)},${(height - 3 - ((v - min) / range) * (height - 6)).toFixed(1)}`)
    .join(' ');
  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} className="sparkline" aria-label="Response times">
      <polyline points={points} fill="none" stroke="var(--brand)" strokeWidth="1.6" strokeLinejoin="round" />
    </svg>
  );
}
