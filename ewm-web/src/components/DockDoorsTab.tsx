import { useState } from 'react';
import type { DockDoor, DoorStatus } from '../types';
import type { useWarehouseStore } from '../store';

type Store = ReturnType<typeof useWarehouseStore>;

const STATUS_META: Record<DoorStatus, { label: string; cls: string; icon: string }> = {
  AVAILABLE: { label: 'Clear',    cls: 'success',  icon: '✅' },
  DOCKED:    { label: 'Docked',   cls: 'info',     icon: '🔵' },
  LOADING:   { label: 'Loading',  cls: 'warning',  icon: '🟠' },
  BLOCKED:   { label: 'Blocked',  cls: 'error',    icon: '🔴' },
};

export function DockDoorsTab({ store }: { store: Store }) {
  const [showDockDialog, setShowDockDialog] = useState(false);
  const selected = store.doors.find(d => d.id === store.selectedDoorId);

  return (
    <div className="two-pane">
      {/* Left: Door List */}
      <div className="pane pane-list">
        <h3 className="pane-title">Gate Status Feeds</h3>
        {store.doors.map(door => {
          const m = STATUS_META[door.status];
          const isSel = door.id === store.selectedDoorId;
          return (
            <div key={door.id} className={`door-row ${isSel ? 'selected' : ''}`} onClick={() => store.setSelectedDoorId(door.id)}>
              <div className={`door-icon status-bg-${m.cls}`}>{m.icon}</div>
              <div className="door-info">
                <div className="door-name">{door.name}</div>
                <div className="door-carrier">{door.carrier ? `${door.carrier} • ${door.licensePlate}` : 'No Trailer Attached'}</div>
              </div>
              <div className="door-right">
                <span className={`badge badge-${m.cls}`}>{m.label}</span>
                {door.status === 'LOADING' && <div className="door-progress-txt">Loading: {door.progress}%</div>}
              </div>
            </div>
          );
        })}
      </div>

      {/* Right: Detail */}
      <div className="pane pane-detail">
        {selected ? (
          <DoorDetail door={selected} store={store} onDockRequested={() => setShowDockDialog(true)} />
        ) : (
          <div className="empty-state">Select a gate to view telemetry</div>
        )}
      </div>

      {showDockDialog && selected && (
        <DockDialog door={selected} store={store} onClose={() => setShowDockDialog(false)} />
      )}
    </div>
  );
}

