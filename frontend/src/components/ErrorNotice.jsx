import Notice from './Notice.jsx';

/** Hien thi loi kem ma loi do backend tra ve, giup doi chieu voi bang ma loi. */
export default function ErrorNotice({ error }) {
  if (!error) return null;
  return (
    <Notice kind="error">
      <strong>{error.message}</strong>
      {error.code && (
        <>
          {' '}
          <code>({error.code})</code>
        </>
      )}
      {error.details && (
        <ul className="bullets" style={{ marginTop: 6 }}>
          {Object.entries(error.details).map(([field, msg]) => (
            <li key={field}>
              <code>{field}</code>: {msg}
            </li>
          ))}
        </ul>
      )}
    </Notice>
  );
}
