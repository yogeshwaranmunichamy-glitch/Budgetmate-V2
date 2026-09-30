package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.TransactionEntity
import com.example.ui.components.ReceiptScannerDialog
import com.example.ui.components.TransactionEditDialog
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.BudgetExceededRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter
import com.example.util.rememberVoiceInputState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: BudgetMateViewModel) {
    val searchVoiceState = rememberVoiceInputState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedPayment by remember { mutableStateOf("ALL") }
    var sortBy by remember { mutableStateOf("DATE_DESC") } // "DATE_DESC", "DATE_ASC", "AMOUNT_DESC", "AMOUNT_ASC"

    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }

    // Date Range Filter States
    var dateRangePreset by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "THIS_WEEK", "THIS_MONTH", "LAST_30_DAYS", "CUSTOM"
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    // Compute active date boundaries (start to end timestamps)
    val dateBounds = remember(dateRangePreset, customStartDate, customEndDate) {
        val cal = java.util.Calendar.getInstance()
        when (dateRangePreset) {
            "TODAY" -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                start to end
            }
            "THIS_WEEK" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            "THIS_MONTH" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            "LAST_30_DAYS" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            "CUSTOM" -> {
                val start = customStartDate ?: 0L
                val end = customEndDate ?: Long.MAX_VALUE
                start to end
            }
            else -> 0L to Long.MAX_VALUE // "ALL"
        }
    }

    // Filter & Sort Logic
    val filteredTransactions = remember(allTransactions, query, selectedType, selectedCategory, selectedPayment, sortBy, dateBounds) {
        val (start, end) = dateBounds
        allTransactions.filter { tx ->
            val matchesDate = tx.date in start..end
            val matchesType = selectedType == "ALL" || tx.type == selectedType
            val matchesCategory = selectedCategory == "ALL" || tx.category.equals(selectedCategory, ignoreCase = true)
            val matchesPayment = selectedPayment == "ALL" || tx.paymentMethod.equals(selectedPayment, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    tx.sourceOrMerchant.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.notes.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)
            matchesDate && matchesType && matchesCategory && matchesPayment && matchesQuery
        }.sortedWith { a, b ->
            when (sortBy) {
                "DATE_ASC" -> a.date.compareTo(b.date)
                "AMOUNT_DESC" -> b.amount.compareTo(a.amount)
                "AMOUNT_ASC" -> a.amount.compareTo(b.amount)
                else -> b.date.compareTo(a.date) // DATE_DESC
            }
        }
    }

    // Spending & Income summary within the chosen date range
    val rangeSpending = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val rangeIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val rangeNet = remember(rangeIncome, rangeSpending) {
        rangeIncome - rangeSpending
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Screen Header & Action shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transactions",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filteredTransactions.size} of ${allTransactions.size} records",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showReceiptDialog = true },
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).size(38.dp)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Receipt OCR", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { showVoiceDialog = true },
                        modifier = Modifier.clip(CircleShape).background(EmeraldPrimary).size(38.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search by merchant, note, amount...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (searchVoiceState.isListening) {
                                    searchVoiceState.stopListening()
                                } else {
                                    searchVoiceState.startListening("English") { spokenText ->
                                        query = spokenText
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Search",
                                tint = if (searchVoiceState.isListening) BudgetExceededRed else EmeraldPrimary
                            )
                        }
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("transactions_search_field"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Type Filter Chips (All, Expense, Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == "ALL",
                    onClick = { selectedType = "ALL" },
                    label = { Text("All (${allTransactions.size})") }
                )
                FilterChip(
                    selected = selectedType == "EXPENSE",
                    onClick = { selectedType = "EXPENSE" },
                    label = { Text("Expenses (${allTransactions.count { it.type == "EXPENSE" }})") }
                )
                FilterChip(
                    selected = selectedType == "INCOME",
                    onClick = { selectedType = "INCOME" },
                    label = { Text("Income (${allTransactions.count { it.type == "INCOME" }})") }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sort & Filter Dropdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort selector
                var sortExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { sortExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (sortBy) {
                                "DATE_DESC" -> "Newest"
                                "DATE_ASC" -> "Oldest"
                                "AMOUNT_DESC" -> "High ₹"
                                "AMOUNT_ASC" -> "Low ₹"
                                else -> "Sort"
                            },
                            fontSize = 11.sp
                        )
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Newest Date") }, onClick = { sortBy = "DATE_DESC"; sortExpanded = false })
                        DropdownMenuItem(text = { Text("Oldest Date") }, onClick = { sortBy = "DATE_ASC"; sortExpanded = false })
                        DropdownMenuItem(text = { Text("Highest Amount") }, onClick = { sortBy = "AMOUNT_DESC"; sortExpanded = false })
                        DropdownMenuItem(text = { Text("Lowest Amount") }, onClick = { sortBy = "AMOUNT_ASC"; sortExpanded = false })
                    }
                }

                // Category selector
                var catFilterExpanded by remember { mutableStateOf(false) }
                val categories = listOf("ALL") + allTransactions.map { it.category }.distinct()
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { catFilterExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedCategory == "ALL") "Category" else selectedCategory,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = catFilterExpanded,
                        onDismissRequest = { catFilterExpanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { selectedCategory = c; catFilterExpanded = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date Range Filter Chips (All Time, Today, This Week, This Month, Last 30 Days, Custom Range)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateOptions = listOf(
                    "ALL" to "All Time",
                    "TODAY" to "Today",
                    "THIS_WEEK" to "This Week",
                    "THIS_MONTH" to "This Month",
                    "LAST_30_DAYS" to "Last 30 Days",
                    "CUSTOM" to if (dateRangePreset == "CUSTOM" && customStartDate != null && customEndDate != null)
                        "${CurrencyFormatter.formatShortDate(customStartDate!!)} - ${CurrencyFormatter.formatShortDate(customEndDate!!)}"
                    else "Custom 📅"
                )

                dateOptions.forEach { (preset, label) ->
                    val isSelected = dateRangePreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (preset == "CUSTOM") {
                                showCustomDateDialog = true
                            } else {
                                dateRangePreset = preset
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (preset == "CUSTOM") {
                            {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("date_filter_${preset.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dedicated Spending in Selected Date Range Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (dateRangePreset != "ALL") EmeraldPrimary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("range_spending_summary_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (dateRangePreset) {
                                    "TODAY" -> "Today's Range"
                                    "THIS_WEEK" -> "This Week's Range"
                                    "THIS_MONTH" -> "This Month's Range"
                                    "LAST_30_DAYS" -> "Last 30 Days Range"
                                    "CUSTOM" -> "Custom Date Range"
                                    else -> "Overall Range Summary"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (dateRangePreset != "ALL") {
                            Text(
                                text = "Reset Filter",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary,
                                modifier = Modifier
                                    .clickable { dateRangePreset = "ALL" }
                                    .testTag("clear_date_range_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Spending Metric
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Spending in Range",
                                    fontSize = 11.sp,
                                    color = ExpenseRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatINR(rangeSpending),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                modifier = Modifier.testTag("range_spending_amount")
                            )
                        }

                        // Income Metric
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Income in Range",
                                    fontSize = 11.sp,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatINR(rangeIncome),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        // Net Balance Metric
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Net Savings",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = CurrencyFormatter.formatINR(rangeNet),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rangeNet >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${filteredTransactions.size} transactions match active criteria",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction List
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching transactions found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionListItem(
                            transaction = tx,
                            onClick = { viewingTransaction = tx }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // FAB to add new transaction
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("add_transaction_page_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    // Detail & Action Dialog
    if (viewingTransaction != null) {
        val tx = viewingTransaction!!
        AlertDialog(
            onDismissRequest = { viewingTransaction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.sourceOrMerchant, fontWeight = FontWeight.Bold)
                    if (tx.isAnomaly) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AnomalyWarning, modifier = Modifier.size(18.dp))
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Type: ${tx.type}", fontWeight = FontWeight.SemiBold)
                    Text("Amount: ${CurrencyFormatter.formatINR(tx.amount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (tx.type == "INCOME") IncomeGreen else ExpenseRed)
                    Text("Category: ${tx.category}")
                    Text("Payment Method: ${tx.paymentMethod}")
                    Text("Date: ${CurrencyFormatter.formatDate(tx.date)}")
                    if (tx.notes.isNotBlank()) Text("Notes: ${tx.notes}")
                    if (tx.isVoiceEntered) Text("Voice Recognized", color = EmeraldPrimary, fontSize = 11.sp)
                    if (tx.isReceiptScanned) Text("Receipt OCR Scanned", color = EmeraldPrimary, fontSize = 11.sp)
                    if (tx.isAnomaly) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AnomalyWarning.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Anomaly Alert: ${tx.anomalyReason}",
                                color = AnomalyWarning,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            viewModel.deleteTransaction(tx)
                            viewingTransaction = null
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                    }

                    IconButton(
                        onClick = {
                            editingTransaction = tx
                            viewingTransaction = null
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EmeraldPrimary)
                    }

                    TextButton(onClick = { viewingTransaction = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showAddDialog) {
        currentUser?.let { user ->
            TransactionEditDialog(
                userId = user.id,
                existingTransaction = null,
                onDismiss = { showAddDialog = false },
                onSave = { viewModel.addTransaction(it) }
            )
        }
    }

    if (editingTransaction != null) {
        currentUser?.let { user ->
            TransactionEditDialog(
                userId = user.id,
                existingTransaction = editingTransaction,
                onDismiss = { editingTransaction = null },
                onSave = { viewModel.updateTransaction(it) }
            )
        }
    }

    if (showVoiceDialog) {
        currentUser?.let { user ->
            VoiceTransactionDialog(
                userId = user.id,
                viewModel = viewModel,
                onDismiss = { showVoiceDialog = false },
                onConfirmSave = { tx, origCat ->
                    viewModel.addTransaction(tx)
                    viewModel.recordVoiceCorrection(tx.notes, origCat, tx.category)
                }
            )
        }
    }

    if (showReceiptDialog) {
        currentUser?.let { user ->
            ReceiptScannerDialog(
                userId = user.id,
                onDismiss = { showReceiptDialog = false },
                onReceiptConfirmed = { tx, scan ->
                    viewModel.saveReceipt(tx, scan)
                }
            )
        }
    }

    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            currentStart = customStartDate ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000),
            currentEnd = customEndDate ?: System.currentTimeMillis(),
            onDismiss = { showCustomDateDialog = false },
            onApply = { start, end ->
                customStartDate = start
                customEndDate = end
                dateRangePreset = "CUSTOM"
                showCustomDateDialog = false
            }
        )
    }
}

@Composable
fun CustomDateRangeDialog(
    currentStart: Long,
    currentEnd: Long,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit
) {
    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    var startDateText by remember { mutableStateOf(sdf.format(java.util.Date(currentStart))) }
    var endDateText by remember { mutableStateOf(sdf.format(java.util.Date(currentEnd))) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun applyPreset(daysBack: Int) {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysBack)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        startDateText = sdf.format(cal.time)
        endDateText = sdf.format(java.util.Date(now))
        errorMessage = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Date Range", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quick Presets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "7 Days" to 7,
                        "14 Days" to 14,
                        "30 Days" to 30,
                        "60 Days" to 60,
                        "90 Days" to 90,
                        "180 Days" to 180
                    ).forEach { (label, days) ->
                        FilterChip(
                            selected = false,
                            onClick = { applyPreset(days) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = startDateText,
                    onValueChange = {
                        startDateText = it
                        errorMessage = null
                    },
                    label = { Text("From Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-05-01") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_date_start_input")
                )

                OutlinedTextField(
                    value = endDateText,
                    onValueChange = {
                        endDateText = it
                        errorMessage = null
                    },
                    label = { Text("To Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-05-31") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_date_end_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ExpenseRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val parsedStart = sdf.parse(startDateText.trim())
                        val parsedEnd = sdf.parse(endDateText.trim())
                        if (parsedStart == null || parsedEnd == null) {
                            errorMessage = "Invalid date format. Use YYYY-MM-DD"
                        } else {
                            val calEnd = java.util.Calendar.getInstance().apply {
                                time = parsedEnd
                                set(java.util.Calendar.HOUR_OF_DAY, 23)
                                set(java.util.Calendar.MINUTE, 59)
                                set(java.util.Calendar.SECOND, 59)
                                set(java.util.Calendar.MILLISECOND, 999)
                            }
                            if (parsedStart.time > calEnd.timeInMillis) {
                                errorMessage = "Start date must be before or equal to End date"
                            } else {
                                onApply(parsedStart.time, calEnd.timeInMillis)
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = "Invalid date format. Use YYYY-MM-DD"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("apply_custom_date_range_button")
            ) {
                Text("Apply Range")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
