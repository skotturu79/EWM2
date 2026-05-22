package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Domain Models ---

enum class DoorStatus {
    AVAILABLE, // Gate Clear (Success / Green)
    DOCKED,    // Truck Attached (Informative / Blue)
    LOADING,   // Active Loading (Critical / Orange)
    BLOCKED    // Issue / Maintenance (Negative / Red)
}

data class DockDoor(
    val id: String,
    val name: String,
    val status: DoorStatus,
    val carrier: String = "",
    val licensePlate: String = "",
    val progress: Int = 0, // 0 to 100
    val totalPallets: Int = 0,
    val loadedPallets: Int = 0,
    val lockState: Boolean = true, // locked
    val temperatureControl: Boolean = false,
    val temperatureF: Double = 68.0,
    val securitySeal: String = "S-94819",
    val arrivalTime: String = "",
    val priority: String = "Medium" // Low, Medium, High
)

data class InventoryItem(
    val sku: String,
    val name: String,
    val binLocation: String,
    val quantity: Int,
    val reorderPoint: Int,
    val unit: String = "PL", // Pallets, Cases, Pieces
    val category: String = "Standard",
    val safetyClass: String = "Non-Haz",
    val weightLb: Double = 1200.0,
    val lastUpdated: String = "Just Now"
)

data class WarehouseZone(
    val name: String,
    val occupied: Int,
    val total: Int,
    val fillPercentage: Int
)

// --- ViewModel ---

class EwmViewModel : ViewModel() {

    // --- State Holders ---

    private val _doors = MutableStateFlow<List<DockDoor>>(emptyList())
    val doors: StateFlow<List<DockDoor>> = _doors.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _zones = MutableStateFlow<List<WarehouseZone>>(emptyList())
    val zones: StateFlow<List<WarehouseZone>> = _zones.asStateFlow()

    // Filter/Search parameters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDoorId = MutableStateFlow<String?>(null)
    val selectedDoorId: StateFlow<String?> = _selectedDoorId.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private var simulationJob: Job? = null

    // Logging/Audit log
    private val _auditLogs = MutableStateFlow<List<String>>(emptyList())
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        // SAP EWM Warehouse Doors setup
        val initialDoors = listOf(
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
                temperatureF = -4.0, // Frozen foods
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

        val initialInventory = listOf(
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
                reorderPoint = 15, // Low stock indicator
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

        val initialZones = listOf(
            WarehouseZone("Cold Aisle (Zone R)", 124, 150, 82),
            WarehouseZone("Bulk Storage (Zone B)", 410, 600, 68),
            WarehouseZone("High Rack A (Zone A)", 89, 100, 89),
            WarehouseZone("Standard Mezzanine (Zone C)", 110, 200, 55)
        )

        _doors.value = initialDoors
        _inventory.value = initialInventory
        _zones.value = initialZones
        _selectedDoorId.value = "door-101" // select first door by default

        logMessage("System initialized. Loaded 5 dock doors and 6 active stock entries.")
    }

    // --- Actions ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectDoor(doorId: String) {
        _selectedDoorId.value = doorId
    }

    fun toggleDoorSecurityLock(doorId: String) {
        _doors.update { list ->
            list.map { door ->
                if (door.id == doorId) {
                    val newState = !door.lockState
                    logMessage("${door.name} dock lock toggled to ${if (newState) "LOCKED" else "UNLOCKED"}.")
                    door.copy(lockState = newState)
                } else door
            }
        }
    }

    fun changeDoorStatus(doorId: String, newStatus: DoorStatus) {
        _doors.update { list ->
            list.map { door ->
                if (door.id == doorId) {
                    logMessage("${door.name} status updated from ${door.status} to $newStatus.")
                    // Set default settings if changing status
                    if (newStatus == DoorStatus.AVAILABLE) {
                        door.copy(
                            status = newStatus,
                            carrier = "",
                            licensePlate = "",
                            progress = 0,
                            loadedPallets = 0,
                            totalPallets = 0,
                            securitySeal = "N/A"
                        )
                    } else if (door.status == DoorStatus.AVAILABLE && newStatus == DoorStatus.DOCKED) {
                        door.copy(
                            status = newStatus,
                            carrier = "Interstate Logistics",
                            licensePlate = "TX-883K",
                            progress = 0,
                            loadedPallets = 0,
                            totalPallets = 24,
                            arrivalTime = currentTimeString(),
                            securitySeal = "SL-" + (10000..99999).random()
                        )
                    } else {
                        door.copy(status = newStatus)
                    }
                } else door
            }
        }
    }

    fun dockTruck(
        doorId: String,
        carrier: String,
        licensePlate: String,
        totalPallets: Int,
        priority: String,
        tempControl: Boolean,
        tempF: Double
    ) {
        _doors.update { list ->
            list.map { door ->
                if (door.id == doorId) {
                    logMessage("Docked Truck at ${door.name}. Carrier: $carrier, Plate: $licensePlate, Pallets: $totalPallets.")
                    door.copy(
                        status = DoorStatus.DOCKED,
                        carrier = carrier.ifBlank { "Carrier S1" },
                        licensePlate = licensePlate.ifBlank { "PL-9901" },
                        totalPallets = if (totalPallets <= 0) 20 else totalPallets,
                        loadedPallets = 0,
                        progress = 0,
                        lockState = true,
                        priority = priority,
                        temperatureControl = tempControl,
                        temperatureF = tempF,
                        arrivalTime = currentTimeString(),
                        securitySeal = "SL-" + (10000..99999).random()
                    )
                } else door
            }
        }
    }

