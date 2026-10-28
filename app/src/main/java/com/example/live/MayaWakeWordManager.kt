package com.example.live

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class MayaWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isPaused = false
    private val handler = Handler(Looper.getMainLooper())

    private val wakeWords = listOf(
        // Maya wake words
        "ok maya", "okay maya", "hey maya", "maya", "hello maya", "hi maya",
        "ओके माया", "माया", "सुनो माया", "नमस्ते माया", "हे माया", "ओए माया",
        // Zoya wake words
        "ok zoya", "okay zoya", "hey zoya", "zoya", "hello zoya", "hi zoya", "joya", "ok joya",
        "ओके जोया", "जोया", "सुनो जोया", "नमस्ते जोया", "हे जोया", "ओए जोया"
    )

    fun startListening() {
        if (isListening) return
        isPaused = false
        handler.post {
            initAndStartRecognizer()
        }
    }

    fun pauseListening() {
        isPaused = true
        isListening = false
        handler.removeCallbacksAndMessages(null)
        handler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("MayaWakeWord", "Error pausing recognizer: ${e.message}")
            }
        }
    }

    fun resumeListening() {
        if (!isPaused) return
        isPaused = false
        handler.removeCallbacksAndMessages(null)
        handler.post {
            initAndStartRecognizer()
        }
    }

    fun destroy() {
        isPaused = true
        isListening = false
        handler.removeCallbacksAndMessages(null)
        handler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("MayaWakeWord", "Error destroying recognizer: ${e.message}")
            }
        }
    }

    private fun initAndStartRecognizer() {
        if (isPaused) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("MayaWakeWord", "Speech recognition not available on this device.")
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                // Super fast silence detection: trigger in 400ms instead of 3000ms delay!
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 400L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 400L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 150L)
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }

            // Suppress the annoying Android system "tring-tring" beep on startListening
            silenceBeepAndStartListening(intent)
            isListening = true
            Log.d("MayaWakeWord", "Wake word listener active: listening for 'Ok Maya' and direct commands...")
        } catch (e: Exception) {
            Log.e("MayaWakeWord", "Failed to start speech recognizer: ${e.message}")
            scheduleRestart(2000)
        }
    }

    private fun silenceBeepAndStartListening(intent: Intent) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        try {
            am?.adjustStreamVolume(android.media.AudioManager.STREAM_SYSTEM, android.media.AudioManager.ADJUST_MUTE, 0)
            am?.adjustStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, android.media.AudioManager.ADJUST_MUTE, 0)
        } catch (_: Exception) {}

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("MayaWakeWord", "startListening error", e)
        }

        handler.postDelayed({
            try {
                am?.adjustStreamVolume(android.media.AudioManager.STREAM_SYSTEM, android.media.AudioManager.ADJUST_UNMUTE, 0)
                am?.adjustStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, android.media.AudioManager.ADJUST_UNMUTE, 0)
            } catch (_: Exception) {}
        }, 300L)
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                if (!isPaused) {
                    val delay = if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        800L
                    } else {
                        1500L
                    }
                    scheduleRestart(delay)
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                checkMatches(matches, isPartial = false)
                if (!isPaused) {
                    scheduleRestart(600)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                checkMatches(matches, isPartial = true)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private var lastHandledPhrase = ""
    private var lastTriggerTime = 0L

    // Direct command triggers that work even without saying 'Ok Maya' if Maya is listening
    private val directCommandKeywords = listOf(
        "phone lock", "screen lock", "lock screen", "lock phone", "lock karo", "lock kar do",
        "screen band", "phone band", "screen off", "display off",
        "स्क्रीन लॉक", "फोन लॉक", "स्क्रीन बंद", "फोन बंद", "लॉक करो", "लॉक कर दो", "स्क्रीन ऑफ",
        "torch", "flashlight", "टॉर्च", "फ्लैशलाइट",
        "volume", "awaaz", "आवाज", "वॉल्यूम",
        "camera", "कैमरा", "selfie", "सेल्फी",
        "gana", "song", "गाना", "गीत",
        "so jao", "sleep", "aaram karo", "सो जाओ",
        "kholo", "open", "खोलो",
        "teacher mode", "टीचर मोड", "study mode",
        "gf mode", "गर्लफ्रेंड मोड",
        "kaise ho", "kya kar rahi ho", "कैसी हो", "क्या कर रही हो", "love you", "dusri girlfriend"
    )

    private fun checkMatches(matches: List<String>?, isPartial: Boolean) {
        if (matches.isNullOrEmpty()) return
        val now = System.currentTimeMillis()

        for (phrase in matches) {
            val lower = phrase.lowercase().trim()
            if (lower.isBlank()) continue

            // Check if contains wake word
            val hasWakeWord = wakeWords.any { lower.contains(it) }
            // Check if contains direct command
            val hasDirectCommand = directCommandKeywords.any { lower.contains(it) }

            if (hasWakeWord || hasDirectCommand) {
                // Deduplicate if identical phrase was handled in the last 1200ms
                if (lower == lastHandledPhrase && (now - lastTriggerTime < 1200L)) {
                    return
                }
                // If a partial result already triggered a command and now results has the same command, ignore
                if (!isPartial && lastHandledPhrase.isNotBlank() && lower.contains(lastHandledPhrase) && (now - lastTriggerTime < 1500L)) {
                    return
                }

                lastHandledPhrase = lower
                lastTriggerTime = now
                Log.i("MayaWakeWord", "TRIGGERED (${if (isPartial) "PARTIAL" else "FINAL"}): Detected in '$lower'!")
                onWakeWordDetected(lower)
                return
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!isPaused) {
                initAndStartRecognizer()
            }
        }, delayMs)
    }
}
