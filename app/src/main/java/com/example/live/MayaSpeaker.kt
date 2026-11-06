package com.example.live

import android.content.Context
import android.media.AudioAttributes
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
 * MayaSpeaker delivers a sweet, natural, affectionate Indian female voice.
 * - Replaces robotic, metallic, or chipmunk TTS with warm, human-like feminine acoustic frequencies.
 * - Natural acoustic sweet pitch (1.06f - 1.08f) without high-frequency phase or robotic distortion.
 * - Automatically finds and binds the highest-quality Google Neural / WaveNet Indian Female voice.
 * - AudioAttributes set to USAGE_ASSISTANT and CONTENT_TYPE_SPEECH for crisp voice DSP enhancement.
 * - Automatic Hindi/Hinglish phonetic mapping for tech terms (WhatsApp, WiFi, Instagram, Alarm, etc.)
 *   so the voice speaks fluently without robotic spelling or hesitation.
 * - Strips emojis, symbols, and formatting so it never reads out literal symbol names or "दिल".
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
            val googleTtsPkg = "com.google.android.tts"
            val hasGoogleTts = try {
                app.packageManager.getPackageInfo(googleTtsPkg, 0)
                true
            } catch (_: Exception) {
                false
            }

            val listener = TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val hindiLocale = Locale("hi", "IN")
                    val result = tts?.setLanguage(hindiLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Hindi not available, falling back to default locale")
                        tts?.setLanguage(Locale.getDefault())
                    }

                    // Configure audio attributes for crystal-clear voice assistant speech
                    try {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts?.setAudioAttributes(audioAttributes)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set AudioAttributes: ${e.message}")
                    }

                    // Select the best natural female voice from available voices
                    selectBestFemaleVoice()

                    // Apply voice persona, pitch & speed
                    appContext?.let { applyVoiceSettings(it) }

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

            tts = if (hasGoogleTts) {
                TextToSpeech(app, listener, googleTtsPkg)
            } else {
                TextToSpeech(app, listener)
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

            // Filter out voices that are explicitly not installed
            val installedVoices = voices.filter { voice ->
                voice.features == null || !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }

            // High-priority Google Neural/WaveNet Indian Hindi female voices
            val preferredVoiceNames = listOf(
                "hi-in-x-hie-network", // Top Google Hindi Neural Female 1
                "hi-in-x-hie-local",
                "hi-in-x-hid-network", // Google Hindi Female 2
                "hi-in-x-hid-local",
                "hi-in-x-cfn-network", // Google Hindi Female 3
                "hi-in-x-cfn-local",
                "hi-in-x-hia-network", // Google Hindi Female 4
                "hi-in-x-hia-local",
                "hi-in-x-hib-local",
                // Indian English / Hinglish female voices as high quality alternatives
                "en-in-x-enc-network",
                "en-in-x-enc-local",
                "en-in-x-end-network",
                "en-in-x-end-local",
                "en-in-x-ene-network",
                "en-in-x-cce-network"
            )

            for (pref in preferredVoiceNames) {
                val match = installedVoices.find { it.name.contains(pref, ignoreCase = true) }
                if (match != null) {
                    bestVoice = match
                    break
                }
            }

            if (bestVoice == null) {
                // Find installed female voice with Indian locale, strictly rejecting male voices
                bestVoice = installedVoices.find { voice ->
                    val isIndian = voice.locale.language == "hi" || voice.locale.country.equals("IN", ignoreCase = true)
                    val isMale = voice.name.contains("male", ignoreCase = true) ||
                            voice.name.contains("man", ignoreCase = true) ||
                            voice.name.contains("boy", ignoreCase = true)
                    val isFemale = voice.name.contains("female", ignoreCase = true) ||
                            voice.name.contains("woman", ignoreCase = true) ||
                            voice.name.contains("girl", ignoreCase = true) ||
                            voice.name.contains("-f0", ignoreCase = true) ||
                            voice.name.contains("hie", ignoreCase = true) ||
                            voice.name.contains("hid", ignoreCase = true) ||
                            voice.name.contains("cfn", ignoreCase = true) ||
                            voice.name.contains("hia", ignoreCase = true) ||
                            voice.name.contains("enc", ignoreCase = true) ||
                            voice.name.contains("end", ignoreCase = true)
                    isIndian && !isMale && isFemale
                }
            }

            if (bestVoice == null) {
                // Fallback: any non-male voice in Hindi
                bestVoice = installedVoices.find { voice ->
                    val isHindi = voice.locale.language == "hi"
                    val isMale = voice.name.contains("male", ignoreCase = true) ||
                            voice.name.contains("man", ignoreCase = true) ||
                            voice.name.contains("boy", ignoreCase = true)
                    isHindi && !isMale
                }
            }

            if (bestVoice != null) {
                tts?.voice = bestVoice
                Log.i(TAG, "Selected natural female TTS voice: ${bestVoice.name} (quality: ${bestVoice.quality})")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not select specific voice: ${e.message}")
        }
    }

    /**
     * Applies voice pitch, speech rate, and persona dynamically based on UserSettingsManager.
     * Tuned with natural, warm, sweet, human-like feminine frequencies (1.05f - 1.09f),
     * avoiding unnatural robotic/chipmunk distortion caused by aggressive pitch shifts (>1.20f).
     */
    fun applyVoiceSettings(context: Context) {
        val voiceName = com.example.settings.UserSettingsManager.getVoicePersona(context).ifBlank {
            com.example.settings.UserSettingsManager.getVoiceName(context)
        }
        val userSpeed = com.example.settings.UserSettingsManager.getSpeechSpeed(context).coerceIn(0.8f, 1.3f)

        // Sweet feminine pitch tuning:
        // 1.06f - 1.08f is the gold standard for warm, sweet, natural Indian female vocal resonance.
        val basePitch = when (voiceName.lowercase().trim()) {
            "aoede" -> 1.07f // मीठी और रोमांटिक (Default Sweet Girlfriend)
            "pari" -> 1.08f // परियों जैसी कोमल और प्यारी
            "zoya" -> 1.06f // गर्मजोशी और प्यार भरी
            "kavya" -> 1.07f // स्पष्ट और मधुर
            "kore" -> 1.09f // चुलबुली और नटखट
            "leda" -> 1.05f // शांत और सौम्य
            "zephyr" -> 1.03f // गंभीर और समझदार
            "laomedeia" -> 1.05f // सॉफ्ट और स्लो
            "despina" -> 1.08f // तेज और स्मार्ट
            "erinome" -> 1.07f // दोस्ताना और खुशमिजाज
            "callirrhoe" -> 1.08f // मॉडर्न और एनर्जेटिक
            "gacrux" -> 1.00f // गहरी और मजबूत
            "pulcherrima" -> 1.06f // सुरमयी और मखमली
            "sulafat" -> 1.04f // धीमी और सुकूनभरी
            "vindemiatrix" -> 1.07f // कॉन्फिडेंट और बोल्ड
            else -> 1.07f
        }

        // Natural cadence tempo (0.96f - 1.0f gives a relaxed, affectionate speaking pace)
        val rate = when (voiceName.lowercase().trim()) {
            "laomedeia", "sulafat" -> (userSpeed * 0.94f) // Softer, slower tempo
            "despina", "callirrhoe", "kore" -> (userSpeed * 1.02f) // Lively, energetic tempo
            "gacrux" -> (userSpeed * 0.95f)
            else -> (userSpeed * 0.98f)
        }

        try {
            tts?.setPitch(basePitch)
            tts?.setSpeechRate(rate)
            Log.d(TAG, "Applied voice settings: persona=$voiceName, pitch=$basePitch, rate=$rate")
        } catch (_: Exception) {}
    }

    /**
     * Cleans text and applies natural phonetic conversions so the speech engine speaks
     * with effortless human Hindi phonology without stuttering on English terms.
     */
    fun cleanTextForSpeech(raw: String): String {
        var text = raw
            // Remove emojis & decorative symbols
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
            .replace("🛡️", "")
            .replace("🦁", "")
            .replace("📸", "")
            .replace("🌐", "")
            .replace("🔊", "")
            .replace("⚡", "")
            .replace("🎭", "")
            .replace("🕊️", "")
            .replace("💋", "")
            // Remove literal standalone "दिल" appended at the end of sentence
            .replace(Regex("\\s+दिल\\b"), "")
            // Remove markdown formatting
            .replace(Regex("[*_~`#>]"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            // Remove all emoji unicode categories
            .replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\p{Sk}]"), "")

        // Natural phonetic substitutions for fluent Hindi/Hinglish speech output
        val phoneticMap = listOf(
            "(?i)\\bwi-?fi\\b" to "वाई-फ़ाई",
            "(?i)\\bwhatsapp\\b" to "व्हाट्सएप",
            "(?i)\\binstagram\\b" to "इंस्टाग्राम",
            "(?i)\\binsta\\b" to "इन्स्टा",
            "(?i)\\byoutube\\b" to "यूट्यूब",
            "(?i)\\bgoogle assistant\\b" to "गूगल असिस्टेंट",
            "(?i)\\bok google\\b" to "ओके गूगल",
            "(?i)\\bhey google\\b" to "हे गूगल",
            "(?i)\\bchatgpt\\b" to "चैट जीपीटी",
            "(?i)\\bgpt\\b" to "जीपीटी",
            "(?i)\\bgemini\\b" to "जेमिनी",
            "(?i)\\btorch\\b" to "टॉर्च",
            "(?i)\\bflashlight\\b" to "फ्लैशलाइट",
            "(?i)\\bbrightness\\b" to "ब्राइटनेस",
            "(?i)\\balarm\\b" to "अलार्म",
            "(?i)\\btimer\\b" to "टाइमर",
            "(?i)\\bbattery\\b" to "बैटरी",
            "(?i)\\bbluetooth\\b" to "ब्लूटूथ",
            "(?i)\\bflight mode\\b" to "फ्लाइट मोड",
            "(?i)\\bairplane mode\\b" to "एरोप्लेन मोड",
            "(?i)\\btouch guard\\b" to "टच गार्ड",
            "(?i)\\btouchguard\\b" to "टच गार्ड",
            "(?i)\\bsos\\b" to "एस ओ एस",
            "(?i)\\bpin\\b" to "पिन",
            "(?i)\\bscreen\\b" to "स्क्रीन",
            "(?i)\\block\\b" to "लॉक",
            "(?i)\\bunlock\\b" to "अनलॉक",
            "(?i)\\bphoto\\b" to "फोटो",
            "(?i)\\bimage\\b" to "इमेज",
            "(?i)\\bwallpaper\\b" to "वॉलपेपर",
            "(?i)\\breel\\b" to "रील",
            "(?i)\\breels\\b" to "रील्स",
            "(?i)\\bsetting\\b" to "सेटिंग",
            "(?i)\\bsettings\\b" to "सेटिंग्स",
            "(?i)\\bcall\\b" to "कॉल",
            "(?i)\\bcaller\\b" to "कॉलर",
            "(?i)\\bmessage\\b" to "मैसेज",
            "(?i)\\bsms\\b" to "एस एम एस",
            "(?i)\\bcamera\\b" to "कैमरा",
            "(?i)\\bselfie\\b" to "सेल्फी",
            "(?i)\\bvideo\\b" to "वीडियो",
            "(?i)\\bgall?ery\\b" to "गैलरी",
            "(?i)\\bapp\\b" to "ऐप",
            "(?i)\\bapps\\b" to "ऐप्स",
            "(?i)\\bwebsite\\b" to "वेबसाइट",
            "(?i)\\bgame\\b" to "गेम",
            "(?i)\\bboss\\b" to "बॉस",
            "(?i)\\bhunter\\b" to "हंटर"
        )

        for ((pattern, replacement) in phoneticMap) {
            text = text.replace(Regex(pattern), replacement)
        }

        return text.replace(Regex("\\s+"), " ").trim()
    }

    fun speak(
        context: Context,
        text: String,
        flush: Boolean = true,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            init(context) {
                speak(text, flush, onDone)
            }
        } else {
            speak(text, flush, onDone)
        }
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

        appContext?.let { applyVoiceSettings(it) }

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
