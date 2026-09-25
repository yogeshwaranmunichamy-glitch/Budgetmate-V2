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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ml.ParsedTripVoiceExpense
import com.example.ml.TripAnalyticsEngine
import com.example.ui.theme.BudgetExceededRed
import com.example.ui.theme.BudgetSafeGreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.BudgetMateViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripVoiceEntryDialog(
    trip: TripEntity,
    viewModel: BudgetMateViewModel,
    onDismiss: () -> Unit
) {
    val companions = remember(trip.companions) {
        trip.companions.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    var transcriptText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf("Tamil + English") }
    var parsedResult by remember { mutableStateOf<ParsedTripVoiceExpense?>(null) }

    // Editable fields once parsed
    var editTitle by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Food & Dining") }
    var editPaidBy by remember { mutableStateOf(companions.firstOrNull() ?: "Me") }
    var editPaymentMethod by remember { mutableStateOf("UPI") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var paidByExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }

    val quickPresets = listOf(
        "Paid 1200 for hotel dinner in Goa" to "English",
        "Goa trip-ku cab 650 rupees Rahul kuduthan" to "Tamil + Eng",
        "Manali hotel booking 4500 rupees paid via UPI" to "Hindi + Eng",
        "Scuba diving 3500 rupees cash la kuduthen" to "Tamil + Eng",
        "Beach shack seafood 1800 paid by Priya" to "English"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("trip_voice_entry_dialog")
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
                    Column {
                        Text(
                            text = "🎙️ Voice Log Trip Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Trip: ${trip.name} (${trip.coverEmoji})",
                            fontSize = 12.sp,
                            color = EmeraldPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Language selection chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Tamil + English", "Hindi + English", "English", "Auto Detect").forEach { lang ->
                        val isSel = selectedLang == lang
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedLang = lang }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Microphone pulse button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (isListening) BudgetExceededRed else EmeraldPrimary)
                        .clickable {
                            isListening = !isListening
                            if (!isListening && transcriptText.isNotBlank()) {
                                val res = TripAnalyticsEngine.parseTripVoiceExpense(transcriptText, companions)
                                parsedResult = res
                                editTitle = res.title
                                editAmount = res.amount.toString()
                                editCategory = res.category
                                editPaidBy = res.paidBy
                                editPaymentMethod = res.paymentMethod
                            }
                        }
                        .testTag("trip_voice_record_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Speech",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isListening) "Listening in $selectedLang..." else "Tap Mic or pick sample below",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Transcript box
                OutlinedTextField(
                    value = transcriptText,
                    onValueChange = {
                        transcriptText = it
                        if (it.isNotBlank()) {
                            val res = TripAnalyticsEngine.parseTripVoiceExpense(it, companions)
                            parsedResult = res
                            editTitle = res.title
                            editAmount = res.amount.toString()
                            editCategory = res.category
                            editPaidBy = res.paidBy
                            editPaymentMethod = res.paymentMethod
                        }
                    },
                    label = { Text("Transcript / Speech Input") },
                    placeholder = { Text("e.g., Goa dinner 1500 Rahul paid") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick presets
                Text(
                    text = "Quick Sample Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickPresets.forEach { (text, tag) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    transcriptText = text
                                    val res = TripAnalyticsEngine.parseTripVoiceExpense(text, companions)
                                    parsedResult = res
                                    editTitle = res.title
                                    editAmount = res.amount.toString()
                                    editCategory = res.category
                                    editPaidBy = res.paidBy
                                    editPaymentMethod = res.paymentMethod
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = text,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                text = "[$tag]",
                                fontSize = 10.sp,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // If parsed, show confirmation card with confidence
                if (parsedResult != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ML Extracted Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "Confidence: ${(parsedResult!!.confidenceScore * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BudgetSafeGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = it },
                                label = { Text("Amount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Category Dropdown
                            ExposedDropdownMenuBox(
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = !categoryExpanded }
                            ) {
                                OutlinedTextField(
                                    value = "${TripAnalyticsEngine.getCategoryEmoji(editCategory)} $editCategory",
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
                                                editCategory = cat
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Paid By Selector
                            ExposedDropdownMenuBox(
                                expanded = paidByExpanded,
                                onExpandedChange = { paidByExpanded = !paidByExpanded }
                            ) {
                                OutlinedTextField(
                                    value = "Paid By: $editPaidBy",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Who Paid") },
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
                                                editPaidBy = comp
                                                paidByExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Payment Method
                            ExposedDropdownMenuBox(
                                expanded = paymentMethodExpanded,
                                onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                            ) {
                                OutlinedTextField(
                                    value = editPaymentMethod,
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
                                                editPaymentMethod = method
                                                paymentMethodExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                parsedResult = null
                                transcriptText = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val parsedAmt = editAmount.toDoubleOrNull() ?: 0.0
                                if (editTitle.isNotBlank() && parsedAmt > 0) {
                                    viewModel.confirmTripVoiceExpense(
                                        title = editTitle,
                                        amount = parsedAmt,
                                        category = editCategory,
                                        paidBy = editPaidBy,
                                        paymentMethod = editPaymentMethod
                                    )
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("trip_voice_confirm_save_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm & Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripVoiceQueryDialog(
    trip: TripEntity,
    viewModel: BudgetMateViewModel,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    var answerText by remember { mutableStateOf<String?>(null) }

    val quickQuestions = listOf(
        "How much spent on food?",
        "What is remaining trip budget?",
        "Who owes whom in Goa trip?",
        "What is our daily spending pace?",
        "Total trip expense so far?"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("trip_voice_query_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "💬 Voice Query Trip",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${trip.name} (${trip.coverEmoji})",
                            fontSize = 12.sp,
                            color = EmeraldPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = queryText,
                    onValueChange = {
                        queryText = it
                        if (it.isNotBlank()) {
                            viewModel.processTripVoiceQuery(it)
                            answerText = viewModel.tripVoiceResponse.value
                        }
                    },
                    label = { Text("Ask a question about trip expenses") },
                    placeholder = { Text("e.g. How much spent on food?") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Suggested Questions:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickQuestions.forEach { q ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    queryText = q
                                    viewModel.processTripVoiceQuery(q)
                                    answerText = viewModel.tripVoiceResponse.value
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = "🗣️ $q", fontSize = 12.sp)
                        }
                    }
                }

                if (answerText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Answer (Instant Local ML):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = answerText!!,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}
