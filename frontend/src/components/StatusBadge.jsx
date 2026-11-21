import { friendlyStatus, statusTone } from '../utils/format.js';

export default function StatusBadge({ status }) {
  return (
    <span className={`status status--${statusTone(status)}`}>
      {friendlyStatus(status)}
    </span>
  );
}