    fun serviceActionLoadPallet(doorId: String) {
        _doors.update { list ->
            list.map { door ->
                if (door.id == doorId && door.status == DoorStatus.LOADING) {
                    val nextLoaded = (door.loadedPallets + 1).coerceAtMost(door.totalPallets)
                    val nextProgress = if (door.totalPallets > 0) (nextLoaded * 100) / door.totalPallets else 100
                    
                    if (nextLoaded == door.totalPallets && door.loadedPallets != door.totalPallets) {
                        logMessage("Loading COMPLETED for ${door.name} (${door.carrier}).")
                        door.copy(
                            loadedPallets = nextLoaded,
                            progress = 100,
                            status = DoorStatus.DOCKED
                        )
                    } else {
                        logMessage("Loaded pallet on ${door.name}: $nextLoaded / ${door.totalPallets}.")
                        door.copy(
                            loadedPallets = nextLoaded,
                            progress = nextProgress
                        )
                    }
                } else door
            }
        }
    }

    fun adjustInventoryStock(sku: String, delta: Int) {
        _inventory.update { list ->
            list.map { item ->
                if (item.sku == sku) {
                    val nextQty = (item.quantity + delta).coerceAtLeast(0)
                    logMessage("Inventory stock adjusted for ${item.sku} ($delta). New stock: $nextQty.")
                    item.copy(quantity = nextQty, lastUpdated = "Just Now")
                } else item
            }
        }
    }

    fun addInventoryItem(sku: String, name: String, location: String, quantity: Int, category: String) {
        val newItem = InventoryItem(
            sku = sku.ifBlank { "SKU-" + (1000..9999).random() + "-XX" },
            name = name.ifBlank { "Standard Cargo Box" },
            binLocation = location.ifBlank { "B-01-01" },
            quantity = if (quantity < 0) 10 else quantity,
            reorderPoint = 20,
            category = category.ifBlank { "Standard" },
            unit = "PC",
            lastUpdated = currentTimeString()
        )
        _inventory.update { listOf(newItem) + it }
        logMessage("Post Goods Receipt compiled. Sku ${newItem.sku} placed in ${newItem.binLocation}.")
    }

    fun transferBin(sku: String, originBin: String, targetBin: String) {
        _inventory.update { list ->
            list.map { item ->
                if (item.sku == sku && item.binLocation == originBin) {
                    logMessage("Stock Transfer verified: ${item.sku} moved from $originBin to $targetBin.")
                    item.copy(binLocation = targetBin, lastUpdated = "Moved")
                } else item
            }
        }
    }

    // --- Loading progress simulator ---

    fun toggleAutomaticSimulation() {
        if (_isSimulating.value) {
            stopSimulation()
        } else {
            startSimulation()
        }
    }

    private fun startSimulation() {
        _isSimulating.value = true
        logMessage("SAP EWM Real-time automation pipeline CONNECTED.")
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1800) // update every 1.8 seconds to feel fast and engaging

                var statusChanged = false
                _doors.update { list ->
                    list.map { door ->
                        if (door.status == DoorStatus.LOADING) {
                            statusChanged = true
                            val nextLoaded = (door.loadedPallets + 1).coerceAtMost(door.totalPallets)
                            val nextProgress = if (door.totalPallets > 0) (nextLoaded * 100) / door.totalPallets else 100

                            if (nextLoaded == door.totalPallets) {
                                logMessage("Auto-simulation: cargo loading finalized for ${door.name}.")
                                door.copy(
                                    loadedPallets = nextLoaded,
                                    progress = 100,
                                    status = DoorStatus.DOCKED // Goes back to finished docking state
                                )
                            } else {
                                door.copy(
                                    loadedPallets = nextLoaded,
                                    progress = nextProgress
                                )
                            }
                        } else if (door.status == DoorStatus.DOCKED && door.progress == 100) {
                            // Automatically release and dock next after a while to make it look alive!
                            // Keep it simple, just let it process.
                            door
                        } else {
                            door
                        }
                    }
                }
                if (statusChanged) {
                    // Update matching zone details slightly
                    _zones.update { zones ->
                        zones.map { zone ->
                            if (zone.name.contains("Bulk")) {
                                val occupied = (zone.occupied + (1..3).random()).coerceIn(0, zone.total)
                                zone.copy(
                                    occupied = occupied,
                                    fillPercentage = (occupied * 100) / zone.total
                                )
                            } else zone
                        }
                    }
                }
            }
        }
    }

    fun stopSimulation() {
        _isSimulating.value = false
        simulationJob?.cancel()
        simulationJob = null
        logMessage("SAP EWM Real-time automation pipeline PAUSED.")
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }

    // --- Helper utilities ---

    private fun logMessage(message: String) {
        val currTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _auditLogs.update { list ->
            listOf("[$currTime] $message") + list.take(15) // limit to past 16 items
        }
    }

    private fun currentTimeString(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }
}
