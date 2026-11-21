export default function Spinner({ label = 'Loading…' }) {
  return (
    <div className="spinner" role="status">
      <span className="spinner__ring" />
      <span>{label}</span>
    </div>
  );
}
