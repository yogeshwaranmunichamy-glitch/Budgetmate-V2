package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.TripEntity
import com.example.data.local.entities.TripExpenseEntity
import com.example.data.local.entities.TripPlanItemEntity
import com.example.ml.TripAnalyticsEngine
import com.example.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditTripDialog(
    tripToEdit: TripEntity? = null,
    onDismiss: () -> Unit,
    onSave: (TripEntity) -> Unit
) {
    var name by remember { mutableStateOf(tripToEdit?.name ?: "") }
    var destination by remember { mutableStateOf(tripToEdit?.destination ?: "") }
    var budget by remember { mutableStateOf(if (tripToEdit != null && tripToEdit.budget > 0) tripToEdit.budget.toInt().toString() else "30000") }
    var companions by remember { mutableStateOf(tripToEdit?.companions ?: "Me, Rahul, Priya, Arun") }
    var coverEmoji by remember { mutableStateOf(tripToEdit?.coverEmoji ?: "🏖️") }
    var status by remember { mutableStateOf(tripToEdit?.status ?: "ACTIVE") }
    var notes by remember { mutableStateOf(tripToEdit?.notes ?: "") }

    val emojis = listOf("🏖️", "🏔️", "✈️", "🌴", "🚗", "🏕️", "🛳️", "⛩️", "🏰", "🌆")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("create_edit_trip_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (tripToEdit == null) "🌴 Create New Trip" else "✏️ Edit Trip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Emoji picker
                Text(
                    text = "Cover Icon:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    emojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (coverEmoji == emoji) EmeraldPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { coverEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Trip Name") },
                    placeholder = { Text("e.g. Goa Beach Vacation 2026") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    placeholder = { Text("e.g. North Goa, India") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Total Trip Budget (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = companions,
                    onValueChange = { companions = it },
                    label = { Text("Travel Companions (for bill split)") },
                    placeholder = { Text("Comma separated: Me, Rahul, Priya") },
                    supportingText = { Text("Used to split expenses and calculate who owes whom", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Trip Notes / Details") },
                    placeholder = { Text("e.g. Flight 6E 302, Resort check-in at 2 PM") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            val parsedBudget = budget.toDoubleOrNull() ?: 25000.0
                            val now = System.currentTimeMillis()
                            val trip = tripToEdit?.copy(
                                name = name.ifBlank { "Vacation Trip" },
                                destination = destination.ifBlank { "Destination" },
                                budget = parsedBudget,
                                companions = companions.ifBlank { "Me" },
                                coverEmoji = coverEmoji,
                                notes = notes,
                                status = status
                            ) ?: TripEntity(
                                userId = 0L,
                                name = name.ifBlank { "Vacation Trip" },
                                destination = destination.ifBlank { "Destination" },
                                startDate = now,
                                endDate = now + (86400000L * 5),
                                budget = parsedBudget,
                                currency = "INR",
                                companions = companions.ifBlank { "Me" },
                                coverEmoji = coverEmoji,
                                notes = notes,
                                status = status
                            )
                            onSave(trip)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_trip_dialog_button")
                    ) {
                        Text("Save Trip", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTripExpenseDialog(
    trip: TripEntity,
    expenseToEdit: TripExpenseEntity? = null,
    onDismiss: () -> Unit,
    onSave: (TripExpenseEntity) -> Unit
) {
    val companions = remember(trip.companions) {
        trip.companions.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amount by remember { mutableStateOf(if (expenseToEdit != null) expenseToEdit.amount.toString() else "") }
    var category by remember { mutableStateOf(expenseToEdit?.category ?: "Food & Dining") }
    var paidBy by remember { mutableStateOf(expenseToEdit?.paidBy ?: companions.firstOrNull() ?: "Me") }
    var splitAmong by remember { mutableStateOf(expenseToEdit?.splitAmong ?: "All") }
    var paymentMethod by remember { mutableStateOf(expenseToEdit?.paymentMethod ?: "UPI") }
    var notes by remember { mutableStateOf(expenseToEdit?.notes ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var paidByExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_trip_expense_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expenseToEdit == null) "➕ Add Trip Expense" else "✏️ Edit Expense",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title / Merchant") },
                    placeholder = { Text("e.g. Scuba diving, Dinner, Resort room") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = "${TripAnalyticsEngine.getCategoryEmoji(category)} $category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        TripAnalyticsEngine.TRIP_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${TripAnalyticsEngine.getCategoryEmoji(cat)} $cat") },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Paid By Companion selector
                ExposedDropdownMenuBox(
                    expanded = paidByExpanded,
                    onExpandedChange = { paidByExpanded = !paidByExpanded }
                ) {
                    OutlinedTextField(
                        value = "Paid By: $paidBy",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Who Paid?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidByExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = paidByExpanded,
                        onDismissRequest = { paidByExpanded = false }
                    ) {
                        companions.forEach { comp ->
                            DropdownMenuItem(
                                text = { Text(comp) },
                                onClick = {
                                    paidBy = comp
                                    paidByExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Split selector
                OutlinedTextField(
                    value = splitAmong,
                    onValueChange = { splitAmong = it },
                    label = { Text("Split Among") },
                    placeholder = { Text("All or comma-separated names") },
                    supportingText = { Text("Default 'All' splits equally among all trip companions", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Method
                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }
                    ) {
                        listOf("UPI", "Cash", "Credit Card", "Debit Card", "Other").forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            val parsedAmt = amount.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && parsedAmt > 0) {
                                val entity = expenseToEdit?.copy(
                                    title = title,
                                    amount = parsedAmt,
                                    category = category,
                                    paidBy = paidBy,
                                    splitAmong = splitAmong.ifBlank { "All" },
                                    paymentMethod = paymentMethod,
                                    notes = notes
                                ) ?: TripExpenseEntity(
                                    tripId = trip.id,
                                    userId = 0L,
                                    title = title,
                                    amount = parsedAmt,
                                    category = category,
                                    paidBy = paidBy,
                                    splitAmong = splitAmong.ifBlank { "All" },
                                    date = System.currentTimeMillis(),
                                    paymentMethod = paymentMethod,
                                    notes = notes
                                )
                                onSave(entity)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_trip_expense_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddTripPlanItemDialog(
    tripId: Long,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var dayNumber by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "📝 Add Itinerary / Checklist Item",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity / Task Title") },
                    placeholder = { Text("e.g. Book dolphin cruise, Pack sunscreen") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Estimated Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0 if non-expense task") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dayNumber,
                    onValueChange = { dayNumber = it },
                    label = { Text("Day Number (0 = Pre-Trip, 1 = Day 1...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            val parsedCost = cost.toDoubleOrNull() ?: 0.0
                            val parsedDay = dayNumber.toIntOrNull() ?: 0
                            if (title.isNotBlank()) {
                                onSave(title, parsedCost, parsedDay)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Item", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
