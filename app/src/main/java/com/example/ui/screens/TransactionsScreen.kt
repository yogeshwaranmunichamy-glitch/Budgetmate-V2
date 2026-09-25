package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: BudgetMateViewModel) {
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

    // Filter & Sort Logic
    val filteredTransactions = remember(allTransactions, query, selectedType, selectedCategory, selectedPayment, sortBy) {
        allTransactions.filter { tx ->
            val matchesType = selectedType == "ALL" || tx.type == selectedType
            val matchesCategory = selectedCategory == "ALL" || tx.category.equals(selectedCategory, ignoreCase = true)
            val matchesPayment = selectedPayment == "ALL" || tx.paymentMethod.equals(selectedPayment, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    tx.sourceOrMerchant.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.notes.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)
            matchesType && matchesCategory && matchesPayment && matchesQuery
        }.sortedWith { a, b ->
            when (sortBy) {
                "DATE_ASC" -> a.date.compareTo(b.date)
                "AMOUNT_DESC" -> b.amount.compareTo(a.amount)
                "AMOUNT_ASC" -> a.amount.compareTo(b.amount)
                else -> b.date.compareTo(a.date) // DATE_DESC
            }
        }
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
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
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
}
