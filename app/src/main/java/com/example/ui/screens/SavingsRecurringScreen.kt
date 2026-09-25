package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.RecurringTransactionEntity
import com.example.data.local.entities.SavingsGoalEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsRecurringScreen(viewModel: BudgetMateViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val recurringList by viewModel.recurringList.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Savings, 1 = Recurring
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var addingFundsGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var fundAmountInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Text(
                text = "Savings & Subscriptions",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Target tracking and automated recurring bills",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Savings Goals (${savingsGoals.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recurring Bills (${recurringList.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Savings Goals Tab
                if (savingsGoals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No savings goals yet. Tap + to set a goal!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savingsGoals, key = { it.id }) { goal ->
                            val pct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100.0 else 0.0
                            val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth().testTag("savings_goal_${goal.id}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Target: ${CurrencyFormatter.formatDate(goal.targetDate)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { viewModel.deleteSavingsGoal(goal) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                        color = EmeraldPrimary,
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Saved", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(CurrencyFormatter.formatINR(goal.currentAmount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldPrimary)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Goal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(CurrencyFormatter.formatINR(goal.targetAmount), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { addingFundsGoal = goal },
                                            modifier = Modifier.height(34.dp).testTag("add_funds_button_${goal.id}")
                                        ) {
                                            Text("+ Add Funds", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            } else {
                // Recurring Transactions Tab
                if (recurringList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recurring transactions configured.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(recurringList, key = { it.id }) { rec ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(EmeraldPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.EventRepeat, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rec.sourceOrMerchant, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${rec.frequency} • Next: ${CurrencyFormatter.formatDate(rec.nextDueDate)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(CurrencyFormatter.formatINR(rec.amount), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    IconButton(onClick = { viewModel.deleteRecurring(rec) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                if (selectedTab == 0) showAddGoalDialog = true
                else showAddRecurringDialog = true
            },
            containerColor = EmeraldPrimary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    // Add Funds Dialog
    if (addingFundsGoal != null) {
        val goal = addingFundsGoal!!
        AlertDialog(
            onDismissRequest = { addingFundsGoal = null },
            title = { Text("Deposit to ${goal.title}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = fundAmountInput,
                    onValueChange = { fundAmountInput = it },
                    label = { Text("Amount to Add (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("fund_amount_input"),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = fundAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.addFundsToGoal(goal.id, amt)
                            addingFundsGoal = null
                            fundAmountInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Add Funds")
                }
            },
            dismissButton = {
                TextButton(onClick = { addingFundsGoal = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Savings Goal Dialog
    if (showAddGoalDialog) {
        var goalTitle by remember { mutableStateOf("") }
        var targetAmountText by remember { mutableStateOf("") }
        var notesText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("New Savings Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal Name (e.g. New Bike, Emergency)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = { targetAmountText = it },
                        label = { Text("Target Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetAmt = targetAmountText.toDoubleOrNull() ?: 0.0
                        if (goalTitle.isNotBlank() && targetAmt > 0) {
                            val defaultTargetDate = System.currentTimeMillis() + (86400000L * 180) // 6 months
                            viewModel.addSavingsGoal(goalTitle, targetAmt, defaultTargetDate, notesText)
                            showAddGoalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Create Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Recurring Transaction Dialog
    if (showAddRecurringDialog) {
        var recName by remember { mutableStateOf("") }
        var recAmountText by remember { mutableStateOf("") }
        var recFrequency by remember { mutableStateOf("MONTHLY") }
        var recCategory by remember { mutableStateOf("Rent") }
        var freqExpanded by remember { mutableStateOf(false) }

        val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")

        AlertDialog(
            onDismissRequest = { showAddRecurringDialog = false },
            title = { Text("New Recurring Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = recName,
                        onValueChange = { recName = it },
                        label = { Text("Merchant / Description (e.g. Netflix, Rent)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = recAmountText,
                        onValueChange = { recAmountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = freqExpanded,
                        onExpandedChange = { freqExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = recFrequency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = freqExpanded,
                            onDismissRequest = { freqExpanded = false }
                        ) {
                            frequencies.forEach { f ->
                                DropdownMenuItem(text = { Text(f) }, onClick = { recFrequency = f; freqExpanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = recAmountText.toDoubleOrNull() ?: 0.0
                        if (recName.isNotBlank() && amt > 0) {
                            val recurring = RecurringTransactionEntity(
                                userId = currentUser?.id ?: 0,
                                type = "EXPENSE",
                                amount = amt,
                                category = recCategory,
                                paymentMethod = "Bank Transfer",
                                sourceOrMerchant = recName,
                                frequency = recFrequency,
                                nextDueDate = System.currentTimeMillis() + (86400000L * 30),
                                notes = recName
                            )
                            viewModel.addRecurringTransaction(recurring)
                            showAddRecurringDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Recurring")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRecurringDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
