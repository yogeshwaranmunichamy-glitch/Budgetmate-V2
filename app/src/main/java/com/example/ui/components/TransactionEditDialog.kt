package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.TransactionEntity
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditDialog(
    userId: Long,
    existingTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var type by remember { mutableStateOf(existingTransaction?.type ?: "EXPENSE") }
    var amountText by remember { mutableStateOf(existingTransaction?.let { if (it.amount > 0) it.amount.toString() else "" } ?: "") }
    var category by remember { mutableStateOf(existingTransaction?.category ?: if (type == "INCOME") "Salary" else "Food") }
    var paymentMethod by remember { mutableStateOf(existingTransaction?.paymentMethod ?: "UPI") }
    var sourceOrMerchant by remember { mutableStateOf(existingTransaction?.sourceOrMerchant ?: "") }
    var notes by remember { mutableStateOf(existingTransaction?.notes ?: "") }

    val expenseCategories = listOf(
        "Food", "Groceries", "Transport", "Petrol", "Shopping", "Rent",
        "Electricity", "Water", "Internet", "Mobile Recharge", "Medical",
        "Education", "Entertainment", "Travel", "Insurance", "EMI", "Subscriptions", "Other"
    )

    val incomeCategories = listOf(
        "Salary", "Freelance", "Business", "Interest", "Bonus", "Other"
    )

    val paymentMethods = listOf("Cash", "UPI", "Credit Card", "Debit Card", "Bank Transfer", "Other")

    val categories = if (type == "INCOME") incomeCategories else expenseCategories

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("transaction_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingTransaction == null) "Add Transaction" else "Edit Transaction",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Income / Expense Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = {
                            type = "EXPENSE"
                            if (category !in expenseCategories) category = "Food"
                        },
                        label = { Text("Expense", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("select_expense_chip")
                    )

                    FilterChip(
                        selected = type == "INCOME",
                        onClick = {
                            type = "INCOME"
                            if (category !in incomeCategories) category = "Salary"
                        },
                        label = { Text("Income", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("select_income_chip")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category dropdown
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().testTag("transaction_category_field")
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    category = c
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Method dropdown
                var payExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = payExpanded,
                    onExpandedChange = { payExpanded = it }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().testTag("transaction_payment_field")
                    )
                    ExposedDropdownMenu(
                        expanded = payExpanded,
                        onDismissRequest = { payExpanded = false }
                    ) {
                        paymentMethods.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    paymentMethod = p
                                    payExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Merchant or Source
                OutlinedTextField(
                    value = sourceOrMerchant,
                    onValueChange = { sourceOrMerchant = it },
                    label = { Text(if (type == "INCOME") "Source (e.g. Employer, Client)" else "Merchant (e.g. Swiggy, Amazon)") },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_source_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_notes_field"),
                    maxLines = 3
                )

                if (existingTransaction?.isAnomaly == true) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AnomalyWarning.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AnomalyWarning)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = existingTransaction.anomalyReason.ifBlank { "Unusual spending flagged by ML engine." },
                                fontSize = 11.sp,
                                color = AnomalyWarning
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                val tx = existingTransaction?.copy(
                                    type = type,
                                    amount = amt,
                                    category = category,
                                    paymentMethod = paymentMethod,
                                    sourceOrMerchant = sourceOrMerchant.ifBlank { category },
                                    notes = notes
                                ) ?: TransactionEntity(
                                    userId = userId,
                                    type = type,
                                    amount = amt,
                                    category = category,
                                    paymentMethod = paymentMethod,
                                    sourceOrMerchant = sourceOrMerchant.ifBlank { category },
                                    date = System.currentTimeMillis(),
                                    notes = notes
                                )
                                onSave(tx)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1.5f).testTag("save_transaction_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
