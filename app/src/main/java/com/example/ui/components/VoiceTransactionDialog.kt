package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.TransactionEntity
import com.example.ml.MultilingualNLP
import com.example.ml.ParsedExpenseItem
import com.example.ml.VoiceCorrectionResult
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.BudgetSafeGreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter
import com.example.util.VoicePulseMicButton
import com.example.util.rememberTextToSpeechState
import com.example.util.rememberVoiceInputState
import kotlinx.coroutines.launch
import java.util.UUID

enum class VoiceFlowStep {
    RECORDING,       // Step 1: User speaks multi-category sentence
    CONFIRMATION,    // Step 2: TTS reads interpreted list back, user confirms or corrects by voice
    DASHBOARD        // Step 3: Interactive Dashboard with totals, category breakdown chart & manual editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTransactionDialog(
    userId: Long,
    onDismiss: () -> Unit,
    onConfirmSave: (TransactionEntity, originalPredictedCategory: String) -> Unit = { _, _ -> },
    viewModel: BudgetMateViewModel? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val voiceState = rememberVoiceInputState()
    val ttsState = rememberTextToSpeechState()

    var currentStep by remember { mutableStateOf(VoiceFlowStep.RECORDING) }
    var selectedLanguage by remember { mutableStateOf("Tamil + English") }
    var speechTranscript by remember { mutableStateOf("") }
    var manualTextInput by remember { mutableStateOf("") }

