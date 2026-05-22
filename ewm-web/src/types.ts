export type DoorStatus = 'AVAILABLE' | 'DOCKED' | 'LOADING' | 'BLOCKED';

export interface DockDoor {
  id: string;
  name: string;
  status: DoorStatus;
  carrier: string;
  licensePlate: string;
  progress: number;
  totalPallets: number;
  loadedPallets: number;
  lockState: boolean;
  temperatureControl: boolean;
  temperatureF: number;
  securitySeal: string;
  arrivalTime: string;
  priority: string;
}

export interface InventoryItem {
  sku: string;
  name: string;
  binLocation: string;
  quantity: number;
  reorderPoint: number;
  unit: string;
  category: string;
  safetyClass: string;
  weightLb: number;
  lastUpdated: string;
}

export interface WarehouseZone {
  name: string;
  occupied: number;
  total: number;
  fillPercentage: number;
}

export interface AuditLog {
  time: string;
  message: string;
}
