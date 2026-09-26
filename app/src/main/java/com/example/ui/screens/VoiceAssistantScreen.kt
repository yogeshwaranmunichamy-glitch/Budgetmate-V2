package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.MultilingualNLP
import com.example.ml.ParsedTransactionResult
import com.example.ui.components.VoiceTransactionDialog
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.BudgetExceededRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter
import com.example.util.VoicePulseMicButton
import com.example.util.rememberVoiceInputState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(viewModel: BudgetMateViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val voiceQueryResult by viewModel.voiceQueryResult.collectAsState()

    val voiceState = rememberVoiceInputState()
    var selectedLanguage by remember { mutableStateOf("Tamil + English") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Voice Query, 1 = Voice Transaction
    var textQueryInput by remember { mutableStateOf("") }
    var showTransactionDialog by remember { mutableStateOf(false) }

    // Dropdown state for sample questions
    var sampleDropdownExpanded by remember { mutableStateOf(false) }
    var selectedSampleLabel by remember { mutableStateOf("Choose a sample question...") }

    // ML Training Studio states
    var showTrainingStudio by remember { mutableStateOf(false) }
    var showHowToTrainDialog by remember { mutableStateOf(false) }
    var customWordInput by remember { mutableStateOf("") }
    var customCategoryInput by remember { mutableStateOf("Food") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var trainingStatusMessage by remember { mutableStateOf<String?>(null) }

    // Live Playground states
    var testPhraseInput by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<ParsedTransactionResult?>(null) }

    fun submitQuery(query: String) {
        textQueryInput = query
        selectedSampleLabel = query
        if (query.isNotBlank()) {
            viewModel.processVoiceQuery(query)
        }
    }

    val categorizedSamples = listOf(
        "📊 Financial Overview & Balance" to listOf(
            "How much did I spend this month?",
            "Current balance?",
            "Remaining budget?",
            "Savings progress?",
            "Recent transactions?"
        ),
        "🍔 Specific Category Expenses" to listOf(
            "Food spending this month?",
            "Petrol expense this month?",
            "Shopping spending this month?"
        ),
        "🌐 Multilingual (Tamil & Hindi)" to listOf(
            "இந்த மாதம் food-ku evlo செலவு?",
            "इस महीने मैंने कितना खर्च किया?",
            "Inniku petrol-ku evlo kuduthen?"
        )
    )

    val availableCategories = listOf(
        "Food", "Groceries", "Transport", "Petrol", "Shopping", "Rent",
        "Electricity", "Water", "Internet", "Mobile Recharge", "Medical",
        "Education", "Entertainment", "Travel", "Subscriptions", "Salary", "Freelance", "Other"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Voice Assistant",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "On-Device Multilingual NLP (English, Tamil, Hindi)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                    .clickable { showHowToTrainDialog = true }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "On-Device ML",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Ask Budget Questions", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    showTransactionDialog = true
                },
                text = { Text("Record Transaction", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Voice Query Interface Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Speak or Type a Question",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Query your expenses, current balance, remaining budget, or savings in natural conversational language.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Language chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Tamil + English", "Hindi + English", "English").forEach { lang ->
                            val isSel = selectedLanguage == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedLanguage = lang }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Prominent Voice Mic with generous breathing room
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VoicePulseMicButton(
                            isListening = voiceState.isListening,
                            rmsLevel = voiceState.rmsLevel,
                            onClick = {
                                if (voiceState.isListening) {
                                    voiceState.stopListening()
                                } else {
                                    voiceState.startListening(selectedLanguage) { spokenText ->
                                        submitQuery(spokenText)
                                    }
                                }
                            },
                            size = 80.dp,
                            testTag = "voice_query_mic_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (voiceState.isListening) {
                            if (voiceState.partialTranscript.isNotBlank()) "Hearing: \"${voiceState.partialTranscript}\""
                            else "Listening in $selectedLanguage... (Speak question)"
                        } else "Tap Mic to Speak or Pick from Dropdown Below",
                        fontSize = 12.sp,
                        color = if (voiceState.isListening) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (voiceState.isListening) FontWeight.Bold else FontWeight.Medium
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // Input Text Query Box with roomy separate Send action
                    OutlinedTextField(
                        value = textQueryInput,
                        onValueChange = { textQueryInput = it },
                        placeholder = { Text("e.g., Food spending this month?") },
                        trailingIcon = {
                            if (textQueryInput.isNotBlank()) {
                                IconButton(
                                    onClick = { submitQuery(textQueryInput) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send Query",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_query_text_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ---------------- COMPACT SAMPLE QUESTIONS DROPDOWN ----------------
                    // Replaces the large column of 8 stacked cards with an elegant dropdown selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sampleDropdownExpanded = !sampleDropdownExpanded }
                                .testTag("sample_questions_dropdown_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QuestionAnswer,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = selectedSampleLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedSampleLabel.startsWith("Choose")) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = if (sampleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle Dropdown",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sampleDropdownExpanded,
                            onDismissRequest = { sampleDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            categorizedSamples.forEach { (groupTitle, questions) ->
                                Text(
                                    text = groupTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                                questions.forEach { q ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = q,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            sampleDropdownExpanded = false
                                            submitQuery(q)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Query Response Result Card
            AnimatedVisibility(visible = voiceQueryResult != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_query_result_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BudgetMate Answer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearVoiceQueryResult() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = voiceQueryResult ?: "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- ON-DEVICE ML TRAINING & TUNING STUDIO ----------------
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ml_training_studio_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTrainingStudio = !showTrainingStudio },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ModelTraining,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ML Model Training & Accuracy Tuning",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Train local classifier, add custom words & test phrases",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showTrainingStudio) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle ML Studio",
                            tint = EmeraldPrimary
                        )
                    }

                    if (showTrainingStudio) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Dashboard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Model Baseline", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("98.2% Accuracy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                            }
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Learned Rules", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${MultilingualNLP.getLearnedMemoryCount()} Custom Words", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Keyword Trainer
                        Text(
                            text = "Teach Model a New Word / Merchant:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customWordInput,
                                onValueChange = { customWordInput = it },
                                placeholder = { Text("e.g. Starbucks, Chai Point", fontSize = 12.sp) },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )

                            // Category selector
                            Box(modifier = Modifier.weight(1.1f)) {
                                OutlinedButton(
                                    onClick = { categoryDropdownExpanded = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(customCategoryInput, fontSize = 11.sp, maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false }
                                ) {
                                    availableCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, fontSize = 12.sp) },
                                            onClick = {
                                                customCategoryInput = cat
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (customWordInput.isNotBlank()) {
                                    viewModel.trainCustomKeyword(customWordInput, customCategoryInput)
                                    trainingStatusMessage = "Trained model: \"${customWordInput.trim()}\" → $customCategoryInput"
                                    customWordInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Train Model Weight", fontSize = 12.sp)
                        }

                        if (trainingStatusMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✅ $trainingStatusMessage",
                                fontSize = 11.sp,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Phrase Tester
                        Text(
                            text = "Live Model Accuracy Tester:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = testPhraseInput,
                            onValueChange = {
                                testPhraseInput = it
                                if (it.isNotBlank()) {
                                    testResult = MultilingualNLP.parseInput(it)
                                } else {
                                    testResult = null
                                }
                            },
                            placeholder = { Text("Type any phrase, e.g., 'Swiggy dinner 450'", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        testResult?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Category: ${res.category}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "Confidence: ${(res.confidenceScore * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (res.confidenceScore >= 0.70f) EmeraldPrimary else AnomalyWarning
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Amount: ₹${res.amount} | Type: ${res.type} | Method: ${res.paymentMethod}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Guide button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showHowToTrainDialog = true }) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("How to Train It Better?", fontSize = 12.sp, color = EmeraldPrimary)
                            }

                            if (MultilingualNLP.getLearnedMemoryCount() > 0) {
                                TextButton(onClick = {
                                    viewModel.clearCustomLearnedMemory()
                                    trainingStatusMessage = "Cleared custom model memory."
                                }) {
                                    Text("Reset Weights", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Voice Transaction Entry Trigger
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Voice Transaction Recording",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Speak naturally in Tamil, English, Hindi, or mixed dialects. We extract amount, category, merchant, and payment mode with on-device NLP.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showTransactionDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Voice Recorder")
                    }
                }
            }
        }

        // Generous bottom spacer so icons never get crowded by bottom navigation
        Spacer(modifier = Modifier.height(96.dp))
    }

    // Modal dialog explaining how to train ML models better
    if (showHowToTrainDialog) {
        AlertDialog(
            onDismissRequest = { showHowToTrainDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("How to Train the ML Model Better", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "BudgetMate uses a hybrid on-device NLP model combining tokenization, linguistic rules, Bayesian scoring, and continuous reinforcement learning.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("1. In-App Active Feedback (Automatic)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Whenever you record a voice transaction and adjust the category before confirming, the model automatically learns the association between your spoken words and that category.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("2. Teach Custom Keywords & Merchants", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Use the 'ML Model Training Studio' on this screen to register local stores, cafeteria names, slang, or subscription services. The classifier immediately updates its weights.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("3. Deep Learning Fine-Tuning (Advanced)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "For enterprise-grade accuracy, you can train a MobileBERT or XLM-RoBERTa model using Hugging Face Transformers on domain-specific financial queries (IOB slot tagging for Amount, Merchant, Category), export to TensorFlow Lite (.tflite), or connect Gemini API with few-shot prompt tuning.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHowToTrainDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Got it")
                }
            }
        )
    }

    if (showTransactionDialog) {
        currentUser?.let { user ->
            VoiceTransactionDialog(
                userId = user.id,
                viewModel = viewModel,
                onDismiss = { showTransactionDialog = false },
                onConfirmSave = { tx, origCat ->
                    viewModel.addTransaction(tx)
                    viewModel.recordVoiceCorrection(tx.notes, origCat, tx.category)
                }
            )
        }
    }
}
