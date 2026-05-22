package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.FioriTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DockDoor
import com.example.ui.viewmodel.DoorStatus
import com.example.ui.viewmodel.EwmViewModel
import com.example.ui.viewmodel.InventoryItem
import com.example.ui.viewmodel.WarehouseZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            MyApplicationTheme(darkTheme = darkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EwmWarehouseDashboard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EwmWarehouseDashboard(
    modifier: Modifier = Modifier,
    viewModel: EwmViewModel = viewModel(),
    darkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {}
) {
    val doors by viewModel.doors.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val zones by viewModel.zones.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDoorId by viewModel.selectedDoorId.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    // Screen State
    var activeTab by remember { mutableStateOf(0) } // 0 = Dock Doors, 1 = Inventory Tracker, 2 = Audit Logs
    val selectedDoor = doors.find { it.id == selectedDoorId }

    // Dialog controllers
    var showDockDialog by remember { mutableStateOf(false) }
    var showGoodsReceiptDialog by remember { mutableStateOf(false) }
    var showTransferBinDialog by remember { mutableStateOf(false) }
    var transferItemSku by remember { mutableStateOf("") }
    var transferOriginBin by remember { mutableStateOf("") }

    // Responsive configuration
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 640

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. SAP Fiori Horizon Global Header ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Warehouse,
                                    contentDescription = "Warehouse Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SAP Extended Warehouse",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "EWM Central • Dallas South",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isSimulating) FioriTheme.statusColors.positive else FioriTheme.statusColors.neutral,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Simulated live line toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable { viewModel.toggleAutomaticSimulation() }
                                .testTag("simulation_toggle")
                        ) {
                            Icon(
                                imageVector = if (isSimulating) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Toggle Simulation",
                                tint = if (isSimulating) FioriTheme.statusColors.critical else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSimulating) "Live Stream" else "Simulation Off",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Light/Dark Theme selector button
                        IconButton(
                            onClick = onThemeToggle,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- 2. SAP Horizon Mobile App KPI / Propose Tiles List ---
                Text(
                    text = "Operational Insights (SAP Propose Tiles)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val occupiedDoorsCount = doors.count { it.status != DoorStatus.AVAILABLE }
                    val blockedDoorsCount = doors.count { it.status == DoorStatus.BLOCKED }
                    val totalDoorsCount = doors.size

                    // Tile 1: Gate Status Capacity
                    FioriKpiTile(
                        title = "Gate Capacity",
                        value = "$occupiedDoorsCount/$totalDoorsCount",
                        subTitle = "Gates Occupied",
                        statusText = "Sync Active",
                        statusColor = FioriTheme.statusColors.informative,
                        accentBrush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                FioriTheme.statusColors.informative
                            )
                        )
                    )

                    // Tile 2: Loading Throughput alerts
                    val loadingPercentage = if (doors.any { it.status == DoorStatus.LOADING }) {
                        doors.filter { it.status == DoorStatus.LOADING }.map { it.progress }.average().toInt()
                    } else 0
                    FioriKpiTile(
                        title = "Unloading Progress",
                        value = "$loadingPercentage%",
                        subTitle = "Avg active gate progress",
                        statusText = if (loadingPercentage > 0) "Operations in motion" else "Ready to load",
                        statusColor = if (loadingPercentage > 0) FioriTheme.statusColors.critical else FioriTheme.statusColors.neutral,
                        accentBrush = Brush.linearGradient(
                            colors = listOf(
                                FioriTheme.statusColors.critical,
                                FioriTheme.statusColors.critical.copy(alpha = 0.5f)
                            )
                        )
                    )

                    // Tile 3: Low Inventory count
                    val lowStockEntries = inventory.count { it.quantity <= it.reorderPoint }
                    FioriKpiTile(
                        title = "Stock Depletion",
                        value = "$lowStockEntries items",
                        subTitle = "Below reorder threshold",
                        statusText = if (lowStockEntries > 0) "Urgent replenish needed" else "Levels optimal",
                        statusColor = if (lowStockEntries > 0) FioriTheme.statusColors.negative else FioriTheme.statusColors.positive,
                        accentBrush = Brush.linearGradient(
                            colors = listOf(
                                FioriTheme.statusColors.negative,
                                FioriTheme.statusColors.negative.copy(alpha = 0.5f)
                            )
                        )
                    )

                    // Tile 4: Storage Fill Rate
                    val averageZoneFill = zones.map { it.fillPercentage }.average().toInt()
                    FioriKpiTile(
                        title = "Zone Capacity",
                        value = "$averageZoneFill%",
                        subTitle = "Total Bulk/Cold utilization",
                        statusText = "Safe limits verified",
                        statusColor = FioriTheme.statusColors.positive,
                        accentBrush = Brush.linearGradient(
                            colors = listOf(
                                FioriTheme.statusColors.positive,
                                FioriTheme.statusColors.informative
                            )
                        )
                    )
                }
            }
        }

        // --- 3. SAP Segmented Control Tabs Navigation ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            val tabLabels = listOf("Dock Doors", "Inventory Tracker", "Audit Feed")
            val tabIcons = listOf(
                Icons.Rounded.LocalShipping,
                Icons.Rounded.Inventory,
                Icons.Rounded.FormatListBulleted
            )

            tabLabels.forEachIndexed { index, title ->
                val selected = activeTab == index
                Button(
                    onClick = { activeTab = index },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(if (index == 0) "doors_tab_button" else if (index == 1) "inventory_tab_button" else "audit_tab_button"),
                    elevation = if (selected) ButtonDefaults.buttonElevation(defaultElevation = 1.dp) else null,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tabIcons[index],
                            contentDescription = title,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- 4. Main Tabbed Layout Container (Adaptive Grid / List + Details) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB: Dock Doors Gate Statuses
                    if (isTablet) {
                        // Two-Pane (Split Master-Detail Layout) for Tablet
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                DockDoorsListPane(
                                    doors = doors,
                                    selectedDoorId = selectedDoorId,
                                    onSelectDoor = { viewModel.selectDoor(it) }
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                            ) {
                                if (selectedDoor != null) {
                                    DockDoorDetailsCard(
                                        door = selectedDoor,
                                        onLockToggle = { viewModel.toggleDoorSecurityLock(it) },
                                        onStatusChange = { id, stat -> viewModel.changeDoorStatus(id, stat) },
                                        onLoadPallet = { viewModel.serviceActionLoadPallet(it) },
                                        onDockRequested = { showDockDialog = true }
                                    )
                                } else {
                                    EmptyStatePane("No Gate Selected", "Select a dock gate from the left list to view telemetry.")
                                }
                            }
                        }
                    } else {
                        // Single-Pane for Compact Phones (Toggle list vs details or stack vertically)
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                DockDoorsListPane(
                                    doors = doors,
                                    selectedDoorId = selectedDoorId,
                                    onSelectDoor = { viewModel.selectDoor(it) }
                                )
                            }
                            // Collapsible Details footer pane at the bottom
                            if (selectedDoor != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .background(Color.Transparent)
                                ) {
                                    DockDoorDetailsCard(
                                        door = selectedDoor,
                                        onLockToggle = { viewModel.toggleDoorSecurityLock(it) },
                                        onStatusChange = { id, stat -> viewModel.changeDoorStatus(id, stat) },
                                        onLoadPallet = { viewModel.serviceActionLoadPallet(it) },
                                        onDockRequested = { showDockDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB: Inventory Tracker
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search SKUs, coordinates, or cargo names...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showGoodsReceiptDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Goods Receipt", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Filter inventory based on search query
                        val filteredInventory = inventory.filter {
                            it.sku.contains(searchQuery, ignoreCase = true) ||
                                    it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.binLocation.contains(searchQuery, ignoreCase = true) ||
                                    it.category.contains(searchQuery, ignoreCase = true)
                        }

                        if (isTablet) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(modifier = Modifier.weight(1.2f)) {
                                    InventoryGridPane(
                                        items = filteredInventory,
                                        onAdjustQty = { sku, d -> viewModel.adjustInventoryStock(sku, d) },
                                        onTransferBinInitiated = { sku, bin ->
                                            transferItemSku = sku
                                            transferOriginBin = bin
                                            showTransferBinDialog = true
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(0.8f)) {
                                    WarehouseZonesPane(zones = zones)
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                InventoryGridPane(
                                    items = filteredInventory,
                                    onAdjustQty = { sku, d -> viewModel.adjustInventoryStock(sku, d) },
                                    onTransferBinInitiated = { sku, bin ->
                                        transferItemSku = sku
                                        transferOriginBin = bin
                                        showTransferBinDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // TAB: Audit Feed log list
                    AuditLogConsolePane(logs = auditLogs)
                }
            }
        }
    }

    // --- 5. Dialogs & Action Overlays ---

    // Docking New Carrier Dialog
    if (showDockDialog && selectedDoor != null) {
        var carrierInput by remember { mutableStateOf("") }
        var plateInput by remember { mutableStateOf("") }
        var totalPalletsInput by remember { mutableStateOf("24") }
        var selectedPriority by remember { mutableStateOf("Medium") }
        var isTemperatureControlled by remember { mutableStateOf(false) }
        var targetTemperature by remember { mutableStateOf("38") }

        Dialog(onDismissRequest = { showDockDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Register Carrier Docking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Assign a freight trailer to ${selectedDoor.name} to start logistics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = carrierInput,
                        onValueChange = { carrierInput = it },
                        label = { Text("Carrier Agency Name") },
                        placeholder = { Text("DHL, FedEx, UPS...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = plateInput,
                        onValueChange = { plateInput = it },
                        label = { Text("Trailer License ID") },
                        placeholder = { Text("TX-4921X") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = totalPalletsInput,
                            onValueChange = { totalPalletsInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Manifest Pallets") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trailer Priority", style = MaterialTheme.typography.labelSmall)
                            val priorities = listOf("Low", "Medium", "High")
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(selectedPriority)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    priorities.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p) },
                                            onClick = {
                                                selectedPriority = p
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTemperatureControlled,
                            onCheckedChange = { isTemperatureControlled = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refrigerated Cold-Chain Trailer", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (isTemperatureControlled) {
                        OutlinedTextField(
                            value = targetTemperature,
                            onValueChange = { targetTemperature = it },
                            label = { Text("Target Temperature (°F)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDockDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val pallets = totalPalletsInput.toIntOrNull() ?: 24
                                val tempDouble = targetTemperature.toDoubleOrNull() ?: 38.0
                                viewModel.dockTruck(
                                    doorId = selectedDoor.id,
                                    carrier = carrierInput,
                                    licensePlate = plateInput,
                                    totalPallets = pallets,
                                    priority = selectedPriority,
                                    tempControl = isTemperatureControlled,
                                    tempF = tempDouble
                                )
                                showDockDialog = false
                            },
                            modifier = Modifier.testTag("submit_dock_button")
                        ) {
                            Text("Dock Vessel")
                        }
                    }
                }
            }
        }
    }

    // Goods Receipt posting dialog
    if (showGoodsReceiptDialog) {
        var skuInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }
        var locationInput by remember { mutableStateOf("") }
        var quantityInput by remember { mutableStateOf("") }
        var categorySelected by remember { mutableStateOf("Standard") }

        Dialog(onDismissRequest = { showGoodsReceiptDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Post Goods Receipt (Inbound)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Declare and verify newly compiled products arriving into the warehouse system.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = skuInput,
                        onValueChange = { skuInput = it },
                        label = { Text("Product Code (SKU)") },
                        placeholder = { Text("SKU-XXXX-XX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Product Label / Description") },
                        placeholder = { Text("Premium Core Module V2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = locationInput,
                            onValueChange = { locationInput = it },
                            label = { Text("Target Bin Coordinates") },
                            placeholder = { Text("A-04-12") },
                            singleLine = true,
                            modifier = Modifier.weight(1.1f)
                        )

                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Stock Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Category Group", style = MaterialTheme.typography.labelSmall)
                        val categories = listOf("Standard", "Electronics", "Batteries", "Perishables", "Safety Gear")
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(categorySelected)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            categorySelected = cat
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showGoodsReceiptDialog = false }) {
                            Text("Reject")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val qty = quantityInput.toIntOrNull() ?: 25
                                viewModel.addInventoryItem(
                                    sku = skuInput,
                                    name = nameInput,
                                    location = locationInput,
                                    quantity = qty,
                                    category = categorySelected
                                )
                                showGoodsReceiptDialog = false
                            },
                            modifier = Modifier.testTag("submit_goods_receipt")
                        ) {
                            Text("Commit Post")
                        }
                    }
                }
            }
        }
    }

    // Material Stock bin-to-bin transfer transfer dialog
    if (showTransferBinDialog) {
        var targetBinInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showTransferBinDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Warehouse Internal Stock Transfer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "You are initiating a relocation order for standard SKU $transferItemSku. Moving from current Bin Location $transferOriginBin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = targetBinInput,
                        onValueChange = { targetBinInput = it },
                        label = { Text("Enter Target Bin Code") },
                        placeholder = { Text("e.g. B-12-05") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTransferBinDialog = false }) {
                            Text("Abort")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (targetBinInput.isNotBlank()) {
                                    viewModel.transferBin(
                                        sku = transferItemSku,
                                        originBin = transferOriginBin,
                                        targetBin = targetBinInput.trim().uppercase()
                                    )
                                }
                                showTransferBinDialog = false
                            },
                            modifier = Modifier.testTag("submit_transfer_bin")
                        ) {
                            Text("Confirm Transfer")
                        }
                    }
                }
            }
        }
    }
}

