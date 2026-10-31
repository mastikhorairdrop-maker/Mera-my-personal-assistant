package com.example.live

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * MayaSpeaker provides ultra-fast, natural, sweet Hindi voice output.
 * - Chooses the best natural Indian Female neural voice available on the device.
 * - Strips emojis and symbols so it NEVER reads "दिल" or weird punctuation out loud.
 * - Perfectly tuned pitch (1.18f) and rate (1.0f) to sound sweet, caring, and human-like.
 * - Watchdog timer guarantees callbacks ALWAYS fire within safe timeout, never freezing the app.
 */
object MayaSpeaker {
    private const val TAG = "MayaSpeaker"

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var isInitialized = false

    private var appContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var onSpeechDoneCallback: (() -> Unit)? = null
    private var watchdogRunnable: Runnable? = null

    fun init(context: Context, onReady: (() -> Unit)? = null) {
        val app = context.applicationContext
        appContext = app

        if (isInitialized && tts != null) {
            onReady?.invoke()
            return
        }

        try {
            tts = TextToSpeech(app) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val hindiLocale = Locale("hi", "IN")
                    val result = tts?.setLanguage(hindiLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Hindi not available, falling back to default locale")
                        tts?.setLanguage(Locale.getDefault())
                    }

                    // Select the best natural female voice from available voices
                    selectBestFemaleVoice()

                    // Sweet, youthful, expressive pitch & pace matching Maya
                    tts?.setPitch(1.18f)
                    tts?.setSpeechRate(1.0f)

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "Maya speaking started: $utteranceId")
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "Maya speaking completed: $utteranceId")
                            mainHandler.post { triggerSpeechDone() }
                        }

                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "Maya speaking error: $utteranceId")
                            mainHandler.post { triggerSpeechDone() }
                        }
                    })

                    isInitialized = true
                    Log.i(TAG, "MayaSpeaker initialized successfully with sweet female voice.")
                    onReady?.invoke()
                } else {
                    Log.e(TAG, "Failed to initialize TextToSpeech: status=$status")
                    isInitialized = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing MayaSpeaker: ${e.message}")
            isInitialized = false
        }
    }

    private fun triggerSpeechDone() {
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        watchdogRunnable = null
        val cb = onSpeechDoneCallback
        onSpeechDoneCallback = null
        cb?.invoke()
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
            .replace("💙", "")
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

        // Lazy-init if needed
        if (tts == null && appContext != null) {
            init(appContext!!)
        }

        val activeTts = tts
        if (activeTts == null) {
            Log.w(TAG, "TTS instance is null when speak requested. Invoking callback immediately to prevent hanging.")
            onDone?.invoke()
            return
        }

        onSpeechDoneCallback = onDone

        // Safety watchdog: guarantees callback fires even if engine fails to report onDone
        val timeoutMs = (clean.length * 90L + 2200L).coerceIn(2200L, 6500L)
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        val watchdog = Runnable {
            Log.w(TAG, "Watchdog timeout reached for speech: '$clean'. Forcing onDone callback.")
            triggerSpeechDone()
        }
        watchdogRunnable = watchdog
        mainHandler.postDelayed(watchdog, timeoutMs)

        try {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = "maya_speech_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }
            activeTts.speak(clean, queueMode, params, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: ${e.message}")
            triggerSpeechDone()
        }
    }

    fun stop() {
        try {
            watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
            watchdogRunnable = null
            tts?.stop()
            onSpeechDoneCallback = null
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
