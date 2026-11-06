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
    private const val KEY_PORCUPINE_ENABLED = "porcupine_enabled"
    private const val KEY_PORCUPINE_SENSITIVITY = "porcupine_sensitivity"

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
        val persona = getPrefs(context).getString("voice_persona", null)
        if (!persona.isNullOrBlank()) return persona
        return getPrefs(context).getString(KEY_VOICE_NAME, "Aoede") ?: "Aoede"
    }

    fun setVoiceName(context: Context, voice: String) {
        getPrefs(context).edit()
            .putString(KEY_VOICE_NAME, voice)
            .putString("voice_persona", voice)
            .apply()
    }

    fun getVoiceModel(context: Context): String {
        return getPrefs(context).getString(KEY_VOICE_MODEL, "Gemini 2.0 Flash Live") ?: "Gemini 2.0 Flash Live"
    }

    fun setVoiceModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_VOICE_MODEL, model).apply()
    }

    fun getSpeechSpeed(context: Context): Float {
        return getPrefs(context).getFloat(KEY_SPEECH_SPEED, 1.0f)
    }

    fun setSpeechSpeed(context: Context, speed: Float) {
        getPrefs(context).edit().putFloat(KEY_SPEECH_SPEED, speed).apply()
    }

    fun getVoiceVolume(context: Context): Float {
        return getPrefs(context).getFloat(KEY_VOICE_VOLUME, 1.0f)
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

    fun getPorcupineEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PORCUPINE_ENABLED, true)
    }

    fun setPorcupineEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PORCUPINE_ENABLED, enabled).apply()
    }

    fun getPorcupineSensitivity(context: Context): Float {
        return getPrefs(context).getFloat(KEY_PORCUPINE_SENSITIVITY, 0.70f)
    }

    fun setPorcupineSensitivity(context: Context, sensitivity: Float) {
        val clamped = sensitivity.coerceIn(0.0f, 1.0f)
        getPrefs(context).edit().putFloat(KEY_PORCUPINE_SENSITIVITY, clamped).apply()
    }

    fun getAutoUnlockPin(context: Context): String {
        return getPrefs(context).getString("auto_unlock_pin", "") ?: ""
    }

    fun setAutoUnlockPin(context: Context, pin: String) {
        getPrefs(context).edit().putString("auto_unlock_pin", pin.trim()).apply()
    }

    fun getTouchGuardEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("touch_guard_enabled", false)
    }

    fun setTouchGuardEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("touch_guard_enabled", enabled).apply()
    }

    fun getMotionAlarmEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("motion_alarm_enabled", true)
    }

    fun setMotionAlarmEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("motion_alarm_enabled", enabled).apply()
    }

    fun getAutoUnlockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("auto_unlock_enabled", true)
    }

    fun setAutoUnlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("auto_unlock_enabled", enabled).apply()
    }

    fun getAirplaneAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("airplane_announcement_enabled", true)
    }

    fun setAirplaneAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("airplane_announcement_enabled", enabled).apply()
    }

    fun getChargingAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("charging_announcement_enabled", true)
    }

    fun setChargingAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("charging_announcement_enabled", enabled).apply()
    }

    fun getUserGender(context: Context): String {
        return getPrefs(context).getString("user_gender", "Ladka") ?: "Ladka"
    }

    fun setUserGender(context: Context, gender: String) {
        getPrefs(context).edit().putString("user_gender", gender).apply()
    }

    fun getUserPhone(context: Context): String {
        return getPrefs(context).getString("user_phone", "") ?: ""
    }

    fun setUserPhone(context: Context, phone: String) {
        getPrefs(context).edit().putString("user_phone", phone.trim()).apply()
    }

    fun getDefaultMusicApp(context: Context): String {
        return getPrefs(context).getString("default_music_app", "YouTube") ?: "YouTube"
    }

    fun setDefaultMusicApp(context: Context, app: String) {
        getPrefs(context).edit().putString("default_music_app", app).apply()
    }

    fun getFavoriteSong(context: Context): String {
        return getPrefs(context).getString("favorite_song", "Kesariya") ?: "Kesariya"
    }

    fun setFavoriteSong(context: Context, song: String) {
        getPrefs(context).edit().putString("favorite_song", song.trim()).apply()
    }

    fun getVoiceEngineMode(context: Context): String {
        return getPrefs(context).getString("voice_engine_mode", "Normal mode") ?: "Normal mode"
    }

    fun setVoiceEngineMode(context: Context, mode: String) {
        getPrefs(context).edit().putString("voice_engine_mode", mode).apply()
    }

    fun getVoiceEngineModel(context: Context): String {
        return getPrefs(context).getString("voice_engine_model", "Maya Swift") ?: "Maya Swift"
    }

    fun setVoiceEngineModel(context: Context, model: String) {
        getPrefs(context).edit().putString("voice_engine_model", model).apply()
    }

    fun getSecretMode(context: Context): String {
        return getPrefs(context).getString("secret_mode", "Romantic") ?: "Romantic"
    }

    fun setSecretMode(context: Context, mode: String) {
        getPrefs(context).edit().putString("secret_mode", mode).apply()
    }

    fun getAutoScrollReels(context: Context): Boolean {
        return getPrefs(context).getBoolean("auto_scroll_reels", true)
    }

    fun setAutoScrollReels(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("auto_scroll_reels", enabled).apply()
    }

    fun getCallAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("call_announcement_enabled", true)
    }

    fun setCallAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("call_announcement_enabled", enabled).apply()
    }

    fun getCallLoudspeakerEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("call_loudspeaker_enabled", false)
    }

    fun setCallLoudspeakerEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("call_loudspeaker_enabled", enabled).apply()
    }

    fun getDrivingModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("driving_mode_enabled", false)
    }

    fun setDrivingModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("driving_mode_enabled", enabled).apply()
    }

    fun getDrivingAutoReplyText(context: Context): String {
        return getPrefs(context).getString("driving_auto_reply_text", "{name} abhi drive kar rahe hain, isliye call nahi utha paye. Free hote hi call back karenge. - Maya") 
            ?: "{name} abhi drive kar rahe hain, isliye call nahi utha paye. Free hote hi call back karenge. - Maya"
    }

    fun setDrivingAutoReplyText(context: Context, text: String) {
        getPrefs(context).edit().putString("driving_auto_reply_text", text).apply()
    }

    fun getStandbyTimerMinutes(context: Context): Int {
        return getPrefs(context).getInt("standby_timer_minutes", 10)
    }

    fun setStandbyTimerMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt("standby_timer_minutes", minutes).apply()
    }

    fun getEmailAddress(context: Context): String {
        return getPrefs(context).getString("email_address", "") ?: ""
    }

    fun setEmailAddress(context: Context, email: String) {
        getPrefs(context).edit().putString("email_address", email.trim()).apply()
    }

    fun getEmailAppPassword(context: Context): String {
        return getPrefs(context).getString("email_app_password", "") ?: ""
    }

    fun setEmailAppPassword(context: Context, pass: String) {
        getPrefs(context).edit().putString("email_app_password", pass.trim()).apply()
    }

    fun getEmailSenderName(context: Context): String {
        return getPrefs(context).getString("email_sender_name", "") ?: ""
    }

    fun setEmailSenderName(context: Context, name: String) {
        getPrefs(context).edit().putString("email_sender_name", name.trim()).apply()
    }

    fun getEmailSignature(context: Context): String {
        return getPrefs(context).getString("email_signature", "Regards,\nMaya AI") ?: "Regards,\nMaya AI"
    }

    fun setEmailSignature(context: Context, sig: String) {
        getPrefs(context).edit().putString("email_signature", sig).apply()
    }

    fun getEmailServer(context: Context): String {
        return getPrefs(context).getString("email_server", "smtp.gmail.com") ?: "smtp.gmail.com"
    }

    fun setEmailServer(context: Context, server: String) {
        getPrefs(context).edit().putString("email_server", server.trim()).apply()
    }

    fun getEmailPort(context: Context): String {
        return getPrefs(context).getString("email_port", "465") ?: "465"
    }

    fun setEmailPort(context: Context, port: String) {
        getPrefs(context).edit().putString("email_port", port.trim()).apply()
    }

    fun getSocialHandle(context: Context): String {
        return getPrefs(context).getString("social_handle", "") ?: ""
    }

    fun setSocialHandle(context: Context, handle: String) {
        getPrefs(context).edit().putString("social_handle", handle.trim()).apply()
    }

    fun getSocialPlatform(context: Context): String {
        return getPrefs(context).getString("social_platform", "Instagram") ?: "Instagram"
    }

    fun setSocialPlatform(context: Context, platform: String) {
        getPrefs(context).edit().putString("social_platform", platform).apply()
    }

    fun getDailyStoryEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("daily_story_enabled", true)
    }

    fun setDailyStoryEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("daily_story_enabled", enabled).apply()
    }

    fun getThemeName(context: Context): String {
        return getPrefs(context).getString("theme_name", "Clay Day") ?: "Clay Day"
    }

    fun setThemeName(context: Context, theme: String) {
        getPrefs(context).edit().putString("theme_name", theme).apply()
    }

    fun get3DCharacterName(context: Context): String {
        return getPrefs(context).getString("3d_character_name", "Maya") ?: "Maya"
    }

    fun set3DCharacterName(context: Context, name: String) {
        getPrefs(context).edit().putString("3d_character_name", name).apply()
    }

    fun getCustomRules(context: Context): List<String> {
        val raw = getPrefs(context).getString("custom_rules_csv", "keep answers short,call me dost,never open WhatsApp without asking") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("||")
    }

    fun setCustomRules(context: Context, rules: List<String>) {
        getPrefs(context).edit().putString("custom_rules_csv", rules.joinToString("||")).apply()
    }

    fun addCustomRule(context: Context, rule: String) {
        val current = getCustomRules(context).toMutableList()
        if (rule.isNotBlank() && !current.contains(rule.trim())) {
            current.add(rule.trim())
            setCustomRules(context, current)
        }
    }

    fun removeCustomRule(context: Context, rule: String) {
        val current = getCustomRules(context).toMutableList()
        current.remove(rule.trim())
        setCustomRules(context, current)
    }

    fun getVoicePersona(context: Context): String {
        return getPrefs(context).getString("voice_persona", null)
            ?: getPrefs(context).getString(KEY_VOICE_NAME, "Aoede")
            ?: "Aoede"
    }

    fun setVoicePersona(context: Context, persona: String) {
        getPrefs(context).edit()
            .putString("voice_persona", persona)
            .putString(KEY_VOICE_NAME, persona)
            .apply()
    }

    fun getPreferredMusicApp(context: Context): String {
        return getPrefs(context).getString("preferred_music_app", "YouTube") ?: "YouTube"
    }

    fun setPreferredMusicApp(context: Context, app: String) {
        getPrefs(context).edit().putString("preferred_music_app", app).apply()
    }

    fun getGeminiApiKey(context: Context): String {
        return getPrefs(context).getString("gemini_user_api_key", "") ?: ""
    }

    fun setGeminiApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString("gemini_user_api_key", key.trim()).apply()
    }

    fun getYoutubeApiKey(context: Context): String {
        return getPrefs(context).getString("youtube_user_api_key", "") ?: ""
    }

    fun setYoutubeApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString("youtube_user_api_key", key.trim()).apply()
    }

    fun getCallAnnouncements(context: Context): Boolean {
        return getPrefs(context).getBoolean("call_announcements", true)
    }

    fun setCallAnnouncements(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("call_announcements", enabled).apply()
    }

    fun getDrivingModeAutoReply(context: Context): Boolean {
        return getPrefs(context).getBoolean("driving_mode_auto_reply", true)
    }

    fun setDrivingModeAutoReply(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("driving_mode_auto_reply", enabled).apply()
    }

    fun getVoicePitch(context: Context): Float {
        return getPrefs(context).getFloat("voice_pitch", 1.07f)
    }

    fun setVoicePitch(context: Context, pitch: Float) {
        getPrefs(context).edit().putFloat("voice_pitch", pitch.coerceIn(0.95f, 1.15f)).apply()
    }

    fun getSecretModeLocked(context: Context): Boolean {
        return getPrefs(context).getBoolean("secret_mode_locked", false)
    }

    fun setSecretModeLocked(context: Context, locked: Boolean) {
        getPrefs(context).edit().putBoolean("secret_mode_locked", locked).apply()
    }

    fun getInstalledSkills(context: Context): Set<String> {
        return getPrefs(context).getStringSet("installed_skills", setOf(
            "skill_whatsapp", "skill_instagram", "skill_youtube", "skill_telephony",
            "skill_files", "skill_research", "skill_coding", "skill_system"
        )) ?: emptySet()
    }

    fun setInstalledSkills(context: Context, skills: Set<String>) {
        getPrefs(context).edit().putStringSet("installed_skills", skills).apply()
    }

    fun getActiveSubAgents(context: Context): Set<String> {
        return getPrefs(context).getStringSet("active_sub_agents", setOf(
            "agent_gemini", "agent_claude", "agent_deepseek", "agent_whisper"
        )) ?: emptySet()
    }

    fun setActiveSubAgents(context: Context, agents: Set<String>) {
        getPrefs(context).edit().putStringSet("active_sub_agents", agents).apply()
    }

    fun getSmtpEmail(context: Context): String {
        return getPrefs(context).getString("smtp_email", "") ?: ""
    }

    fun setSmtpEmail(context: Context, email: String) {
        getPrefs(context).edit().putString("smtp_email", email.trim()).apply()
    }

    fun getSmtpPassword(context: Context): String {
        return getPrefs(context).getString("smtp_password", "") ?: ""
    }

    fun setSmtpPassword(context: Context, pass: String) {
        getPrefs(context).edit().putString("smtp_password", pass).apply()
    }

    fun getSmtpHost(context: Context): String {
        return getPrefs(context).getString("smtp_host", "smtp.gmail.com") ?: "smtp.gmail.com"
    }

    fun setSmtpHost(context: Context, host: String) {
        getPrefs(context).edit().putString("smtp_host", host.trim()).apply()
    }

    fun getSmtpPort(context: Context): Int {
        return getPrefs(context).getInt("smtp_port", 587)
    }

    fun setSmtpPort(context: Context, port: Int) {
        getPrefs(context).edit().putInt("smtp_port", port).apply()
    }

    fun getCaptionStyle(context: Context): String {
        return getPrefs(context).getString("caption_style", "Poetic / Shayarana") ?: "Poetic / Shayarana"
    }

    fun setCaptionStyle(context: Context, style: String) {
        getPrefs(context).edit().putString("caption_style", style).apply()
    }

    fun getAutoDailyStory(context: Context): Boolean {
        return getPrefs(context).getBoolean("auto_daily_story", true)
    }

    fun setAutoDailyStory(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("auto_daily_story", enabled).apply()
    }

    fun getThemeMode(context: Context): String {
        return getPrefs(context).getString("theme_mode", "neon") ?: "neon"
    }

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString("theme_mode", mode).apply()
    }

    fun getAppearanceType(context: Context): String {
        return getPrefs(context).getString("appearance_type", "orb") ?: "orb"
    }

    fun setAppearanceType(context: Context, type: String) {
        getPrefs(context).edit().putString("appearance_type", type).apply()
    }

    fun getEchoGuard(context: Context): Boolean {
        return getPrefs(context).getBoolean("echo_guard", true)
    }

    fun setEchoGuard(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("echo_guard", enabled).apply()
    }

    fun getFloatingBubble(context: Context): Boolean {
        return getPrefs(context).getBoolean("floating_bubble", false)
    }

    fun setFloatingBubble(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("floating_bubble", enabled).apply()
    }

    fun getHapticsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("haptics_enabled", true)
    }

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun getTextStreamSpeed(context: Context): String {
        return getPrefs(context).getString("text_stream_speed", "Fast (तेज)") ?: "Fast (तेज)"
    }

    fun setTextStreamSpeed(context: Context, speed: String) {
        getPrefs(context).edit().putString("text_stream_speed", speed).apply()
    }

    fun getFaceEnrolled(context: Context): Boolean {
        return getPrefs(context).getBoolean("face_enrolled", false)
    }

    fun setFaceEnrolled(context: Context, enrolled: Boolean) {
        getPrefs(context).edit().putBoolean("face_enrolled", enrolled).apply()
    }

    fun getSosContact(context: Context): String {
        return getPrefs(context).getString("sos_contact", "112") ?: "112"
    }

    fun setSosContact(context: Context, contact: String) {
        getPrefs(context).edit().putString("sos_contact", contact.trim()).apply()
    }

    fun getSosTriggerWord(context: Context): String {
        return getPrefs(context).getString("sos_trigger_word", "Maya Bachao") ?: "Maya Bachao"
    }

    fun setSosTriggerWord(context: Context, word: String) {
        getPrefs(context).edit().putString("sos_trigger_word", word.trim()).apply()
    }

    fun getSosSirenEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("sos_siren_enabled", true)
    }

    fun setSosSirenEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("sos_siren_enabled", enabled).apply()
    }

    fun getActiveEventTriggers(context: Context): Set<String> {
        return getPrefs(context).getStringSet("active_event_triggers", setOf(
            "trig_charger_in", "trig_batt_low", "trig_screen_on", "trig_morning", "trig_night"
        )) ?: emptySet()
    }

    fun setActiveEventTriggers(context: Context, triggers: Set<String>) {
        getPrefs(context).edit().putStringSet("active_event_triggers", triggers).apply()
    }
}
