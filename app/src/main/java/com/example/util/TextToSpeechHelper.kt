package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import java.util.UUID

class TextToSpeechState(
    val isReady: Boolean,
    val isSpeaking: Boolean,
    val lastSpokenText: String,
    val errorMessage: String?,
    private val onSpeak: (text: String, onDone: (() -> Unit)?) -> Unit,
    private val onStop: () -> Unit
) {
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        onSpeak(text, onDone)
    }

    fun stop() {
        onStop()
    }
}

@Composable
fun rememberTextToSpeechState(): TextToSpeechState {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var lastSpokenText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentOnDone by remember { mutableStateOf<(() -> Unit)?>(null) }
    val onDoneUpdated = rememberUpdatedState(currentOnDone)

    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        // Set locale: try Indian English, fall back to default
                        val langResult = tts?.setLanguage(Locale("en", "IN"))
                        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts?.setLanguage(Locale.getDefault())
                        }
                        tts?.setSpeechRate(1.0f)
                        tts?.setPitch(1.0f)
                        isReady = true
                    } catch (e: Exception) {
                        isReady = true
                    }
                } else {
                    errorMessage = "Text-to-speech initialization failed"
                    isReady = false
                }
            }

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    onDoneUpdated.value?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeaking = false
                }
            })

            ttsInstance = tts
        } catch (e: Exception) {
            errorMessage = "TTS not available on this device"
        }

        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {}
            ttsInstance = null
        }
    }

    return remember(isReady, isSpeaking, lastSpokenText, errorMessage, ttsInstance) {
        TextToSpeechState(
            isReady = isReady,
            isSpeaking = isSpeaking,
            lastSpokenText = lastSpokenText,
            errorMessage = errorMessage,
            onSpeak = { text, onDone ->
                if (text.isNotBlank() && ttsInstance != null) {
                    try {
                        lastSpokenText = text
                        currentOnDone = onDone
                        val utteranceId = UUID.randomUUID().toString()
                        val params = Bundle().apply {
                            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                        }
                        ttsInstance?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                        isSpeaking = true
                    } catch (e: Exception) {
                        isSpeaking = false
                        onDone?.invoke()
                    }
                } else {
                    onDone?.invoke()
                }
            },
            onStop = {
                try {
                    ttsInstance?.stop()
                } catch (_: Exception) {}
                isSpeaking = false
            }
        )
    }
}
