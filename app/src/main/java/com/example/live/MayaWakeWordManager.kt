package com.example.live

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlin.math.sqrt

class MayaWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var isListening = false

    @Volatile
    private var isPaused = false

    @Volatile
    private var isRecognizing = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var vadThread: Thread? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // Wake words in English & Hindi
    private val wakeWords = listOf(
        "ok maya", "okay maya", "hey maya", "maya", "hello maya", "hi maya",
        "ओके माया", "माया", "सुनो माया", "नमस्ते माया", "हे माया", "ओए माया",
        "ok zoya", "okay zoya", "hey zoya", "zoya", "hello zoya", "hi zoya", "joya", "ok joya",
        "ओके जोया", "जोया", "सुनो जोया", "नमस्ते जोया", "हे जोया", "ओए जोया"
    )

    private val directCommandKeywords = listOf(
        "phone lock", "screen lock", "lock screen", "lock phone", "lock karo", "lock kar do",
        "screen band", "phone band", "screen off", "display off",
        "स्क्रीन लॉक", "फोन लॉक", "स्क्रीन बंद", "फोन बंद", "लॉक करो", "लॉक कर दो", "स्क्रीन ऑफ",
        "whatsapp", "व्हाट्सएप", "व्हाट्सऐप", "send karo", "bhejo", "भेजो", "सेंड करो", "मैसेज",
        "gemini", "जेमिनी", "photo banao", "फोटो बनाओ", "तस्वीर बनाओ",
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

    fun startListening() {
        if (isListening) return
        isPaused = false
        isListening = true
        startSilentVoiceDetector()
    }

    fun pauseListening() {
        isPaused = true
        isListening = false
        stopSilentVoiceDetector()
        stopSpeechRecognizer()
    }

    fun resumeListening() {
        if (!isPaused) return
        isPaused = false
        isListening = true
        startSilentVoiceDetector()
    }

    fun destroy() {
        isPaused = true
        isListening = false
        stopSilentVoiceDetector()
        stopSpeechRecognizer()
    }

    /**
     * Completely silent Voice Activity Detector using AudioRecord.
     * This produces ZERO earcon beeps and ZERO "tring-tring" chimes!
     */
    private fun startSilentVoiceDetector() {
        if (isPaused) return
        stopSilentVoiceDetector()

        vadThread = Thread {
            try {
                val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = if (minBufSize > 0) minBufSize * 2 else 4096

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w("MayaWakeWord", "AudioRecord not initialized, fallback to recognizer.")
                    audioRecord = null
                    return@Thread
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(1024)

                var consecutiveSpeechFrames = 0
                val speechThreshold = 1400.0 // RMS threshold for human speech above background

                while (!isPaused && isListening && !isRecognizing) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)

                        if (rms > speechThreshold) {
                            consecutiveSpeechFrames++
                            // Need at least 2 consecutive voice frames (~120ms of speech) to trigger
                            if (consecutiveSpeechFrames >= 2) {
                                Log.i("MayaWakeWord", "Voice detected silently (RMS: $rms)! Launching single recognizer pass...")
                                consecutiveSpeechFrames = 0
                                stopSilentVoiceDetector()
                                handler.post {
                                    triggerSingleRecognitionPass()
                                }
                                break
                            }
                        } else {
                            if (consecutiveSpeechFrames > 0) consecutiveSpeechFrames--
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MayaWakeWord", "Error in silent VAD loop: ${e.message}")
            }
        }.apply {
            name = "MayaSilentVAD"
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    private fun stopSilentVoiceDetector() {
        try {
            vadThread?.interrupt()
            vadThread = null
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            audioRecord = null
        } catch (e: Exception) {
            Log.e("MayaWakeWord", "Error stopping AudioRecord VAD: ${e.message}")
        }
    }

    /**
     * Triggered ONLY when voice is actually heard. Runs ONCE to transcribe without endless chime loops.
     */
    fun triggerSingleRecognitionPass() {
        if (isPaused || isRecognizing) return
        isRecognizing = true

        handler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    isRecognizing = false
                    startSilentVoiceDetector()
                    return@post
                }

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
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200L)
                }

                // Briefly mute system earcon to suppress the "tring" sound
                silenceEarconAndStart(intent)
            } catch (e: Exception) {
                Log.e("MayaWakeWord", "Error starting speech recognition pass: ${e.message}")
                stopSpeechRecognizer()
                isRecognizing = false
                if (!isPaused) startSilentVoiceDetector()
            }
        }
    }

    private fun silenceEarconAndStart(intent: Intent) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            am?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
            am?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
        } catch (_: Exception) {}

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("MayaWakeWord", "startListening error", e)
        }

        handler.postDelayed({
            try {
                am?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                am?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
            } catch (_: Exception) {}
        }, 300L)
    }

    private fun stopSpeechRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        isRecognizing = false
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("MayaWakeWord", "End of speech received.")
            }

            override fun onError(error: Int) {
                Log.w("MayaWakeWord", "SpeechRecognizer error: $error")
                onRecognitionFinished()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                checkMatches(matches, isPartial = false)
                onRecognitionFinished()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                checkMatches(matches, isPartial = true)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun onRecognitionFinished() {
        stopSpeechRecognizer()
        isRecognizing = false
        // Return to silent voice detector standby after 400ms pause
        handler.postDelayed({
            if (!isPaused && isListening) {
                startSilentVoiceDetector()
            }
        }, 400L)
    }

    private var lastHandledPhrase = ""
    private var lastTriggerTime = 0L

    private fun checkMatches(matches: List<String>?, isPartial: Boolean) {
        if (matches.isNullOrEmpty()) return
        val now = System.currentTimeMillis()

        for (phrase in matches) {
            val lower = phrase.lowercase().trim()
            if (lower.isBlank()) continue

            if (isPartial) {
                val isMultiWordComplex = lower.contains("whatsapp") || lower.contains("व्हाट्सएप") ||
                        lower.contains("gemini") || lower.contains("जेमिनी") ||
                        lower.contains("send") || lower.contains("bhejo") || lower.contains("भेजो") ||
                        lower.contains("gana") || lower.contains("song") || lower.contains("गाना") ||
                        lower.contains("photo") || lower.contains("फोटो") || lower.contains("call")
                if (isMultiWordComplex) {
                    continue
                }
            }

            val hasWakeWord = wakeWords.any { lower.contains(it) }
            val hasDirectCommand = directCommandKeywords.any { lower.contains(it) }

            if (hasWakeWord || hasDirectCommand) {
                if (lower == lastHandledPhrase && (now - lastTriggerTime < 1800L)) {
                    return
                }
                if (!isPartial && lastHandledPhrase.isNotBlank() && lower.contains(lastHandledPhrase) && (now - lastTriggerTime < 2200L)) {
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
}
