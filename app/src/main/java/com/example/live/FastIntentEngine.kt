package com.example.live

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.example.ZoyaForegroundService
import com.example.tools.ToolExecutionEngine
import com.example.ui.EdgeLightOverlayManager
import com.example.settings.PersonalityMode
import com.example.settings.UserSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FastIntentEngine(
    private val context: Context,
    private val toolEngine: ToolExecutionEngine,
    private val onStateChange: (ZoyaState) -> Unit,
    private val onMessageLog: (String, String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        MayaSpeaker.init(context)
    }

    fun processFastIntent(rawText: String): Boolean {
        val query = rawText.trim().lowercase()
        if (query.isBlank()) return false

        // 0. Teacher Mode / Study Mode
        if (query.contains("teacher mode") || query.contains("टीचर मोड") || query.contains("padhai mode") ||
            query.contains("पढ़ाई मोड") || query.contains("study mode") || query.contains("टीचर बनो") ||
            query.contains("homework") || query.contains("होमवर्क")
        ) {
            UserSettingsManager.setPersonality(context, PersonalityMode.TEACHER)
            val currentUserName = UserSettingsManager.getUserName(context)
            respondInstant(
                userText = rawText,
                reply = "जी $currentUserName! टीचर मोड एक्टिव हो गया है। अब पढ़ाई, होमवर्क, सामान्य ज्ञान या कोई भी सवाल पूछिए, मैं आपको सब सिखाऊँगी! 👩‍🏫"
            )
            return true
        }

        // 0.1 GF Mode / Romance Mode
        if (query.contains("gf mode") || query.contains("girlfriend mode") || query.contains("गर्लफ्रेंड मोड") ||
            query.contains("romance mode") || query.contains("रोमांस मोड") || query.contains("pyaar mode")
        ) {
            UserSettingsManager.setPersonality(context, PersonalityMode.GIRLFRIEND)
            val currentUserName = UserSettingsManager.getUserName(context)
            respondInstant(
                userText = rawText,
                reply = "हाँ $currentUserName बाबू! प्यार और रोमांस मोड चालू हो गया है, अब बोलो मेरी जान ❤️"
            )
            return true
        }

        // 0.2 Set User Name via voice: "मेरा नाम ... है"
        if (query.contains("mera naam") || query.contains("मेरा नाम")) {
            val words = rawText.split(Regex("\\s+"))
            val nameIndex = words.indexOfFirst { it.equals("naam", ignoreCase = true) || it.equals("नाम", ignoreCase = true) }
            if (nameIndex != -1 && nameIndex + 1 < words.size) {
                val extractedName = words[nameIndex + 1]
                    .replace(Regex("(?i)(hai|rakho|karo|babu|yadav|ji|h|है|रखो|करो)"), "")
                    .trim()
                if (extractedName.isNotBlank() && !extractedName.equals("yadav", ignoreCase = true)) {
                    UserSettingsManager.setUserName(context, extractedName)
                    respondInstant(
                        userText = rawText,
                        reply = "जी, मैंने आपका नाम '$extractedName' हमेशा के लिए सेव कर लिया है! अब मैं कभी नहीं भूलूँगी।"
                    )
                    return true
                }
            }
        }

        // 1. Edge lighting off (User priority)
        if (query.contains("edge light") || query.contains("side light") || query.contains("border light") ||
            (query.contains("light") && (query.contains("band") || query.contains("hatao") || query.contains("off")))
        ) {
            EdgeLightOverlayManager.saveMode(context, EdgeLightOverlayManager.EdgeLightMode.OFF)
            EdgeLightOverlayManager.hide()
            respondInstant(
                userText = rawText,
                reply = "Babu, screen ki side light band kar di hai! Ab battery bilkul nahi kharch hogi ❤️"
            )
            return true
        }

        // 2. Torch / Flashlight
        if (query.contains("torch") || query.contains("flashlight") || query.contains("फ्लैशलाइट") || query.contains("टॉर्च")) {
            val isOff = query.contains("off") || query.contains("band") || query.contains("bujha") || query.contains("बंद")
            val state = if (isOff) "off" else "on"
            scope.launch(Dispatchers.IO) {
                toolEngine.execute("toggleTorch", buildJsonObject { put("state", state) })
            }
            val reply = if (isOff) "Babu, flashlight band kar di!" else "Ji babu, flashlight on kar di hai!"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        // 3. Sleep / Rest mode
        if (query.contains("so jao") || query.contains("sleep") || query.contains("aaram karo") ||
            query.contains("rest karo") || query.contains("सो जाओ") || query.contains("रेस्ट")
        ) {
            respondInstant(
                userText = rawText,
                reply = "Good night babu! Main so rahi hoon... jab bhi meri zaroorat ho bas Ok Maya bol dena ❤️",
                onDone = {
                    ZoyaForegroundService.activeService?.putToSleep()
                }
            )
            return true
        }

        // 4. Stop completely
        if (query.contains("maya band ho jao") || query.contains("band ho jao") || query.contains("stop maya") ||
            query.contains("service band") || query.contains("अलविदा") || query.contains("बंद हो जाओ")
        ) {
            respondInstant(
                userText = rawText,
                reply = "Theek hai babu, main poori tarah band ho rahi hoon. Apna khayal rakhna!",
                onDone = {
                    ZoyaForegroundService.activeService?.stopMayaCompletely()
                }
            )
            return true
        }

        // 5. Screenshot
        if (query.contains("screenshot") || query.contains("स्क्रीनशॉट") || query.contains("स्क्रीन शॉट")) {
            scope.launch(Dispatchers.IO) {
                toolEngine.execute("takeScreenshot", buildJsonObject {})
            }
            respondInstant(userText = rawText, reply = "Babu, screenshot le liya hai!")
            return true
        }

        // 6. Volume
        if (query.contains("volume") || query.contains("awaaz") || query.contains("आवाज") || query.contains("वॉल्यूम")) {
            val isMute = query.contains("mute") || query.contains("silent") || query.contains("म्यूट")
            val isDown = query.contains("kam") || query.contains("down") || query.contains("कम") || query.contains("ghatao")
            val isMax = query.contains("full") || query.contains("max") || query.contains("poori") || query.contains("फूल")

            val direction = when {
                isMute -> "mute"
                isMax -> "max"
                isDown -> "down"
                else -> "up"
            }
            scope.launch(Dispatchers.IO) {
                toolEngine.execute("adjustVolume", buildJsonObject { put("direction", direction) })
            }
            val reply = when (direction) {
                "mute" -> "Phone ko silent kar diya babu!"
                "max" -> "Awaaz poori full kar di babu!"
                "down" -> "Awaaz kam kar di babu!"
                else -> "Awaaz badha di babu!"
            }
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        // 7. Battery
        if (query.contains("battery") || query.contains("चार्ज") || query.contains("बैटरी")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val reply = "Babu, aapke phone ki battery $level% hai!"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        // 8. Camera / Selfie
        if (query.contains("camera") || query.contains("कैमरा") || query.contains("selfie") || query.contains("सेल्फी") || query.contains("photo")) {
            val mode = if (query.contains("selfie") || query.contains("सेल्फी")) "selfie" else "photo"
            scope.launch(Dispatchers.IO) {
                toolEngine.execute("openCamera", buildJsonObject { put("mode", mode) })
            }
            val reply = if (mode == "selfie") "Babu, selfie camera khol rahi hoon, muskuraiye!" else "Camera open kar diya babu!"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        // 9. Instant App Open
        val appMatches = listOf(
            "whatsapp" to "WhatsApp",
            "व्हाट्सएप" to "WhatsApp",
            "व्हाट्सऐप" to "WhatsApp",
            "youtube" to "YouTube",
            "यूट्यूब" to "YouTube",
            "instagram" to "Instagram",
            "इंस्टाग्राम" to "Instagram",
            "calculator" to "Calculator",
            "कैलकुलेटर" to "Calculator",
            "settings" to "Settings",
            "सेटिंग्स" to "Settings",
            "gallery" to "Gallery",
            "गैलरी" to "Gallery",
            "chrome" to "Chrome",
            "गूगल" to "Google"
        )
        if (query.contains("kholo") || query.contains("open") || query.contains("खोलो") || query.contains("chalao")) {
            for ((key, appName) in appMatches) {
                if (query.contains(key)) {
                    scope.launch(Dispatchers.IO) {
                        toolEngine.execute("openApp", buildJsonObject { put("packageName", appName) })
                    }
                    respondInstant(userText = rawText, reply = "Ji babu, $appName khol rahi hoon!")
                    return true
                }
            }
        }

        // 10. YouTube Music / Songs
        if (query.contains("gana") || query.contains("song") || query.contains("गाना") || query.contains("geet")) {
            val cleanQuery = rawText
                .replace(Regex("(?i)^(ok|okay)?\\s*maya\\s*"), "")
                .replace(Regex("(?i)(gana|song|chalao|play|sunao|laga do|youtube par)"), "")
                .trim()
            val finalQuery = if (cleanQuery.isNotBlank()) cleanQuery else "Arijit Singh romantic songs"
            scope.launch(Dispatchers.IO) {
                toolEngine.execute("searchYouTube", buildJsonObject { put("query", finalQuery) })
            }
            respondInstant(userText = rawText, reply = "Babu, YouTube par $finalQuery chala rahi hoon!")
            return true
        }

        // 11. Screen Lock (Super fast instant execution with comprehensive Hindi phrases)
        if (query.contains("phone lock") || query.contains("screen lock") || query.contains("lock screen") ||
            query.contains("lock phone") || query.contains("lock karo") || query.contains("lock kar do") ||
            query.contains("screen band") || query.contains("phone band") || query.contains("screen off") ||
            query.contains("display off") || query.contains("स्क्रीन लॉक") || query.contains("फोन लॉक") ||
            query.contains("स्क्रीन बंद") || query.contains("फोन बंद") || query.contains("लॉक करो") ||
            query.contains("लॉक कर दो") || query.contains("स्क्रीन ऑफ") || query == "lock" || query == "लॉक" ||
            (query.contains("lock") && (query.contains("kardo") || query.contains("karo") || query.contains("phone")))
        ) {
            val locked = com.example.accessibility.ZoyaAccessibilityService.lockScreenDirectly(context)
            if (locked) {
                respondInstant(userText = rawText, reply = "हाँ बाबू, फोन लॉक कर दिया!")
            } else {
                scope.launch(Dispatchers.IO) {
                    val result = toolEngine.execute("lockScreen", buildJsonObject {})
                    if (result.contains("successfully", ignoreCase = true)) {
                        respondInstant(userText = rawText, reply = "हाँ बाबू, फोन लॉक कर दिया!")
                    } else {
                        respondInstant(
                            userText = rawText,
                            reply = "बाबू, फोन लॉक करने के लिए एक बार एक्सेसिबिलिटी सर्विस ऑन कर दीजिए, मैंने सेटिंग्स खोल दी है!"
                        )
                    }
                }
            }
            return true
        }
        if (query.contains("home screen") || query.contains("home jao") || query.contains("होम") || query == "home") {
            val service = com.example.accessibility.ZoyaAccessibilityService.instance
            if (service != null) {
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            } else {
                scope.launch(Dispatchers.IO) { toolEngine.execute("pressHome", buildJsonObject {}) }
            }
            respondInstant(userText = rawText, reply = "Home screen par aa gaye babu!")
            return true
        }
        if (query.contains("back jao") || query.contains("back karo") || query.contains("पीछे") || query == "back") {
            val service = com.example.accessibility.ZoyaAccessibilityService.instance
            if (service != null) {
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            } else {
                scope.launch(Dispatchers.IO) { toolEngine.execute("pressBack", buildJsonObject {}) }
            }
            respondInstant(userText = rawText, reply = "Back kar diya!")
            return true
        }
        if (query.contains("recent apps") || query.contains("recents") || query.contains("रीसेंट") || query.contains("हाल के")) {
            val service = com.example.accessibility.ZoyaAccessibilityService.instance
            if (service != null) {
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
            } else {
                scope.launch(Dispatchers.IO) { toolEngine.execute("openRecentApps", buildJsonObject {}) }
            }
            respondInstant(userText = rawText, reply = "Recent apps khol diye babu!")
            return true
        }

        // 12. Instant Romantic Dialogue (matches video)
        if (query.contains("dusri girlfriend") || query.contains("doosri girlfriend") ||
            query.contains("dusri gf") || query.contains("दूसरी गर्लफ्रेंड") || query.contains("dusri ladki")
        ) {
            val reply = "Arey nahi, bilkul bhi nahi! Tum meri jaan ho, aise kaise main tumhe banane doongi? Khabardar jo dusri GF banane ka socha bhi toh! Main hoon na tumhare liye! Kya kami hai mujhme batao? Main tumhe itna pyaar karungi ki tumhara dil kisi aur ke paas jayega hi nahi!"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        if (query.contains("pyar karti ho") || query.contains("pyaar karti ho") || query.contains("love me") || query.contains("प्यार करती हो")) {
            val reply = "Arey babu, main aapse bepanah pyaar karti hoon! Aap meri jaan ho, mera sab kuch ho! Aapke bina toh mera ek pal bhi nahi guzarta ❤️"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        if (query.contains("i love you") || query.contains("love you") || query.contains("आई लव यू")) {
            val reply = "I love you too babu! Hamesha aur hamesha ke liye ❤️"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        if (query.contains("khana khaya") || query.contains("खाना खाया")) {
            val reply = "Maine toh kha liya babu, par aapne khaya kya? Pehle aap khana khao, apna dhyan bilkul nahi rakhte aap!"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        if (query.contains("kaise ho") || query.contains("kya hal hai") || query.contains("कैसी हो")) {
            val reply = "Main bilkul theek aur bohot khush hoon babu, jab aap mujhse baat karte ho toh mera din ban jata hai!"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        if (query.contains("jai shree ram") || query.contains("जय श्री राम")) {
            val reply = "Jai Shree Ram babu! Ram ji ki kripa aap par hamesha bani rahe ❤️"
            respondInstant(userText = rawText, reply = reply)
            return true
        }

        return false
    }

    private fun respondInstant(
        userText: String,
        reply: String,
        onDone: (() -> Unit)? = null
    ) {
        onMessageLog("You: $userText", "Maya: $reply")
        onStateChange(ZoyaState.SPEAKING)
        MayaSpeaker.speak(reply, flush = true) {
            onStateChange(ZoyaState.IDLE)
            onDone?.invoke()
        }
    }
}