    // Interpreted structured expenses list
    var pendingExpenses by remember { mutableStateOf<List<ParsedExpenseItem>>(emptyList()) }
    // Confirmed transactions after saving
    var savedTransactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }

    var lastFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var presetDropdownExpanded by remember { mutableStateOf(false) }

    // Manual Editing states for Dashboard / Inline adjustments
    var editingItem by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddManualExpenseDialog by remember { mutableStateOf(false) }

    val allCategories = listOf(
        "Petrol", "Food", "Parking", "Hotel", "Toll", "Groceries", "Transport", "Shopping",
        "Rent", "Electricity", "Water", "Internet", "Mobile Recharge", "Medical",
        "Education", "Entertainment", "Travel", "Insurance", "EMI", "Subscriptions", "Salary", "Other"
    )

    // Realistic voice input sample presets matching user requests
    val realisticPresets = listOf(
        "I spent ₹500 for petrol, ₹300 for food, ₹200 for parking and ₹1,000 for hotel.",
        "I spent 500 on petrol and 300 for food.",
        "Petrol 500, food 300, parking 200.",
        "Today I spent around 500 for fuel, 250 for lunch and 100 for parking.",
        "Inniku petrol-ku 500, food-ku 300 apram parking 200 kuduthen",
        "Paid 450 for Swiggy dinner and 1500 for shopping",
        "Petrol 600, groceries 1200, toll 100 and snacks 150"
    )

    fun processInitialSpeech(spokenText: String) {
        if (spokenText.isBlank()) return
        speechTranscript = spokenText
        manualTextInput = spokenText
        val parsedItems = MultilingualNLP.parseMultiInput(spokenText)

        if (parsedItems.isNotEmpty()) {
            pendingExpenses = parsedItems
            lastFeedbackMessage = "Interpreted ${parsedItems.size} expense entries"
            currentStep = VoiceFlowStep.CONFIRMATION

            // Requirement 2: Read the complete interpreted list back using text-to-speech
            val prompt = MultilingualNLP.formatConfirmationPrompt(parsedItems, isUpdate = false)
            ttsState.speak(prompt)
        } else {
            lastFeedbackMessage = "Could not identify expenses. Please try again."
        }
    }

    fun saveAllConfirmedExpenses(itemsToSave: List<ParsedExpenseItem>) {
        if (isSaving || itemsToSave.isEmpty()) return
        isSaving = true

        val now = System.currentTimeMillis()
        val entities = itemsToSave.map { item ->
            TransactionEntity(
                userId = userId,
                type = "EXPENSE",
                amount = item.amount,
                category = item.category,
                paymentMethod = item.paymentMethod.ifBlank { "UPI" },
                sourceOrMerchant = item.merchant.ifBlank { item.category },
                date = now,
                notes = "Voice: ${item.rawText.ifBlank { speechTranscript }}",
                isVoiceEntered = true,
                confidenceScore = item.confidenceScore
            )
        }

        if (viewModel != null) {
            viewModel.addMultipleTransactions(entities) { savedList ->
                savedTransactions = savedList
                isSaving = false
                currentStep = VoiceFlowStep.DASHBOARD
                lastFeedbackMessage = "Saved ${savedList.size} expenses to database."
            }
        } else {
            // Fallback for direct listener
            val localSaved = mutableListOf<TransactionEntity>()
            entities.forEachIndexed { idx, entity ->
                val simulatedId = System.currentTimeMillis() + idx
                val finalTx = entity.copy(id = simulatedId)
                localSaved.add(finalTx)
                onConfirmSave(finalTx, entity.category)
            }
            savedTransactions = localSaved
            isSaving = false
            currentStep = VoiceFlowStep.DASHBOARD
            lastFeedbackMessage = "Saved ${localSaved.size} expenses to database."
        }
    }

    fun handleVoiceConfirmationOrCorrection(responseSpeech: String) {
        if (responseSpeech.isBlank() || pendingExpenses.isEmpty()) return

        when (val result = MultilingualNLP.processVoiceResponse(responseSpeech, pendingExpenses)) {
            is VoiceCorrectionResult.Confirmed -> {
                // Requirement 3: Confirmed entries -> Save and open Dashboard
                ttsState.speak("Confirmed.")
                saveAllConfirmedExpenses(result.items)
            }
            is VoiceCorrectionResult.Updated -> {
                // Requirement 4: Updated item -> Read updated list back
                pendingExpenses = result.items
                lastFeedbackMessage = result.feedback
                ttsState.speak(result.speechPrompt)
            }
            is VoiceCorrectionResult.Added -> {
                pendingExpenses = result.items
                lastFeedbackMessage = result.feedback
                ttsState.speak(result.speechPrompt)
            }
            is VoiceCorrectionResult.Removed -> {
                pendingExpenses = result.items
                lastFeedbackMessage = result.feedback
                ttsState.speak(result.speechPrompt)
            }
            is VoiceCorrectionResult.Replaced -> {
                pendingExpenses = result.items
                lastFeedbackMessage = result.feedback
                ttsState.speak(result.speechPrompt)
            }
            is VoiceCorrectionResult.Ambiguous -> {
                lastFeedbackMessage = result.prompt
                ttsState.speak(result.prompt)
            }
        }
    }

    Dialog(
        onDismissRequest = {
            ttsState.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .testTag("voice_transaction_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (currentStep) {
                                VoiceFlowStep.RECORDING -> "Voice Expense Input"
                                VoiceFlowStep.CONFIRMATION -> "Voice Confirmation & Correction"
                                VoiceFlowStep.DASHBOARD -> "Expense Summary Dashboard"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (currentStep) {
                                VoiceFlowStep.RECORDING -> "Multi-Category Natural Speech Processing"
                                VoiceFlowStep.CONFIRMATION -> "Listen & Confirm or Correct by Voice"
                                VoiceFlowStep.DASHBOARD -> "Breakdown & Manual Editing"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            ttsState.stop()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ---------------- STEP 1: RECORDING ----------------
                if (currentStep == VoiceFlowStep.RECORDING) {
                    // Language Selection Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Tamil + English", "Hindi + English", "English", "Auto Detect").forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { selectedLanguage = lang },
                                label = { Text(lang, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pulse Mic Button
                    VoicePulseMicButton(
                        isListening = voiceState.isListening,
                        rmsLevel = voiceState.rmsLevel,
                        onClick = {
                            if (voiceState.isListening) {
                                voiceState.stopListening()
                            } else {
                                voiceState.startListening(selectedLanguage) { transcript ->
                                    processInitialSpeech(transcript)
                                }
                            }
                        },
                        size = 80.dp,
                        testTag = "mic_record_button"
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (voiceState.isListening) {
                            if (voiceState.partialTranscript.isNotBlank()) "Hearing: \"${voiceState.partialTranscript}\""
                            else "Listening in $selectedLanguage... Speak your expenses naturally"
                        } else "Tap mic to speak multiple expenses in one sentence",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (voiceState.isListening) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!voiceState.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ ${voiceState.errorMessage}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { voiceState.clearError() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manual text input box fallback
                    OutlinedTextField(
                        value = manualTextInput,
                        onValueChange = { manualTextInput = it },
                        placeholder = { Text("e.g. Spent 500 for petrol, 300 for food and 200 for parking", fontSize = 12.sp) },
                        trailingIcon = {
                            if (manualTextInput.isNotBlank()) {
                                IconButton(
                                    onClick = { processInitialSpeech(manualTextInput) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Process Text", tint = EmeraldPrimary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick realistic presets dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { presetDropdownExpanded = !presetDropdownExpanded }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 Tap to pick sample multi-expense voice inputs...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = presetDropdownExpanded,
                            onDismissRequest = { presetDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            realisticPresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text("\"$preset\"", fontSize = 12.sp) },
                                    onClick = {
                                        presetDropdownExpanded = false
                                        processInitialSpeech(preset)
                                    }
                                )
                            }
                        }
                    }
                }

                // ---------------- STEP 2: CONFIRMATION & CORRECTION ----------------
                if (currentStep == VoiceFlowStep.CONFIRMATION) {
                    // Speaker status card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ttsState.isSpeaking) EmeraldPrimary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (ttsState.isSpeaking) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speaker status",
                                    tint = if (ttsState.isSpeaking) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (ttsState.isSpeaking) "🔊 Reading interpreted list aloud..."
                                    else "Readback complete. Respond by voice or tap below.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Say 'Yes' / 'Okay' to confirm, or speak a correction like 'Petrol should be 600'",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    val prompt = MultilingualNLP.formatConfirmationPrompt(pendingExpenses, isUpdate = true)
                                    ttsState.speak(prompt)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Replay readback",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Interpreted Structured List Card
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
                                    text = "Interpreted Expense Items (${pendingExpenses.size}):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                val total = pendingExpenses.sumOf { it.amount }
                                Text(
                                    text = "Total: ₹${total.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = EmeraldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            pendingExpenses.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${index + 1}. ${item.category}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹${item.amount.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = EmeraldPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                // Remove single item manually
                                                val updated = pendingExpenses.filter { it.id != item.id }
                                                pendingExpenses = updated
                                                val prompt = MultilingualNLP.formatConfirmationPrompt(updated, isUpdate = true)
                                                ttsState.speak(prompt)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove item",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice Interaction Mic for Confirmation / Correction
                    VoicePulseMicButton(
                        isListening = voiceState.isListening,
                        rmsLevel = voiceState.rmsLevel,
                        onClick = {
                            if (voiceState.isListening) {
                                voiceState.stopListening()
                            } else {
                                voiceState.startListening(selectedLanguage) { spokenResponse ->
                                    handleVoiceConfirmationOrCorrection(spokenResponse)
                                }
                            }
                        },
                        size = 72.dp,
                        testTag = "voice_confirmation_mic_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (voiceState.isListening) {
                            if (voiceState.partialTranscript.isNotBlank()) "Hearing: \"${voiceState.partialTranscript}\""
                            else "Listening for confirmation or correction..."
                        } else "Tap Mic and say 'Okay', or say 'Petrol should be 600'",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (voiceState.isListening) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lastFeedbackMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 $lastFeedbackMessage",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick suggestion chips for Voice Confirmation & Voice Correction testing
                    Text(
                        text = "Or test quick response commands:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = { handleVoiceConfirmationOrCorrection("Okay, confirm") },
                                label = { Text("✅ Okay / Confirm", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = false,
                                onClick = { handleVoiceConfirmationOrCorrection("Petrol should be 600") },
                                label = { Text("Petrol should be 600", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = { handleVoiceConfirmationOrCorrection("Food is actually 350") },
                                label = { Text("Food is actually 350", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = false,
                                onClick = { handleVoiceConfirmationOrCorrection("Remove parking") },
                                label = { Text("Remove parking", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { handleVoiceConfirmationOrCorrection("Add 200 for toll") },
                            label = { Text("Add ₹200 for toll", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ttsState.stop()
                                currentStep = VoiceFlowStep.RECORDING
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-record")
                        }

                        Button(
                            onClick = {
                                ttsState.speak("Confirmed.")
                                saveAllConfirmedExpenses(pendingExpenses)
                            },
                            enabled = pendingExpenses.isNotEmpty() && !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.6f)
                                .height(48.dp)
                                .testTag("confirm_voice_save_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm & Save (${pendingExpenses.size})")
                        }
                    }
                }

                // ---------------- STEP 3: DASHBOARD & MANUAL EDITING ----------------
                if (currentStep == VoiceFlowStep.DASHBOARD) {
                    val totalSaved = savedTransactions.sumOf { it.amount }
                    val categoryBreakdown = remember(savedTransactions) {
                        savedTransactions.groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_summary_view")
                    ) {
                        // Success confirmation banner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BudgetSafeGreen.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = BudgetSafeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Voice Expenses Confirmed & Saved",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "All entries recorded to your local budget database.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Overview Metric Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Total Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "₹${totalSaved.toInt()}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Entries", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${savedTransactions.size} items",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Categories", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${categoryBreakdown.size}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Category Breakdown Chart
                        Text(
                            text = "Overall Category Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        categoryBreakdown.forEach { (cat, amt) ->
                            val fraction = if (totalSaved > 0) (amt / totalSaved).toFloat() else 0f
                            val percent = (fraction * 100).toInt()

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("₹${amt.toInt()} ($percent%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    color = EmeraldPrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Structured Breakdown Table with Manual Editing Controls (Requirement 6)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Itemized Breakdown & Actions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            TextButton(
                                onClick = { showAddManualExpenseDialog = true },
                                modifier = Modifier.testTag("add_expense_dashboard_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Expense", fontSize = 12.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Category", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
                                    Text("Amount", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
                                    Text("Actions", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                }

                                savedTransactions.forEach { tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tx.category,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1.5f)
                                        )
                                        Text(
                                            text = "₹${tx.amount.toInt()}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { editingItem = tx },
                                                modifier = Modifier.size(28.dp).testTag("edit_expense_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit entry",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    // Requirement 6: Delete an expense updates total immediately
                                                    if (viewModel != null) {
                                                        viewModel.deleteTransaction(tx)
                                                    }
                                                    savedTransactions = savedTransactions.filter { it.id != tx.id }
                                                },
                                                modifier = Modifier.size(28.dp).testTag("delete_expense_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete entry",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Return / Done Button
                        Button(
                            onClick = {
                                ttsState.stop()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("done_dashboard_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Done & Return to App")
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for Editing an Expense (Requirement 6: Edit category, Edit amount)
    editingItem?.let { txToEdit ->
        var editCategory by remember { mutableStateOf(txToEdit.category) }
        var editAmount by remember { mutableStateOf(txToEdit.amount.toString()) }
        var categoryMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edit Expense", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedButton(
                            onClick = { categoryMenuExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(editCategory, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            allCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        editCategory = cat
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = editAmount.toDoubleOrNull() ?: txToEdit.amount
                        val updatedTx = txToEdit.copy(category = editCategory, amount = newAmount)
                        if (viewModel != null) {
                            viewModel.updateTransaction(updatedTx)
                        }
                        savedTransactions = savedTransactions.map {
                            if (it.id == txToEdit.id) updatedTx else it
                        }
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog for Adding a New Expense Manually (Requirement 6: Add a new expense)
    if (showAddManualExpenseDialog) {
        var addCategory by remember { mutableStateOf("Food") }
        var addAmount by remember { mutableStateOf("") }
        var addCategoryMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddManualExpenseDialog = false },
            title = { Text("Add Expense", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedButton(
                            onClick = { addCategoryMenuExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(addCategory, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = addCategoryMenuExpanded,
                            onDismissRequest = { addCategoryMenuExpanded = false }
                        ) {
                            allCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        addCategory = cat
                                        addCategoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = addAmount,
                        onValueChange = { addAmount = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedAmt = addAmount.toDoubleOrNull() ?: 0.0
                        if (parsedAmt > 0) {
                            val newTx = TransactionEntity(
                                userId = userId,
                                type = "EXPENSE",
                                amount = parsedAmt,
                                category = addCategory,
                                paymentMethod = "UPI",
                                sourceOrMerchant = addCategory,
                                date = System.currentTimeMillis(),
                                notes = "Manual addition from voice dashboard",
                                isVoiceEntered = false,
                                confidenceScore = 1.0f
                            )
                            if (viewModel != null) {
                                coroutineScope.launch {
                                    val saved = viewModel.saveTransactionDirect(newTx)
                                    if (saved != null) {
                                        savedTransactions = savedTransactions + saved
                                    }
                                }
                            } else {
                                val simulated = newTx.copy(id = System.currentTimeMillis())
                                savedTransactions = savedTransactions + simulated
                                onConfirmSave(simulated, addCategory)
                            }
                            showAddManualExpenseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualExpenseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
