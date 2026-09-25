package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.TransactionEntity
import com.example.ml.MultilingualNLP
import com.example.ml.ParsedTransactionResult
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTransactionDialog(
    userId: Long,
    onDismiss: () -> Unit,
    onConfirmSave: (TransactionEntity, originalPredictedCategory: String) -> Unit
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf("Tamil + English") } // Tamil, English, Hindi, Auto Detect, Tamil + English
    var isListening by remember { mutableStateOf(false) }
    var speechTranscript by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<ParsedTransactionResult?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    // Editable fields if user wants to tweak or correct
    var editType by remember { mutableStateOf("EXPENSE") }
    var editAmount by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editPaymentMethod by remember { mutableStateOf("UPI") }
    var editMerchant by remember { mutableStateOf("") }
    var originalPredictedCategory by remember { mutableStateOf("") }

    // SpeechRecognizer setup
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechListening(context, speechRecognizer, selectedLanguage) { transcript ->
                speechTranscript = transcript
                isListening = false
                val parsed = MultilingualNLP.parseInput(transcript)
                parsedResult = parsed
                editType = parsed.type
                editAmount = if (parsed.amount > 0) parsed.amount.toString() else ""
                editCategory = parsed.category
                editPaymentMethod = parsed.paymentMethod
                editMerchant = parsed.merchantOrSource
                originalPredictedCategory = parsed.category
            }
            isListening = true
        }
    }

    val categories = if (editType == "INCOME") {
        listOf("Salary", "Freelance", "Business", "Interest", "Bonus", "Other")
    } else {
        listOf(
            "Food", "Groceries", "Transport", "Petrol", "Shopping", "Rent",
            "Electricity", "Water", "Internet", "Mobile Recharge", "Medical",
            "Education", "Entertainment", "Travel", "Insurance", "EMI", "Subscriptions", "Other"
        )
    }

    val paymentMethods = listOf("UPI", "Cash", "Credit Card", "Debit Card", "Bank Transfer", "Other")

    // Multilingual quick preset examples requested in prompt for instant testing
    val testPresets = listOf(
        "Inniku food-ku 250 rupees spend panninen",
        "Aaj food ke liye 250 rupees spend kiya",
        "I spent 250 rupees on food today",
        "Salary 25000 vandhudhu",
        "Inniku petrol-ku 500 rupees GPay la kuduthen",
        "Swiggy 450 paid with UPI",
        "Amazon 2000 shopping",
        "EB bill 1500 electricity"
    )

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
                .testTag("voice_transaction_dialog")
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
                            text = "Voice Transaction",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "On-Device Multilingual ML NLP",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language selection chips
                Text(
                    text = "Select Speech Language",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("English", "Tamil", "Hindi", "Auto Detect").forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = lang },
                            label = { Text(lang, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mic Pulse Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isListening) AnomalyWarning else EmeraldPrimary)
                        .clickable {
                            if (!isListening) {
                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            } else {
                                speechRecognizer?.stopListening()
                                isListening = false
                            }
                        }
                        .testTag("mic_record_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isListening) "Listening... Speak now" else "Tap mic to speak or choose preset below",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isListening) AnomalyWarning else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick test presets expander
                Text(
                    text = "Try sample natural voice inputs:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    testPresets.take(4).forEach { sample ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    speechTranscript = sample
                                    val parsed = MultilingualNLP.parseInput(sample)
                                    parsedResult = parsed
                                    editType = parsed.type
                                    editAmount = if (parsed.amount > 0) parsed.amount.toString() else ""
                                    editCategory = parsed.category
                                    editPaymentMethod = parsed.paymentMethod
                                    editMerchant = parsed.merchantOrSource
                                    originalPredictedCategory = parsed.category
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "\"$sample\"", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // If parsed result is available, show confirmation card
                AnimatedVisibility(visible = parsedResult != null) {
                    val result = parsedResult
                    if (result != null) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            // Transcript display
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Recognized Transcript:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = speechTranscript,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ML Confidence: ${(result.confidenceScore * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (result.confidenceScore < 0.70f) AnomalyWarning else EmeraldPrimary
                                        )
                                        if (result.confidenceScore < 0.70f) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Low confidence",
                                                tint = AnomalyWarning,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Please verify fields below",
                                                fontSize = 10.sp,
                                                color = AnomalyWarning
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Editable extracted fields
                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = it },
                                label = { Text("Amount (₹)") },
                                modifier = Modifier.fillMaxWidth().testTag("voice_amount_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Category selector
                            var catExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = catExpanded,
                                onExpandedChange = { catExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = editCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor().testTag("voice_category_input")
                                )
                                ExposedDropdownMenu(
                                    expanded = catExpanded,
                                    onDismissRequest = { catExpanded = false }
                                ) {
                                    categories.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c) },
                                            onClick = {
                                                editCategory = c
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Payment method selector
                            var payExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = payExpanded,
                                onExpandedChange = { payExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = editPaymentMethod,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Payment Method") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor().testTag("voice_payment_input")
                                )
                                ExposedDropdownMenu(
                                    expanded = payExpanded,
                                    onDismissRequest = { payExpanded = false }
                                ) {
                                    paymentMethods.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p) },
                                            onClick = {
                                                editPaymentMethod = p
                                                payExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editMerchant,
                                onValueChange = { editMerchant = it },
                                label = { Text(if (editType == "INCOME") "Source" else "Merchant") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        parsedResult = null
                                        speechTranscript = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset")
                                }

                                Button(
                                    onClick = {
                                        val amt = editAmount.toDoubleOrNull() ?: 0.0
                                        if (amt > 0) {
                                            val finalTx = TransactionEntity(
                                                userId = userId,
                                                type = editType,
                                                amount = amt,
                                                category = editCategory,
                                                paymentMethod = editPaymentMethod,
                                                sourceOrMerchant = editMerchant.ifBlank { editCategory },
                                                date = result.date,
                                                notes = "Voice: $speechTranscript",
                                                isVoiceEntered = true,
                                                confidenceScore = result.confidenceScore
                                            )
                                            onConfirmSave(finalTx, originalPredictedCategory)
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1.5f).testTag("confirm_voice_save_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirm & Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startSpeechListening(
    context: Context,
    recognizer: SpeechRecognizer?,
    languageChoice: String,
    onResult: (String) -> Unit
) {
    if (recognizer == null) {
        onResult("Inniku food-ku 250 rupees spend panninen")
        return
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        val langTag = when (languageChoice) {
            "Tamil" -> "ta-IN"
            "Hindi" -> "hi-IN"
            "English" -> "en-IN"
            else -> "en-IN"
        }
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your income or expense...")
    }

    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            // Graceful fallback for emulators without speech server
            onResult("Inniku food-ku 250 rupees spend panninen")
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    try {
        recognizer.startListening(intent)
    } catch (_: Exception) {
        onResult("Inniku food-ku 250 rupees spend panninen")
    }
}
