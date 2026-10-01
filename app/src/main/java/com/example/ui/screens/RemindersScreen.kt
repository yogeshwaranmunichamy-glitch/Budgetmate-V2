package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.PaymentReminderEntity
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: BudgetMateViewModel) {
    val context = LocalContext.current
    val reminders by viewModel.paymentReminders.collectAsState()
    val khataParties by viewModel.khataParties.collectAsState()
    val activeAlarm by viewModel.activeAlarmReminder.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "TO_COLLECT", "TO_PAY", "BILL", "DUE_SOON", "COMPLETED"
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<PaymentReminderEntity?>(null) }

    val now = System.currentTimeMillis()
    val dayMillis = 86400000L

    // Metrics
    val pendingList = reminders.filter { it.status != "COMPLETED" }
    val toCollectSum = pendingList.filter { it.reminderType == "TO_COLLECT" }.sumOf { it.amount }
    val toPaySum = pendingList.filter { it.reminderType in listOf("TO_PAY", "BILL") }.sumOf { it.amount }
    val dueSoonCount = pendingList.count { it.dueDate <= now + (dayMillis * 2) }

    val filteredReminders = reminders.filter { reminder ->
        when (selectedFilter) {
            "TO_COLLECT" -> reminder.reminderType == "TO_COLLECT" && reminder.status != "COMPLETED"
            "TO_PAY" -> reminder.reminderType == "TO_PAY" && reminder.status != "COMPLETED"
            "BILL" -> reminder.reminderType == "BILL" && reminder.status != "COMPLETED"
            "DUE_SOON" -> reminder.status != "COMPLETED" && reminder.dueDate <= now + (dayMillis * 2)
            "COMPLETED" -> reminder.status == "COMPLETED"
            else -> reminder.status != "COMPLETED"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Payment & Loan Reminders", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Udhar collection, EMI loans & alarm alerts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen("dashboard") },
                        modifier = Modifier.testTag("reminders_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("add_reminder_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment Reminder")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Summary Cards Row: To Collect vs To Pay vs Due Soon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // To Collect (Receivables / Udhar)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("To Collect", fontSize = 11.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyFormatter.formatINR(toCollectSum),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            Text("Udhar & credit given", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // To Pay (Loans & EMIs)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("To Pay", fontSize = 11.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyFormatter.formatINR(toPaySum),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Text("Loans, EMIs & Bills", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Due Soon Badge
                    Card(
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (dueSoonCount > 0) AnomalyWarning.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = if (dueSoonCount > 0) AnomalyWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Alerts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$dueSoonCount Due",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dueSoonCount > 0) AnomalyWarning else MaterialTheme.colorScheme.onSurface
                            )
                            Text("Within 48 hours", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 2. Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf(
                        "ALL" to "All Active (${pendingList.size})",
                        "TO_COLLECT" to "To Collect (Udhar)",
                        "TO_PAY" to "To Pay (Loans/EMIs)",
                        "DUE_SOON" to "Due Soon",
                        "COMPLETED" to "Completed"
                    )
                    filterOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 3. Reminders List
            if (filteredReminders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AlarmOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No reminders found in this tab", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Tap '+ Reminder' to create a repayment, loan EMI or bill alert.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredReminders, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        onMarkPaid = { viewModel.markPaymentReminderCompleted(reminder) },
                        onSnooze = { viewModel.snoozePaymentReminder(reminder, 24) },
                        onToggleAlarm = { viewModel.togglePaymentReminderAlarm(reminder) },
                        onTestAlarm = { viewModel.triggerAlarmTest(reminder) },
                        onEdit = { editingReminder = reminder },
                        onDelete = { viewModel.deletePaymentReminder(reminder) },
                        onSendNudge = {
                            val msg = "Hi ${reminder.personOrEntity}, gentle reminder that payment of ${CurrencyFormatter.formatINR(reminder.amount)} for '${reminder.title}' is due by ${formatDate(reminder.dueDate)}. Please settle via UPI/GPay. Thank you!"
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Send Payment Reminder via"))
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || editingReminder != null) {
        PaymentReminderDialog(
            existing = editingReminder,
            khataParties = khataParties.map { it.name },
            onDismiss = {
                showAddDialog = false
                editingReminder = null
            },
            onSave = { reminder ->
                viewModel.savePaymentReminder(reminder)
                showAddDialog = false
                editingReminder = null
            }
        )
    }

    // Active Alarm Simulator Dialog
    if (activeAlarm != null) {
        val alarmItem = activeAlarm!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlarmTest() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AnomalyWarning),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("⏰ PAYMENT REMINDER ALARM!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AnomalyWarning)
                }
            },
            text = {
                Column {
                    Text(
                        text = alarmItem.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${alarmItem.personOrEntity} • ${CurrencyFormatter.formatINR(alarmItem.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (alarmItem.reminderType == "TO_COLLECT") IncomeGreen else ExpenseRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (alarmItem.reminderType == "TO_COLLECT") {
                            "Time to collect ₹${alarmItem.amount.toInt()} from ${alarmItem.personOrEntity}. Send them a quick reminder."
                        } else {
                            "Repayment alert: Settle ₹${alarmItem.amount.toInt()} with ${alarmItem.personOrEntity} before due cutoff."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (alarmItem.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Notes: ${alarmItem.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markPaymentReminderCompleted(alarmItem)
                        viewModel.dismissAlarmTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark As Paid")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.snoozePaymentReminder(alarmItem, 24)
                            viewModel.dismissAlarmTest()
                        }
                    ) {
                        Text("Snooze 1 Day")
                    }
                    Button(
                        onClick = { viewModel.dismissAlarmTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Stop Alarm", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}

@Composable
fun ReminderItemCard(
    reminder: PaymentReminderEntity,
    onMarkPaid: () -> Unit,
    onSnooze: () -> Unit,
    onToggleAlarm: () -> Unit,
    onTestAlarm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSendNudge: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isOverdue = reminder.dueDate < now && reminder.status != "COMPLETED"
    val isDueToday = !isOverdue && (reminder.dueDate - now) <= 86400000L

    val typeColor = when (reminder.reminderType) {
        "TO_COLLECT" -> IncomeGreen
        "TO_PAY" -> ExpenseRed
        else -> AnomalyWarning
    }

    val typeLabel = when (reminder.reminderType) {
        "TO_COLLECT" -> "TO COLLECT (UDHAR)"
        "TO_PAY" -> "TO PAY (LOAN/EMI)"
        else -> "BILL PAYMENT"
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("reminder_card_${reminder.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type badge + Due timing tag + More menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (reminder.status == "COMPLETED") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IncomeGreen.copy(alpha = 0.2f)
                        ) {
                            Text("PAID / SETTLED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IncomeGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    } else if (isOverdue) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ExpenseRed.copy(alpha = 0.2f)
                        ) {
                            Text("⚠️ OVERDUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ExpenseRed, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    } else if (isDueToday) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AnomalyWarning.copy(alpha = 0.2f)
                        ) {
                            Text("🔔 DUE TODAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AnomalyWarning, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Reminder") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Reminder") },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Person Row with Large Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reminder.personOrEntity,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = CurrencyFormatter.formatINR(reminder.amount),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Due Date, Time, and Alarm Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatDate(reminder.dueDate)} at ${reminder.reminderTime}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                    if (reminder.repeatInterval != "NONE") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(${reminder.repeatInterval.lowercase()})", fontSize = 10.sp, color = EmeraldPrimary)
                    }
                }

                // Alarm Bell Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (reminder.isAlarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = "Alarm status",
                        tint = if (reminder.isAlarmEnabled) AnomalyWarning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onToggleAlarm() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (reminder.isAlarmEnabled) "Alarm ON" else "Off",
                        fontSize = 10.sp,
                        color = if (reminder.isAlarmEnabled) AnomalyWarning else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (reminder.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reminder.notes,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Ring Alarm Test Button
                Button(
                    onClick = onTestAlarm,
                    colors = ButtonDefaults.buttonColors(containerColor = AnomalyWarning),
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Alarm", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 2. WhatsApp / SMS Nudge (For Udhar collection)
                if (reminder.reminderType == "TO_COLLECT" || reminder.phoneNumber.isNotBlank()) {
                    OutlinedButton(
                        onClick = onSendNudge,
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Nudge", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Snooze (+1 Day)
                if (reminder.status != "COMPLETED") {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.weight(0.9f).height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+1 Day", fontSize = 10.sp)
                    }
                }

                // 4. Mark Paid
                if (reminder.status != "COMPLETED") {
                    Button(
                        onClick = onMarkPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        modifier = Modifier.weight(0.9f).height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Paid", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentReminderDialog(
    existing: PaymentReminderEntity?,
    khataParties: List<String>,
    onDismiss: () -> Unit,
    onSave: (PaymentReminderEntity) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var personOrEntity by remember { mutableStateOf(existing?.personOrEntity ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    var reminderType by remember { mutableStateOf(existing?.reminderType ?: "TO_COLLECT") }
    var dueDate by remember { mutableLongStateOf(existing?.dueDate ?: (System.currentTimeMillis() + 86400000L)) }
    var reminderTime by remember { mutableStateOf(existing?.reminderTime ?: "10:00 AM") }
    var isAlarmEnabled by remember { mutableStateOf(existing?.isAlarmEnabled ?: true) }
    var phoneNumber by remember { mutableStateOf(existing?.phoneNumber ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var repeatInterval by remember { mutableStateOf(existing?.repeatInterval ?: "NONE") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing != null) "Edit Payment Reminder" else "Add Payment Reminder",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reminder Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf(
                        "TO_COLLECT" to "To Collect (Udhar)",
                        "TO_PAY" to "To Pay (Loan/EMI)",
                        "BILL" to "Bill Payment"
                    )
                    types.forEach { (type, label) ->
                        FilterChip(
                            selected = reminderType == type,
                            onClick = { reminderType = type },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (type == "TO_COLLECT") IncomeGreen else if (type == "TO_PAY") ExpenseRed else AnomalyWarning,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Person / Entity Input
                OutlinedTextField(
                    value = personOrEntity,
                    onValueChange = { personOrEntity = it },
                    label = {
                        Text(
                            when (reminderType) {
                                "TO_COLLECT" -> "Customer / Debtor Name (e.g. Ramesh)"
                                "TO_PAY" -> "Lender / Bank Name (e.g. HDFC Bank, Suresh)"
                                else -> "Biller Name (e.g. Electricity, Landlord)"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("reminder_person_input"),
                    singleLine = true
                )

                // Quick suggestions from Khata customers
                if (khataParties.isNotEmpty()) {
                    Text("Quick pick from Khata contacts:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        khataParties.take(3).forEach { partyName ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    personOrEntity = partyName
                                    if (title.isBlank()) title = "$partyName Udhar Repayment"
                                }
                            ) {
                                Text(partyName, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title / Purpose") },
                    placeholder = { Text(if (reminderType == "TO_COLLECT") "e.g. Ramesh ₹500 grocery udhar" else "e.g. Car Loan EMI") },
                    modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
                    singleLine = true
                )

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("reminder_amount_input"),
                    singleLine = true
                )

                // Due Date Presets
                Text("Due Date & Alarm Schedule:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dayMs = 86400000L
                    val datePresets = listOf(
                        "Today" to System.currentTimeMillis(),
                        "Tomorrow" to System.currentTimeMillis() + dayMs,
                        "In 3 Days" to System.currentTimeMillis() + (dayMs * 3),
                        "In 1 Week" to System.currentTimeMillis() + (dayMs * 7)
                    )
                    datePresets.forEach { (label, timestamp) ->
                        FilterChip(
                            selected = Math.abs(dueDate - timestamp) < (dayMs / 2),
                            onClick = { dueDate = timestamp },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }

                // Time picker presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("09:00 AM", "12:00 PM", "04:00 PM", "08:00 PM").forEach { time ->
                        FilterChip(
                            selected = reminderTime == time,
                            onClick = { reminderTime = time },
                            label = { Text(time, fontSize = 10.sp) }
                        )
                    }
                }

                // Alarm Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ring Alarm & Push Notification", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Plays audible alarm sound and vibrates on due time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isAlarmEnabled,
                        onCheckedChange = { isAlarmEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }

                // Repeat Interval
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("NONE" to "Once", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly EMI").forEach { (interval, label) ->
                        FilterChip(
                            selected = repeatInterval == interval,
                            onClick = { repeatInterval = interval },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }

                // Phone number
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Contact Phone (for WhatsApp nudge)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Account Details") },
                    placeholder = { Text("e.g. Account number, promised return date") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = ExpenseRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (personOrEntity.isBlank()) {
                        errorMessage = "Please enter person or lender name."
                        return@Button
                    }
                    if (title.isBlank()) {
                        title = "$personOrEntity ${if (reminderType == "TO_COLLECT") "Udhar" else "Repayment"}"
                    }
                    if (amount <= 0.0) {
                        errorMessage = "Please enter a valid amount."
                        return@Button
                    }

                    onSave(
                        PaymentReminderEntity(
                            id = existing?.id ?: 0L,
                            userId = existing?.userId ?: 1L,
                            title = title,
                            personOrEntity = personOrEntity,
                            amount = amount,
                            reminderType = reminderType,
                            dueDate = dueDate,
                            reminderTime = reminderTime,
                            isAlarmEnabled = isAlarmEnabled,
                            phoneNumber = phoneNumber,
                            notes = notes,
                            repeatInterval = repeatInterval,
                            status = existing?.status ?: "PENDING"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_reminder_confirm_button")
            ) {
                Text(if (existing != null) "Update" else "Save Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return sdf.format(Date(timestamp))
}
