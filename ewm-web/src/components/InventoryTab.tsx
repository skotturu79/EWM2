import { useState } from 'react';
import type { useWarehouseStore } from '../store';

type Store = ReturnType<typeof useWarehouseStore>;

export function InventoryTab({ store }: { store: Store }) {
  const [showReceipt, setShowReceipt] = useState(false);
  const [transferSku, setTransferSku] = useState('');
  const [transferFrom, setTransferFrom] = useState('');

  const filtered = store.inventory.filter(i =>
    i.sku.toLowerCase().includes(store.searchQuery.toLowerCase()) ||
    i.name.toLowerCase().includes(store.searchQuery.toLowerCase()) ||
    i.binLocation.toLowerCase().includes(store.searchQuery.toLowerCase()) ||
    i.category.toLowerCase().includes(store.searchQuery.toLowerCase())
  );

  return (
    <div className="inventory-layout">
      {/* Search + Receipt button */}
      <div className="inventory-toolbar">
        <div className="search-wrap">
          <span className="search-icon">🔍</span>
          <input className="input search-input" placeholder="Search SKUs, bins, or names…" value={store.searchQuery} onChange={e => store.setSearchQuery(e.target.value)} />
        </div>
        <button className="btn btn-primary" onClick={() => setShowReceipt(true)}>+ Goods Receipt</button>
      </div>

      <div className="inventory-body">
        {/* Item Grid */}
        <div className="inventory-grid">
          {filtered.length === 0 ? (
            <div className="empty-state">No items match your search</div>
          ) : (
            filtered.map(item => {
              const low = item.quantity <= item.reorderPoint;
              return (
                <div key={item.sku} className={`inv-card ${low ? 'inv-low' : ''}`}>
                  <div className="inv-card-top">
                    <span className="inv-category">{item.category}</span>
                    <span className="inv-bin">{item.binLocation}</span>
                  </div>
                  <div className="inv-name">{item.name}</div>
                  <div className="inv-sku">SKU: {item.sku}</div>
                  <div className="inv-specs">
                    <span>{item.weightLb} lbs</span>
                    <span className={item.safetyClass.includes('Hazard') ? 'hazard-txt' : ''}>{item.safetyClass}</span>
                  </div>
                  <div className={`inv-stock-row ${low ? 'low-bg' : 'ok-bg'}`}>
                    <div>
                      <div className="inv-stock-label">{low ? '⚠️ Stock Level' : 'Stock Level'}</div>
                      <div className={`inv-stock-val ${low ? 'error-txt' : ''}`}>{item.quantity} {item.unit}</div>
                    </div>
                    {low
                      ? <span className="badge badge-error">Low (ROP: {item.reorderPoint})</span>
                      : <span className="inv-ok">✅ Optimal ({item.lastUpdated})</span>}
                  </div>
                  <div className="inv-actions">
                    <button className="btn btn-sm btn-ghost" title="Transfer Bin" onClick={() => { setTransferSku(item.sku); setTransferFrom(item.binLocation); }}>⇄ Transfer</button>
                    <div className="qty-btns">
                      <button className="btn btn-sm btn-outline qty-btn" onClick={() => store.adjustStock(item.sku, -1)}>−</button>
                      <button className="btn btn-sm btn-outline qty-btn" onClick={() => store.adjustStock(item.sku, 1)}>+</button>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Zone Capacities */}
        <div className="zones-pane">
          <h3 className="pane-title">Zone Capacities (EWM)</h3>
          {store.zones.map(z => {
            const color = z.fillPercentage >= 85 ? '#bb0000' : z.fillPercentage >= 70 ? '#d05900' : '#107e3e';
            return (
              <div key={z.name} className="zone-row">
                <div className="zone-header">
                  <span className="zone-name">{z.name}</span>
                  <span className="zone-stats">{z.occupied}/{z.total} bins ({z.fillPercentage}%)</span>
                </div>
                <div className="zone-bar-bg">
                  <div className="zone-bar-fill" style={{ width: `${z.fillPercentage}%`, background: color }} />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Goods Receipt Dialog */}
      {showReceipt && <GoodsReceiptDialog store={store} onClose={() => setShowReceipt(false)} />}

      {/* Transfer Dialog */}
      {transferSku && <TransferDialog sku={transferSku} from={transferFrom} store={store} onClose={() => { setTransferSku(''); setTransferFrom(''); }} />}
    </div>
  );
}

function GoodsReceiptDialog({ store, onClose }: { store: Store; onClose: () => void }) {
  const [sku, setSku] = useState('');
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const [qty, setQty] = useState('');
  const [category, setCategory] = useState('Standard');

  const submit = () => {
    store.addInventory(sku, name, location, parseInt(qty) || 25, category);
    onClose();
  };

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog" onClick={e => e.stopPropagation()}>
        <h2 className="dialog-title">Post Goods Receipt (Inbound)</h2>
        <p className="dialog-sub">Declare newly arrived products into the warehouse system.</p>
        <div className="dialog-fields">
          <label>Product Code (SKU)<input className="input" value={sku} onChange={e => setSku(e.target.value)} placeholder="SKU-XXXX-XX" /></label>
          <label>Product Label / Description<input className="input" value={name} onChange={e => setName(e.target.value)} placeholder="Premium Core Module V2" /></label>
          <div className="dialog-row">
            <label>Target Bin<input className="input" value={location} onChange={e => setLocation(e.target.value)} placeholder="A-04-12" /></label>
            <label>Quantity<input className="input" type="number" value={qty} onChange={e => setQty(e.target.value)} /></label>
          </div>
          <label>Category
            <select className="input" value={category} onChange={e => setCategory(e.target.value)}>
              {['Standard', 'Electronics', 'Batteries', 'Perishables', 'Safety Gear'].map(c => <option key={c}>{c}</option>)}
            </select>
          </label>
        </div>
        <div className="dialog-actions">
          <button className="btn btn-ghost" onClick={onClose}>Reject</button>
          <button className="btn btn-primary" onClick={submit}>Commit Post</button>
        </div>
      </div>
    </div>
  );
}

function TransferDialog({ sku, from, store, onClose }: { sku: string; from: string; store: Store; onClose: () => void }) {
  const [to, setTo] = useState('');
  const submit = () => {
    if (to.trim()) { store.transferBin(sku, from, to.trim().toUpperCase()); }
    onClose();
  };
  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog" onClick={e => e.stopPropagation()}>
        <h2 className="dialog-title">Warehouse Internal Stock Transfer</h2>
        <p className="dialog-sub">Relocating SKU <strong>{sku}</strong> from Bin <strong>{from}</strong>.</p>
        <div className="dialog-fields">
          <label>Target Bin Code<input className="input" value={to} onChange={e => setTo(e.target.value)} placeholder="e.g. B-12-05" /></label>
        </div>
        <div className="dialog-actions">
          <button className="btn btn-ghost" onClick={onClose}>Abort</button>
          <button className="btn btn-primary" onClick={submit}>Confirm Transfer</button>
        </div>
      </div>
    </div>
  );
}
