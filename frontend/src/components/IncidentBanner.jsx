import { Link } from 'react-router-dom';
import { formatDuration } from '../format.js';

/** The "it's them, not you" card shown when a provider has a community-wide outage. */
export default function IncidentBanner({ incident, providerName, reportLink }) {
  return (
    <div className="banner-them">
      <div className="banner-body">
        <div className="banner-tag">{providerName || incident.provider} outage</div>
        <div className="banner-title">It’s <em>them</em>, not you.</div>
        <div>{incident.summary}</div>
        <div className="small banner-hint">Started {formatDuration(incident.startedAt)} ago. Your code is probably fine.</div>
      </div>
      {reportLink && (
        <Link className="btn btn-light" to={reportLink}>View failure report</Link>
      )}
    </div>
  );
}
