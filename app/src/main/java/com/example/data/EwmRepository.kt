package com.example.data

import com.example.ui.viewmodel.DockDoor
import com.example.ui.viewmodel.DoorStatus
import com.example.ui.viewmodel.InventoryItem
import com.example.ui.viewmodel.WarehouseZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * EwmRepository is the single source of truth for warehouse data.
 *
 * Architecture:
 * - Currently backed by [MockEwmDataSource] which returns realistic SAP EWM sample data.
 * - Replace MockEwmDataSource calls with EwmApiService (Retrofit) calls to connect to
 *   a real SAP EWM OData endpoint (e.g. /sap/opu/odata/sap/EWM_WAREHOUSEORDER_SRV/).
 * - Room database caching can be layered in between for offline support.
 *
 * Usage: Inject or instantiate in [com.example.ui.viewmodel.EwmViewModel].
 */
class EwmRepository {

    /**
     * Loads initial dock door state.
     * Simulates a network round-trip (≈400 ms) that a real SAP EWM API call would take.
     * Replace [MockEwmDataSource.doors] with an EwmApiService.getDockDoors() call.
     */
    suspend fun fetchDockDoors(): Result<List<DockDoor>> = withContext(Dispatchers.IO) {
        runCatching {
            delay(400) // Simulate network latency
            MockEwmDataSource.doors
        }
    }

    /**
     * Loads inventory / stock data.
     * Replace with EwmApiService.getInventory() pointing to an SAP EWM OData Materials service.
     */
    suspend fun fetchInventory(): Result<List<InventoryItem>> = withContext(Dispatchers.IO) {
        runCatching {
            delay(350)
            MockEwmDataSource.inventory
        }
    }

    /**
     * Loads warehouse zone capacity data.
     * Replace with EwmApiService.getZones() pointing to an SAP EWM storage-type service.
     */
    suspend fun fetchZones(): Result<List<WarehouseZone>> = withContext(Dispatchers.IO) {
        runCatching {
            delay(200)
            MockEwmDataSource.zones
        }
    }
}

// ---------------------------------------------------------------------------
// Mock Data Source
// Mirrors the data structure that a real SAP EWM OData API (v4) would return.
// Field names align with SAP EWM entity naming conventions where possible.
// ---------------------------------------------------------------------------
object MockEwmDataSource {

    val doors: List<DockDoor> = listOf(
        DockDoor(
            id = "door-101",
            name = "Gate D-101",
            status = DoorStatus.LOADING,
            carrier = "DHL Freight",
            licensePlate = "TX-298F",
            progress = 68,
            totalPallets = 30,
            loadedPallets = 20,
            lockState = true,
            temperatureControl = true,
            temperatureF = -4.0,
            securitySeal = "SL-91048",
            arrivalTime = "08:15 AM",
            priority = "High"
        ),
        DockDoor(
            id = "door-102",
            name = "Gate D-102",
            status = DoorStatus.DOCKED,
            carrier = "FedEx Custom Critical",
            licensePlate = "CA-081B",
            progress = 0,
            totalPallets = 18,
            loadedPallets = 0,
            lockState = true,
            temperatureControl = false,
            securitySeal = "SL-49219",
            arrivalTime = "11:30 AM",
            priority = "Medium"
        ),
        DockDoor(
            id = "door-103",
            name = "Gate D-103",
            status = DoorStatus.AVAILABLE,
            lockState = false,
            securitySeal = "N/A"
        ),
        DockDoor(
            id = "door-104",
            name = "Gate D-104",
            status = DoorStatus.AVAILABLE,
            lockState = false,
            securitySeal = "N/A"
        ),
        DockDoor(
            id = "door-105",
            name = "Gate D-105",
            status = DoorStatus.BLOCKED,
            lockState = true,
            securitySeal = "LOCKOUT-991",
            priority = "Low"
        )
    )

    val inventory: List<InventoryItem> = listOf(
        InventoryItem(
            sku = "SKU-9904-EL",
            name = "Horizon Microcircuits Module 4",
            binLocation = "A-04-12",
            quantity = 142,
            reorderPoint = 30,
            unit = "PC",
            category = "Electronics",
            weightLb = 150.0
        ),
        InventoryItem(
            sku = "SKU-2081-SM",
            name = "Lithium-Ion Pack 54V 10AH",
            binLocation = "A-12-04",
            quantity = 45,
            reorderPoint = 15,
            unit = "PL",
            category = "Batteries",
            safetyClass = "Hazardous Class 9",
            weightLb = 1870.0
        ),
        InventoryItem(
            sku = "SKU-7740-ST",
            name = "Standard M12 Carbon Steel Bolts",
            binLocation = "B-03-22",
            quantity = 1500,
            reorderPoint = 500,
            unit = "PC",
            category = "Fasteners",
            weightLb = 920.0
        ),
        InventoryItem(
            sku = "SKU-1025-CD",
            name = "Organic Dairy Mix (Cold Chain)",
            binLocation = "R-01-08",
            quantity = 24,
            reorderPoint = 8,
            unit = "PL",
            category = "Perishables",
            safetyClass = "Temperature Controlled",
            weightLb = 1200.0
        ),
        InventoryItem(
            sku = "SKU-3104-PV",
            name = "Industrial High-Pressure Valves",
            binLocation = "C-08-11",
            quantity = 12,
            reorderPoint = 15,
            unit = "PC",
            category = "Valves",
            weightLb = 340.0
        ),
        InventoryItem(
            sku = "SKU-4402-TX",
            name = "Protective Industrial Helmets (Blue)",
            binLocation = "D-15-02",
            quantity = 350,
            reorderPoint = 100,
            unit = "PC",
            category = "Safety Gear",
            weightLb = 410.0
        )
    )

    val zones: List<WarehouseZone> = listOf(
        WarehouseZone("Cold Aisle (Zone R)", 124, 150, 82),
        WarehouseZone("Bulk Storage (Zone B)", 410, 600, 68),
        WarehouseZone("High Rack A (Zone A)", 89, 100, 89),
        WarehouseZone("Standard Mezzanine (Zone C)", 110, 200, 55)
    )
}