function DoorDetail({ door, store, onDockRequested }: { door: DockDoor; store: Store; onDockRequested: () => void }) {
  const m = STATUS_META[door.status];
  return (
    <div className="door-detail">
      <div className="detail-header">
        <div>
          <h2 className="detail-title">{door.name}</h2>
          <div className="lock-status">{door.lockState ? '🔒 Dock Locked' : '🔓 Lock Released'}</div>
        </div>
        <span className={`badge badge-${m.cls} badge-lg`}>{m.label}</span>
      </div>

      <div className="detail-divider" />

      {door.status === 'AVAILABLE' && (
        <div className="detail-empty-state">
          <div className="big-icon">✅</div>
          <div className="detail-state-title">Ready for Delivery</div>
          <div className="detail-state-sub">Dock a carrier truck to verify manifest</div>
        </div>
      )}

      {door.status === 'BLOCKED' && (
        <div className="detail-empty-state error-bg">
          <div className="big-icon">⚠️</div>
          <div className="detail-state-title error-txt">MAINTENANCE LOCKOUT ACTIVE</div>
          <div className="detail-state-sub">Security incident or structural check. Gate disabled.</div>
        </div>
      )}

      {(door.status === 'DOCKED' || door.status === 'LOADING') && (
        <div className="detail-grid">
          <KV label="Carrier Agency" value={door.carrier} />
          <KV label="Tractor Plate" value={door.licensePlate} />
          <KV label="Priority Level" value={door.priority} />
          <KV label="Security Seal" value={door.securitySeal} />
          <KV label="Time Arrived" value={door.arrivalTime || 'Just Now'} />
          <KV label="Cold Chain" value={door.temperatureControl ? `Refrigerated (${door.temperatureF}°F)` : 'Ambient Cargo'} />

          <div className="progress-section">
            <div className="progress-header">
              <span>Cargo Manifest Verification</span>
              <span className="progress-pct">{door.progress}% Verified</span>
            </div>
            <div className="progress-label">Pallets: {door.loadedPallets} / {door.totalPallets}</div>
            <div className="progress-bar-bg">
              <div className={`progress-bar-fill ${door.progress === 100 ? 'complete' : ''}`} style={{ width: `${door.progress}%` }} />
            </div>
          </div>
        </div>
      )}

      <div className="detail-divider" />

      <div className="detail-actions">
        <button className="btn btn-outline" onClick={() => store.toggleLock(door.id)}>
          {door.lockState ? '🔓 Release Dock Lock' : '🔒 Engage Dock Lock'}
        </button>
        {door.status === 'AVAILABLE' && <button className="btn btn-primary" onClick={onDockRequested}>🚛 Dock Truck Vessel</button>}
        {door.status === 'DOCKED'    && <button className="btn btn-primary" onClick={() => store.changeStatus(door.id, 'LOADING')}>▶ Initiate Loading</button>}
        {door.status === 'LOADING'   && <button className="btn btn-warning" onClick={() => store.loadPallet(door.id)}>+ Scan & Load Pallet</button>}
        {door.status === 'BLOCKED'   && <button className="btn btn-primary" onClick={() => store.changeStatus(door.id, 'AVAILABLE')}>✔ Resolve & Clear</button>}
      </div>

      <div className="status-override">
        <label className="override-label">Set Status Manually:</label>
        <div className="override-btns">
          {(['AVAILABLE', 'DOCKED', 'LOADING', 'BLOCKED'] as DoorStatus[]).map(s => (
            <button key={s} className={`btn btn-sm ${door.status === s ? 'active-status' : 'btn-ghost'}`} onClick={() => store.changeStatus(door.id, s)}>{s}</button>
          ))}
        </div>
      </div>
    </div>
  );
}

function KV({ label, value }: { label: string; value: string }) {
  return (
    <div className="kv">
      <div className="kv-label">{label}</div>
      <div className="kv-value">{value}</div>
    </div>
  );
}

function DockDialog({ door, store, onClose }: { door: DockDoor; store: Store; onClose: () => void }) {
  const [carrier, setCarrier] = useState('');
  const [plate, setPlate] = useState('');
  const [pallets, setPallets] = useState('24');
  const [priority, setPriority] = useState('Medium');
  const [tempControl, setTempControl] = useState(false);
  const [tempF, setTempF] = useState('38');

  const submit = () => {
    store.dockTruck(door.id, carrier, plate, parseInt(pallets) || 24, priority, tempControl, parseFloat(tempF) || 38);
    onClose();
  };

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog" onClick={e => e.stopPropagation()}>
        <h2 className="dialog-title">Register Carrier Docking</h2>
        <p className="dialog-sub">Assign a freight trailer to {door.name} to start logistics.</p>
        <div className="dialog-fields">
          <label>Carrier Agency Name<input className="input" value={carrier} onChange={e => setCarrier(e.target.value)} placeholder="DHL, FedEx, UPS…" /></label>
          <label>Trailer License ID<input className="input" value={plate} onChange={e => setPlate(e.target.value)} placeholder="TX-4921X" /></label>
          <div className="dialog-row">
            <label>Manifest Pallets<input className="input" type="number" value={pallets} onChange={e => setPallets(e.target.value)} /></label>
            <label>Priority
              <select className="input" value={priority} onChange={e => setPriority(e.target.value)}>
                {['Low', 'Medium', 'High'].map(p => <option key={p}>{p}</option>)}
              </select>
            </label>
          </div>
          <label className="checkbox-row">
            <input type="checkbox" checked={tempControl} onChange={e => setTempControl(e.target.checked)} />
            Refrigerated Cold-Chain Trailer
          </label>
          {tempControl && <label>Target Temperature (°F)<input className="input" type="number" value={tempF} onChange={e => setTempF(e.target.value)} /></label>}
        </div>
        <div className="dialog-actions">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={submit}>Dock Vessel</button>
        </div>
      </div>
    </div>
  );
}
