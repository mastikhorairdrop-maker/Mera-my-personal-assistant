package com.example.live

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * MayaSpeaker provides ultra-fast, zero-lag local text-to-speech
 * using Android's built-in TTS engine. Delivers instant voice responses
 * in under 100ms with a warm, sweet, romantic tone.
 */
object MayaSpeaker {
    private const val TAG = "MayaSpeaker"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onSpeechDoneCallback: (() -> Unit)? = null

    fun init(context: Context, onReady: (() -> Unit)? = null) {
        if (isInitialized && tts != null) {
            onReady?.invoke()
            return
        }

        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val hindiLocale = Locale("hi", "IN")
                    val result = tts?.setLanguage(hindiLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Hindi not available, falling back to default locale")
                        tts?.setLanguage(Locale.getDefault())
                    }

                    // Sweet, youthful, expressive pitch & pace
                    tts?.setPitch(1.18f)
                    tts?.setSpeechRate(1.05f)

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "Maya speaking started: $utteranceId")
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "Maya speaking completed: $utteranceId")
                            onSpeechDoneCallback?.invoke()
                            onSpeechDoneCallback = null
                        }

                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "Maya speaking error: $utteranceId")
                            onSpeechDoneCallback?.invoke()
                            onSpeechDoneCallback = null
                        }
                    })

                    isInitialized = true
                    Log.i(TAG, "MayaSpeaker initialized successfully.")
                    onReady?.invoke()
                } else {
                    Log.e(TAG, "Failed to initialize TextToSpeech: status=$status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing MayaSpeaker: ${e.message}")
        }
    }

    fun speak(
        text: String,
        flush: Boolean = true,
        onDone: (() -> Unit)? = null
    ) {
        if (text.isBlank()) return
        onSpeechDoneCallback = onDone

        try {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "maya_speech_${System.currentTimeMillis()}")
            }
            tts?.speak(text, queueMode, params, "maya_speech_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: ${e.message}")
            onDone?.invoke()
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
