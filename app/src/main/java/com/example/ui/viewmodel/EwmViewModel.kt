package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EwmRepository
import kotlinx.coroutines.Job
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
    val progress: Int = 0,
    val totalPallets: Int = 0,
    val loadedPallets: Int = 0,
    val lockState: Boolean = true,
    val temperatureControl: Boolean = false,
    val temperatureF: Double = 68.0,
    val securitySeal: String = "S-94819",
    val arrivalTime: String = "",
    val priority: String = "Medium"
)

data class InventoryItem(
    val sku: String,
    val name: String,
    val binLocation: String,
    val quantity: Int,
    val reorderPoint: Int,
    val unit: String = "PL",
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

    // --- Dependencies ---
    private val repository = EwmRepository()

    // --- State Holders ---

    private val _doors = MutableStateFlow<List<DockDoor>>(emptyList())
    val doors: StateFlow<List<DockDoor>> = _doors.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _zones = MutableStateFlow<List<WarehouseZone>>(emptyList())
    val zones: StateFlow<List<WarehouseZone>> = _zones.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDoorId = MutableStateFlow<String?>(null)
    val selectedDoorId: StateFlow<String?> = _selectedDoorId.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _isDataLoading = MutableStateFlow(true)
    val isDataLoading: StateFlow<Boolean> = _isDataLoading.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<String>>(emptyList())
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    private var simulationJob: Job? = null

    init {
        loadDataFromRepository()
    }

    // ---------------------------------------------------------------------------
    // Data Loading via Repository
    // ---------------------------------------------------------------------------

    private fun loadDataFromRepository() {
        viewModelScope.launch {
            _isDataLoading.value = true

            // Load all three data sources concurrently
            val doorsJob = launch {
                repository.fetchDockDoors()
                    .onSuccess { _doors.value = it }
                    .onFailure { logMessage("Failed to load dock doors: ${it.message}") }
            }
            val inventoryJob = launch {
                repository.fetchInventory()
                    .onSuccess { _inventory.value = it }
                    .onFailure { logMessage("Failed to load inventory: ${it.message}") }
            }
            val zonesJob = launch {
                repository.fetchZones()
                    .onSuccess { _zones.value = it }
                    .onFailure { logMessage("Failed to load zones: ${it.message}") }
            }

            doorsJob.join()
            inventoryJob.join()
            zonesJob.join()

            _isDataLoading.value = false
            _selectedDoorId.value = _doors.value.firstOrNull()?.id
            logMessage("System initialized. Loaded ${_doors.value.size} dock doors and ${_inventory.value.size} active stock entries.")
        }
    }

    // ---------------------------------------------------------------------------
    // Dock Door Actions
    // ---------------------------------------------------------------------------

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
                        door.copy(loadedPallets = nextLoaded, progress = 100, status = DoorStatus.DOCKED)
                    } else {
                        logMessage("Loaded pallet on ${door.name}: $nextLoaded / ${door.totalPallets}.")
                        door.copy(loadedPallets = nextLoaded, progress = nextProgress)
                    }
                } else door
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Inventory Actions
    // ---------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------
    // Live Simulation
    // ---------------------------------------------------------------------------

    fun toggleAutomaticSimulation() {
        if (_isSimulating.value) stopSimulation() else startSimulation()
    }

    private fun startSimulation() {
        _isSimulating.value = true
        logMessage("SAP EWM Real-time automation pipeline CONNECTED.")
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1800)

                var statusChanged = false
                _doors.update { list ->
                    list.map { door ->
                        if (door.status == DoorStatus.LOADING) {
                            statusChanged = true
                            val nextLoaded = (door.loadedPallets + 1).coerceAtMost(door.totalPallets)
                            val nextProgress = if (door.totalPallets > 0) (nextLoaded * 100) / door.totalPallets else 100

                            if (nextLoaded == door.totalPallets) {
                                logMessage("Auto-simulation: cargo loading finalized for ${door.name}.")
                                door.copy(loadedPallets = nextLoaded, progress = 100, status = DoorStatus.DOCKED)
                            } else {
                                door.copy(loadedPallets = nextLoaded, progress = nextProgress)
                            }
                        } else door
                    }
                }

                if (statusChanged) {
                    _zones.update { zones ->
                        zones.map { zone ->
                            if (zone.name.contains("Bulk")) {
                                val occupied = (zone.occupied + (1..3).random()).coerceIn(0, zone.total)
                                zone.copy(occupied = occupied, fillPercentage = (occupied * 100) / zone.total)
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

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun logMessage(message: String) {
        val currTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _auditLogs.update { list ->
            listOf("[$currTime] $message") + list.take(15)
        }
    }

    private fun currentTimeString(): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
}
