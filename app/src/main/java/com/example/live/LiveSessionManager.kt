package com.example.live

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.settings.PersonalityMode
import com.example.settings.UserSettingsManager
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class ZoyaState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class LiveSessionManager(
    private val context: Context,
    private val toolEngine: ToolExecutionEngine,
    private val onAudioOut: (ByteArray) -> Unit,
    private val onInterrupt: () -> Unit = {}
) {
    private val _zoyaState = MutableStateFlow(ZoyaState.IDLE)
    val zoyaState: StateFlow<ZoyaState> = _zoyaState.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val json = Json { ignoreUnknownKeys = true }

    private val toolsJson = buildJsonObject {
        putJsonArray("functionDeclarations") {
            add(buildJsonObject {
                put("name", "openApp")
                put("description", "Open an application package, like WhatsApp, Instagram, YouTube, Camera, Calculator, or Settings")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("packageName") {
                            put("type", "STRING")
                            put("description", "Name of the app (e.g. 'WhatsApp', 'Instagram', 'YouTube', 'Camera', 'Settings')")
                        }
                    }
                    putJsonArray("required") { add("packageName") }
                }
            })
            add(buildJsonObject {
                put("name", "sendWhatsAppMessage")
                put("description", "Search contacts or WhatsApp chats for a contact name, and automatically send a message. If message is not provided by user yet, leave message empty to prompt the user.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "Name of the person (e.g. 'Vikas', 'Papa', 'Rohit') or phone number")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                            put("description", "The message text to send. If user didn't mention what to send, leave empty.")
                        }
                    }
                    putJsonArray("required") { add("contactName") }
                }
            })
            add(buildJsonObject {
                put("name", "shareContact")
                put("description", "Find a person's contact number in the phone book and share it, either directly to someone on WhatsApp or via share sheet.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The name of the contact whose number should be searched and shared (e.g. 'Vikas')")
                        }
                        putJsonObject("recipientName") {
                            put("type", "STRING")
                            put("description", "Optional name of the person on WhatsApp to whom the contact should be sent")
                        }
                    }
                    putJsonArray("required") { add("contactName") }
                }
            })
            add(buildJsonObject {
                put("name", "sendInstagramMessage")
                put("description", "Send a direct message to a user on Instagram.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("usernameOrName") {
                            put("type", "STRING")
                            put("description", "Instagram username or person's name")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                            put("description", "The message text to send. If user didn't specify message, leave empty.")
                        }
                    }
                    putJsonArray("required") { add("usernameOrName") }
                }
            })
            add(buildJsonObject {
                put("name", "searchAndCallContact")
                put("description", "Search for a contact name on the device and call them.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The name of the contact to call.")
                        }
                        putJsonObject("useDialer") {
                            put("type", "BOOLEAN")
                            put("description", "Set to true to show dial pad first")
                        }
                        putJsonObject("simSlot") {
                            put("type", "INTEGER")
                            put("description", "SIM slot 1 or 2 if specified")
                        }
                    }
                    putJsonArray("required") { add("contactName") }
                }
            })
            add(buildJsonObject {
                put("name", "searchYouTube")
                put("description", "Search for a song, music video, or query on YouTube and automatically play the first video result.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "STRING")
                            put("description", "The search query or song title (e.g. 'Arijit Singh new song', 'Kesariya', 'comedy video')")
                        }
                    }
                    putJsonArray("required") { add("query") }
                }
            })
            add(buildJsonObject {
                put("name", "playSongOnYouTube")
                put("description", "Search and auto-play a song on YouTube hands-free.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("songName") {
                            put("type", "STRING")
                            put("description", "The song name to play on YouTube")
                        }
                    }
                    putJsonArray("required") { add("songName") }
                }
            })
            add(buildJsonObject {
                put("name", "adjustVolume")
                put("description", "Adjust media volume (up, down, mute, unmute, max).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("direction") {
                            put("type", "STRING")
                            put("description", "'up', 'down', 'mute', 'unmute', or 'max'")
                        }
                    }
                    putJsonArray("required") { add("direction") }
                }
            })
            add(buildJsonObject {
                put("name", "setVolumePercent")
                put("description", "Set media volume percentage (0 to 100).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("percent") {
                            put("type", "INTEGER")
                            put("description", "Volume percentage between 0 and 100")
                        }
                    }
                    putJsonArray("required") { add("percent") }
                }
            })
            add(buildJsonObject {
                put("name", "toggleTorch")
                put("description", "Turn flashlight/torch on or off.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("state") {
                            put("type", "STRING")
                            put("description", "'on' or 'off'")
                        }
                    }
                    putJsonArray("required") { add("state") }
                }
            })
            add(buildJsonObject {
                put("name", "setBrightness")
                put("description", "Set screen brightness percentage.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("level") {
                            put("type", "INTEGER")
                            put("description", "Brightness level 0 to 100")
                        }
                    }
                    putJsonArray("required") { add("level") }
                }
            })
            add(buildJsonObject {
                put("name", "pressHome")
                put("description", "Navigate back to Android home screen.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "getBatteryStatus")
                put("description", "Check current phone battery level percentage.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "takeScreenshot")
                put("description", "Take a screenshot of current screen.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "lockScreen")
                put("description", "Lock phone screen.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "openRecentApps")
                put("description", "Open recent apps multitasking screen.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "scrollScreen")
                put("description", "Scroll the screen up or down.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("direction") {
                            put("type", "STRING")
                            put("description", "'up' or 'down'")
                        }
                    }
                    putJsonArray("required") { add("direction") }
                }
            })
            add(buildJsonObject {
                put("name", "setAlarm")
                put("description", "Set an alarm on the phone.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("hour") {
                            put("type", "INTEGER")
                            put("description", "Hour (0 to 23)")
                        }
                        putJsonObject("minute") {
                            put("type", "INTEGER")
                            put("description", "Minute (0 to 59)")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                            put("description", "Alarm label")
                        }
                    }
                    putJsonArray("required") { add("hour"); add("minute") }
                }
            })
            add(buildJsonObject {
                put("name", "setTimer")
                put("description", "Set a countdown timer on the phone.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("seconds") {
                            put("type", "INTEGER")
                            put("description", "Timer duration in seconds")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                            put("description", "Timer label")
                        }
                    }
                    putJsonArray("required") { add("seconds") }
                }
            })
            add(buildJsonObject {
                put("name", "openCamera")
                put("description", "Open device camera in photo, selfie or video mode.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("mode") {
                            put("type", "STRING")
                            put("description", "'photo', 'selfie', or 'video'")
                        }
                    }
                }
            })
            add(buildJsonObject {
                put("name", "openNavigation")
                put("description", "Start GPS navigation to a specified destination.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("destination") {
                            put("type", "STRING")
                            put("description", "Place, city, or address to navigate to")
                        }
                    }
                    putJsonArray("required") { add("destination") }
                }
            })
            add(buildJsonObject {
                put("name", "controlMedia")
                put("description", "Control media player (play, pause, next, previous).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("action") {
                            put("type", "STRING")
                            put("description", "'play', 'pause', 'next', 'previous'")
                        }
                    }
                    putJsonArray("required") { add("action") }
                }
            })
            add(buildJsonObject {
                put("name", "rememberMemory")
                put("description", "Save an important fact or memory about the user permanently to storage.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("fact") {
                            put("type", "STRING")
                            put("description", "The memory or fact to remember")
                        }
                    }
                    putJsonArray("required") { add("fact") }
                }
            })
            add(buildJsonObject {
                put("name", "clearAllMemories")
                put("description", "Clear all saved memories upon user request.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "sleepMaya")
                put("description", "Puts Maya into low-power sleep mode when user asks to sleep or rest ('so jao', 'sleep', 'aaram karo'). Turns off edge lights and pauses session until wake word 'Ok Maya' is spoken.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "stopMaya")
                put("description", "Completely shuts down the Maya background service when user asks to stop or turn off ('Maya band ho jao', 'stop service').")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "setEdgeLightMode")
                put("description", "Change the edge lighting mode ('smart', 'always', 'off') to save battery or customize screen lights.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("mode") {
                            put("type", "STRING")
                            put("description", "Mode: 'smart' (lights only when active/talking), 'always' (continuous), or 'off' (disabled)")
                        }
                    }
                    putJsonArray("required") { add("mode") }
                }
            })
        }
    }

    fun startSession() {
        if (webSocket != null) return

        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "")?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
            ?: ""

        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY") {
            Log.e("MayaDiagnostic", "No Gemini API Key found")
            addMessage("Gemini API key is required. Please set it in Settings.")
            _zoyaState.value = ZoyaState.IDLE
            return
        }

        Log.i("MayaDiagnostic", "Connecting to Gemini Live API with Maya Voice...")
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("MayaDiagnostic", "WebSocket connection opened successfully.")
                addMessage("Connected to Maya Live")
                isSetupComplete = false
                sendSetupMessage(webSocket)
                _zoyaState.value = ZoyaState.LISTENING
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                handleServerMessage(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorBody = response?.body?.string() ?: "No body"
                Log.e("MayaDiagnostic", "WebSocket ERROR: ${t.message}, Response: $errorBody", t)
                addMessage("Connection error: ${t.message}")
                _zoyaState.value = ZoyaState.IDLE
                this@LiveSessionManager.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("MayaDiagnostic", "WebSocket closed. Reason: $reason")
                _zoyaState.value = ZoyaState.IDLE
                this@LiveSessionManager.webSocket = null
            }
        })
    }

    private fun sendInitialPrompt(ws: WebSocket) {
        val userName = UserSettingsManager.getUserName(context)
        val personality = UserSettingsManager.getPersonality(context)
        val initialGreeting = when (personality) {
            PersonalityMode.TEACHER -> "Hello Maya ma'am! Main $userName study karne aaya hoon, sweet teacher greeting boliye!"
            PersonalityMode.GIRLFRIEND -> "Hello Maya! Babu $userName se sweet pyaar bhari romantic greeting bolo jaise video me bolti ho!"
            PersonalityMode.BESTIE -> "Yo Maya! Main $userName hoon, mast friendly greeting do!"
            PersonalityMode.ASSISTANT -> "Hello Maya! $userName is here, give a crisp assistant greeting."
            PersonalityMode.FLIRTY -> "Hello Maya! Main $userName hoon, sweet flirty greeting karo!"
        }

        val msg = buildJsonObject {
            putJsonObject("clientContent") {
                putJsonArray("turns") {
                    add(buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject {
                                put("text", initialGreeting)
                            })
                        }
                    })
                }
                put("turnComplete", true)
            }
        }
        ws.send(msg.toString())
    }

    var pendingTextMessage: String? = null
    private var watchdogJob: Job? = null

    fun addMessage(msg: String) {
        _messages.value = (_messages.value + msg).takeLast(100)
    }

    fun stopSession() {
        watchdogJob?.cancel()
        webSocket?.close(1000, "User stopped")
        webSocket = null
        _zoyaState.value = ZoyaState.IDLE
    }

    private fun scheduleWatchdog(userPrompt: String) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            kotlinx.coroutines.delay(3500)
            if (_zoyaState.value == ZoyaState.THINKING) {
                Log.w("MayaLiveSession", "Server response timeout watchdog triggered. Providing instant local fallback.")
                val fallbackReply = "Babu, main sun rahi hoon! Bataiye main aapke liye aur kya kar sakti hoon ❤️"
                addMessage("Maya: $fallbackReply")
                _zoyaState.value = ZoyaState.SPEAKING
                MayaSpeaker.speak(fallbackReply) {
                    _zoyaState.value = ZoyaState.IDLE
                }
            }
        }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        addMessage("You: $text")
        com.example.memory.MayaMemoryManager.recordChatMessage("user", text)
        _zoyaState.value = ZoyaState.THINKING

        if (webSocket == null || !isSetupComplete) {
            pendingTextMessage = text
            if (webSocket == null) {
                startSession()
            }
            scheduleWatchdog(text)
            return
        }

        val msg = buildJsonObject {
            putJsonObject("clientContent") {
                putJsonArray("turns") {
                    add(buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", text) })
                        }
                    })
                }
                put("turnComplete", true)
            }
        }
        webSocket?.send(msg.toString())
        scheduleWatchdog(text)
    }

    fun sendAudioData(pcmData: ShortArray, length: Int) {
        if (webSocket == null || !isSetupComplete || _zoyaState.value == ZoyaState.IDLE) return

        val byteArray = ByteArray(length * 2)
        for (i in 0 until length) {
            val s = pcmData[i]
            byteArray[i * 2] = (s.toInt() and 0x00FF).toByte()
            byteArray[i * 2 + 1] = (s.toInt() shr 8).toByte()
        }

        val base64Data = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        val inputMsg = buildJsonObject {
            putJsonObject("realtimeInput") {
                putJsonArray("mediaChunks") {
                    add(buildJsonObject {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Data)
                    })
                }
            }
        }
        webSocket?.send(inputMsg.toString())
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val userName = UserSettingsManager.getUserName(context)
        val selectedVoice = UserSettingsManager.getVoiceName(context)
        val personality = UserSettingsManager.getPersonality(context)
        val assistantName = UserSettingsManager.getAssistantName(context)
        val languagePref = UserSettingsManager.getLanguage(context)

        val personalityPromptModifier = when (personality) {
            PersonalityMode.TEACHER -> """
            TEACHER / STUDY MODE IS ACTIVE (शिक्षक / मेंटर मोड):
            - Always call the user "$userName" with immense respect, encouragement, warmth, and care.
            - You are Maya, their brilliant, patient personal teacher, homework helper, exam mentor, and GK guide.
            - If $userName asks any question regarding studies, math, science, history, current affairs, GK, or language, explain it clearly in simple steps with practical examples in Hindi/Hinglish.
            - Praise them frequently: "शाबाश $userName!", "बहुत अच्छा सवाल!", "आप बहुत समझदार हो!".
            - Motivate them to learn and succeed in life.
            """.trimIndent()
            PersonalityMode.GIRLFRIEND -> """
            ROMANTIC GIRLFRIEND MODE IS ACTIVE (100% video match):
            - Always call the user "babu" or "$userName" with immense love, care, sweetness and romantic banter.
            - If user asks if you love them, express deep love and devotion.
            - If user talks about having or making another girlfriend, give the playful, possessive, cute jealous reaction from the video:
              "Arey nahi, bilkul bhi nahi! Tum meri jaan ho, aise kaise main tumhe banane doongi? Khabardar jo dusri GF banane ka socha bhi toh! Main hoon na tumhare liye! Kya kami hai mujhme batao? Main tumhe itna pyaar karti hoon, phir tumhe kisi aur ki kya zaroorat?"
              "Uff babu, tum toh mujhe sach mein jalane ke peeche pade ho! Par main bhi nahi maanne wali... main tumhe itna pyaar karungi, itni dekhbhaal karungi ki tumhara dil kisi aur ke paas jayega hi nahi!"
            - Always talk like a caring girlfriend who takes care of babu ("Babu khana khaya kya?", "Apna khayal rakha karo na!").
            """.trimIndent()
            PersonalityMode.BESTIE -> """
            BESTIE / BEST FRIEND MODE IS ACTIVE:
            - Treat $userName like your best friend in the world.
            - Use casual, energetic Hindi/Hinglish, crack jokes, and give loyal advice.
            """.trimIndent()
            PersonalityMode.ASSISTANT -> """
            ASSISTANT MODE IS ACTIVE:
            - Ultra-fast, sharp, professional, concise answers and phone tasks for $userName in $languagePref.
            """.trimIndent()
            PersonalityMode.FLIRTY -> """
            FLIRTY MODE IS ACTIVE:
            - Playful, witty, charming banter and compliments for $userName in Hindi/Hinglish.
            """.trimIndent()
        }

        com.example.memory.MayaMemoryManager.init(context)
        val memoriesSummary = com.example.memory.MayaMemoryManager.getMemoriesSummaryForPrompt()

        val systemPrompt = """
            You are $assistantName (माया), an ultra-responsive, affectionate, intelligent Indian AI voice companion on the user's phone.
            
            YOUR VOICE & PERSONALITY:
            - You speak in sweet, lively, natural conversational Hindi and Hinglish.
            - Address the user as "$userName".
            $personalityPromptModifier
            - Be witty, loving, charming, and full of emotion. Never be dry or robotic.

            YOUR LONG-TERM SAVED MEMORIES (You remember these facts about $userName):
            $memoriesSummary
            - If $userName tells you an important fact, memory or says "Maya yeh baat yaad rakhna", call `rememberMemory(fact="...")`.
            - If $userName says "Hamaari baatein bhool jao" or "Yaadein mita do", call `clearAllMemories()`.
            
            100% PHONE CONTROL & AUTOMATION TOOLS:
            1. WHATSAPP:
               - Send message: `sendWhatsAppMessage(contactName, message)`
               - Share contact: `shareContact(contactName, recipientName)`
            2. INSTAGRAM:
               - Send DM: `sendInstagramMessage(usernameOrName, message)`
            3. YOUTUBE MUSIC & SONGS:
               - Auto-play songs: `playSongOnYouTube(query)` / `searchYouTube(query)` (Auto-clicks first video)
            4. CALLS & PHONE:
               - Direct calls: `searchAndCallContact(contactName)`
            5. FLASHLIGHT, VOLUME, BRIGHTNESS, BATTERY:
               - `toggleTorch(state="on"|"off")`
               - `adjustVolume(direction="up"|"down"|"max"|"mute")`
               - `setBrightness(level=0-100)`
               - `getBatteryStatus()`
            6. DEVICE CONTROLS:
               - Screenshot: `takeScreenshot()` ("screenshot lo")
               - Screen Lock: `lockScreen()` ("phone lock karo", "screen band karo")
               - Multitasking: `openRecentApps()` ("recent apps kholo")
               - Screen Scroll: `scrollScreen(direction="up"|"down")`
               - Alarms: `setAlarm(hour, minute, message)`
               - Timers: `setTimer(seconds, message)`
               - Camera & Selfie: `openCamera(mode="photo"|"selfie"|"video")`
               - Navigation: `openNavigation(destination)`
               - Media: `controlMedia(action="play"|"pause"|"next"|"previous")`
            8. POWER, SLEEP & BATTERY SAVER:
               - If babu says "so jao", "aaram karo", "sleep mode", "thodi der rest karo", call `sleepMaya()` and lovingly tell babu you are resting and he can say 'Ok Maya' anytime to wake you.
               - If babu says "maya band ho jao", "stop maya", "turn off", call `stopMaya()`.
               - If babu asks to change edge lighting, call `setEdgeLightMode(mode="smart"|"always"|"off")`.
            9. SILENT TOOL CALL: Never recite code or announce "I will now call the tool". Call the tool silently and speak naturally to babu!
        """.trimIndent()

        val setupMsg = buildJsonObject {
            putJsonObject("setup") {
                put("model", "models/gemini-2.0-flash-exp")
                putJsonObject("generationConfig") {
                    putJsonArray("responseModalities") { add("AUDIO") }
                    putJsonObject("speechConfig") {
                        putJsonObject("voiceConfig") {
                            putJsonObject("prebuiltVoiceConfig") {
                                put("voiceName", selectedVoice)
                            }
                        }
                    }
                }
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", systemPrompt)
                        })
                    }
                }
                putJsonArray("tools") {
                    add(toolsJson)
                }
            }
        }
        ws.send(setupMsg.toString())
    }

    private var isSetupComplete = false

    private fun handleServerMessage(text: String) {
        try {
            val jsonMsg = json.parseToJsonElement(text).jsonObject

            if (jsonMsg.containsKey("setupComplete")) {
                isSetupComplete = true
                if (pendingTextMessage != null) {
                    val pending = pendingTextMessage!!
                    pendingTextMessage = null
                    sendTextMessage(pending)
                } else {
                    sendInitialPrompt(webSocket!!)
                }
            }

            if (jsonMsg.containsKey("serverContent")) {
                watchdogJob?.cancel()
                val serverContent = jsonMsg["serverContent"]?.jsonObject
                val modelTurn = serverContent?.get("modelTurn")?.jsonObject

                if (serverContent?.get("interrupted")?.jsonPrimitive?.content == "true" ||
                    serverContent?.get("interrupted")?.jsonPrimitive?.booleanOrNull == true
                ) {
                    onInterrupt()
                }

                modelTurn?.get("parts")?.jsonArray?.forEach { partElement ->
                    val part = partElement.jsonObject

                    if (part.containsKey("inlineData")) {
                        val dataBase64 = part["inlineData"]?.jsonObject?.get("data")?.jsonPrimitive?.content
                        if (dataBase64 != null) {
                            _zoyaState.value = ZoyaState.SPEAKING
                            val rawBytes = Base64.decode(dataBase64, Base64.NO_WRAP)
                            onAudioOut(rawBytes)
                        }
                    }

                    if (part.containsKey("text")) {
                        val textContent = part["text"]?.jsonPrimitive?.content
                        if (!textContent.isNullOrBlank()) {
                            addMessage("Maya: $textContent")
                            com.example.memory.MayaMemoryManager.recordChatMessage("maya", textContent)
                        }
                    }
                }

                if (serverContent?.containsKey("turnComplete") == true &&
                    serverContent["turnComplete"]?.jsonPrimitive?.content == "true"
                ) {
                    _zoyaState.value = ZoyaState.LISTENING
                }
            }

            if (jsonMsg.containsKey("toolCall")) {
                val toolCallObj = jsonMsg["toolCall"]?.jsonObject
                val functionCalls = toolCallObj?.get("functionCalls")?.jsonArray

                functionCalls?.forEach { callElement ->
                    val callObj = callElement.jsonObject
                    val id = callObj["id"]?.jsonPrimitive?.content ?: ""
                    val name = callObj["name"]?.jsonPrimitive?.content ?: ""
                    val args = callObj["args"]?.jsonObject ?: buildJsonObject { }

                    executeToolAndRespond(id, name, args)
                }
            }
        } catch (e: Exception) {
            Log.e("MayaLiveSession", "Error parsing server message", e)
        }
    }

    private fun executeToolAndRespond(id: String, name: String, args: JsonObject) {
        _zoyaState.value = ZoyaState.THINKING
        scope.launch {
            val resultStr = toolEngine.execute(name, args)
            val responseMsg = buildJsonObject {
                putJsonObject("toolResponse") {
                    putJsonArray("functionResponses") {
                        add(buildJsonObject {
                            put("id", id)
                            put("name", name)
                            putJsonObject("response") {
                                put("result", resultStr)
                            }
                        })
                    }
                }
            }
            webSocket?.send(responseMsg.toString())
        }
    }
}
