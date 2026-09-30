package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.components.CategoryDoughnutChart
import com.example.ui.components.ReceiptScannerDialog
import com.example.ui.components.SmsTransactionScannerDialog
import com.example.ui.components.TransactionEditDialog
import com.example.ui.components.TripVoiceEntryDialog
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(viewModel: BudgetMateViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()
    val isTripModeActive by viewModel.isTripModeActive.collectAsState()
    val tripAnalytics by viewModel.tripAnalytics.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val khataParties by viewModel.khataParties.collectAsState()
    val inventoryItems by viewModel.inventoryItems.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showTripVoiceDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // Swipe up for Credit / Pull down for Debit on Plus FAB
    var initialAddType by remember { mutableStateOf("EXPENSE") }
    var fabDragOffsetY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var isDraggingFab by remember { mutableStateOf(false) }
    var showSwipeChoiceDialog by remember { mutableStateOf(false) }

    // Dashboard Date Range Filter State
    var dashDateRangePreset by remember { mutableStateOf("THIS_MONTH") } // "THIS_MONTH", "TODAY", "THIS_WEEK", "LAST_30_DAYS", "ALL", "CUSTOM"
    var dashCustomStartDate by remember { mutableStateOf<Long?>(null) }
    var dashCustomEndDate by remember { mutableStateOf<Long?>(null) }
    var showDashCustomDateDialog by remember { mutableStateOf(false) }

    val dashDateBounds = remember(dashDateRangePreset, dashCustomStartDate, dashCustomEndDate) {
        val cal = java.util.Calendar.getInstance()
        when (dashDateRangePreset) {
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
                start to cal.timeInMillis
            }
            "THIS_WEEK" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis to System.currentTimeMillis()
            }
            "THIS_MONTH" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis to System.currentTimeMillis()
            }
            "LAST_30_DAYS" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis to System.currentTimeMillis()
            }
            "CUSTOM" -> {
                val start = dashCustomStartDate ?: 0L
                val end = dashCustomEndDate ?: Long.MAX_VALUE
                start to end
            }
            else -> 0L to Long.MAX_VALUE
        }
    }

    val dashRangeTransactions = remember(transactions, dashDateBounds) {
        val (start, end) = dashDateBounds
        transactions.filter { it.date in start..end }
    }
    val dashRangeSpending = remember(dashRangeTransactions) {
        dashRangeTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val dashRangeIncome = remember(dashRangeTransactions) {
        dashRangeTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val dashRangeNet = remember(dashRangeIncome, dashRangeSpending) {
        dashRangeIncome - dashRangeSpending
    }

    val totalBalance = viewModel.getTotalBalance()
    val currentMonthIncome = viewModel.getCurrentMonthIncomeSum()
    val currentMonthExpense = viewModel.getCurrentMonthExpenseSum()
    val currentMonthSavings = (currentMonthIncome - currentMonthExpense).coerceAtLeast(0.0)

    val totalBudget = budgets.sumOf { it.monthlyLimit }
    val remainingBudget = (totalBudget - currentMonthExpense).coerceAtLeast(0.0)
    val budgetPercent = if (totalBudget > 0) (currentMonthExpense / totalBudget) * 100 else 0.0

    val categoryExpenses = viewModel.getCategoryExpensesForCurrentMonth()
    val recentTransactions = transactions.take(6)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // User Welcome Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${currentUser?.name ?: "User"}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Financial Snapshot • ${CurrencyFormatter.formatMonthYear(System.currentTimeMillis())}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Actions (SMS Reader, Receipt OCR, Voice)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // On-Demand Bank SMS Reader Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showSmsDialog = true }
                                .testTag("dashboard_sms_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = "Scan SMS", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        }

                        // Receipt Scanner Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showReceiptDialog = true }
                                .testTag("dashboard_receipt_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Receipt", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        }

                        // Voice Entry Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                                .clickable {
                                    if (isTripModeActive && activeTrip != null) {
                                        showTripVoiceDialog = true
                                    } else {
                                        showVoiceDialog = true
                                    }
                                }
                                .testTag("dashboard_voice_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Entry", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // Total Balance Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("total_balance_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Total Net Balance",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatINR(totalBalance),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Month Income
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Text(CurrencyFormatter.formatINR(currentMonthIncome), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            // Month Expenses
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Expenses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Text(CurrencyFormatter.formatINR(currentMonthExpense), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Transaction & Feature Action Tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Credit Shortcut
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                initialAddType = "INCOME"
                                showAddDialog = true
                            }
                            .testTag("quick_action_credit")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Credit (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }
                    }

                    // Debit Shortcut
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                initialAddType = "EXPENSE"
                                showAddDialog = true
                            }
                            .testTag("quick_action_debit")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Debit (-)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                    }

                    // Bank SMS Reader Shortcut
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showSmsDialog = true }
                            .testTag("quick_action_sms")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Scan SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    }

                    // Date Range Filters Shortcut
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setScreen("transactions") }
                            .testTag("quick_action_dates")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Dates 📅", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Range Spending Filter Card (Directly addresses "filter for data range to see the spending in that range")
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_date_range_spending_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Spending in Date Range",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "View All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary,
                                modifier = Modifier.clickable { viewModel.setScreen("transactions") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Date Range Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "THIS_MONTH" to "This Month",
                                "TODAY" to "Today",
                                "THIS_WEEK" to "This Week",
                                "LAST_30_DAYS" to "Last 30 Days",
                                "ALL" to "All Time",
                                "CUSTOM" to if (dashDateRangePreset == "CUSTOM" && dashCustomStartDate != null && dashCustomEndDate != null)
                                    "${CurrencyFormatter.formatShortDate(dashCustomStartDate!!)} - ${CurrencyFormatter.formatShortDate(dashCustomEndDate!!)}"
                                else "Custom 📅"
                            ).forEach { (preset, label) ->
                                val selected = dashDateRangePreset == preset
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (preset == "CUSTOM") {
                                            showDashCustomDateDialog = true
                                        } else {
                                            dashDateRangePreset = preset
                                        }
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Key Metrics in Range
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Spending in Range
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Spending", fontSize = 11.sp, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = CurrencyFormatter.formatINR(dashRangeSpending),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }

                            // Income in Range
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Income", fontSize = 11.sp, color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = CurrencyFormatter.formatINR(dashRangeIncome),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }

                            // Net in Range
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = CurrencyFormatter.formatINR(dashRangeNet),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dashRangeNet >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${dashRangeTransactions.size} transactions in selected range",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // On-Demand Data Extraction Hub (Receipt OCR & Bank SMS)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_smart_import_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Smart On-Demand Import",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Extract transactions automatically from incoming SMS or Receipt photos",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Bank SMS Card
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showSmsDialog = true }
                                    .testTag("dashboard_smart_sms_card")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Sms, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bank SMS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Read inbox or paste SMS like OCR", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Receipt OCR Card
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showReceiptDialog = true }
                                    .testTag("dashboard_smart_receipt_card")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Receipt OCR", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Upload image or take photo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Shop & Small Industry Business Hub Banner (Play Store Top Reviewed Feature)
            item {
                val totalReceivable = viewModel.getTotalKhataReceivable()
                val totalPayable = viewModel.getTotalKhataPayable()
                val totalStockValue = viewModel.getTotalStockValuation()

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setScreen("shop") }
                        .testTag("dashboard_shop_hub_card")
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = shopProfile?.businessName ?: "Shop & Industry Hub",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EmeraldPrimary.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("KHATA & GST", fontSize = 8.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = "${shopProfile?.businessType ?: "Retail & Manufacturing"} • ${khataParties.size} Khata Parties • ${inventoryItems.size} Stock Items",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = "Open Shop", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("To Collect (Udhar)", fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                                Text(CurrencyFormatter.formatINR(totalReceivable), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = IncomeGreen)
                            }
                            Column {
                                Text("To Pay (Dues)", fontSize = 10.sp, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                                Text(CurrencyFormatter.formatINR(totalPayable), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ExpenseRed)
                            }
                            Column {
                                Text("Stock Worth", fontSize = 10.sp, color = TealAccent, fontWeight = FontWeight.SemiBold)
                                Text(CurrencyFormatter.formatINR(totalStockValue), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Active Trip Planning Mode Banner (Latest feature)
            if (activeTrip != null) {
                val trip = activeTrip!!
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTripModeActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setScreen("trips") }
                            .testTag("dashboard_active_trip_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(trip.coverEmoji, fontSize = 22.sp)
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = trip.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (isTripModeActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(EmeraldPrimary)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("TRIP MODE", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "📍 ${trip.destination} • ${trip.companions.split(",").size} travelers",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val spent = tripAnalytics?.totalSpent ?: 0.0
                                    Text(
                                        text = "Spent ${CurrencyFormatter.formatINR(spent)} of ${CurrencyFormatter.formatINR(trip.budget)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isTripModeActive) {
                                    IconButton(
                                        onClick = { showTripVoiceDialog = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = "Trip Voice Log", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.FlightTakeoff,
                                    contentDescription = "Open Trip",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Month Budget Utilization Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
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
                            Text(
                                text = "Monthly Budget Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%.0f", budgetPercent)}% Used",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (budgetPercent > 100) ExpenseRed else EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (budgetPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                            color = if (budgetPercent > 100) ExpenseRed else if (budgetPercent > 80) AnomalyWarning else EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Budget: ${CurrencyFormatter.formatINR(totalBudget)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Remaining: ${CurrencyFormatter.formatINR(remainingBudget)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Savings Goals Summary Card
            if (savingsGoals.isNotEmpty()) {
                item {
                    val firstGoal = savingsGoals.first()
                    val pct = if (firstGoal.targetAmount > 0) (firstGoal.currentAmount / firstGoal.targetAmount) * 100 else 0.0

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.setScreen("savings") }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(firstGoal.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${CurrencyFormatter.formatINR(firstGoal.currentAmount)} of ${CurrencyFormatter.formatINR(firstGoal.targetAmount)} (${String.format("%.0f", pct)}%)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("View All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmeraldPrimary)
                        }
                    }
                }
            }

            // Category Wise Expenses Chart
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Category-wise Spending",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        CategoryDoughnutChart(categoryData = categoryExpenses)
                    }
                }
            }

            // Spending Insights Teaser (with Ad gate button as required by prompt)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ML Spending Insights & Summaries",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Access weekly, monthly, and yearly statistical spending breakdowns, anomaly diagnostics, and future expense forecasting.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.requestSummaryOrInsightsWithAd("Spending Insights & Summaries") {
                                    viewModel.setScreen("reports")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("unlock_insights_button")
                        ) {
                            Text("View Detailed Reports & Insights", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { viewModel.setScreen("transactions") },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("View All", fontSize = 12.sp)
                    }
                }
            }

            // Recent transactions items
            if (recentTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions yet. Tap + to add an expense or income.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(recentTransactions) { tx ->
                    TransactionListItem(
                        transaction = tx,
                        onClick = { editingTransaction = tx }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Floating Action Button with Swipe Up (Credit) / Pull Down (Debit) Gestures
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Resting visual badge hint so user immediately discovers swipe up/down
            if (!isDraggingFab && fabDragOffsetY == 0f) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .offset(y = (-46).dp)
                        .clickable { showSwipeChoiceDialog = true }
                        .testTag("cue_swipe_hint_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⬆ Credit  |  ⬇ Debit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // Visual cues while dragging or hovering
            if (isDraggingFab || fabDragOffsetY != 0f) {
                // Swipe Up: Credit indicator
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (fabDragOffsetY < -30f) IncomeGreen else IncomeGreen.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .offset(y = (-75).dp)
                        .testTag("cue_swipe_credit")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Credit (+Income)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Pull Down: Debit indicator
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (fabDragOffsetY > 30f) ExpenseRed else ExpenseRed.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .offset(y = 75.dp)
                        .testTag("cue_swipe_debit")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Debit (-Expense)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    showSwipeChoiceDialog = true
                },
                containerColor = if (fabDragOffsetY < -30f) IncomeGreen else if (fabDragOffsetY > 30f) ExpenseRed else EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .offset { IntOffset(0, (fabDragOffsetY * 0.4f).roundToInt()) }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDrag = 0f
                            var hasMoved = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    // Pointer released
                                    if (hasMoved) {
                                        if (totalDrag < -30f) {
                                            initialAddType = "INCOME"
                                            showAddDialog = true
                                        } else if (totalDrag > 30f) {
                                            initialAddType = "EXPENSE"
                                            showAddDialog = true
                                        } else {
                                            showSwipeChoiceDialog = true
                                        }
                                    } else {
                                        // Regular tap
                                        showSwipeChoiceDialog = true
                                    }
                                    fabDragOffsetY = 0f
                                    isDraggingFab = false
                                    break
                                }

                                val dragAmount = change.position.y - down.position.y
                                totalDrag = dragAmount
                                if (Math.abs(totalDrag) > 12f) {
                                    hasMoved = true
                                    isDraggingFab = true
                                    fabDragOffsetY = totalDrag.coerceIn(-120f, 120f)
                                    change.consume()
                                }
                            }
                        }
                    }
                    .testTag("add_transaction_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction: Swipe up for Credit, pull down for Debit")
            }
        }
    }

    // Dialogs
    if (showSwipeChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showSwipeChoiceDialog = false },
            title = { Text("Choose Transaction Type", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💡 Gesture Shortcut: Drag the (+) icon UP for Credit, or pull DOWN for Debit!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSwipeChoiceDialog = false
                                initialAddType = "INCOME"
                                showAddDialog = true
                            }
                            .testTag("choice_credit_income")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Credit (+ Income / Cash In)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = IncomeGreen)
                                Text("Swipe UP shortcut • Salary, payments, deposits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSwipeChoiceDialog = false
                                initialAddType = "EXPENSE"
                                showAddDialog = true
                            }
                            .testTag("choice_debit_expense")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Debit (- Expense / Cash Out)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ExpenseRed)
                                Text("Pull DOWN shortcut • Food, fuel, purchases, bills", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSwipeChoiceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        currentUser?.let { user ->
            TransactionEditDialog(
                userId = user.id,
                existingTransaction = null,
                initialType = initialAddType,
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

    if (showTripVoiceDialog && activeTrip != null) {
        TripVoiceEntryDialog(
            trip = activeTrip!!,
            viewModel = viewModel,
            onDismiss = { showTripVoiceDialog = false }
        )
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

    if (showDashCustomDateDialog) {
        com.example.ui.screens.CustomDateRangeDialog(
            currentStart = dashCustomStartDate ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000),
            currentEnd = dashCustomEndDate ?: System.currentTimeMillis(),
            onDismiss = { showDashCustomDateDialog = false },
            onApply = { start, end ->
                dashCustomStartDate = start
                dashCustomEndDate = end
                dashDateRangePreset = "CUSTOM"
                showDashCustomDateDialog = false
            }
        )
    }
}

@Composable
fun TransactionListItem(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("transaction_item_${transaction.id}")
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
                        if (transaction.type == "INCOME") IncomeGreen.copy(alpha = 0.15f)
                        else ExpenseRed.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.type == "INCOME") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (transaction.type == "INCOME") IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.sourceOrMerchant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (transaction.isAnomaly) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Anomaly",
                            tint = AnomalyWarning,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category} • ${transaction.paymentMethod} • ${CurrencyFormatter.formatShortDate(transaction.date)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (transaction.type == "INCOME") "+" else "-"}${CurrencyFormatter.formatINR(transaction.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (transaction.type == "INCOME") IncomeGreen else ExpenseRed
            )
        }
    }
}
