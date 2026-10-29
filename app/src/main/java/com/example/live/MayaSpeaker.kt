package com.example.live

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * MayaSpeaker provides ultra-fast, natural, sweet Hindi voice output.
 * - Chooses the best natural Indian Female neural voice available on the device.
 * - Strips emojis and symbols so it NEVER reads "दिल" or weird punctuation out loud.
 * - Perfectly tuned pitch (1.22f) and rate (0.98f) to sound sweet, caring, and human-like.
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

                    // Select the best natural female voice from available voices
                    selectBestFemaleVoice()

                    // Sweet, youthful, expressive pitch & pace matching Maya in videos
                    tts?.setPitch(1.22f)
                    tts?.setSpeechRate(0.98f)

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
                    Log.i(TAG, "MayaSpeaker initialized with natural sweet female voice.")
                    onReady?.invoke()
                } else {
                    Log.e(TAG, "Failed to initialize TextToSpeech: status=$status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing MayaSpeaker: ${e.message}")
        }
    }

    private fun selectBestFemaleVoice() {
        try {
            val voices = tts?.voices ?: return
            var bestVoice: Voice? = null

            // Prioritize high-quality neural female Hindi voices
            val preferredVoiceNames = listOf(
                "hi-in-x-hie-network",
                "hi-in-x-hie-local",
                "hi-in-x-hid-network",
                "hi-in-x-hid-local",
                "hi-in-x-hic-network",
                "hi-in-x-hic-local"
            )

            for (pref in preferredVoiceNames) {
                val match = voices.find { it.name.contains(pref, ignoreCase = true) }
                if (match != null) {
                    bestVoice = match
                    break
                }
            }

            if (bestVoice == null) {
                // Find any female voice with Hindi locale
                bestVoice = voices.find { voice ->
                    val isHindi = voice.locale.language == "hi" || voice.locale.country.equals("IN", ignoreCase = true)
                    val isFemale = voice.name.contains("female", ignoreCase = true) ||
                            voice.name.contains("-f0", ignoreCase = true) ||
                            voice.name.contains("hie", ignoreCase = true) ||
                            voice.name.contains("hid", ignoreCase = true)
                    isHindi && isFemale
                }
            }

            if (bestVoice != null) {
                tts?.voice = bestVoice
                Log.i(TAG, "Selected natural female TTS voice: ${bestVoice.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not select specific voice: ${e.message}")
        }
    }

    /**
     * Cleans text before speaking to eliminate emojis, symbols, and artifacts.
     * Prevents Android TTS from literally pronouncing "दिल", "लाल दिल", or symbol names!
     */
    fun cleanTextForSpeech(raw: String): String {
        return raw
            // Remove common heart and affection emojis
            .replace("❤️", "")
            .replace("💖", "")
            .replace("💕", "")
            .replace("🥰", "")
            .replace("😘", "")
            .replace("😍", "")
            .replace("😊", "")
            .replace("🌸", "")
            .replace("✨", "")
            .replace("🔥", "")
            .replace("👑", "")
            .replace("🌹", "")
            // Also remove literal "दिल" if it was appended as a standalone word at the end of sentence
            .replace(Regex("\\s+दिल\\b"), "")
            // Remove markdown formatting
            .replace(Regex("[*_~`#>]"), "")
            // Remove all emoji unicode ranges
            .replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\p{Sk}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun speak(
        text: String,
        flush: Boolean = true,
        onDone: (() -> Unit)? = null
    ) {
        val clean = cleanTextForSpeech(text)
        if (clean.isBlank()) {
            onDone?.invoke()
            return
        }
        onSpeechDoneCallback = onDone

        try {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "maya_speech_${System.currentTimeMillis()}")
            }
            tts?.speak(clean, queueMode, params, "maya_speech_${System.currentTimeMillis()}")
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
