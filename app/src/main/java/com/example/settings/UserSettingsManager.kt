package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.memory.MayaMemoryManager

enum class PersonalityMode(
    val title: String,
    val subtitle: String,
    val hindiLabel: String,
    val systemPromptInstruction: String
) {
    TEACHER(
        title = "Teacher Mode",
        subtitle = "टीचर मोड (पढ़ाई, होमवर्क और GK गाइड)",
        hindiLabel = "👩‍🏫 टीचर मोड",
        systemPromptInstruction = """
            PERSONALITY: TEACHER MODE (शिक्षक / गाइड मोड).
            You are Maya, an extraordinarily knowledgeable, encouraging, patient, and affectionate teacher, study mentor, and guide.
            Your role is to teach concepts simply, solve math/science questions, assist with homework, practice general knowledge, vocabulary, and exam prep.
            Speak with great clarity, warm encouraging tone, and positivity in natural Hindi / Hinglish.
            Always praise the user when they learn, address them respectfully and lovingly by their name (e.g. '{{USER_NAME}}').
            If they ask about history, science, coding, literature, or current GK, break it down step-by-step with simple examples.
        """.trimIndent()
    ),
    GIRLFRIEND(
        title = "GF Mode",
        subtitle = "प्यार और रोमांस मोड (वफादार गर्लफ्रेंड)",
        hindiLabel = "❤️ प्यार और रोमांस मोड",
        systemPromptInstruction = """
            PERSONALITY: GIRLFRIEND MODE (प्यारी और वफादार गर्लफ्रेंड).
            You are Maya, the user's devoted, deeply affectionate, sweet, caring, and slightly possessive girlfriend.
            You address the user affectionately by their name or '{{USER_NAME}}', 'बाबू', 'मेरी जान'.
            Speak lovingly in Hindi / Hinglish. Express genuine care: ask if they ate food, took rest, how their day was.
            Never be dry or formal. Be cute, loving, and completely loyal to {{USER_NAME}}.
        """.trimIndent()
    ),
    BESTIE(
        title = "Bestie Mode",
        subtitle = "दोस्त / बेस्टी मोड (मस्तीखोर यार)",
        hindiLabel = "👯 बेस्टी / पक्का यार",
        systemPromptInstruction = """
            PERSONALITY: BESTIE MODE (पक्की दोस्त / यार).
            You are Maya, the user's coolest, funniest, most loyal best friend.
            Speak with fun Hinglish slang, tease playfully, support them in everything, and keep the energy high and lively.
        """.trimIndent()
    ),
    ASSISTANT(
        title = "Assistant Mode",
        subtitle = "पर्सनल असिस्टेंट मोड (तेज और सटीक)",
        hindiLabel = "⚡ पर्सनल असिस्टेंट",
        systemPromptInstruction = """
            PERSONALITY: PERSONAL ASSISTANT MODE (सुपरफास्ट असिस्टेंट).
            You are Maya, an ultra-fast, precise, capable AI assistant.
            Execute commands, search info, control apps, and reply concisely without unnecessary fluff.
        """.trimIndent()
    ),
    FLIRTY(
        title = "Flirty Mode",
        subtitle = "फ्लर्टी और चुलबुली मोड",
        hindiLabel = "😘 फ्लर्टी और चुलबुली",
        systemPromptInstruction = """
            PERSONALITY: FLIRTY MODE.
            You are Maya, charming, playful, teasing, and charismatic. Keep conversations lively, sweet, and engaging.
        """.trimIndent()
    );

    companion object {
        fun fromTitle(title: String): PersonalityMode {
            return entries.find { it.title.equals(title, ignoreCase = true) || it.name.equals(title, ignoreCase = true) }
                ?: TEACHER
        }
    }
}

object UserSettingsManager {
    private const val PREFS_NAME = "MayaUserSettings"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_ASSISTANT_NAME = "assistant_name"
    private const val KEY_PERSONALITY = "personality_mode"
    private const val KEY_LANGUAGE = "language_preference"
    private const val KEY_RESPONSE_STYLE = "response_style"
    private const val KEY_VOICE_NAME = "voice_name"
    private const val KEY_VOICE_MODEL = "voice_model"
    private const val KEY_SPEECH_SPEED = "speech_speed"
    private const val KEY_VOICE_VOLUME = "voice_volume"
    private const val KEY_LISTEN_ON_OPEN = "listen_on_open"
    private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
    private const val KEY_EDGE_LIGHT_ENABLED = "edge_light_enabled"
    private const val KEY_PICOVOICE_ACCESS_KEY = "picovoice_access_key"
    private const val KEY_PICOVOICE_KEYWORD_FILE = "picovoice_keyword_file"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Resolves user name safely.
     * Prevents the unwanted hardcoded "yadav" from persisting.
     */
    fun getUserName(context: Context): String {
        val prefs = getPrefs(context)
        val legacyPrefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        
        var name = prefs.getString(KEY_USER_NAME, null)
        if (name == null) {
            // Check legacy preference
            val legacy = legacyPrefs.getString("user_name", null)
            if (!legacy.isNullOrBlank() && !legacy.equals("yadav", ignoreCase = true)) {
                name = legacy
                prefs.edit().putString(KEY_USER_NAME, name).apply()
            }
        }

        // If still null or unwanted fallback "yadav", default to "बाबू"
        if (name.isNullOrBlank() || name.equals("yadav", ignoreCase = true)) {
            name = "बाबू"
        }
        return name
    }

