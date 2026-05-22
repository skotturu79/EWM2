import type { AuditLog } from '../types';

export function AuditTab({ logs }: { logs: AuditLog[] }) {
  return (
    <div className="audit-pane">
      <div className="audit-header">
        <h3 className="pane-title">System Action Telemetry Log</h3>
        <span>🔔</span>
      </div>
      {logs.length === 0 ? (
        <div className="empty-state">No actions logged yet.</div>
      ) : (
        <div className="audit-list">
          {logs.map((log, i) => (
            <div key={i} className="audit-row">
              <span className="audit-dot" />
              <span className="audit-time">[{log.time}]</span>
              <span className="audit-msg">{log.message}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