// --- Composable Subpanels & Custom Components ---

// SAP Fiori Horizon Custom KPI Propose Tile Card
@Composable
fun FioriKpiTile(
    title: String,
    value: String,
    subTitle: String,
    statusText: String,
    statusColor: Color,
    accentBrush: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .width(180.dp)
            .height(130.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp, 4.dp)
                            .background(accentBrush, RoundedCornerShape(2.dp))
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Dock Doors Master List View
@Composable
fun DockDoorsListPane(
    doors: List<DockDoor>,
    selectedDoorId: String?,
    onSelectDoor: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxSize(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Gate Status Feeds",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(doors) { door ->
                    val isSelected = door.id == selectedDoorId
                    DockDoorRow(
                        door = door,
                        isSelected = isSelected,
                        onClick = { onSelectDoor(door.id) }
                    )
                }
            }
        }
    }
}

// Dock Door List Row
@Composable
fun DockDoorRow(
    door: DockDoor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Determine colors based on status
    val statusColors = FioriTheme.statusColors
    val (statusLabel, statusColor, statusBg) = when (door.status) {
        DoorStatus.AVAILABLE -> Triple("Clear", statusColors.positive, statusColors.positiveBg)
        DoorStatus.DOCKED -> Triple("Docked", statusColors.informative, statusColors.informativeBg)
        DoorStatus.LOADING -> Triple("Loading", statusColors.critical, statusColors.criticalBg)
        DoorStatus.BLOCKED -> Triple("Blocked", statusColors.negative, statusColors.negativeBg)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("door_card_${door.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (door.status == DoorStatus.BLOCKED) Icons.Rounded.Warning else Icons.Rounded.LocalShipping,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = door.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (door.carrier.isNotBlank()) {
                        Text(
                            text = "${door.carrier} • ${door.licensePlate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "No Trailer Attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                FioriStatusBadge(text = statusLabel, tint = statusColor, background = statusBg)
                if (door.status == DoorStatus.LOADING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Loading: ${door.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FioriTheme.statusColors.critical
                    )
                }
            }
        }
    }
}

