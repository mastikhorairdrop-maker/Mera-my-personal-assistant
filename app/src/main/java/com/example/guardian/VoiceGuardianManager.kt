package com.example.guardian

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.live.MayaSpeaker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * VoiceGuardianManager
 *
 * Provides Owner Voice Recognition & Enrollment ("मालिक की आवाज़ पहचानना"):
 * 1. Enrolls owner's voice fingerprint by capturing fundamental frequency (pitch),
 *    zero-crossing rate, energy distribution, and spectral centroid.
 * 2. Real-time voice comparison against owner's acoustic fingerprint.
 * 3. Enforces owner-only access: when Voice Guardian is ON, commands spoken by
 *    unfamiliar voices are rejected with "क्षमा करें, मैं केवल अपने मालिक की आवाज़ सुनती हूँ।"
 */
object VoiceGuardianManager {
    private const val TAG = "VoiceGuardian"
    private const val PREFS_NAME = "MayaVoiceGuardianPrefs"

    private const val KEY_GUARDIAN_ENABLED = "voice_guardian_enabled"
    private const val KEY_ENROLLED = "owner_enrolled"
    private const val KEY_ENROLLED_DATE = "owner_enrolled_date"
    private const val KEY_MODEL_NAME = "recognition_model_name"

    // Acoustic features
    private const val KEY_OWNER_PITCH = "owner_pitch"
    private const val KEY_OWNER_CENTROID = "owner_centroid"
    private const val KEY_OWNER_ZCR = "owner_zcr"
    private const val KEY_OWNER_RMS = "owner_rms"

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isRecordingOrTesting = false
        private set

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isGuardianEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_GUARDIAN_ENABLED, false)
    }

    fun setGuardianEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_GUARDIAN_ENABLED, enabled).apply()
    }

    fun isOwnerEnrolled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENROLLED, false)
    }

    fun getEnrolledDate(context: Context): String {
        return getPrefs(context).getString(KEY_ENROLLED_DATE, "") ?: ""
    }

    fun getModelName(context: Context): String {
        return getPrefs(context).getString(KEY_MODEL_NAME, "Powerful · 26 MB · use me hai") ?: "Powerful · 26 MB · use me hai"
    }

    fun setModelName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_MODEL_NAME, name).apply()
    }

    fun clearOwnerEnrollment(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_ENROLLED, false)
            .remove(KEY_ENROLLED_DATE)
            .remove(KEY_OWNER_PITCH)
            .remove(KEY_OWNER_CENTROID)
            .remove(KEY_OWNER_ZCR)
            .remove(KEY_OWNER_RMS)
            .apply()
    }

    /**
     * Data structure holding voice acoustic features
     */
    data class VoiceFeatures(
        val pitchHz: Float,
        val spectralCentroid: Float,
        val zeroCrossingRate: Float,
        val rmsEnergy: Float
    )

    /**
     * Enrolls the owner's voice by recording 4 seconds of speech.
     */
    fun enrollOwnerVoice(
        context: Context,
        onProgress: (percent: Int) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        if (isRecordingOrTesting) {
            onComplete(false, "रिकॉर्डिंग पहले से चालू है!")
            return
        }

        isRecordingOrTesting = true
        Thread {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = if (minBuf > 0) minBuf * 2 else 4096

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    mainHandler.post {
                        isRecordingOrTesting = false
                        onComplete(false, "माइक्रोफ़ोन एक्सेस नहीं मिल सका!")
                    }
                    return@Thread
                }

                audioRecord.startRecording()
                val totalFrames = sampleRate * 4 // 4 seconds of audio
                val shortBuffer = ShortArray(totalFrames)
                var readTotal = 0
                val chunkSize = 1600 // 100ms chunks

                while (readTotal < totalFrames) {
                    val count = audioRecord.read(shortBuffer, readTotal, chunkSize.coerceAtMost(totalFrames - readTotal))
                    if (count > 0) {
                        readTotal += count
                        val pct = ((readTotal.toFloat() / totalFrames) * 100).toInt().coerceIn(0, 100)
                        mainHandler.post { onProgress(pct) }
                    } else {
                        Thread.sleep(20)
                    }
                }

                // Extract features
                val features = extractFeatures(shortBuffer, readTotal, sampleRate)

                if (features.rmsEnergy < 200f) {
                    mainHandler.post {
                        isRecordingOrTesting = false
                        onComplete(false, "आवाज़ बहुत धीमी थी! कृपया माइक्रोफ़ोन के पास आकर थोड़ा तेज़ बोलें।")
                    }
                    return@Thread
                }

                // Save features as owner's reference fingerprint
                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
                getPrefs(context).edit()
                    .putBoolean(KEY_ENROLLED, true)
                    .putString(KEY_ENROLLED_DATE, dateStr)
                    .putFloat(KEY_OWNER_PITCH, features.pitchHz)
                    .putFloat(KEY_OWNER_CENTROID, features.spectralCentroid)
                    .putFloat(KEY_OWNER_ZCR, features.zeroCrossingRate)
                    .putFloat(KEY_OWNER_RMS, features.rmsEnergy)
                    .apply()

                mainHandler.post {
                    isRecordingOrTesting = false
                    MayaSpeaker.speak("मालिक, आपकी आवाज़ सफलतापूर्वक पहचान मॉडल में सेव हो गई है!")
                    onComplete(true, "मालिक की आवाज़ सफलतापूर्वक एनरोल हो गई! ($dateStr) ✅")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in enrollOwnerVoice", e)
                mainHandler.post {
                    isRecordingOrTesting = false
                    onComplete(false, "रिकॉर्डिंग में त्रुटि: ${e.message}")
                }
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }.start()
    }

    /**
     * Tests spoken voice against the enrolled owner profile.
     */
    fun testVoiceMatch(
        context: Context,
        onProgress: (percent: Int) -> Unit,
        onResult: (matchPercent: Int, isOwner: Boolean, message: String) -> Unit
    ) {
        if (!isOwnerEnrolled(context)) {
            onResult(0, false, "पहले '+ Owner ki awaaz record karo' से आवाज़ एनरोल करें!")
            return
        }

        if (isRecordingOrTesting) {
            onResult(0, false, "टेस्ट पहले से चालू है!")
            return
        }

        isRecordingOrTesting = true
        Thread {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = if (minBuf > 0) minBuf * 2 else 4096

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    mainHandler.post {
                        isRecordingOrTesting = false
                        onResult(0, false, "माइक्रोफ़ोन एक्सेस नहीं मिल सका!")
                    }
                    return@Thread
                }

                audioRecord.startRecording()
                val totalFrames = sampleRate * 3 // 3 seconds test
                val shortBuffer = ShortArray(totalFrames)
                var readTotal = 0
                val chunkSize = 1600

                while (readTotal < totalFrames) {
                    val count = audioRecord.read(shortBuffer, readTotal, chunkSize.coerceAtMost(totalFrames - readTotal))
                    if (count > 0) {
                        readTotal += count
                        val pct = ((readTotal.toFloat() / totalFrames) * 100).toInt().coerceIn(0, 100)
                        mainHandler.post { onProgress(pct) }
                    } else {
                        Thread.sleep(20)
                    }
                }

                val testFeatures = extractFeatures(shortBuffer, readTotal, sampleRate)
                val prefs = getPrefs(context)
                val ownerPitch = prefs.getFloat(KEY_OWNER_PITCH, 150f)
                val ownerCentroid = prefs.getFloat(KEY_OWNER_CENTROID, 1200f)
                val ownerZcr = prefs.getFloat(KEY_OWNER_ZCR, 0.08f)

                val matchScore = calculateMatchPercentage(testFeatures, ownerPitch, ownerCentroid, ownerZcr)
                val isOwner = matchScore >= 65

                mainHandler.post {
                    isRecordingOrTesting = false
                    val msg = if (isOwner) {
                        MayaSpeaker.speak("पहचान सफल! मालिक, आपकी आवाज़ $matchScore प्रतिशत मैच हो गई है।")
                        "मालिक की आवाज़ पहचानी गई! (Match: $matchScore%) ✅"
                    } else {
                        MayaSpeaker.speak("चेतावनी! अपरिचित आवाज़ डिटेक्ट हुई। मैच केवल $matchScore प्रतिशत है।")
                        "अपरिचित आवाज़ (Match: $matchScore%) ❌ - यह मालिक की आवाज़ नहीं है!"
                    }
                    onResult(matchScore, isOwner, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in testVoiceMatch", e)
                mainHandler.post {
                    isRecordingOrTesting = false
                    onResult(0, false, "टेस्ट में त्रुटि: ${e.message}")
                }
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }.start()
    }

    /**
     * Real-time verification of audio chunk during active listening
     */
    fun isSpeakerOwner(context: Context, audioChunk: ShortArray, length: Int): Boolean {
        if (!isGuardianEnabled(context) || !isOwnerEnrolled(context)) {
            return true // Guard is off or not enrolled, allow all
        }

        val features = extractFeatures(audioChunk, length, 16000)
        if (features.rmsEnergy < 250f) return true // Background silence, ignore

        val prefs = getPrefs(context)
        val ownerPitch = prefs.getFloat(KEY_OWNER_PITCH, 150f)
        val ownerCentroid = prefs.getFloat(KEY_OWNER_CENTROID, 1200f)
        val ownerZcr = prefs.getFloat(KEY_OWNER_ZCR, 0.08f)

        val score = calculateMatchPercentage(features, ownerPitch, ownerCentroid, ownerZcr)
        return score >= 60
    }

    private fun extractFeatures(buffer: ShortArray, length: Int, sampleRate: Int): VoiceFeatures {
        var sumSquares = 0.0
        var zeroCrossings = 0

        for (i in 0 until length) {
            val v = buffer[i].toDouble()
            sumSquares += v * v
            if (i > 0 && ((buffer[i] >= 0 && buffer[i - 1] < 0) || (buffer[i] < 0 && buffer[i - 1] >= 0))) {
                zeroCrossings++
            }
        }

        val rms = sqrt(sumSquares / length.coerceAtLeast(1)).toFloat()
        val zcr = (zeroCrossings.toFloat() / length.coerceAtLeast(1))

        // Estimate fundamental pitch using Autocorrelation
        val minLag = sampleRate / 350 // max 350 Hz
        val maxLag = sampleRate / 75  // min 75 Hz
        var bestLag = -1
        var maxCorr = 0.0

        for (lag in minLag..maxLag.coerceAtMost(length / 2)) {
            var corr = 0.0
            for (i in 0 until (length - lag).coerceAtMost(2000)) {
                corr += buffer[i].toDouble() * buffer[i + lag].toDouble()
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        val pitch = if (bestLag > 0) (sampleRate.toFloat() / bestLag) else 150f

        // Spectral centroid approximation from zero crossing & autocorrelation
        val centroid = (zcr * (sampleRate / 2f) * 1.8f).coerceIn(400f, 3500f)

        return VoiceFeatures(
            pitchHz = pitch,
            spectralCentroid = centroid,
            zeroCrossingRate = zcr,
            rmsEnergy = rms
        )
    }

    private fun calculateMatchPercentage(
        sample: VoiceFeatures,
        ownerPitch: Float,
        ownerCentroid: Float,
        ownerZcr: Float
    ): Int {
        // Pitch difference penalty (up to 40 points)
        val pitchDiff = abs(sample.pitchHz - ownerPitch) / ownerPitch.coerceAtLeast(50f)
        val pitchScore = ((1.0f - pitchDiff.coerceIn(0.0f, 1.0f)) * 40).toInt()

        // Centroid difference penalty (up to 35 points)
        val centroidDiff = abs(sample.spectralCentroid - ownerCentroid) / ownerCentroid.coerceAtLeast(500f)
        val centroidScore = ((1.0f - centroidDiff.coerceIn(0.0f, 1.0f)) * 35).toInt()

        // ZCR difference penalty (up to 25 points)
        val zcrDiff = abs(sample.zeroCrossingRate - ownerZcr) / ownerZcr.coerceAtLeast(0.02f)
        val zcrScore = ((1.0f - zcrDiff.coerceIn(0.0f, 1.0f)) * 25).toInt()

        val total = (pitchScore + centroidScore + zcrScore).coerceIn(0, 100)
        return total
    }
}
