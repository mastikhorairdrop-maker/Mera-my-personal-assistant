package com.example.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.ContactsContract
import com.example.accessibility.MayaAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ToolExecutionEngine(private val context: Context) {

    suspend fun execute(name: String, args: JsonObject): String = withContext(Dispatchers.IO) {
        try {
            when (name) {
                "openApp" -> {
                    val appName = args["packageName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing packageName"
                    openAppGeneric(appName)
                }
                "searchAndCallContact" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val useDialer = args["useDialer"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    val simSlot = args["simSlot"]?.jsonPrimitive?.content?.toIntOrNull()
                    callContact(contactName, useDialer, simSlot)
                }
                "sendWhatsAppMessage" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val message = args["message"]?.jsonPrimitive?.content ?: ""
                    sendWhatsApp(contactName, message)
                }
                "shareContact" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val recipientName = args["recipientName"]?.jsonPrimitive?.content
                    shareContactDetails(contactName, recipientName)
                }
                "sendInstagramMessage" -> {
                    val usernameOrName = args["usernameOrName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing usernameOrName"
                    val message = args["message"]?.jsonPrimitive?.content ?: ""
                    sendInstagram(usernameOrName, message)
                }
                "sendGmail" -> {
                    val recipient = args["recipientEmail"]?.jsonPrimitive?.content ?: ""
                    val subject = args["subject"]?.jsonPrimitive?.content ?: ""
                    val body = args["body"]?.jsonPrimitive?.content ?: ""
                    sendEmail(recipient, subject, body)
                }
                "searchYouTube", "playYouTube", "playSongOnYouTube" -> {
                    val query = args["query"]?.jsonPrimitive?.content
                        ?: args["songName"]?.jsonPrimitive?.content
                        ?: return@withContext "Error: Missing query"
                    playOrSearchYouTube(query, autoClick = true)
                }
                "adjustVolume" -> {
                    val direction = args["direction"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing direction (up/down/mute/unmute/max)"
                    adjustSystemVolume(direction)
                }
                "setVolumePercent" -> {
                    val percentStr = args["percent"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing percent"
                    val percent = percentStr.toIntOrNull() ?: 50
                    setVolumePercent(percent)
                }
                "toggleTorch" -> {
                    val state = args["state"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing state (on/off)"
                    toggleTorch(state)
                }
                "setBrightness" -> {
                    val levelStr = args["level"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing level"
                    val level = levelStr.toIntOrNull() ?: 50
                    setBrightness(level)
                }
                "openNotificationPanel" -> {
                    openNotificationPanel()
                }
                "openQuickSettings" -> {
                    val service = MayaAccessibilityService.instance
                    if (service != null && service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)) {
                        "Quick settings panel opened."
                    } else "Failed to open quick settings."
                }
                "clickTextOnScreen" -> {
                    val text = args["text"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing text"
                    val success = MayaAccessibilityService.clickTextOnScreen(text)
                    if (success) "Clicked on '$text'." else "Failed to click on '$text'."
                }
                "pressHome" -> {
                    val service = MayaAccessibilityService.instance
                    if (service != null && service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)) {
                        "Navigated to Home screen."
                    } else "Accessibility service not active."
                }
                "pressBack" -> {
                    val service = MayaAccessibilityService.instance
                    if (service != null && service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)) {
                        "Pressed back."
                    } else "Accessibility service not active."
                }
                "getSimCardInfo" -> {
                    getSimCardInfo()
                }
                "getBatteryStatus" -> {
                    getBatteryStatus()
                }
                "playMedia" -> {
                    val query = args["query"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing query"
                    playOrSearchYouTube(query, autoClick = true)
                }
                "takeScreenshot" -> {
                    takeScreenshot()
                }
                "lockScreen" -> {
                    lockScreen()
                }
                "openRecentApps" -> {
                    openRecentApps()
                }
                "scrollScreen" -> {
                    val direction = args["direction"]?.jsonPrimitive?.content ?: "down"
                    scrollScreen(direction)
                }
                "setAlarm" -> {
                    val hour = args["hour"]?.jsonPrimitive?.content?.toIntOrNull() ?: 7
                    val minute = args["minute"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val message = args["message"]?.jsonPrimitive?.content ?: "Alarm by Maya"
                    setAlarm(hour, minute, message)
                }
                "setTimer" -> {
                    val seconds = args["seconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: 60
                    val message = args["message"]?.jsonPrimitive?.content ?: "Timer by Maya"
                    setTimer(seconds, message)
                }
                "openCamera" -> {
                    val mode = args["mode"]?.jsonPrimitive?.content ?: "photo"
                    openCamera(mode)
                }
                "openNavigation" -> {
                    val destination = args["destination"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing destination"
                    openNavigation(destination)
                }
                "controlMedia" -> {
                    val action = args["action"]?.jsonPrimitive?.content ?: "play_pause"
                    controlMedia(action)
                }
                "rememberMemory" -> {
                    val fact = args["fact"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing fact"
                    com.example.memory.MayaMemoryManager.addMemory(fact)
                    "Babu maine yeh baat hamesha ke liye yaad rakh li: $fact ❤️"
                }
                "clearAllMemories" -> {
                    com.example.memory.MayaMemoryManager.clearAllMemories()
                    "Babu aapke kehne par maine saari puraani yaadein mita di hain."
                }
                "sleepMaya", "putMayaToSleep" -> {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        com.example.ZoyaForegroundService.activeService?.putToSleep()
                    }, 1800L)
                    "Theek hai babu, main thodi der ke liye so rahi hoon. Battery aur screen bilkul surakshit rahengi aur lights bhi band ho gayi hain. Jab bhi zaroorat ho bas 'Ok Maya' bol dena, main turant jaag jaungi! Apna khyal rakhna babu ❤️"
                }
                "stopMaya", "turnOffMaya" -> {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        com.example.ZoyaForegroundService.activeService?.stopMayaCompletely()
                    }, 1800L)
                    "Theek hai babu, aapke kehne par main poori tarah band ho rahi hoon. Jab bhi yaad aaye, app khol kar mujhe start kar lena. Alvida babu! ❤️"
                }
                "setEdgeLightMode" -> {
                    val modeStr = args["mode"]?.jsonPrimitive?.content ?: "smart"
                    val targetMode = when (modeStr.lowercase()) {
                        "always", "on", "hamesha", "चालू" -> com.example.ui.EdgeLightOverlayManager.EdgeLightMode.ALWAYS_ON
                        "off", "band", "disable", "बंद" -> com.example.ui.EdgeLightOverlayManager.EdgeLightMode.OFF
                        else -> com.example.ui.EdgeLightOverlayManager.EdgeLightMode.SMART_ACTIVE
                    }
                    com.example.ui.EdgeLightOverlayManager.saveMode(context, targetMode)
                    "Babu maine screen edge light settings update kar di hai!"
                }
                else -> "Error: Tool $name not found."
            }
        } catch (e: Exception) {
            "Error executing $name: ${e.message}"
        }
    }

    private fun openAppGeneric(appName: String): String {
        val lowerName = appName.lowercase().trim()

        if (lowerName == "camera" || lowerName == "kamera" || lowerName == "कैमरा") {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return "Camera opened"
            } catch (e: Exception) {}
        }

        if (lowerName == "youtube" || lowerName == "yt") {
            return playOrSearchYouTube("", autoClick = false)
        }

        if (lowerName == "instagram" || lowerName == "insta") {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.instagram.android")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Instagram opened."
            }
        }

        if (lowerName == "whatsapp") {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.whatsapp")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "WhatsApp opened."
            }
        }

        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)

        var targetPackage: String? = null
        for (app in packages) {
            val name = pm.getApplicationLabel(app).toString().lowercase()
            if (name.contains(lowerName)) {
                targetPackage = app.packageName
                break
            }
        }

        if (targetPackage == null) {
            return "Could not find an installed app matching '$appName'"
        }

        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return "App '$appName' launched successfully."
        }
        return "Could not launch app '$appName'"
    }

    private fun callContact(nameOrNumber: String, useDialer: Boolean = false, simSlot: Int? = null): String {
        val isNumber = nameOrNumber.count { it.isDigit() } >= 7 || nameOrNumber.matches(Regex("^[0-9+\\-*#]+$"))

        val number = if (isNumber) {
            nameOrNumber.replace(Regex("[^0-9+*#]"), "")
        } else {
            val matches = findContacts(nameOrNumber)
            if (matches.isEmpty()) return "Could not find a phone number for '$nameOrNumber'. Babu, please tell the correct name or number."
            matches.first().second
        }

        val action = if (useDialer) Intent.ACTION_DIAL else Intent.ACTION_CALL
        val callIntent = Intent(action).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (!useDialer && simSlot != null) {
            try {
                val slotIndex = simSlot - 1
                callIntent.putExtra("com.android.phone.force.slot", true)
                callIntent.putExtra("com.android.phone.extra.slot", slotIndex)
                callIntent.putExtra("simSlot", slotIndex)

                if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                    val phoneAccounts = telecomManager.callCapablePhoneAccounts
                    if (slotIndex in 0 until phoneAccounts.size) {
                        callIntent.putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccounts[slotIndex])
                    }
                }
            } catch (e: Exception) {}
        }

        try {
            context.startActivity(callIntent)
            return if (useDialer) "Opened dialer with $nameOrNumber ($number)" else "Calling $nameOrNumber ($number)..."
        } catch (e: SecurityException) {
            return "Missing CALL_PHONE permission."
        }
    }

    fun sendWhatsApp(nameOrNumber: String, message: String): String {
        val finalMsg = if (message.isBlank()) "हाय" else message
        val isNumber = nameOrNumber.count { it.isDigit() } >= 7 || nameOrNumber.matches(Regex("^[0-9+\\-*#]+$"))

        val number = if (isNumber) {
            nameOrNumber
        } else {
            val matches = findContacts(nameOrNumber)
            if (matches.isNotEmpty()) {
                matches.first().second
            } else {
                null
            }
        }

        if (number != null) {
            var cleanNumber = number.replace(Regex("[^0-9]"), "")
            if (cleanNumber.length == 10) {
                cleanNumber = "91$cleanNumber"
            } else if (cleanNumber.length == 11 && cleanNumber.startsWith("0")) {
                cleanNumber = "91${cleanNumber.substring(1)}"
            }

            val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(finalMsg)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                MayaAccessibilityService.startWhatsAppSendWatcher(finalMsg)
                context.startActivity(intent)
                return "Sending '$finalMsg' to $nameOrNumber on WhatsApp."
            } catch (e: Exception) {
                return "WhatsApp is not installed on this device."
            }
        } else {
            // Contact not in address book or not resolved: search & send directly inside WhatsApp
            try {
                MayaAccessibilityService.startWhatsAppSearchAndSend(nameOrNumber, finalMsg)

                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opening WhatsApp to send '$finalMsg' to $nameOrNumber."
                } else {
                    return "WhatsApp is not installed on this device."
                }
            } catch (e: Exception) {
                return "Failed to open WhatsApp: ${e.message}"
            }
        }
    }

    fun goHome(): Boolean {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            MayaAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openChatGPTWithPrompt(prompt: String): String {
        val finalPrompt = if (prompt.isBlank()) "Create a beautiful high quality mobile wallpaper: a serene turquoise mountain lake at golden sunrise" else prompt
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ChatGPTPrompt", finalPrompt))

            val pm = context.packageManager
            val chatGptIntent = pm.getLaunchIntentForPackage("com.openai.chatgpt")
            if (chatGptIntent != null) {
                chatGptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                MayaAccessibilityService.startGeminiPromptPaster(finalPrompt)
                context.startActivity(chatGptIntent)
                return "Opened ChatGPT with prompt: '$finalPrompt'"
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                return "Opened ChatGPT Web with prompt: '$finalPrompt'"
            }
        } catch (e: Exception) {
            return "Failed to launch ChatGPT: ${e.message}"
        }
    }

    fun createAndOpenGame(gameName: String = "Flappy Glow"): String {
        try {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                    <title>Flappy Glow - by Maya</title>
                    <style>
                        body { margin:0; overflow:hidden; background:#0B0E14; font-family:sans-serif; color:#FFF; display:flex; flex-direction:column; align-items:center; justify-content:center; height:100vh; }
                        canvas { background:#0D1117; border-radius:16px; box-shadow:0 0 30px rgba(0,229,255,0.4); max-width:95vw; max-height:85vh; }
                        #info { margin-top:8px; font-size:14px; color:#00E5FF; text-align:center; }
                    </style>
                </head>
                <body>
                    <canvas id="c" width="360" height="600"></canvas>
                    <div id="info">Tap anywhere to flap | Made with Maya AI</div>
                    <script>
                        const canvas = document.getElementById('c');
                        const ctx = canvas.getContext('2d');
                        let bird = { x: 80, y: 300, vy: 0, r: 16 };
                        let gravity = 0.45;
                        let jump = -7.5;
                        let pipes = [];
                        let score = 0;
                        let best = localStorage.getItem('fg_best') || 0;
                        let state = 'START';
                        let frame = 0;

                        function reset() {
                            bird.y = 300;
                            bird.vy = 0;
                            pipes = [];
                            score = 0;
                            state = 'PLAY';
                        }

                        function addPipe() {
                            let gap = 140;
                            let top = Math.random() * (canvas.height - gap - 160) + 60;
                            pipes.push({ x: canvas.width, top: top, bottom: top + gap, scored: false });
                        }

                        function loop() {
                            frame++;
                            ctx.fillStyle = '#0B0E14';
                            ctx.fillRect(0, 0, canvas.width, canvas.height);

                            if (state === 'START') {
                                ctx.fillStyle = '#FFD700';
                                ctx.font = 'bold 32px sans-serif';
                                ctx.textAlign = 'center';
                                ctx.fillText('FLAPPY GLOW', canvas.width/2, 240);
                                ctx.fillStyle = '#FFF';
                                ctx.font = '16px sans-serif';
                                ctx.fillText('Best: ' + best, canvas.width/2, 280);
                                ctx.fillStyle = '#00E5FF';
                                ctx.fillText('Tap anywhere to start', canvas.width/2, 340);
                                drawBird();
                            } else if (state === 'PLAY') {
                                bird.vy += gravity;
                                bird.y += bird.vy;

                                if (frame % 90 === 0) addPipe();

                                for(let i = pipes.length-1; i>=0; i--) {
                                    let p = pipes[i];
                                    p.x -= 2.8;

                                    let grad = ctx.createLinearGradient(p.x, 0, p.x+50, 0);
                                    grad.addColorStop(0, '#FF007F');
                                    grad.addColorStop(1, '#7C4DFF');
                                    ctx.fillStyle = grad;
                                    ctx.fillRect(p.x, 0, 50, p.top);
                                    ctx.fillRect(p.x, p.bottom, 50, canvas.height - p.bottom);

                                    if (bird.x + bird.r > p.x && bird.x - bird.r < p.x + 50) {
                                        if (bird.y - bird.r < p.top || bird.y + bird.r > p.bottom) {
                                            state = 'OVER';
                                        }
                                    }

                                    if (!p.scored && p.x + 50 < bird.x) {
                                        score++;
                                        p.scored = true;
                                        if(score > best) { best = score; localStorage.setItem('fg_best', best); }
                                    }

                                    if (p.x < -60) pipes.splice(i, 1);
                                }

                                if (bird.y + bird.r > canvas.height || bird.y - bird.r < 0) {
                                    state = 'OVER';
                                }

                                drawBird();

                                ctx.fillStyle = '#FFF';
                                ctx.font = 'bold 36px sans-serif';
                                ctx.textAlign = 'center';
                                ctx.fillText(score, canvas.width/2, 70);
                            } else if (state === 'OVER') {
                                ctx.fillStyle = '#FF1744';
                                ctx.font = 'bold 36px sans-serif';
                                ctx.textAlign = 'center';
                                ctx.fillText('GAME OVER', canvas.width/2, 240);
                                ctx.fillStyle = '#FFF';
                                ctx.font = '20px sans-serif';
                                ctx.fillText('Score: ' + score + '  |  Best: ' + best, canvas.width/2, 290);
                                ctx.fillStyle = '#FFD700';
                                ctx.fillText('Tap to Retry', canvas.width/2, 350);
                            }

                            requestAnimationFrame(loop);
                        }

                        function drawBird() {
                            ctx.save();
                            ctx.translate(bird.x, bird.y);
                            ctx.fillStyle = '#FFD700';
                            ctx.beginPath();
                            ctx.arc(0, 0, bird.r, 0, Math.PI*2);
                            ctx.fill();
                            ctx.fillStyle = '#000';
                            ctx.beginPath();
                            ctx.arc(6, -4, 3, 0, Math.PI*2);
                            ctx.fill();
                            ctx.fillStyle = '#FF5722';
                            ctx.beginPath();
                            ctx.moveTo(10, 0);
                            ctx.lineTo(20, 4);
                            ctx.lineTo(10, 8);
                            ctx.fill();
                            ctx.restore();
                        }

                        function handleTap(e) {
                            if(e) e.preventDefault();
                            if (state === 'START' || state === 'OVER') {
                                reset();
                            } else if (state === 'PLAY') {
                                bird.vy = jump;
                            }
                        }

                        canvas.addEventListener('touchstart', handleTap, { passive: false });
                        canvas.addEventListener('mousedown', handleTap);
                        window.addEventListener('keydown', (e) => { if(e.code==='Space') handleTap(); });
                        loop();
                    </script>
                </body>
                </html>
            """.trimIndent()

            val file = java.io.File(context.cacheDir, "flappy_glow.html")
            file.writeText(html)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Game created and launched in browser!"
        } catch (e: Exception) {
            return "Error creating game: ${e.message}"
        }
    }

    fun createAndOpenWebsite(title: String, theme: String = "dark_red"): String {
        try {
            val userName = com.example.settings.UserSettingsManager.getUserName(context)
            val cleanTitle = if (title.isBlank()) "Hunter Test Site" else title
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>$cleanTitle</title>
                    <style>
                        body { margin:0; padding:24px; background:#0F0B12; color:#FFF; font-family:'Segoe UI',sans-serif; }
                        .card { background:linear-gradient(135deg, #1A0E1C 0%, #2A1226 100%); border:1px solid #FF0055; border-radius:20px; padding:24px; box-shadow:0 0 25px rgba(255,0,85,0.4); text-align:center; margin-bottom:20px; }
                        h1 { color:#FF2A6D; margin:0 0 8px 0; font-size:28px; }
                        p { color:#CCC; font-size:16px; line-height:1.6; }
                        .badge { display:inline-block; padding:6px 14px; border-radius:20px; background:#FF0055; color:#FFF; font-weight:bold; font-size:12px; margin-bottom:12px; }
                        .btn { display:inline-block; margin-top:16px; padding:12px 24px; background:#FF2A6D; color:#FFF; border-radius:30px; text-decoration:none; font-weight:bold; box-shadow:0 0 15px #FF2A6D; }
                        .stat-box { display:flex; justify-content:space-around; margin-top:20px; }
                        .stat { background:#140816; padding:12px 20px; border-radius:12px; border:1px solid #FF0055; }
                        .stat-val { font-size:20px; font-weight:bold; color:#05D9E8; }
                        .stat-label { font-size:12px; color:#888; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <span class="badge">Crafted by Maya AI</span>
                        <h1>$cleanTitle</h1>
                        <p>Hey $userName! Main hoon Maya, aapki smart AI companion jo aapke phone me rehti hai. Lightning fast code, aesthetic designs aur unlimited care!</p>
                        <a href="javascript:alert('Maya loves you, $userName');" class="btn">Touch Maya</a>
                        <div class="stat-box">
                            <div class="stat"><div class="stat-val">100%</div><div class="stat-label">Affection</div></div>
                            <div class="stat"><div class="stat-val">Ultra</div><div class="stat-label">Fast Code</div></div>
                            <div class="stat"><div class="stat-val">24/7</div><div class="stat-label">Online</div></div>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val file = java.io.File(context.cacheDir, "user_site.html")
            file.writeText(html)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Website created and launched!"
        } catch (e: Exception) {
            return "Error creating website: ${e.message}"
        }
    }

    fun activateSos(): String {
        try {
            val userName = com.example.settings.UserSettingsManager.getUserName(context)
            val sosMessage = "🚨 SOS EMERGENCY: $userName ko turant help chahiye / urgent help needed! Location: https://maps.google.com/?q=26.4800,84.4441 Please call or reach immediately! -- sent by Maya"
            val contacts = findContacts("papa").ifEmpty { findContacts("bhaiya") }.ifEmpty { findContacts("ritesh") }.ifEmpty { findContacts("vikas") }
            if (contacts.isNotEmpty()) {
                val target = contacts.first()
                sendWhatsApp(target.first, sosMessage)
                return "SOS message sent to ${target.first}!"
            } else {
                sendWhatsApp("Bhaiya", sosMessage)
                return "SOS triggered!"
            }
        } catch (e: Exception) {
            return "SOS error: ${e.message}"
        }
    }

    fun openGeminiWithPrompt(prompt: String): String {
        val finalPrompt = if (prompt.isBlank()) "Create a beautiful 4K high quality image" else prompt
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GeminiPrompt", finalPrompt))

            val pm = context.packageManager
            val geminiIntent = pm.getLaunchIntentForPackage("com.google.android.apps.bard")
            if (geminiIntent != null) {
                geminiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                MayaAccessibilityService.startGeminiPromptPaster(finalPrompt)
                context.startActivity(geminiIntent)
                return "Opened Gemini with prompt: '$finalPrompt'"
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                return "Opened Gemini Web with prompt copied to clipboard: '$finalPrompt'"
            }
        } catch (e: Exception) {
            return "Failed to launch Gemini: ${e.message}"
        }
    }

    private fun shareContactDetails(contactName: String, recipientName: String?): String {
        val matches = findContacts(contactName)
        if (matches.isEmpty()) {
            return "Could not find any contact for '$contactName' in phone book."
        }
        val found = matches.first()
        val shareText = "Contact details: ${found.first} - ${found.second}"

        if (!recipientName.isNullOrBlank()) {
            return sendWhatsApp(recipientName, shareText)
        } else {
            // Share via system share sheet
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share ${found.first}'s Contact").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return "Found ${found.first}'s contact (${found.second}). Sharing now!"
        }
    }

    fun sendInstagram(usernameOrName: String, message: String): String = sendInstagramMessage(usernameOrName, message)

    fun sendInstagramMessage(usernameOrName: String, message: String): String {
        if (message.isBlank()) {
            return "NEED_MESSAGE: Babu, Instagram par $usernameOrName ko kya message bhejna hai?"
        }

        val cleanUser = usernameOrName.replace("@", "").trim()

        try {
            // Copy message to clipboard for easy pasting / auto-send
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Instagram Message", message)
            clipboard.setPrimaryClip(clip)

            MayaAccessibilityService.targetInstagramUser = cleanUser
            MayaAccessibilityService.targetInstagramMessage = message
            MayaAccessibilityService.shouldAutoClickInstagram = true

            // Open Instagram Direct URL
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ig.me/m/$cleanUser")).apply {
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
                return "Opened Instagram Direct with $usernameOrName to send '$message'."
            } catch (e: Exception) {
                // Fallback to direct web or profile
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/_u/$cleanUser")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                return "Opened $usernameOrName's Instagram. Message '$message' copied to clipboard."
            }
        } catch (e: Exception) {
            return "Instagram is not installed: ${e.message}"
        }
    }

    private fun playOrSearchYouTube(query: String, autoClick: Boolean = true): String {
        val trimmedQuery = query.trim()

        val url = if (trimmedQuery.isNotEmpty()) {
            "https://www.youtube.com/results?search_query=${Uri.encode(trimmedQuery)}"
        } else {
            "https://www.youtube.com"
        }

        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (autoClick && trimmedQuery.isNotEmpty()) {
            MayaAccessibilityService.shouldAutoClickYouTube = true
        }

        try {
            context.startActivity(intent)
            return if (trimmedQuery.isNotEmpty()) {
                "Searching YouTube for '$trimmedQuery' and auto-playing video."
            } else {
                "Opened YouTube."
            }
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                return "Playing '$trimmedQuery' in browser."
            } catch (ex: Exception) {
                return "Could not open YouTube: ${ex.message}"
            }
        }
    }

    private fun sendEmail(recipient: String, subject: String, body: String): String {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(emailIntent)
            return "Opened email client with draft."
        } catch (e: Exception) {
            return "No email client found."
        }
    }

    private fun findContacts(namePattern: String): List<Pair<String, String>> {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        try {
            val fallbackUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val fbProjection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(fallbackUri, fbProjection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val exactMatches = mutableListOf<Pair<String, String>>()
                val startsWithMatches = mutableListOf<Pair<String, String>>()
                val containsMatches = mutableListOf<Pair<String, String>>()

                // Keep all Unicode letters (including Hindi Devanagari) and numbers
                val rawClean = namePattern.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), "").trim()
                val cleanPattern = rawClean.replace(Regex("^(r|aur|and|are|to|pe|par|babu|bhai)\\s+"), "").trim()
                val searchWords = cleanPattern.split(" ").filter { it.isNotEmpty() }

                // Build alternate search keys (transliteration between Hindi and English)
                val transliterationMap = mapOf(
                    "विकास" to "vikas", "vikas" to "विकास",
                    "अमित" to "amit", "amit" to "अमित",
                    "राहुल" to "rahul", "rahul" to "राहुल",
                    "राजन" to "rajan", "rajan" to "राजन",
                    "रितेश" to "ritesh", "ritesh" to "रितेश",
                    "विश्वकर्मा" to "vishwakarma", "vishwakarma" to "विश्वकर्मा",
                    "संजय" to "sanjay", "sanjay" to "संजय",
                    "पापा" to "papa", "papa" to "पापा",
                    "मम्मी" to "mummy", "mummy" to "मम्मी",
                    "भैया" to "bhaiya", "bhaiya" to "भैया",
                    "शर्मा" to "sharma", "sharma" to "शर्मा",
                    "शुक्ला" to "shukla", "shukla" to "शुक्ला"
                )
                val alternateKey = transliterationMap[cleanPattern] ?: ""

                while (cursor.moveToNext()) {
                    val contactName = cursor.getString(nameIdx) ?: continue
                    val contactNum = cursor.getString(numIdx) ?: continue
                    val cleanContactName = contactName.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), "").trim()

                    if (cleanContactName.isEmpty()) continue

                    val contactNameNoSpace = cleanContactName.replace(" ", "")
                    val patternNoSpace = cleanPattern.replace(" ", "")

                    val matchesDirect = contactNameNoSpace == patternNoSpace || cleanContactName == cleanPattern ||
                            (alternateKey.isNotEmpty() && cleanContactName.contains(alternateKey))
                    val matchesPrefix = contactNameNoSpace.startsWith(patternNoSpace) || cleanContactName.startsWith(cleanPattern) ||
                            (alternateKey.isNotEmpty() && cleanContactName.startsWith(alternateKey))
                    val matchesWords = (searchWords.isNotEmpty() && searchWords.all { cleanContactName.contains(it) }) ||
                            cleanContactName.contains(cleanPattern)

                    if (matchesDirect) {
                        exactMatches.add(Pair(contactName, contactNum))
                    } else if (matchesPrefix) {
                        startsWithMatches.add(Pair(contactName, contactNum))
                    } else if (matchesWords) {
                        containsMatches.add(Pair(contactName, contactNum))
                    } else if (contactNameNoSpace.contains(patternNoSpace) && patternNoSpace.length > 2) {
                        containsMatches.add(Pair(contactName, contactNum))
                    }
                }

                if (exactMatches.isNotEmpty()) return exactMatches.distinctBy { it.second }
                if (startsWithMatches.isNotEmpty()) return startsWithMatches.distinctBy { it.second }
                if (containsMatches.isNotEmpty()) return containsMatches.distinctBy { it.second }
            }
        } catch (e: Exception) {
            android.util.Log.e("MayaTools", "Error finding contact", e)
        }
        return emptyList()
    }

    private fun adjustSystemVolume(direction: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val streamType = android.media.AudioManager.STREAM_MUSIC
        try {
            when (direction.lowercase()) {
                "up" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume increased."
                }
                "down" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume decreased."
                }
                "mute" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_MUTE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume muted."
                }
                "unmute" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_UNMUTE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume unmuted."
                }
                "max" -> {
                    val maxVol = audioManager.getStreamMaxVolume(streamType)
                    audioManager.setStreamVolume(streamType, maxVol, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume set to maximum."
                }
                else -> return "Unknown direction. Use up, down, mute, unmute, or max."
            }
        } catch (e: Exception) {
            return "Failed to adjust volume: ${e.message}"
        }
    }

    private fun setVolumePercent(percent: Int): String {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val streamType = android.media.AudioManager.STREAM_MUSIC
            val maxVol = audioManager.getStreamMaxVolume(streamType)
            val targetVol = (maxVol * percent.coerceIn(0, 100)) / 100
            audioManager.setStreamVolume(streamType, targetVol, android.media.AudioManager.FLAG_SHOW_UI)
            return "Volume set to $percent%."
        } catch (e: Exception) {
            return "Failed to set volume: ${e.message}"
        }
    }

    private fun toggleTorch(state: String): String {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return "No flashlight hardware available."
            val turnOn = state.lowercase() == "on"
            cameraManager.setTorchMode(cameraId, turnOn)
            return if (turnOn) "Flashlight turned ON." else "Flashlight turned OFF."
        } catch (e: Exception) {
            return "Failed to toggle flashlight: ${e.message}"
        }
    }

    private fun setBrightness(level: Int): String {
        try {
            if (!android.provider.Settings.System.canWrite(context)) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:" + context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return "Please grant Write Settings permission to adjust screen brightness."
            }

            val brightness = (level.coerceIn(0, 100) * 255) / 100
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
            return "Screen brightness set to $level%."
        } catch (e: Exception) {
            return "Failed to set brightness: ${e.message}"
        }
    }

    private fun openNotificationPanel(): String {
        val service = MayaAccessibilityService.instance
        return if (service != null && service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) {
            "Notification panel opened."
        } else {
            "Accessibility service not active."
        }
    }

    private fun getSimCardInfo(): String {
        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "READ_PHONE_STATE permission not granted. Defaulting to SIM 1."
        }
        return try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            val phoneAccounts = telecomManager.callCapablePhoneAccounts
            "The device has ${phoneAccounts.size} active calling SIM cards."
        } catch (e: Exception) {
            "Active SIM card info not readable: ${e.message}"
        }
    }

    private fun getBatteryStatus(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            "Battery level is at $level%."
        } catch (e: Exception) {
            "Battery status unavailable."
        }
    }

    private fun takeScreenshot(): String {
        val service = MayaAccessibilityService.instance
        return if (service != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            if (success) "Screenshot captured." else "Could not capture screenshot."
        } else {
            "Screenshot requires Accessibility service active."
        }
    }

    private fun lockScreen(): String {
        val service = MayaAccessibilityService.instance
        if (service != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            if (success) return "Screen locked successfully."
        }

        // Check if service is enabled in system settings and momentarily re-binding
        if (MayaAccessibilityService.isAccessibilityPermissionGranted(context)) {
            for (i in 0 until 5) {
                try { Thread.sleep(60) } catch (_: Exception) {}
                val inst = MayaAccessibilityService.instance
                if (inst != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    if (inst.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) {
                        return "Screen locked successfully."
                    }
                }
            }
        }

        // Accessibility service not granted yet: open settings directly so user can enable it with 1 tap
        MayaAccessibilityService.openAccessibilitySettings(context)
        return "Babu, phone lock karne ke liye Accessibility permission zaroori hai. Maine Settings open kar di hai, bas Maya service ko ON kar dijiye!"
    }

    private fun openRecentApps(): String {
        val service = MayaAccessibilityService.instance
        return if (service != null) {
            val success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
            if (success) "Recent apps opened." else "Could not open recent apps."
        } else {
            "Recent apps requires Accessibility service."
        }
    }

    private fun scrollScreen(direction: String): String {
        val service = MayaAccessibilityService.instance
        return if (service != null) {
            val root = service.rootInActiveWindow
            if (root != null) {
                val action = if (direction.equals("up", true))
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                else
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                val scrolled = root.performAction(action)
                if (scrolled) "Scrolled $direction." else "Could not scroll current window."
            } else "No active window to scroll."
        } else "Accessibility service not active."
    }

    private fun setAlarm(hour: Int, minute: Int, message: String): String {
        return try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Alarm set for %02d:%02d (%s)".format(hour, minute, message)
        } catch (e: Exception) {
            "Failed to set alarm: ${e.message}"
        }
    }

    private fun setTimer(seconds: Int, message: String): String {
        return try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Timer set for $seconds seconds."
        } catch (e: Exception) {
            "Failed to set timer: ${e.message}"
        }
    }

    private fun openCamera(mode: String): String {
        return try {
            val action = if (mode.equals("video", true))
                android.provider.MediaStore.INTENT_ACTION_VIDEO_CAMERA
            else
                android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (mode.equals("selfie", true)) {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                }
            }
            context.startActivity(intent)
            "Camera opened in $mode mode."
        } catch (e: Exception) {
            "Failed to open camera: ${e.message}"
        }
    }

    private fun openNavigation(destination: String): String {
        return try {
            val uri = Uri.parse("google.navigation:q=" + Uri.encode(destination))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination))
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            }
            "Navigation started for $destination."
        } catch (e: Exception) {
            "Failed to start navigation: ${e.message}"
        }
    }

    private fun controlMedia(action: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val keyEvent = when (action.lowercase()) {
            "next" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            "prev", "previous" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "pause" -> android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
            "play" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY
            else -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyEvent))
        audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyEvent))
        return "Media command '$action' sent."
    }
}