// Dock Door Detail View Controls
@Composable
fun DockDoorDetailsCard(
    door: DockDoor,
    onLockToggle: (String) -> Unit,
    onStatusChange: (String, DoorStatus) -> Unit,
    onLoadPallet: (String) -> Unit,
    onDockRequested: () -> Unit
) {
    val statusColors = FioriTheme.statusColors
    val (statusLabel, statusColor, statusBg) = when (door.status) {
        DoorStatus.AVAILABLE -> Triple("Vessel Gates Free", statusColors.positive, statusColors.positiveBg)
        DoorStatus.DOCKED -> Triple("Truck Attached", statusColors.informative, statusColors.informativeBg)
        DoorStatus.LOADING -> Triple("Cargo Transferring", statusColors.critical, statusColors.criticalBg)
        DoorStatus.BLOCKED -> Triple("Gate Lockout Alert", statusColors.negative, statusColors.negativeBg)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Details Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = door.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (door.lockState) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (door.lockState) FioriTheme.statusColors.negative else FioriTheme.statusColors.positive
                            )
                            Text(
                                text = if (door.lockState) "Lock Locked Secure" else "Lock Released (Open)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    FioriStatusBadge(text = statusLabel, tint = statusColor, background = statusBg)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Detail Information grid
                if (door.status == DoorStatus.AVAILABLE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FioriTheme.statusColors.positive, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Ready for Delivery", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Dock a carrier truck to verify manifest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                } else if (door.status == DoorStatus.BLOCKED) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(statusBg.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = statusColor, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("MAINTENANCE LOCKOUT ACTIVE", fontWeight = FontWeight.Black, color = statusColor)
                            Text("Security incident or structural check. Gate disabled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                } else {
                    // Active docked truck telemetry
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailItemKeyVal("Carrier Agency", door.carrier)
                            DetailItemKeyVal("Tractor Plate", door.licensePlate)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailItemKeyVal("Priority Level", door.priority)
                            DetailItemKeyVal("Security Seal Code", door.securitySeal)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailItemKeyVal("Time Arrived", door.arrivalTime.ifBlank { "Just Now" })
                            DetailItemKeyVal(
                                "Cold Chain Profile",
                                if (door.temperatureControl) "Refrigerated (${door.temperatureF}°F)" else "Ambient Cargo"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Indicator Panel
                        Text(
                            text = "Cargo Manifest Verification Progress",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pallets Loaded: ${door.loadedPallets} / ${door.totalPallets} PL",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${door.progress}% Verified",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { door.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (door.progress == 100) FioriTheme.statusColors.positive else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Interactive Telemetry Controls Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Security dock lock toggle
                    OutlinedButton(
                        onClick = { onLockToggle(door.id) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_lock_button_${door.id}"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        colors = if (door.lockState) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors(containerColor = statusColors.positive.copy(alpha = 0.1f), contentColor = statusColors.positive)
                    ) {
                        Icon(
                            imageVector = if (door.lockState) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (door.lockState) "Release Dock Lock" else "Engage Dock Lock", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (door.status == DoorStatus.AVAILABLE) {
                        Button(
                            onClick = onDockRequested,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dock_truck_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dock Truck Vessel", fontWeight = FontWeight.Bold)
                        }
                    } else if (door.status == DoorStatus.DOCKED) {
                        Button(
                            onClick = { onStatusChange(door.id, DoorStatus.LOADING) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_loading_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Initiate Loading", fontWeight = FontWeight.Bold)
                        }
                    } else if (door.status == DoorStatus.LOADING) {
                        Button(
                            onClick = { onLoadPallet(door.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_pallet_button_${door.id}"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FioriTheme.statusColors.critical)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan & Load Pallet", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // For blocked doors
                        Button(
                            onClick = { onStatusChange(door.id, DoorStatus.AVAILABLE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("release_blocked_gate"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Resolve Lockout & Clear", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Override Dropdown selector row
                var expandedStatusDrop by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedStatusDrop = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Gate Status Manually", fontWeight = FontWeight.Medium)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedStatusDrop,
                        onDismissRequest = { expandedStatusDrop = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DoorStatus.values().forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.name) },
                                onClick = {
                                    onStatusChange(door.id, st)
                                    expandedStatusDrop = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItemKeyVal(key: String, value: String) {
    Column {
        Text(text = key, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// Inventory Grid Layout Pane
@Composable
fun InventoryGridPane(
    items: List<InventoryItem>,
    onAdjustQty: (String, Int) -> Unit,
    onTransferBinInitiated: (String, String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyStatePane("No Stock Items Found", "Modify search query or click Goods Receipt to post standard inventory cargo.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.sku }) { item ->
                InventoryItemCard(
                    item = item,
                    onAdjustQty = { onAdjustQty(item.sku, it) },
                    onTransfer = { onTransferBinInitiated(item.sku, item.binLocation) }
                )
            }
        }
    }
}

// Single Inventory Item Card
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onAdjustQty: (Int) -> Unit,
    onTransfer: () -> Unit
) {
    val isLowStock = item.quantity <= item.reorderPoint
    val statusColors = FioriTheme.statusColors

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    width = if (isLowStock) 1.5.dp else 1.dp,
                    color = if (isLowStock) statusColors.negative else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category, Bin Coordinates & Warning Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )

                // Coordinates styled monospace
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = item.binLocation,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Label Title
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // SKU code
            Text(
                text = "SKU: ${item.sku}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Spec parameters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Warehouse Load",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${item.weightLb} lbs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Safety Class",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = item.safetyClass,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.safetyClass.contains("Hazard")) statusColors.negative else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock Details with warning indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isLowStock) statusColors.negativeBg else MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Stock Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLowStock) statusColors.negative else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isLowStock) statusColors.negative else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isLowStock) {
                    FioriStatusBadge(
                        text = "Low Stock (${item.reorderPoint} ROP)",
                        tint = statusColors.negative,
                        background = Color.White
                    )
                } else {
                    Text(
                        text = "Optimal (" + item.lastUpdated + ")",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColors.positive,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bin relocation transfer
                IconButton(
                    onClick = onTransfer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Transfer Bin Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Adjust Quantity plus/minus
                IconButton(
                    onClick = { onAdjustQty(-1) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Reduce Quality",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onAdjustQty(1) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Augment Quality",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Warehouse Zone Capacity visualizer lists
@Composable
fun WarehouseZonesPane(
    zones: List<WarehouseZone>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxSize(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Zone Capacities (EWM)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(zones) { zone ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = zone.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${zone.occupied}/${zone.total} bins (${zone.fillPercentage}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val barColor = if (zone.fillPercentage >= 85) {
                            FioriTheme.statusColors.negative
                        } else if (zone.fillPercentage >= 70) {
                            FioriTheme.statusColors.critical
                        } else {
                            FioriTheme.statusColors.positive
                        }

                        LinearProgressIndicator(
                            progress = { zone.fillPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// SAP Event logs / audit tracker console pane
@Composable
fun AuditLogConsolePane(logs: List<String>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxSize(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "System Action Telemetry Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No actions logged yet.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(logs) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Beautiful Custom Status Badging oval capsule
@Composable
fun FioriStatusBadge(
    text: String,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = background,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(tint, CircleShape)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

@Composable
fun EmptyStatePane(
    title: String,
    supportText: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supportText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
