package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(viewModel: BudgetMateViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()
    val tripExpenses by viewModel.tripExpenses.collectAsState()
    val paymentReminders by viewModel.paymentReminders.collectAsState()
    val isTripModeActive by viewModel.isTripModeActive.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val khataParties by viewModel.khataParties.collectAsState()
    val inventoryItems by viewModel.inventoryItems.collectAsState()
    val appMode by viewModel.appMode.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var voiceInitialPanel by remember { mutableStateOf("PERSONAL") }
    var showTripVoiceDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // Gesture control variables for FAB (+): Swipe Up for Credit, Pull Down for Debit
    var initialAddType by remember { mutableStateOf("EXPENSE") }
    var fabDragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingFab by remember { mutableStateOf(false) }
    var showSwipeChoiceDialog by remember { mutableStateOf(false) }

    // Dashboard Date Range Filter State
    var dashDateRangePreset by remember { mutableStateOf("THIS_MONTH") }
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

    val totalReceivable = viewModel.getTotalKhataReceivable()
    val totalPayable = viewModel.getTotalKhataPayable()
    val totalStockValue = viewModel.getTotalStockValuation()

    val totalBudget = budgets.sumOf { it.monthlyLimit }
    val budgetPercent = if (totalBudget > 0) (currentMonthExpense / totalBudget) * 100 else 0.0

    val categoryExpenses = viewModel.getCategoryExpensesForCurrentMonth()
    val recentTransactions = transactions.take(6)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. TOP SEGMENTED SWITCH: Personal vs Business Mode (Direct user request)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            // Personal Mode Button
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (appMode == "PERSONAL") EmeraldPrimary else Color.Transparent,
                                modifier = Modifier
                                    .clickable { viewModel.setAppMode("PERSONAL") }
                                    .testTag("toggle_personal_mode")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (appMode == "PERSONAL") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Personal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (appMode == "PERSONAL") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Business Mode Button
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (appMode == "BUSINESS") EmeraldPrimary else Color.Transparent,
                                modifier = Modifier
                                    .clickable { viewModel.setAppMode("BUSINESS") }
                                    .testTag("toggle_business_mode")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (appMode == "BUSINESS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Business & Khata",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (appMode == "BUSINESS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. HEADER WITH USER GREETING & PERSISTENT VOICE ASSISTANT IN MAIN WINDOW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${currentUser?.name ?: "User"}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (appMode == "PERSONAL") "Personal Expenses & Savings" else "Shop Khata & Small Industry Hub",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Persistent Action Icons on Main Window
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bank SMS Reader
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showSmsDialog = true }
                                .testTag("top_action_sms"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = "Bank SMS", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }

                        // Receipt OCR
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showReceiptDialog = true }
                                .testTag("top_action_receipt"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "Receipt OCR", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }

                        // Persistent Glowing Voice AI Assistant in Main Window (Direct User Request)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = EmeraldPrimary,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .clickable {
                                    voiceInitialPanel = appMode
                                    showVoiceDialog = true
                                }
                                .testTag("top_action_voice_ai")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Assistant", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Voice AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // PERSONAL MODE CONTENT (Clean, Uncluttered, Focused)
            // =========================================================================
            if (appMode == "PERSONAL") {
                // Personal Balance Hero Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("personal_balance_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Personal Net Balance",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatINR(totalBalance),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Income
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(IncomeGreen.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Income", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(CurrencyFormatter.formatINR(currentMonthIncome), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                // Expenses
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(ExpenseRed.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Expenses", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(CurrencyFormatter.formatINR(currentMonthExpense), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Date Range Filter & Real-Time Spending Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().testTag("personal_date_range_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Spending in Range", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(
                                    text = "Full History",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.clickable { viewModel.setScreen("transactions") }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Date Range Chips
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
                                    "LAST_30_DAYS" to "30 Days",
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

                            Spacer(modifier = Modifier.height(10.dp))

                            // Spending Numbers in Range
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Spending", fontSize = 11.sp, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatINR(dashRangeSpending),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Income", fontSize = 11.sp, color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatINR(dashRangeIncome),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Savings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = CurrencyFormatter.formatINR(dashRangeNet),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dashRangeNet >= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Action Buttons Row (Credit, Debit, SMS, OCR)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Credit (+)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    initialAddType = "INCOME"
                                    showAddDialog = true
                                }
                                .testTag("personal_quick_credit")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Credit (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                        }

                        // Debit (-)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    initialAddType = "EXPENSE"
                                    showAddDialog = true
                                }
                                .testTag("personal_quick_debit")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Debit (-)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }

                        // Bank SMS
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showSmsDialog = true }
                                .testTag("personal_quick_sms")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Bank SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }
                        }

                        // Receipt OCR
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showReceiptDialog = true }
                                .testTag("personal_quick_ocr")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Bill OCR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Payment & Loan Reminders Alert Banner
                val pendingReminders = paymentReminders.filter { it.status != "COMPLETED" }
                if (pendingReminders.isNotEmpty()) {
                    val nextDue = pendingReminders.firstOrNull()
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setScreen("reminders") }
                                .testTag("home_payment_reminders_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(AnomalyWarning.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Alarm, contentDescription = null, tint = AnomalyWarning, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Payment & Loan Reminders", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AnomalyWarning.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        "${pendingReminders.size} Active",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AnomalyWarning,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (nextDue != null) {
                                                Text(
                                                    text = "Next: ${nextDue.personOrEntity} (${CurrencyFormatter.formatINR(nextDue.amount)}) • ${if (nextDue.reminderType == "TO_COLLECT") "To Collect" else "To Pay"}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Icon(Icons.Default.ChevronRight, contentDescription = "Open Reminders", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Active Trip Planning Card on Home (Restored per user request)
                if (activeTrip != null) {
                    item {
                        val trip = activeTrip!!
                        val totalTripBudget = trip.budget
                        val tripSpent = remember(tripExpenses, trip.id) { tripExpenses.filter { it.tripId == trip.id }.sumOf { it.amount } }
                        val tripRemaining = (totalTripBudget - tripSpent).coerceAtLeast(0.0)
                        val tripPercent = if (totalTripBudget > 0) (tripSpent / totalTripBudget) * 100 else 0.0
                        val tripCurrencySymbol = if (trip.currency == "USD") "$" else if (trip.currency == "EUR") "€" else "₹"

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_active_trip_banner")
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
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldPrimary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(trip.coverEmoji, fontSize = 22.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = trip.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = EmeraldPrimary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        "TRIP ACTIVE",
                                                        fontSize = 8.sp,
                                                        color = EmeraldPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${trip.destination} • ${trip.currency} ($tripCurrencySymbol)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(onClick = { viewModel.setScreen("trips") }) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Open Trip", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Trip Budget", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$tripCurrencySymbol${totalTripBudget.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Spent", fontSize = 10.sp, color = ExpenseRed)
                                        Text("$tripCurrencySymbol${tripSpent.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ExpenseRed)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Remaining", fontSize = 10.sp, color = IncomeGreen)
                                        Text("$tripCurrencySymbol${tripRemaining.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = IncomeGreen)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (tripPercent / 100f).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (tripPercent > 90) ExpenseRed else EmeraldPrimary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Trip Voice Log Button
                                    Button(
                                        onClick = {
                                            voiceInitialPanel = "TRIP"
                                            showVoiceDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Trip Voice Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // View Details
                                    OutlinedButton(
                                        onClick = { viewModel.setScreen("trips") },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Trip Split & Plans ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Invitation to Plan a Trip
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setScreen("trips") }
                                .testTag("home_plan_trip_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Plan a Trip & Vacation Split", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Group bill splitting, multi-currency budget & travel itineraries", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Compact Budget & Goals Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().testTag("personal_budget_goals_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Monthly Budget & Goals", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text(
                                    text = "Manage",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { viewModel.setScreen("budget") }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (totalBudget > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Budget Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${budgetPercent.toInt()}% of ${CurrencyFormatter.formatINR(totalBudget)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (budgetPercent / 100f).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (budgetPercent > 90) ExpenseRed else EmeraldPrimary
                                )
                            } else {
                                Text("No monthly budget set yet. Tap 'Manage' to set category spending limits.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Category Breakdown Chart
                if (categoryExpenses.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Expense Categories", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                CategoryDoughnutChart(
                                    categoryData = categoryExpenses
                                )
                            }
                        }
                    }
                }

                // Recent Personal Transactions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "See All (${transactions.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary,
                            modifier = Modifier.clickable { viewModel.setScreen("transactions") }
                        )
                    }
                }

                if (recentTransactions.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No transactions recorded yet", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Tap the (+) button below or use Voice AI to log your first expense or income!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(recentTransactions) { tx ->
                        TransactionListItem(
                            transaction = tx,
                            onClick = { editingTransaction = tx }
                        )
                    }
                }
            }

            // =========================================================================
            // BUSINESS MODE CONTENT (Dedicated for Retail Shop & Small Industry Ledger)
            // =========================================================================
            if (appMode == "BUSINESS") {
                // Business Financial Overview Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("business_overview_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        shopProfile?.businessName ?: "My Business",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "Shop Hub ➔",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.clickable { viewModel.setScreen("shop") }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Khata Receivables & Payables
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Udhar to Collect
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("To Collect (Udhar)", fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                                        Text(CurrencyFormatter.formatINR(totalReceivable), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = IncomeGreen)
                                        Text("${khataParties.filter { it.type == "CUSTOMER" }.size} Customers", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Dues to Pay
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("To Pay (Supplier)", fontSize = 10.sp, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                                        Text(CurrencyFormatter.formatINR(totalPayable), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ExpenseRed)
                                        Text("${khataParties.filter { it.type == "SUPPLIER" }.size} Suppliers", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Stock Value
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.12f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Stock Worth", fontSize = 10.sp, color = TealAccent, fontWeight = FontWeight.SemiBold)
                                        Text(CurrencyFormatter.formatINR(totalStockValue), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${inventoryItems.size} Products", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Business Quick Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // + Cash Sale
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    initialAddType = "INCOME"
                                    showAddDialog = true
                                }
                                .testTag("business_quick_sale")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("+ Cash Sale", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                        }

                        // - Purchase/Expense
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    initialAddType = "EXPENSE"
                                    showAddDialog = true
                                }
                                .testTag("business_quick_expense")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("- Purchase", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }

                        // GST Invoices
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setScreen("shop") }
                                .testTag("business_quick_invoice")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("GST Bills", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }
                        }

                        // Stock Inventory
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setScreen("shop") }
                                .testTag("business_quick_stock")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Stock (Items)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Top Customers with Outstanding Udhar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Khata Dues",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All Customers ➔",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary,
                            modifier = Modifier.clickable { viewModel.setScreen("shop") }
                        )
                    }
                }

                val dueCustomers = khataParties.filter { it.type == "CUSTOMER" && it.currentBalance > 0 }
                if (dueCustomers.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("All customer accounts are clear! No pending udhar.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(dueCustomers.take(4)) { customer ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setScreen("shop") }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(customer.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(CurrencyFormatter.formatINR(customer.currentBalance), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ExpenseRed)
                                    Text("Pending Udhar", fontSize = 10.sp, color = ExpenseRed)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }

        // =========================================================================
        // PERSISTENT MAIN WINDOW FLOATING ACTIONS:
        // 1. Voice AI Assistant Floating Action Button (Always on Main Window)
        // 2. Gesture Control (+) FAB: Swipe UP for Credit, Pull DOWN for Debit
        // =========================================================================

        // 1. Floating Voice Assistant Pill (Bottom-Start)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = EmeraldPrimary,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp, start = 16.dp)
                .clickable {
                    voiceInitialPanel = appMode
                    showVoiceDialog = true
                }
                .testTag("main_floating_voice_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Assistant", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Voice AI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
        }

        // 2. Gesture-Controlled (+) FAB (Bottom-End)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Resting visual badge hint: Swipe UP for Credit, Pull DOWN for Debit
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

    // Choice Dialog on Tap
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
                                Text("Swipe UP shortcut • Salary, sales, payments", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                initialPanel = voiceInitialPanel,
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

    if (showSmsDialog) {
        currentUser?.let { user ->
            SmsTransactionScannerDialog(
                userId = user.id,
                onDismiss = { showSmsDialog = false },
                onConfirmSave = { tx ->
                    viewModel.addTransaction(tx)
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
