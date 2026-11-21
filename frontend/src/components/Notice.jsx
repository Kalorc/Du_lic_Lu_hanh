export default function Notice({ kind = 'info', children }) {
  const role = kind === 'error' ? 'alert' : kind === 'ok' ? 'status' : undefined;
  return <div className={`notice ${kind}`} role={role}>{children}</div>;
}