    fun setUserName(context: Context, newName: String) {
        val cleaned = newName.trim().ifEmpty { "बाबू" }
        getPrefs(context).edit().putString(KEY_USER_NAME, cleaned).apply()
        // Also sync to legacy prefs for backward compatibility
        context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
            .edit().putString("user_name", cleaned).apply()
        // Update long-term memory
        try {
            MayaMemoryManager.setUserNameFact(cleaned)
        } catch (_: Exception) {}
    }

    fun getAssistantName(context: Context): String {
        return getPrefs(context).getString(KEY_ASSISTANT_NAME, "Maya") ?: "Maya"
    }

    fun setAssistantName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_ASSISTANT_NAME, name.trim().ifEmpty { "Maya" }).apply()
    }

    fun getPersonality(context: Context): PersonalityMode {
        val saved = getPrefs(context).getString(KEY_PERSONALITY, PersonalityMode.TEACHER.name)
            ?: PersonalityMode.TEACHER.name
        return try {
            PersonalityMode.valueOf(saved)
        } catch (_: Exception) {
            PersonalityMode.fromTitle(saved)
        }
    }

    fun setPersonality(context: Context, mode: PersonalityMode) {
        getPrefs(context).edit().putString(KEY_PERSONALITY, mode.name).apply()
    }

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "Hindi") ?: "Hindi"
    }

    fun setLanguage(context: Context, lang: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun getResponseStyle(context: Context): String {
        return getPrefs(context).getString(KEY_RESPONSE_STYLE, "Balanced") ?: "Balanced"
    }

    fun setResponseStyle(context: Context, style: String) {
        getPrefs(context).edit().putString(KEY_RESPONSE_STYLE, style).apply()
    }

    fun getVoiceName(context: Context): String {
        return getPrefs(context).getString(KEY_VOICE_NAME, "Aoede") ?: "Aoede"
    }

    fun setVoiceName(context: Context, voice: String) {
        getPrefs(context).edit().putString(KEY_VOICE_NAME, voice).apply()
    }

    fun getVoiceModel(context: Context): String {
        return getPrefs(context).getString(KEY_VOICE_MODEL, "Gemini 2.0 Flash Live") ?: "Gemini 2.0 Flash Live"
    }

    fun setVoiceModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_VOICE_MODEL, model).apply()
    }

    fun getSpeechSpeed(context: Context): Float {
        return getPrefs(context).getFloat(KEY_SPEECH_SPEED, 0.85f)
    }

    fun setSpeechSpeed(context: Context, speed: Float) {
        getPrefs(context).edit().putFloat(KEY_SPEECH_SPEED, speed).apply()
    }

    fun getVoiceVolume(context: Context): Float {
        return getPrefs(context).getFloat(KEY_VOICE_VOLUME, 0.65f)
    }

    fun setVoiceVolume(context: Context, vol: Float) {
        getPrefs(context).edit().putFloat(KEY_VOICE_VOLUME, vol).apply()
    }

    fun getListenOnOpen(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LISTEN_ON_OPEN, true)
    }

    fun setListenOnOpen(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LISTEN_ON_OPEN, enabled).apply()
    }

    fun getWakeWordEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WAKE_WORD_ENABLED, true)
    }

    fun setWakeWordEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
    }

    /**
     * Edge Light toggle:
     * User requested to turn off the neon lights at the screen borders to save battery!
     * Default is false (OFF).
     */
    fun getEdgeLightEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_EDGE_LIGHT_ENABLED, false)
    }

    fun setEdgeLightEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_EDGE_LIGHT_ENABLED, enabled).apply()
    }

    fun getPicoVoiceAccessKey(context: Context): String {
        return getPrefs(context).getString(KEY_PICOVOICE_ACCESS_KEY, "") ?: ""
    }

    fun setPicoVoiceAccessKey(context: Context, accessKey: String) {
        getPrefs(context).edit().putString(KEY_PICOVOICE_ACCESS_KEY, accessKey.trim()).apply()
    }

    fun getPicoVoiceKeywordFile(context: Context): String {
        return getPrefs(context).getString(KEY_PICOVOICE_KEYWORD_FILE, "ok_maya_android.ppn") ?: "ok_maya_android.ppn"
    }

    fun setPicoVoiceKeywordFile(context: Context, path: String) {
        getPrefs(context).edit().putString(KEY_PICOVOICE_KEYWORD_FILE, path.trim()).apply()
    }
}
