package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ZoyaAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ZoyaAccessibilityService? = null

        // WhatsApp flags
        var shouldAutoClickWhatsApp = false
        var targetWhatsAppMessage: String? = null

        var shouldSearchAndSendWhatsApp = false
        var targetWhatsAppContact: String? = null

        // Gemini flags
        var shouldPasteGeminiPrompt = false
        var targetGeminiPrompt: String? = null

        // Instagram flags
        var shouldAutoClickInstagram = false
            set(value) {
                field = value
                if (value) {
                    mainHandler.postDelayed({ field = false }, 12000)
                    scheduleInstagramSendRetry(0)
                }
            }
        var targetInstagramUser: String? = null
        var targetInstagramMessage: String? = null

        // YouTube flags
        var shouldAutoClickYouTube = false
            set(value) {
                field = value
                if (value) {
                    mainHandler.postDelayed({ field = false }, 12000)
                    scheduleYouTubeClickRetry(0)
                }
            }

        private val mainHandler = Handler(Looper.getMainLooper())

        fun isServiceRunning(): Boolean = instance != null

        fun isAccessibilityPermissionGranted(context: android.content.Context): Boolean {
            if (instance != null) return true
            return try {
                val enabledServices = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
                val expectedPkg = context.packageName
                enabledServices.contains(expectedPkg)
            } catch (e: Exception) {
                false
            }
        }

        fun openAccessibilitySettings(context: android.content.Context) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("MayaAccessibility", "Could not open accessibility settings", e)
            }
        }

        fun lockScreenDirectly(context: android.content.Context): Boolean {
            val inst = instance
            if (inst != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val locked = inst.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                if (locked) return true
            }
            return false
        }

        fun dispatchGestureClick(x: Float, y: Float): Boolean {
            val inst = instance ?: return false
            if (x <= 0f || y <= 0f) return false
            val path = Path()
            path.moveTo(x, y)
            path.lineTo(x, y)

            val builder = GestureDescription.Builder()
            builder.addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            val gesture = builder.build()

            return inst.dispatchGesture(gesture, null, null)
        }

        fun clickTextOnScreen(text: String): Boolean {
            val inst = instance ?: return false
            val root = inst.rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                var current: AccessibilityNodeInfo? = node
                while (current != null) {
                    val bounds = Rect()
                    current.getBoundsInScreen(bounds)
                    if (!bounds.isEmpty && (current.isClickable || current == node)) {
                        val x = bounds.centerX().toFloat()
                        val y = bounds.centerY().toFloat()
                        if (dispatchGestureClick(x, y)) return true
                    }
                    current = current.parent
                }
            }
            return false
        }

        fun startWhatsAppSendWatcher(message: String?) {
            shouldAutoClickWhatsApp = true
            targetWhatsAppMessage = message
            scheduleWhatsAppSendRetry(0)
            mainHandler.postDelayed({ shouldAutoClickWhatsApp = false }, 15000)
        }

        fun startWhatsAppSearchAndSend(contact: String, message: String) {
            shouldSearchAndSendWhatsApp = true
            targetWhatsAppContact = contact
            targetWhatsAppMessage = message
            scheduleWhatsAppSearchRetry(0)
            mainHandler.postDelayed({ shouldSearchAndSendWhatsApp = false }, 18000)
        }

        fun startGeminiPromptPaster(prompt: String) {
            shouldPasteGeminiPrompt = true
            targetGeminiPrompt = prompt
            scheduleGeminiPromptRetry(0)
            mainHandler.postDelayed({ shouldPasteGeminiPrompt = false }, 15000)
        }

        private fun scheduleWhatsAppSendRetry(attempt: Int) {
            if (!shouldAutoClickWhatsApp || attempt > 25) return
            mainHandler.postDelayed({
                val inst = instance
                if (inst != null && shouldAutoClickWhatsApp) {
                    val root = inst.rootInActiveWindow
                    if (root != null) {
                        // Check if text needs to be inserted into compose entry
                        val msg = targetWhatsAppMessage
                        if (!msg.isNullOrBlank()) {
                            val entryBoxes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
                                .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/entry") }
                            for (box in entryBoxes) {
                                val currentText = box.text?.toString() ?: ""
                                if (currentText.isBlank()) {
                                    val args = Bundle().apply {
                                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, msg)
                                    }
                                    box.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                                }
                            }
                        }

                        val clicked = inst.searchAndClickSendButton(root)
                        if (clicked) {
                            Log.d("MayaAccessibility", "WhatsApp send button clicked on retry attempt $attempt")
                            shouldAutoClickWhatsApp = false
                            return@postDelayed
                        }
                    }
                    scheduleWhatsAppSendRetry(attempt + 1)
                }
            }, 300)
        }

        private fun scheduleWhatsAppSearchRetry(attempt: Int) {
            if (!shouldSearchAndSendWhatsApp || attempt > 25) return
            mainHandler.postDelayed({
                val inst = instance
                if (inst != null && shouldSearchAndSendWhatsApp) {
                    val root = inst.rootInActiveWindow
                    val contact = targetWhatsAppContact
                    val message = targetWhatsAppMessage ?: ""

                    if (root != null && !contact.isNullOrBlank()) {
                        // Step 1: Check if contact is directly visible on screen (e.g. recent chat list)
                        val directNodes = root.findAccessibilityNodeInfosByText(contact)
                        val validChatNode = directNodes.firstOrNull { node ->
                            val text = node.text?.toString() ?: ""
                            val desc = node.contentDescription?.toString() ?: ""
                            text.contains(contact, ignoreCase = true) || desc.contains(contact, ignoreCase = true)
                        }

                        if (validChatNode != null) {
                            if (inst.performSafeClick(validChatNode)) {
                                Log.d("MayaAccessibility", "Clicked visible WhatsApp chat '$contact' directly!")
                                shouldSearchAndSendWhatsApp = false
                                startWhatsAppSendWatcher(message)
                                return@postDelayed
                            }
                        }

                        // Step 2: Look for search input already open
                        val searchInputs = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/search_input")
                            .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/search_src_text") }
                            .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/search_input") }

                        if (searchInputs.isNotEmpty()) {
                            val input = searchInputs[0]
                            val currentText = input.text?.toString() ?: ""
                            if (!currentText.equals(contact, ignoreCase = true)) {
                                val args = Bundle().apply {
                                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, contact)
                                }
                                input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                            }

                            // Check filtered results after search text set
                            mainHandler.postDelayed({
                                val activeRoot = inst.rootInActiveWindow ?: return@postDelayed
                                val results = activeRoot.findAccessibilityNodeInfosByText(contact)
                                for (resNode in results) {
                                    if (resNode != input && inst.performSafeClick(resNode)) {
                                        Log.d("MayaAccessibility", "Clicked search result for '$contact'")
                                        shouldSearchAndSendWhatsApp = false
                                        startWhatsAppSendWatcher(message)
                                        return@postDelayed
                                    }
                                }
                            }, 500)
                            return@postDelayed
                        } else {
                            // Step 3: Click Search icon to open search input
                            val searchButtons = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/menuitem_search")
                                .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/search_holder") }
                                .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/menuitem_search") }

                            if (searchButtons.isNotEmpty()) {
                                inst.performSafeClick(searchButtons[0])
                            } else {
                                // Search by content description
                                val descNodes = root.findAccessibilityNodeInfosByText("Search")
                                    .ifEmpty { root.findAccessibilityNodeInfosByText("खोजें") }
                                if (descNodes.isNotEmpty()) {
                                    inst.performSafeClick(descNodes[0])
                                }
                            }
                        }
                    }
                    scheduleWhatsAppSearchRetry(attempt + 1)
                }
            }, 350)
        }

        private fun scheduleGeminiPromptRetry(attempt: Int) {
            if (!shouldPasteGeminiPrompt || attempt > 25) return
            mainHandler.postDelayed({
                val inst = instance
                val prompt = targetGeminiPrompt
                if (inst != null && shouldPasteGeminiPrompt && !prompt.isNullOrBlank()) {
                    val root = inst.rootInActiveWindow
                    if (root != null) {
                        // Find Gemini chat input field
                        val inputNodes = root.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/chat_input_text")
                            .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/input_box") }
                            .ifEmpty { root.findAccessibilityNodeInfosByText("Ask Gemini") }
                            .ifEmpty { root.findAccessibilityNodeInfosByText("जेमिनी से पूछें") }

                        var textInputNode: AccessibilityNodeInfo? = inputNodes.firstOrNull()
                        if (textInputNode == null) {
                            // Find any editable node in window
                            textInputNode = findFirstEditableNode(root)
                        }

                        if (textInputNode != null) {
                            val args = Bundle().apply {
                                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, prompt)
                            }
                            textInputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                            textInputNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)

                            // Click Send button
                            mainHandler.postDelayed({
                                val activeRoot = inst.rootInActiveWindow ?: return@postDelayed
                                val sendButtons = activeRoot.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/send_button")
                                    .ifEmpty { activeRoot.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/submit_button") }
                                    .ifEmpty { activeRoot.findAccessibilityNodeInfosByText("Send") }
                                    .ifEmpty { activeRoot.findAccessibilityNodeInfosByText("भेजें") }

                                for (btn in sendButtons) {
                                    if (inst.performSafeClick(btn)) {
                                        Log.d("MayaAccessibility", "Gemini Send prompt button clicked!")
                                        shouldPasteGeminiPrompt = false
                                        return@postDelayed
                                    }
                                }
                                // Recursive fallback to find send icon
                                if (inst.searchAndClickSendButton(activeRoot)) {
                                    shouldPasteGeminiPrompt = false
                                }
                            }, 400)
                            return@postDelayed
                        }
                    }
                    scheduleGeminiPromptRetry(attempt + 1)
                }
            }, 350)
        }

        private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isEditable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findFirstEditableNode(child)
                if (found != null) return found
            }
            return null
        }

        private fun scheduleInstagramSendRetry(attempt: Int) {
            if (!shouldAutoClickInstagram || attempt > 15) return
            mainHandler.postDelayed({
                val inst = instance
                if (inst != null && shouldAutoClickInstagram) {
                    val root = inst.rootInActiveWindow
                    if (root != null) {
                        val clicked = inst.searchAndClickInstagramSend(root)
                        if (clicked) {
                            Log.d("MayaAccessibility", "Instagram send button clicked on retry $attempt")
                            shouldAutoClickInstagram = false
                            return@postDelayed
                        }
                    }
                    scheduleInstagramSendRetry(attempt + 1)
                }
            }, 400)
        }

        private fun scheduleYouTubeClickRetry(attempt: Int) {
            if (!shouldAutoClickYouTube || attempt > 15) return
            mainHandler.postDelayed({
                val inst = instance
                if (inst != null && shouldAutoClickYouTube) {
                    val root = inst.rootInActiveWindow
                    if (root != null) {
                        val clicked = inst.searchAndClickYouTubeFirstVideo(root)
                        if (clicked) {
                            Log.d("MayaAccessibility", "YouTube first video clicked on retry attempt $attempt")
                            shouldAutoClickYouTube = false
                            return@postDelayed
                        }
                    }
                    scheduleYouTubeClickRetry(attempt + 1)
                }
            }, 400)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("MayaAccessibility", "Maya Automation Accessibility Service Connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: ""

        // WhatsApp auto-click send
        if (shouldAutoClickWhatsApp && packageName.contains("whatsapp")) {
            val root = rootInActiveWindow ?: return
            val clicked = searchAndClickSendButton(root)
            if (clicked) {
                Log.d("MayaAccessibility", "WhatsApp Send button clicked from event!")
                shouldAutoClickWhatsApp = false
            }
        }

        // WhatsApp search & send flow
        if (shouldSearchAndSendWhatsApp && packageName.contains("whatsapp")) {
            handleWhatsAppSearchFlow()
        }

        // Gemini prompt flow
        if (shouldPasteGeminiPrompt && (packageName.contains("bard") || packageName.contains("gemini"))) {
            scheduleGeminiPromptRetry(0)
        }

        // Instagram auto-click send
        if (shouldAutoClickInstagram && packageName.contains("instagram")) {
            val root = rootInActiveWindow ?: return
            val clicked = searchAndClickInstagramSend(root)
            if (clicked) {
                Log.d("MayaAccessibility", "Instagram send clicked successfully!")
                shouldAutoClickInstagram = false
            }
        }

        // YouTube auto-click first video
        if (shouldAutoClickYouTube && packageName.contains("youtube")) {
            val root = rootInActiveWindow ?: return
            val clicked = searchAndClickYouTubeFirstVideo(root)
            if (clicked) {
                Log.d("MayaAccessibility", "YouTube video clicked from event!")
                shouldAutoClickYouTube = false
            }
        }
    }

    // --- WhatsApp Automation ---

    fun searchAndClickSendButton(node: AccessibilityNodeInfo): Boolean {
        val idsToTry = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/entry_send_button",
            "com.whatsapp:id/send_button",
            "com.whatsapp:id/voice_note_or_send_button",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp.w4b:id/entry_send_button",
            "com.whatsapp:id/send_button_container",
            "com.whatsapp:id/btn_send"
        )
        for (id in idsToTry) {
            val sendButtons = node.findAccessibilityNodeInfosByViewId(id)
            for (button in sendButtons) {
                if (performSafeClick(button)) {
                    return true
                }
            }
        }
        return recursiveSearchAndClickSend(node)
    }

    private fun recursiveSearchAndClickSend(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        if (desc == "send" || desc == "send message" || desc == "भेजें" ||
            desc.contains("bheje") || desc.contains("sandesh") || desc == "envio" || text == "send" || text == "भेजें"
        ) {
            if (performSafeClick(node)) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (recursiveSearchAndClickSend(child)) return true
            }
        }
        return false
    }

    private fun handleWhatsAppSearchFlow() {
        scheduleWhatsAppSearchRetry(0)
    }

    // --- Instagram Automation ---

    fun searchAndClickInstagramSend(root: AccessibilityNodeInfo): Boolean {
        val sendIds = listOf(
            "com.instagram.android:id/row_thread_composer_button_send",
            "com.instagram.android:id/action_bar_button_action",
            "com.instagram.android:id/button_send"
        )
        for (id in sendIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (performSafeClick(node)) return true
            }
        }

        // Recursive search for Send description / text in Instagram
        val descNodes = root.findAccessibilityNodeInfosByText("Send")
        for (node in descNodes) {
            if (performSafeClick(node)) return true
        }
        return false
    }

    // --- YouTube Automation ---

    fun searchAndClickYouTubeFirstVideo(root: AccessibilityNodeInfo): Boolean {
        val videoIds = listOf(
            "com.google.android.youtube:id/thumbnail",
            "com.google.android.youtube:id/video_info_view",
            "com.google.android.youtube:id/title",
            "com.google.android.youtube:id/compact_renderer"
        )
        for (id in videoIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                if (!bounds.isEmpty && bounds.centerY() > 220) {
                    if (performSafeClick(node)) return true
                }
            }
        }
        return recursiveSearchYouTubeVideo(root)
    }

    private fun recursiveSearchYouTubeVideo(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (bounds.centerY() > 240 && bounds.height() > 80 && bounds.width() > 200) {
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val isVideo = desc.contains("views") || desc.contains("video") ||
                    desc.contains("minute") || desc.contains("ago") || desc.contains("official")

            if (isVideo || node.isClickable) {
                if (performSafeClick(node)) return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (recursiveSearchYouTubeVideo(child)) return true
            }
        }
        return false
    }

    private fun performSafeClick(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty && bounds.centerX() > 0 && bounds.centerY() > 0) {
            val x = bounds.centerX().toFloat()
            val y = bounds.centerY().toFloat()
            if (dispatchGestureClick(x, y)) return true
        }

        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            parent = parent.parent
        }
        return false
    }

    override fun onInterrupt() {
        Log.d("MayaAccessibility", "Maya Automation Accessibility Service Interrupted")
    }
}
