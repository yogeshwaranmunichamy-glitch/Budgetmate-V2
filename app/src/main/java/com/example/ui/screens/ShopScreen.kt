package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.DailyCashRegisterEntity
import com.example.data.local.entities.InventoryItemEntity
import com.example.data.local.entities.KhataPartyEntity
import com.example.data.local.entities.ShopInvoiceEntity
import com.example.ui.components.DailyCashRegisterDialog
import com.example.ui.components.InventoryItemDialog
import com.example.ui.components.InvoiceCreateDialog
import com.example.ui.components.InvoiceSlipDialog
import com.example.ui.components.KhataEntryDialog
import com.example.ui.components.KhataPartyDialog
import com.example.ui.components.ShopProfileDialog
import com.example.ui.components.StockAdjustmentDialog
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(viewModel: BudgetMateViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isShopModeActive by viewModel.isShopModeActive.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val khataParties by viewModel.khataParties.collectAsState()
    val selectedParty by viewModel.selectedKhataParty.collectAsState()
    val selectedPartyEntries by viewModel.selectedPartyEntries.collectAsState()
    val inventoryItems by viewModel.inventoryItems.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()
    val shopInvoices by viewModel.shopInvoices.collectAsState()
    val cashRegister by viewModel.dailyCashRegister.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Khata, 1: Inventory, 2: Invoices, 3: Cash Counter
    val tabTitles = listOf("Khata (Ledger)", "Stock & Items", "Billing & GST", "Cash Counter")

    // Filter states
    var khataTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "CUSTOMER", "SUPPLIER"
    var partySearchQuery by remember { mutableStateOf("") }
    var itemCategoryFilter by remember { mutableStateOf("ALL") }
    var itemSearchQuery by remember { mutableStateOf("") }

    // Dialog visibility states
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<KhataPartyEntity?>(null) }
    var showKhataEntryDialog by remember { mutableStateOf(false) }
    var entryDialogInitialType by remember { mutableStateOf("GAVE") }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var adjustingStockItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var stockAdjustType by remember { mutableStateOf("STOCK_IN") }

    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var viewingInvoiceSlip by remember { mutableStateOf<ShopInvoiceEntity?>(null) }

    var showCashRegisterDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val totalReceivable = viewModel.getTotalKhataReceivable()
    val totalPayable = viewModel.getTotalKhataPayable()
    val totalStockValue = viewModel.getTotalStockValuation()

    // Handle back button when party detail is open
    if (selectedParty != null) {
        BackHandler {
            viewModel.selectKhataParty(null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Card(
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = shopProfile?.businessName ?: "Shop & Industry Hub",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${shopProfile?.businessType ?: "Retail & Manufacturing"} • ${if (!shopProfile?.gstin.isNullOrBlank()) "GST: " + shopProfile?.gstin else "Cash Register"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { showProfileDialog = true }, modifier = Modifier.testTag("shop_edit_profile_btn")) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Card Business Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // To Collect (Receivable)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("To Collect (Len)", fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(CurrencyFormatter.formatINR(totalReceivable), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = IncomeGreen)
                                Text("${khataParties.count { it.type == "CUSTOMER" && it.currentBalance > 0 }} customers", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // To Pay (Payable)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("To Pay (Den)", fontSize = 10.sp, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(CurrencyFormatter.formatINR(totalPayable), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ExpenseRed)
                                Text("${khataParties.count { it.type == "SUPPLIER" && it.currentBalance > 0 }} suppliers", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Inventory Worth
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.12f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Stock Worth", fontSize = 10.sp, color = TealAccent, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(CurrencyFormatter.formatINR(totalStockValue), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("${inventoryItems.size} items", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EmeraldPrimary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("shop_tab_$index")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Khata Ledger
                        if (selectedParty != null) {
                            // Party Ledger View
                            PartyDetailView(
                                party = selectedParty!!,
                                entries = selectedPartyEntries,
                                onBack = { viewModel.selectKhataParty(null) },
                                onAddGave = {
                                    entryDialogInitialType = "GAVE"
                                    showKhataEntryDialog = true
                                },
                                onAddGot = {
                                    entryDialogInitialType = "GOT"
                                    showKhataEntryDialog = true
                                },
                                onSettle = {
                                    viewModel.settleKhataParty(selectedParty!!)
                                },
                                onSendReminder = {
                                    val text = viewModel.generatePaymentReminderMessage(selectedParty!!)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Send Payment Reminder")
                                    context.startActivity(shareIntent)
                                }
                            )
                        } else {
                            // Parties List
                            PartiesListView(
                                parties = khataParties,
                                currentFilter = khataTypeFilter,
                                searchQuery = partySearchQuery,
                                onFilterChange = { khataTypeFilter = it },
                                onSearchChange = { partySearchQuery = it },
                                onSelectParty = { viewModel.selectKhataParty(it) },
                                onAddParty = {
                                    editingParty = null
                                    showAddPartyDialog = true
                                }
                            )
                        }
                    }
                    1 -> {
                        // Inventory & Stock
                        InventoryListView(
                            items = inventoryItems,
                            lowStockItems = lowStockItems,
                            categoryFilter = itemCategoryFilter,
                            searchQuery = itemSearchQuery,
                            onCategoryChange = { itemCategoryFilter = it },
                            onSearchChange = { itemSearchQuery = it },
                            onAddItem = {
                                editingItem = null
                                showAddItemDialog = true
                            },
                            onStockIn = { itm ->
                                adjustingStockItem = itm
                                stockAdjustType = "STOCK_IN"
                            },
                            onStockOut = { itm ->
                                adjustingStockItem = itm
                                stockAdjustType = "STOCK_OUT"
                            },
                            onEditItem = { itm ->
                                editingItem = itm
                                showAddItemDialog = true
                            }
                        )
                    }
                    2 -> {
                        // Invoices & Billing
                        InvoicesListView(
                            invoices = shopInvoices,
                            onCreateInvoice = { showCreateInvoiceDialog = true },
                            onViewSlip = { viewingInvoiceSlip = it }
                        )
                    }
                    3 -> {
                        // Daily Cash Register (Galla)
                        CashRegisterView(
                            register = cashRegister,
                            onUpdateRegister = { showCashRegisterDialog = true }
                        )
                    }
                }
            }
        }
    }

    // ---------------- Dialogs ----------------
    if (showAddPartyDialog) {
        currentUser?.let { user ->
            KhataPartyDialog(
                userId = user.id,
                existingParty = editingParty,
                initialType = if (khataTypeFilter == "SUPPLIER") "SUPPLIER" else "CUSTOMER",
                onDismiss = { showAddPartyDialog = false },
                onSave = { party ->
                    showAddPartyDialog = false
                    if (editingParty != null) viewModel.updateKhataParty(party)
                    else viewModel.saveKhataParty(party)
                }
            )
        }
    }

    if (showKhataEntryDialog && selectedParty != null) {
        KhataEntryDialog(
            party = selectedParty!!,
            initialType = entryDialogInitialType,
            onDismiss = { showKhataEntryDialog = false },
            onSave = { type, amount, desc, billNo, paymentMethod ->
                showKhataEntryDialog = false
                viewModel.recordKhataEntry(
                    partyId = selectedParty!!.id,
                    type = type,
                    amount = amount,
                    description = desc,
                    billNumber = billNo,
                    paymentMethod = paymentMethod
                )
            }
        )
    }

    if (showAddItemDialog) {
        currentUser?.let { user ->
            InventoryItemDialog(
                userId = user.id,
                existingItem = editingItem,
                onDismiss = { showAddItemDialog = false },
                onSave = { item ->
                    showAddItemDialog = false
                    if (editingItem != null) viewModel.updateInventoryItem(item)
                    else viewModel.saveInventoryItem(item)
                }
            )
        }
    }

    if (adjustingStockItem != null) {
        StockAdjustmentDialog(
            item = adjustingStockItem!!,
            initialType = stockAdjustType,
            onDismiss = { adjustingStockItem = null },
            onAdjust = { type, qty, price, reason, note ->
                val itm = adjustingStockItem!!
                adjustingStockItem = null
                viewModel.adjustStock(
                    itemId = itm.id,
                    type = type,
                    quantity = qty,
                    unitPrice = price,
                    reason = reason,
                    note = note
                )
            }
        )
    }

    if (showCreateInvoiceDialog) {
        currentUser?.let { user ->
            InvoiceCreateDialog(
                userId = user.id,
                parties = khataParties,
                inventoryItems = inventoryItems,
                onDismiss = { showCreateInvoiceDialog = false },
                onSaveInvoice = { inv, autoKhata, autoDeduct ->
                    showCreateInvoiceDialog = false
                    viewModel.createShopInvoice(inv, autoKhata, autoDeduct)
                }
            )
        }
    }

    if (viewingInvoiceSlip != null) {
        val slipText = viewModel.generateInvoiceSlipText(viewingInvoiceSlip!!)
        InvoiceSlipDialog(
            invoice = viewingInvoiceSlip!!,
            shopProfile = shopProfile,
            slipText = slipText,
            onDismiss = { viewingInvoiceSlip = null }
        )
    }

    if (showCashRegisterDialog) {
        currentUser?.let { user ->
            DailyCashRegisterDialog(
                userId = user.id,
                existingRegister = cashRegister,
                onDismiss = { showCashRegisterDialog = false },
                onSave = { reg ->
                    showCashRegisterDialog = false
                    viewModel.saveDailyCashRegister(reg)
                }
            )
        }
    }

    if (showProfileDialog) {
        currentUser?.let { user ->
            ShopProfileDialog(
                userId = user.id,
                existingProfile = shopProfile,
                onDismiss = { showProfileDialog = false },
                onSave = { prof ->
                    showProfileDialog = false
                    viewModel.saveShopProfile(prof)
                }
            )
        }
    }
}

