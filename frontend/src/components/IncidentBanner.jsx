import { Link } from 'react-router-dom';
import { formatDuration } from '../format.js';

/** The "it's them, not you" banner shown when a provider has a community-wide outage. */
export default function IncidentBanner({ incident, providerName, reportLink }) {
  return (
    <div className="banner-them">
      <div className="banner-icon">!</div>
      <div className="banner-body">
        <div className="banner-title">It’s them, not you: {providerName || incident.provider} is having an outage</div>
        <div>{incident.summary}</div>
        <div className="small">Started {formatDuration(incident.startedAt)} ago. Your code is probably fine.</div>
      </div>
      {reportLink && (
        <Link className="btn btn-light" to={reportLink}>View failure report</Link>
      )}
    </div>
  );
}
