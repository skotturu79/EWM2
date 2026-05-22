import { useState } from 'react';
import { useWarehouseStore } from './store';
import { DockDoorsTab } from './components/DockDoorsTab';
import { InventoryTab } from './components/InventoryTab';
import { AuditTab } from './components/AuditTab';
import './App.css';

const TABS = ['Dock Doors', 'Inventory Tracker', 'Audit Feed'];

export default function App() {
  const [activeTab, setActiveTab] = useState(0);
  const [darkMode, setDarkMode] = useState(false);
  const store = useWarehouseStore();

  const occupiedCount = store.doors.filter(d => d.status !== 'AVAILABLE').length;
  const loadingAvg = (() => { const ld = store.doors.filter(d => d.status === 'LOADING'); return ld.length ? Math.round(ld.reduce((a, d) => a + d.progress, 0) / ld.length) : 0; })();
  const lowStockCount = store.inventory.filter(i => i.quantity <= i.reorderPoint).length;
  const avgZoneFill = Math.round(store.zones.reduce((a, z) => a + z.fillPercentage, 0) / store.zones.length);

  return (
    <div className={`app ${darkMode ? 'dark' : 'light'}`}>
      {/* ── Global Header ── */}
      <header className="fiori-header">
        <div className="header-left">
          <div className="app-icon">🏭</div>
          <div>
            <div className="app-subtitle">SAP Extended Warehouse</div>
            <div className="app-title">
              EWM Central • Dallas South
              <span className={`live-dot ${store.isSimulating ? 'active' : ''}`} />
            </div>
          </div>
        </div>
        <div className="header-right">
          <button
            className={`sim-toggle ${store.isSimulating ? 'on' : ''}`}
            onClick={store.toggleSimulation}
          >
            {store.isSimulating ? '⏸ Live Stream' : '▶ Simulation Off'}
          </button>
          <button className="theme-btn" onClick={() => setDarkMode(d => !d)}>
            {darkMode ? '☀️' : '🌙'}
          </button>
        </div>
      </header>

      {/* ── KPI Tiles ── */}
      <div className="kpi-row">
        <KpiTile title="Gate Capacity" value={`${occupiedCount}/${store.doors.length}`} sub="Gates Occupied" status="Sync Active" color="info" />
        <KpiTile title="Unloading Progress" value={`${loadingAvg}%`} sub="Avg active gate progress" status={loadingAvg > 0 ? 'Operations in motion' : 'Ready to load'} color={loadingAvg > 0 ? 'warning' : 'neutral'} />
        <KpiTile title="Stock Depletion" value={`${lowStockCount} items`} sub="Below reorder threshold" status={lowStockCount > 0 ? 'Urgent replenish needed' : 'Levels optimal'} color={lowStockCount > 0 ? 'error' : 'success'} />
        <KpiTile title="Zone Capacity" value={`${avgZoneFill}%`} sub="Total utilization" status="Safe limits verified" color="success" />
      </div>

      {/* ── Tab Bar ── */}
      <div className="tab-bar">
        {TABS.map((t, i) => (
          <button key={t} className={`tab-btn ${activeTab === i ? 'active' : ''}`} onClick={() => setActiveTab(i)}>
            {i === 0 ? '🚛' : i === 1 ? '📦' : '📋'} {t}
          </button>
        ))}
      </div>

      {/* ── Tab Content ── */}
      <main className="main-content">
        {activeTab === 0 && <DockDoorsTab store={store} />}
        {activeTab === 1 && <InventoryTab store={store} />}
        {activeTab === 2 && <AuditTab logs={store.auditLogs} />}
      </main>
    </div>
  );
}

function KpiTile({ title, value, sub, status, color }: { title: string; value: string; sub: string; status: string; color: string }) {
  return (
    <div className={`kpi-tile kpi-${color}`}>
      <div className="kpi-title">{title}</div>
      <div className="kpi-value">{value}</div>
      <div className="kpi-sub">{sub}</div>
      <div className="kpi-status">
        <span className="kpi-dot" /> {status}
      </div>
    </div>
  );
}
