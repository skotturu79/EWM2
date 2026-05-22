import { useState, useEffect, useRef, useCallback } from 'react';
import type { DockDoor, InventoryItem, WarehouseZone, AuditLog, DoorStatus } from './types';
import { initialDoors, initialInventory, initialZones } from './mockData';

function nowTime() {
  return new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export function useWarehouseStore() {
  const [doors, setDoors] = useState<DockDoor[]>(initialDoors);
  const [inventory, setInventory] = useState<InventoryItem[]>(initialInventory);
  const [zones, setZones] = useState<WarehouseZone[]>(initialZones);
  const [selectedDoorId, setSelectedDoorId] = useState<string>('door-101');
  const [searchQuery, setSearchQuery] = useState('');
  const [isSimulating, setIsSimulating] = useState(false);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([
    { time: nowTime(), message: 'System initialized. Loaded 5 dock doors and 6 active stock entries.' }
  ]);
  const simRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const log = useCallback((message: string) => {
    setAuditLogs(prev => [{ time: nowTime(), message }, ...prev.slice(0, 15)]);
  }, []);

  const toggleSimulation = useCallback(() => {
    setIsSimulating(prev => !prev);
  }, []);

  useEffect(() => {
    if (isSimulating) {
      log('SAP EWM Real-time automation pipeline CONNECTED.');
      simRef.current = setInterval(() => {
        setDoors(prev => prev.map(door => {
          if (door.status === 'LOADING') {
            const next = Math.min(door.loadedPallets + 1, door.totalPallets);
            const prog = door.totalPallets > 0 ? Math.round((next / door.totalPallets) * 100) : 100;
            if (next === door.totalPallets) {
              log(`Auto-simulation: cargo loading finalized for ${door.name}.`);
              return { ...door, loadedPallets: next, progress: 100, status: 'DOCKED' };
            }
            return { ...door, loadedPallets: next, progress: prog };
          }
          return door;
        }));
        setZones(prev => prev.map(z =>
          z.name.includes('Bulk')
            ? (() => { const occ = Math.min(z.occupied + Math.ceil(Math.random() * 3), z.total); return { ...z, occupied: occ, fillPercentage: Math.round((occ / z.total) * 100) }; })()
            : z
        ));
      }, 1800);
    } else {
      if (simRef.current) { clearInterval(simRef.current); simRef.current = null; }
      log('SAP EWM Real-time automation pipeline PAUSED.');
    }
    return () => { if (simRef.current) clearInterval(simRef.current); };
  }, [isSimulating, log]);

  const toggleLock = useCallback((id: string) => {
    setDoors(prev => prev.map(d => {
      if (d.id !== id) return d;
      log(`${d.name} dock lock toggled to ${!d.lockState ? 'LOCKED' : 'UNLOCKED'}.`);
      return { ...d, lockState: !d.lockState };
    }));
  }, [log]);

  const changeStatus = useCallback((id: string, status: DoorStatus) => {
    setDoors(prev => prev.map(d => {
      if (d.id !== id) return d;
      log(`${d.name} status updated to ${status}.`);
      if (status === 'AVAILABLE') return { ...d, status, carrier: '', licensePlate: '', progress: 0, loadedPallets: 0, totalPallets: 0, securitySeal: 'N/A' };
      return { ...d, status };
    }));
  }, [log]);

  const dockTruck = useCallback((id: string, carrier: string, plate: string, pallets: number, priority: string, tempControl: boolean, tempF: number) => {
    setDoors(prev => prev.map(d => {
      if (d.id !== id) return d;
      log(`Docked Truck at ${d.name}. Carrier: ${carrier}, Plate: ${plate}, Pallets: ${pallets}.`);
      return { ...d, status: 'DOCKED', carrier: carrier || 'Carrier S1', licensePlate: plate || 'PL-9901', totalPallets: pallets || 20, loadedPallets: 0, progress: 0, lockState: true, priority, temperatureControl: tempControl, temperatureF: tempF, arrivalTime: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }), securitySeal: `SL-${Math.floor(10000 + Math.random() * 90000)}` };
    }));
  }, [log]);

  const loadPallet = useCallback((id: string) => {
    setDoors(prev => prev.map(d => {
      if (d.id !== id || d.status !== 'LOADING') return d;
      const next = Math.min(d.loadedPallets + 1, d.totalPallets);
      const prog = d.totalPallets > 0 ? Math.round((next / d.totalPallets) * 100) : 100;
      if (next === d.totalPallets) { log(`Loading COMPLETED for ${d.name}.`); return { ...d, loadedPallets: next, progress: 100, status: 'DOCKED' }; }
      log(`Loaded pallet on ${d.name}: ${next}/${d.totalPallets}.`);
      return { ...d, loadedPallets: next, progress: prog };
    }));
  }, [log]);

  const adjustStock = useCallback((sku: string, delta: number) => {
    setInventory(prev => prev.map(i => {
      if (i.sku !== sku) return i;
      const qty = Math.max(0, i.quantity + delta);
      log(`Inventory adjusted for ${sku} (${delta > 0 ? '+' : ''}${delta}). New: ${qty}.`);
      return { ...i, quantity: qty, lastUpdated: 'Just Now' };
    }));
  }, [log]);

  const addInventory = useCallback((sku: string, name: string, location: string, quantity: number, category: string) => {
    const item: InventoryItem = { sku: sku || `SKU-${Math.floor(1000 + Math.random() * 9000)}-XX`, name: name || 'Standard Cargo', binLocation: location || 'B-01-01', quantity: quantity || 10, reorderPoint: 20, unit: 'PC', category: category || 'Standard', safetyClass: 'Non-Haz', weightLb: 500, lastUpdated: 'Just Now' };
    setInventory(prev => [item, ...prev]);
    log(`Goods Receipt posted. SKU ${item.sku} placed in ${item.binLocation}.`);
  }, [log]);

  const transferBin = useCallback((sku: string, from: string, to: string) => {
    setInventory(prev => prev.map(i => {
      if (i.sku !== sku || i.binLocation !== from) return i;
      log(`Stock Transfer: ${sku} moved from ${from} to ${to}.`);
      return { ...i, binLocation: to, lastUpdated: 'Moved' };
    }));
  }, [log]);

  return { doors, inventory, zones, selectedDoorId, setSelectedDoorId, searchQuery, setSearchQuery, isSimulating, toggleSimulation, auditLogs, toggleLock, changeStatus, dockTruck, loadPallet, adjustStock, addInventory, transferBin };
}
