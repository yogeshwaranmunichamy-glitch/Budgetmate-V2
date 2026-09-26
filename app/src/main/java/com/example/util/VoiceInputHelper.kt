package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.BudgetExceededRed
import com.example.ui.theme.EmeraldPrimary
import java.util.Locale

class VoiceInputState(
    val isListening: Boolean,
    val rmsLevel: Float,
    val partialTranscript: String,
    val errorMessage: String?,
    val hasPermission: Boolean,
    private val onStartListening: (language: String, onResult: (String) -> Unit) -> Unit,
    private val onStopListening: () -> Unit,
    private val onClearError: () -> Unit
) {
    fun startListening(language: String = "Tamil + English", onResult: (String) -> Unit) {
        onStartListening(language, onResult)
    }

    fun stopListening() {
        onStopListening()
    }

    fun clearError() {
        onClearError()
    }
}

@Composable
fun rememberVoiceInputState(): VoiceInputState {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var partialTranscript by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var pendingLanguage by remember { mutableStateOf("Tamil + English") }
    var pendingOnResult by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val currentPendingOnResult = rememberUpdatedState(pendingOnResult)

    val hasAudioPermission = remember(context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    var permissionGranted by remember { mutableStateOf(hasAudioPermission) }

    // SpeechRecognizer instance
    var speechRecognizer by remember {
        mutableStateOf<SpeechRecognizer?>(null)
    }

    // Fallback System Intent Launcher (used if SpeechRecognizer service is unavailable)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                errorMessage = null
                currentPendingOnResult.value?.invoke(spokenText)
            } else {
                errorMessage = "No speech detected. Please try again."
            }
        }
    }

    fun launchSystemSpeechIntent(langChoice: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langTag = mapLanguageChoiceToTag(langChoice)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak clearly into your microphone...")
        }
        try {
            systemSpeechLauncher.launch(intent)
        } catch (_: Exception) {
            errorMessage = "Voice recognition service is not available on this device."
        }
    }

    fun startListeningInternal(langChoice: String, onResult: (String) -> Unit) {
        errorMessage = null
        partialTranscript = ""
        pendingOnResult = onResult

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            // SpeechRecognizer service unavailable -> Fallback to system voice dialog
            isListening = true
            launchSystemSpeechIntent(langChoice)
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            val recognizer = speechRecognizer
            if (recognizer == null) {
                isListening = true
                launchSystemSpeechIntent(langChoice)
                return
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val langTag = mapLanguageChoiceToTag(langChoice)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    errorMessage = null
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Map rmsdB (-2 to 10 typical) to 0.0 - 1.0 range
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    rmsLevel = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    rmsLevel = 0f
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error. Please try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout."
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly and try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please wait a moment."
                        SpeechRecognizer.ERROR_SERVER -> "Speech server error. Please try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic to try again."
                        else -> "Speech recognition encountered an issue ($error)."
                    }
                    errorMessage = errorMsg
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    rmsLevel = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val topText = matches?.firstOrNull()
                    if (!topText.isNullOrBlank()) {
                        errorMessage = null
                        currentPendingOnResult.value?.invoke(topText)
                    } else {
                        errorMessage = "No speech detected. Please try again."
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let {
                        partialTranscript = it
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
            isListening = true
        } catch (_: Exception) {
            // Fallback to system intent
            isListening = true
            launchSystemSpeechIntent(langChoice)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            errorMessage = null
            pendingOnResult?.let { callback ->
                startListeningInternal(pendingLanguage, callback)
            }
        } else {
            isListening = false
            errorMessage = "Microphone permission is required to capture voice input. Please grant permission."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }

    return remember(isListening, rmsLevel, partialTranscript, errorMessage, permissionGranted) {
        VoiceInputState(
            isListening = isListening,
            rmsLevel = rmsLevel,
            partialTranscript = partialTranscript,
            errorMessage = errorMessage,
            hasPermission = permissionGranted,
            onStartListening = { lang, onResult ->
                pendingLanguage = lang
                pendingOnResult = onResult
                val currentPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                permissionGranted = currentPermission

                if (currentPermission) {
                    startListeningInternal(lang, onResult)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStopListening = {
                isListening = false
                rmsLevel = 0f
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) {}
            },
            onClearError = {
                errorMessage = null
            }
        )
    }
}

fun mapLanguageChoiceToTag(languageChoice: String): String {
    return when (languageChoice) {
        "Tamil", "ta", "Tamil + English" -> "ta-IN"
        "Hindi", "hi", "Hindi + English" -> "hi-IN"
        "English", "en" -> "en-IN"
        "Auto Detect" -> Locale.getDefault().toLanguageTag()
        else -> "en-IN"
    }
}

@Composable
fun VoicePulseMicButton(
    isListening: Boolean,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    testTag: String = "voice_pulse_mic_button",
    contentDescription: String = "Voice Microphone"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size + 16.dp)
    ) {
        // Outer animated ripple ring when listening
        if (isListening) {
            val dynamicScale = (1.0f + rmsLevel * 0.4f).coerceIn(1.0f, 1.45f) * pulseScale
            Box(
                modifier = Modifier
                    .size(size + 14.dp)
                    .scale(dynamicScale)
                    .clip(CircleShape)
                    .background(BudgetExceededRed.copy(alpha = 0.25f))
            )
        }

        // Inner main button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (isListening) BudgetExceededRed else EmeraldPrimary)
                .clickable { onClick() }
                .testTag(testTag)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.Mic,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