// ---------------- Khata Parties List Sub-View ----------------
@Composable
fun PartiesListView(
    parties: List<KhataPartyEntity>,
    currentFilter: String,
    searchQuery: String,
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectParty: (KhataPartyEntity) -> Unit,
    onAddParty: () -> Unit
) {
    val filtered = parties.filter { party ->
        val matchesType = when (currentFilter) {
            "CUSTOMER" -> party.type == "CUSTOMER"
            "SUPPLIER" -> party.type == "SUPPLIER"
            else -> true
        }
        val matchesSearch = party.name.contains(searchQuery, ignoreCase = true) ||
                party.phone.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Filter Pills & Search
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search name or phone...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(50.dp)
                )

                Button(
                    onClick = onAddParty,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(50.dp).testTag("add_party_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 13.sp)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "All (${parties.size})",
                    "CUSTOMER" to "Customers (${parties.count { it.type == "CUSTOMER" }})",
                    "SUPPLIER" to "Suppliers (${parties.count { it.type == "SUPPLIER" }})"
                ).forEach { (f, label) ->
                    FilterChip(
                        selected = currentFilter == f,
                        onClick = { onFilterChange(f) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No parties found", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tap + Add to record credit or payments for customers & suppliers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filtered) { party ->
                KhataPartyCard(party = party, onClick = { onSelectParty(party) })
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun KhataPartyCard(
    party: KhataPartyEntity,
    onClick: () -> Unit
) {
    val isCustomer = party.type == "CUSTOMER"
    val balance = party.currentBalance

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("khata_party_card_${party.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCustomer) EmeraldPrimary.copy(alpha = 0.15f)
                        else TealAccent.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = party.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isCustomer) EmeraldPrimary else TealAccent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = party.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(party.type, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (party.phone.isNotBlank()) "📞 ${party.phone}" else party.address.ifBlank { "No phone saved" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Balance indicator
            Column(horizontalAlignment = Alignment.End) {
                if (balance == 0.0) {
                    Text("Settled", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                } else if (isCustomer) {
                    Text("You'll Get", fontSize = 10.sp, color = IncomeGreen)
                    Text(CurrencyFormatter.formatINR(balance), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IncomeGreen)
                } else {
                    Text("You'll Give", fontSize = 10.sp, color = ExpenseRed)
                    Text(CurrencyFormatter.formatINR(balance), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ExpenseRed)
                }
            }
        }
    }
}

// ---------------- Party Detailed Ledger Timeline View ----------------
@Composable
fun PartyDetailView(
    party: KhataPartyEntity,
    entries: List<com.example.data.local.entities.KhataEntryEntity>,
    onBack: () -> Unit,
    onAddGave: () -> Unit,
    onAddGot: () -> Unit,
    onSettle: () -> Unit,
    onSendReminder: () -> Unit
) {
    val isCustomer = party.type == "CUSTOMER"

    Column(modifier = Modifier.fillMaxSize()) {
        // Party Header with Back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(party.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${party.type} • ${if (party.phone.isNotBlank()) party.phone else "No phone"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (isCustomer && party.currentBalance > 0) {
                Button(
                    onClick = onSendReminder,
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    modifier = Modifier.testTag("send_reminder_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reminder", fontSize = 11.sp)
                }
            }
        }

        // Net Balance Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (party.currentBalance > 0) {
                    if (isCustomer) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                } else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (party.currentBalance == 0.0) "Net Balance: Fully Settled"
                        else if (isCustomer) "Net Amount to Collect (Len)"
                        else "Net Amount to Pay (Den)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = CurrencyFormatter.formatINR(party.currentBalance),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = if (isCustomer) IncomeGreen else ExpenseRed
                    )
                }

                if (party.currentBalance > 0) {
                    OutlinedButton(onClick = onSettle) {
                        Text("Settle All", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: + Gave (Udhar) and - Got (Jama)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddGave,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                modifier = Modifier.weight(1f).testTag("gave_entry_button")
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("🔴 You Gave (Udhar)")
            }

            Button(
                onClick = onAddGot,
                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                modifier = Modifier.weight(1f).testTag("got_entry_button")
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("🟢 You Got (Jama)")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Transaction History", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))

        // Entries List
        if (entries.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
            ) {
                Text(
                    text = "No ledger entries yet. Tap 'You Gave' or 'You Got' above to record credit/payment.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (entry.type == "GAVE") ExpenseRed.copy(alpha = 0.15f)
                                        else IncomeGreen.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (entry.type == "GAVE") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (entry.type == "GAVE") ExpenseRed else IncomeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.description.ifBlank { if (entry.type == "GAVE") "Credit Given" else "Payment Received" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${CurrencyFormatter.formatShortDate(entry.date)} • ${entry.paymentMethod} ${if (entry.billNumber.isNotBlank()) "• Bill #" + entry.billNumber else ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "${if (entry.type == "GAVE") "-" else "+"}${CurrencyFormatter.formatINR(entry.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (entry.type == "GAVE") ExpenseRed else IncomeGreen
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}

// ---------------- Inventory List Sub-View ----------------
@Composable
fun InventoryListView(
    items: List<InventoryItemEntity>,
    lowStockItems: List<InventoryItemEntity>,
    categoryFilter: String,
    searchQuery: String,
    onCategoryChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onStockIn: (InventoryItemEntity) -> Unit,
    onStockOut: (InventoryItemEntity) -> Unit,
    onEditItem: (InventoryItemEntity) -> Unit
) {
    val categories = listOf("ALL", "Raw Materials", "Finished Goods", "Retail Goods", "Spares & Tools")

    val filtered = items.filter { itm ->
        val matchesCat = if (categoryFilter == "ALL") true else itm.category == categoryFilter
        val matchesSearch = itm.name.contains(searchQuery, ignoreCase = true) || itm.sku.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesSearch
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Low stock warning banner if any items are critical
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AnomalyWarning.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AnomalyWarning, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Low Stock Alert (${lowStockItems.size} items)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AnomalyWarning
                            )
                            Text(
                                text = lowStockItems.joinToString(", ") { "${it.name} (${it.currentStock} ${it.unit})" },
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Search & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search items or SKU...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(50.dp)
                )

                Button(
                    onClick = onAddItem,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(50.dp).testTag("add_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Item", fontSize = 13.sp)
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No items found", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tap + Item to add raw materials, retail stock, or finished products.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filtered) { itm ->
                InventoryItemCard(
                    item = itm,
                    onStockIn = { onStockIn(itm) },
                    onStockOut = { onStockOut(itm) },
                    onEdit = { onEditItem(itm) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItemEntity,
    onStockIn: () -> Unit,
    onStockOut: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.category} • SKU: ${item.sku.ifBlank { "N/A" }} • GST: ${item.taxRate.toInt()}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stock status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (item.isOutOfStock) ExpenseRed.copy(alpha = 0.15f)
                            else if (item.isLowStock) AnomalyWarning.copy(alpha = 0.15f)
                            else IncomeGreen.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (item.isOutOfStock) "Out of Stock"
                        else if (item.isLowStock) "Low Stock"
                        else "In Stock",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isOutOfStock) ExpenseRed
                        else if (item.isLowStock) AnomalyWarning
                        else IncomeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price & Stock Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Stock Quantity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${item.currentStock} ${item.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (item.isLowStock) AnomalyWarning else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text("Buy Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatINR(item.purchasePrice), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Column {
                    Text("Sell Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatINR(item.sellingPrice), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                }

                // Quick Stock In / Out Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onStockIn,
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+ In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onStockOut,
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("- Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- Invoices & Billing Sub-View ----------------
@Composable
fun InvoicesListView(
    invoices: List<ShopInvoiceEntity>,
    onCreateInvoice: () -> Unit,
    onViewSlip: (ShopInvoiceEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Top Action Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GST & Cash POS Billing", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Create instant bills, deduct stock, and print/share WhatsApp invoices", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onCreateInvoice,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.testTag("create_invoice_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Bill")
                    }
                }
            }
        }

        item {
            Text("Recent Invoices & Bills (${invoices.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (invoices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No invoices generated yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tap 'New Bill' to generate your first customer bill or invoice slip.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(invoices) { inv ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewSlip(inv) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "${inv.partyName} • ${CurrencyFormatter.formatShortDate(inv.date)} • ${inv.paymentMode}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(CurrencyFormatter.formatINR(inv.grandTotal), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (inv.paymentStatus == "PAID") IncomeGreen.copy(alpha = 0.15f)
                                        else AnomalyWarning.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (inv.paymentStatus == "PAID") "Paid" else "Credit / Due",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (inv.paymentStatus == "PAID") IncomeGreen else AnomalyWarning
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ---------------- Daily Cash Counter (Galla) Sub-View ----------------
@Composable
fun CashRegisterView(
    register: DailyCashRegisterEntity?,
    onUpdateRegister: () -> Unit
) {
    val opening = register?.openingCash ?: 5000.0
    val cashSales = register?.cashSales ?: 0.0
    val khataCash = register?.cashKhataCollected ?: 0.0
    val cashExpenses = register?.cashShopExpenses ?: 0.0
    val supplierPaid = register?.cashSupplierPaid ?: 0.0
    val upiIn = register?.upiCollected ?: 0.0
    val actualCash = register?.actualClosingCash ?: opening

    val totalCashIn = cashSales + khataCash
    val totalCashOut = cashExpenses + supplierPaid
    val expectedCash = opening + totalCashIn - totalCashOut
    val diff = actualCash - expectedCash

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Expected vs Actual Cash Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Actual Cash in Drawer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(CurrencyFormatter.formatINR(actualCash), fontWeight = FontWeight.Bold, fontSize = 26.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Button(
                            onClick = onUpdateRegister,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Count Cash")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Expected Drawer Cash:", fontSize = 12.sp)
                        Text(CurrencyFormatter.formatINR(expectedCash), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tally Status:", fontSize = 12.sp)
                        Text(
                            text = if (diff == 0.0) "✅ Balanced & Matched"
                            else if (diff > 0) "⚠️ Excess +${CurrencyFormatter.formatINR(diff)}"
                            else "❌ Shortage ${CurrencyFormatter.formatINR(diff)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (diff == 0.0) IncomeGreen else if (diff > 0) AnomalyWarning else ExpenseRed
                        )
                    }
                }
            }
        }

        // Daily Inflows & Outflows Breakdown
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Today's Drawer Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Morning Opening Cash
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1. Morning Opening Cash", fontSize = 12.sp)
                        Text(CurrencyFormatter.formatINR(opening), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Cash Sales
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("2. Direct Cash Sales (+)", fontSize = 12.sp, color = IncomeGreen)
                        Text("+${CurrencyFormatter.formatINR(cashSales)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Khata Cash Received
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("3. Khata Customer Udhar Collected (+)", fontSize = 12.sp, color = IncomeGreen)
                        Text("+${CurrencyFormatter.formatINR(khataCash)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Daily Expenses Paid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("4. Staff Wages / Shop Expenses (-)", fontSize = 12.sp, color = ExpenseRed)
                        Text("-${CurrencyFormatter.formatINR(cashExpenses)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Supplier Cash Paid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("5. Supplier Cash Payments (-)", fontSize = 12.sp, color = ExpenseRed)
                        Text("-${CurrencyFormatter.formatINR(supplierPaid)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    // Digital UPI Inflow
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bank / UPI QR Collections (Online)", fontSize = 12.sp, color = TealAccent)
                        Text(CurrencyFormatter.formatINR(upiIn), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
