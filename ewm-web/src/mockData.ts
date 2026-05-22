import type { DockDoor, InventoryItem, WarehouseZone } from './types';

export const initialDoors: DockDoor[] = [
  {
    id: 'door-101', name: 'Gate D-101', status: 'LOADING',
    carrier: 'DHL Freight', licensePlate: 'TX-298F',
    progress: 68, totalPallets: 30, loadedPallets: 20,
    lockState: true, temperatureControl: true, temperatureF: -4,
    securitySeal: 'SL-91048', arrivalTime: '08:15 AM', priority: 'High',
  },
  {
    id: 'door-102', name: 'Gate D-102', status: 'DOCKED',
    carrier: 'FedEx Custom Critical', licensePlate: 'CA-081B',
    progress: 0, totalPallets: 18, loadedPallets: 0,
    lockState: true, temperatureControl: false, temperatureF: 68,
    securitySeal: 'SL-49219', arrivalTime: '11:30 AM', priority: 'Medium',
  },
  {
    id: 'door-103', name: 'Gate D-103', status: 'AVAILABLE',
    carrier: '', licensePlate: '', progress: 0, totalPallets: 0, loadedPallets: 0,
    lockState: false, temperatureControl: false, temperatureF: 68,
    securitySeal: 'N/A', arrivalTime: '', priority: 'Low',
  },
  {
    id: 'door-104', name: 'Gate D-104', status: 'AVAILABLE',
    carrier: '', licensePlate: '', progress: 0, totalPallets: 0, loadedPallets: 0,
    lockState: false, temperatureControl: false, temperatureF: 68,
    securitySeal: 'N/A', arrivalTime: '', priority: 'Low',
  },
  {
    id: 'door-105', name: 'Gate D-105', status: 'BLOCKED',
    carrier: '', licensePlate: '', progress: 0, totalPallets: 0, loadedPallets: 0,
    lockState: true, temperatureControl: false, temperatureF: 68,
    securitySeal: 'LOCKOUT-991', arrivalTime: '', priority: 'Low',
  },
];

export const initialInventory: InventoryItem[] = [
  { sku: 'SKU-9904-EL', name: 'Horizon Microcircuits Module 4', binLocation: 'A-04-12', quantity: 142, reorderPoint: 30, unit: 'PC', category: 'Electronics', safetyClass: 'Non-Haz', weightLb: 150, lastUpdated: 'Today' },
  { sku: 'SKU-2081-SM', name: 'Lithium-Ion Pack 54V 10AH', binLocation: 'A-12-04', quantity: 45, reorderPoint: 15, unit: 'PL', category: 'Batteries', safetyClass: 'Hazardous Class 9', weightLb: 1870, lastUpdated: 'Today' },
  { sku: 'SKU-7740-ST', name: 'Standard M12 Carbon Steel Bolts', binLocation: 'B-03-22', quantity: 1500, reorderPoint: 500, unit: 'PC', category: 'Fasteners', safetyClass: 'Non-Haz', weightLb: 920, lastUpdated: 'Today' },
  { sku: 'SKU-1025-CD', name: 'Organic Dairy Mix (Cold Chain)', binLocation: 'R-01-08', quantity: 24, reorderPoint: 8, unit: 'PL', category: 'Perishables', safetyClass: 'Temperature Controlled', weightLb: 1200, lastUpdated: 'Today' },
  { sku: 'SKU-3104-PV', name: 'Industrial High-Pressure Valves', binLocation: 'C-08-11', quantity: 12, reorderPoint: 15, unit: 'PC', category: 'Valves', safetyClass: 'Non-Haz', weightLb: 340, lastUpdated: 'Today' },
  { sku: 'SKU-4402-TX', name: 'Protective Industrial Helmets (Blue)', binLocation: 'D-15-02', quantity: 350, reorderPoint: 100, unit: 'PC', category: 'Safety Gear', safetyClass: 'Non-Haz', weightLb: 410, lastUpdated: 'Today' },
];

export const initialZones: WarehouseZone[] = [
  { name: 'Cold Aisle (Zone R)', occupied: 124, total: 150, fillPercentage: 82 },
  { name: 'Bulk Storage (Zone B)', occupied: 410, total: 600, fillPercentage: 68 },
  { name: 'High Rack A (Zone A)', occupied: 89, total: 100, fillPercentage: 89 },
  { name: 'Standard Mezzanine (Zone C)', occupied: 110, total: 200, fillPercentage: 55 },
];
