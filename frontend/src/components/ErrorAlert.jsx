export default function ErrorAlert({ error, title = 'Something went wrong' }) {
  if (!error) {
    return null;
  }

  const fields = error.fieldErrors || {};
  const fieldEntries = Object.entries(fields);

  return (
    <div className="alert alert--danger" role="alert">
      <strong>{title}</strong>
      <p>{error.message}</p>
      {fieldEntries.length > 0 && (
        <ul>
          {fieldEntries.map(([field, message]) => (
            <li key={field}>
              <strong>{field}:</strong> {message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
