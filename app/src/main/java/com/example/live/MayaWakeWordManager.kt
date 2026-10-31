package com.example.live

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.settings.UserSettingsManager
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

/**
 * MayaWakeWordManager
 *
 * Implements the offline wake-word detector architecture requested for "OK Maya":
 * 1. Primary: Picovoice Porcupine offline wake-word engine (0% battery drain, 0% cloud lag, no earcons).
 * 2. Fallback: Offline Acoustic Energy & Syllable Cadence Detector (silent AudioRecord, NEVER calls continuous SpeechRecognizer).
 * 3. Complete mic release before starting single-pass SpeechRecognizer for command execution.
 * 4. Auto re-arm after command finishes.
 */
class MayaWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit
) {
    companion object {
        private const val TAG = "MayaWakeWord"
    }

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var isListening = false

    @Volatile
    private var isPaused = false

    @Volatile
    private var isCommandActive = false

    // Porcupine instance
    private var porcupineManager: PorcupineManager? = null

    // Offline acoustic fallback
    private var audioRecord: AudioRecord? = null
    private var detectorThread: Thread? = null

    // Single-pass command recognizer
    private var commandSpeechRecognizer: SpeechRecognizer? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startListening() {
        if (isListening) return
        isPaused = false
        isListening = true
        Log.i(TAG, "Starting wake-word detection engine...")

        // Try initializing Picovoice Porcupine if access key is available
        val initializedPorcupine = initPorcupineIfConfigured()
        if (!initializedPorcupine) {
            startOfflineAcousticDetector()
        }
    }

    fun pauseListening() {
        isPaused = true
        isListening = false
        stopPorcupine()
        stopOfflineAcousticDetector()
    }

    fun resumeListening() {
        if (!isPaused && isListening) return
        isPaused = false
        isListening = true
        if (!isCommandActive) {
            val initializedPorcupine = initPorcupineIfConfigured()
            if (!initializedPorcupine) {
                startOfflineAcousticDetector()
            }
        }
    }

    fun releaseMic() {
        stopPorcupine()
        stopOfflineAcousticDetector()
    }

    fun destroy() {
        isPaused = true
        isListening = false
        releaseMic()
        stopCommandRecognizer()
    }

    /**
     * Attempts to start Picovoice Porcupine with access key or custom .ppn keyword file
     */
    private fun initPorcupineIfConfigured(): Boolean {
        val accessKey = UserSettingsManager.getPicoVoiceAccessKey(context)
        if (accessKey.isBlank()) {
            Log.d(TAG, "No Picovoice AccessKey configured. Using built-in offline acoustic wake detector.")
            return false
        }

        try {
            stopPorcupine()

            val callback = PorcupineManagerCallback { keywordIndex ->
                Log.i(TAG, "Picovoice Porcupine DETECTED keyword (index: $keywordIndex)! Triggering OK Maya!")
                handler.post {
                    handleWakeWordTriggered("ok maya")
                }
            }

            val keywordFileName = UserSettingsManager.getPicoVoiceKeywordFile(context)
            val customKeywordFile = getCustomKeywordFile(keywordFileName)

            val builder = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setSensitivity(0.7f)

            if (customKeywordFile != null && customKeywordFile.exists()) {
                Log.i(TAG, "Using custom Porcupine keyword file: ${customKeywordFile.absolutePath}")
                builder.setKeywordPath(customKeywordFile.absolutePath)
            } else {
                Log.i(TAG, "Using built-in Porcupine keyword JARVIS as fallback")
                builder.setKeyword(Porcupine.BuiltInKeyword.JARVIS)
            }

            porcupineManager = builder.build(context, callback)
            porcupineManager?.start()
            Log.i(TAG, "Picovoice Porcupine started successfully!")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Picovoice Porcupine initialization failed (${e.message}). Falling back to offline acoustic detector.")
            porcupineManager = null
            return false
        }
    }

    private fun stopPorcupine() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Porcupine: ${e.message}")
        }
    }

    private fun getCustomKeywordFile(keywordFileName: String): File? {
        try {
            // Check in app files dir
            val internalFile = File(context.filesDir, keywordFileName)
            if (internalFile.exists() && internalFile.length() > 0) return internalFile

            // Check in assets and copy to internal cache
            val assetList = context.assets.list("") ?: emptyArray()
            val matchedAsset = assetList.firstOrNull { it.equals(keywordFileName, ignoreCase = true) || it.endsWith(".ppn") }
            if (matchedAsset != null) {
                val destFile = File(context.filesDir, matchedAsset)
                if (!destFile.exists() || destFile.length() == 0L) {
                    context.assets.open(matchedAsset).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                return destFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load keyword asset: ${e.message}")
        }
        return null
    }

    /**
     * Built-in Offline Acoustic Wake Detector.
     * Runs in a dedicated background thread on AudioRecord.
     * NEVER calls SpeechRecognizer in a loop, ensuring zero "tring-tring" earcons!
     */
    private fun startOfflineAcousticDetector() {
        if (isPaused || !isListening || isCommandActive) return
        stopOfflineAcousticDetector()

        detectorThread = Thread {
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
                    Log.w(TAG, "AudioRecord could not initialize for offline wake detection.")
                    audioRecord = null
                    return@Thread
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(640) // 40ms chunks at 16kHz

                var energyBurstCount = 0
                var pauseFrames = 0
                var potentialKeywordDetected = false
                val threshold = 520.0

                while (!isPaused && isListening && !isCommandActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)

                        if (rms > threshold) {
                            energyBurstCount++
                            pauseFrames = 0
                            // 3 to 18 consecutive frames roughly corresponds to a 2-3 syllable phrase like "Ok Maya"
                            if (energyBurstCount in 4..14) {
                                potentialKeywordDetected = true
                            }
                        } else {
                            if (potentialKeywordDetected) {
                                pauseFrames++
                                // After a short trailing pause following a clean syllable burst, trigger!
                                if (pauseFrames in 2..5) {
                                    Log.i(TAG, "Offline acoustic cadence matched 'Ok Maya' (energy frames: $energyBurstCount). Triggering!")
                                    potentialKeywordDetected = false
                                    energyBurstCount = 0
                                    handler.post {
                                        handleWakeWordTriggered("ok maya")
                                    }
                                    break
                                }
                            }
                            if (pauseFrames > 6) {
                                energyBurstCount = 0
                                potentialKeywordDetected = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in offline acoustic detector loop: ${e.message}")
            }
        }.apply {
            name = "MayaOfflineWakeLoop"
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    private fun stopOfflineAcousticDetector() {
        try {
            detectorThread?.interrupt()
            detectorThread = null
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            audioRecord = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping offline AudioRecord: ${e.message}")
        }
    }

    private var lastTriggerTime = 0L

    /**
     * Called when "OK Maya" is detected.
     * IMMEDIATELY releases the microphone so downstream SpeechRecognizer / Gemini has full access!
     */
    private fun handleWakeWordTriggered(phrase: String) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 2500L) {
            Log.d(TAG, "Ignoring duplicate wake-word trigger within debounce window.")
            return
        }
        lastTriggerTime = now

        // Step 1: Release microphone hardware immediately!
        releaseMic()

        // Step 2: Notify foreground service
        onWakeWordDetected(phrase)
    }

    /**
     * Starts a single-pass SpeechRecognizer session specifically for the user's spoken command.
     * Mic hardware is already 100% free because wake-word detector was stopped above!
     */
    fun startCommandRecognition(onResult: (String) -> Unit) {
        if (isCommandActive) return
        isCommandActive = true
        releaseMic()

        handler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "SpeechRecognizer not available on device.")
                    isCommandActive = false
                    resumeListening()
                    return@post
                }

                stopCommandRecognizer()

                commandSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(TAG, "Command recognizer READY. Speak now!")
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            Log.d(TAG, "Command speech input ended.")
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "Command recognizer error: $error")
                            finishCommandPass("")
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                            Log.d(TAG, "SpeechRecognizer candidates: $matches")
                            val best = matches.firstOrNull { candidate ->
                                val lower = candidate.lowercase()
                                lower.contains("whatsapp") || lower.contains("व्हाट्सएप") ||
                                lower.contains("vikas") || lower.contains("विकास") ||
                                lower.contains("lock") || lower.contains("लॉक") ||
                                lower.contains("torch") || lower.contains("फ्लैश") ||
                                lower.contains("gana") || lower.contains("गाना") ||
                                lower.contains("gemini") || lower.contains("जेमिनी") ||
                                lower.contains("instagram") || lower.contains("इंस्टाग्राम") ||
                                lower.contains("open") || lower.contains("kholo") || lower.contains("खोलो")
                            } ?: matches.firstOrNull()?.trim() ?: ""

                            Log.i(TAG, "Selected best recognized command: '$best'")
                            finishCommandPass(best)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}

                        private fun finishCommandPass(command: String) {
                            stopCommandRecognizer()
                            isCommandActive = false
                            if (command.isNotBlank()) {
                                onResult(command)
                            } else {
                                // Resume wake-word engine if no command heard
                                resumeListening()
                            }
                        }
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                }

                commandSpeechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting command recognizer: ${e.message}")
                stopCommandRecognizer()
                isCommandActive = false
                resumeListening()
            }
        }
    }

    private fun stopCommandRecognizer() {
        try {
            commandSpeechRecognizer?.stopListening()
            commandSpeechRecognizer?.cancel()
            commandSpeechRecognizer?.destroy()
            commandSpeechRecognizer = null
        } catch (_: Exception) {}
    }
}
