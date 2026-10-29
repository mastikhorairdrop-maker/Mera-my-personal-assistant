package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MayaAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MayaAccessibility"
        var instance: MayaAccessibilityService? = null

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
        var targetInstagramUser: String? = null
        var targetInstagramMessage: String? = null

        // YouTube flags
        var shouldAutoClickYouTube = false

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
                Log.e(TAG, "Could not open accessibility settings", e)
            }
        }

        fun lockScreenDirectly(context: android.content.Context): Boolean {
            val inst = instance
            if (inst != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return try {
                    inst.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } catch (e: Exception) {
                    Log.e(TAG, "Error performing lock screen global action", e)
                    false
                }
            }
            return false
        }

        fun dispatchGestureClick(x: Float, y: Float): Boolean {
            val inst = instance ?: return false
            if (x <= 0f || y <= 0f) return false
            return try {
                val path = Path().apply {
                    moveTo(x, y)
                    lineTo(x, y)
                }
                val builder = GestureDescription.Builder()
                builder.addStroke(GestureDescription.StrokeDescription(path, 0, 80))
                inst.dispatchGesture(builder.build(), null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching gesture click", e)
                false
            }
        }

        fun clickTextOnScreen(text: String): Boolean {
            val inst = instance ?: return false
            val root = inst.rootInActiveWindow ?: return false
            return try {
                val nodes = root.findAccessibilityNodeInfosByText(text)
                for (node in nodes) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    if (!bounds.isEmpty) {
                        val x = bounds.centerX().toFloat()
                        val y = bounds.centerY().toFloat()
                        if (dispatchGestureClick(x, y)) return true
                    }
                    if (inst.performSafeClick(node)) return true
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error clicking text on screen", e)
                false
            }
        }

        fun startWhatsAppSendWatcher(message: String?) {
            shouldAutoClickWhatsApp = true
            targetWhatsAppMessage = message
            scheduleWhatsAppSendRetry(0)
            mainHandler.postDelayed({ shouldAutoClickWhatsApp = false }, 18000)
        }

        fun startWhatsAppSearchAndSend(contact: String, message: String) {
            shouldSearchAndSendWhatsApp = true
            targetWhatsAppContact = contact
            targetWhatsAppMessage = message
            scheduleWhatsAppSearchRetry(0)
            mainHandler.postDelayed({ shouldSearchAndSendWhatsApp = false }, 20000)
        }

        fun startGeminiPromptPaster(prompt: String) {
            shouldPasteGeminiPrompt = true
            targetGeminiPrompt = prompt
            scheduleGeminiPromptRetry(0)
            mainHandler.postDelayed({ shouldPasteGeminiPrompt = false }, 20000)
        }

        fun startInstagramSendWatcher(username: String, message: String) {
            shouldAutoClickInstagram = true
            targetInstagramUser = username
            targetInstagramMessage = message
            scheduleInstagramSendRetry(0)
            mainHandler.postDelayed({ shouldAutoClickInstagram = false }, 18000)
        }

        private fun scheduleWhatsAppSendRetry(attempt: Int) {
            if (!shouldAutoClickWhatsApp || attempt > 30) return
            mainHandler.postDelayed({
                val inst = instance
                if (inst != null && shouldAutoClickWhatsApp) {
                    try {
                        val root = inst.rootInActiveWindow
                        if (root != null) {
                            // 1. Ensure text is in the message entry box
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

                            // 2. Click Send Button
                            var clicked = inst.searchAndClickSendButton(root)
                            if (!clicked) {
                                val entryBoxes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
                                    .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/entry") }
                                if (entryBoxes.isNotEmpty()) {
                                    val bounds = Rect()
                                    entryBoxes[0].getBoundsInScreen(bounds)
                                    val sendX = inst.resources.displayMetrics.widthPixels * 0.92f
                                    val sendY = bounds.centerY().toFloat()
                                    if (sendX > 0 && sendY > 0) {
                                        clicked = dispatchGestureClick(sendX, sendY)
                                    }
                                }
                            }
                            if (clicked) {
                                Log.d(TAG, "WhatsApp send button clicked on retry attempt $attempt")
                                shouldAutoClickWhatsApp = false
                                return@postDelayed
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in scheduleWhatsAppSendRetry", e)
                    }
                    scheduleWhatsAppSendRetry(attempt + 1)
                }
            }, 250)
        }

        private fun scheduleWhatsAppSearchRetry(attempt: Int) {
            if (!shouldSearchAndSendWhatsApp || attempt > 30) return
            mainHandler.postDelayed({
                val inst = instance
                if (inst != null && shouldSearchAndSendWhatsApp) {
                    try {
                        val root = inst.rootInActiveWindow
                        val contact = targetWhatsAppContact
                        val message = targetWhatsAppMessage ?: ""

                        if (root != null && !contact.isNullOrBlank()) {
                            // Step 1: Check if contact is directly visible in active chats
                            val directNodes = root.findAccessibilityNodeInfosByText(contact)
                            val validChatNode = directNodes.firstOrNull { node ->
                                val text = node.text?.toString() ?: ""
                                val desc = node.contentDescription?.toString() ?: ""
                                text.contains(contact, ignoreCase = true) || desc.contains(contact, ignoreCase = true)
                            }

                            if (validChatNode != null) {
                                if (inst.performSafeClick(validChatNode)) {
                                    Log.d(TAG, "Clicked visible WhatsApp chat '$contact' directly!")
                                    shouldSearchAndSendWhatsApp = false
                                    startWhatsAppSendWatcher(message)
                                    return@postDelayed
                                }
                            }

                            // Step 2: Search input field
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

                                mainHandler.postDelayed({
                                    val activeRoot = inst.rootInActiveWindow ?: return@postDelayed
                                    var results = activeRoot.findAccessibilityNodeInfosByText(contact)
                                    if (results.isEmpty() && contact.contains(" ")) {
                                        val firstName = contact.split(" ").first()
                                        results = activeRoot.findAccessibilityNodeInfosByText(firstName)
                                    }
                                    for (resNode in results) {
                                        if (resNode != input && inst.performSafeClick(resNode)) {
                                            Log.d(TAG, "Clicked search result for '$contact'")
                                            shouldSearchAndSendWhatsApp = false
                                            startWhatsAppSendWatcher(message)
                                            return@postDelayed
                                        }
                                    }
                                    // Fallback: tap top item in WhatsApp search results list
                                    val firstResultY = (inst.resources.displayMetrics.density * 135f)
                                    val centerX = inst.resources.displayMetrics.widthPixels * 0.45f
                                    if (dispatchGestureClick(centerX, firstResultY)) {
                                        Log.d(TAG, "Dispatched physical click on top WhatsApp search result")
                                        shouldSearchAndSendWhatsApp = false
                                        startWhatsAppSendWatcher(message)
                                    }
                                }, 500)
                                return@postDelayed
                            } else {
                                // Step 3: Open Search
                                val searchButtons = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/menuitem_search")
                                    .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/search_holder") }
                                    .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/menuitem_search") }

                                if (searchButtons.isNotEmpty()) {
                                    inst.performSafeClick(searchButtons[0])
                                } else {
                                    val descNodes = root.findAccessibilityNodeInfosByText("Search")
                                        .ifEmpty { root.findAccessibilityNodeInfosByText("खोजें") }
                                    if (descNodes.isNotEmpty()) {
                                        inst.performSafeClick(descNodes[0])
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in scheduleWhatsAppSearchRetry", e)
                    }
                    scheduleWhatsAppSearchRetry(attempt + 1)
                }
            }, 300)
        }

        private fun scheduleGeminiPromptRetry(attempt: Int) {
            if (!shouldPasteGeminiPrompt || attempt > 30) return
            mainHandler.postDelayed({
                val inst = instance
                val prompt = targetGeminiPrompt
                if (inst != null && shouldPasteGeminiPrompt && !prompt.isNullOrBlank()) {
                    try {
                        val root = inst.rootInActiveWindow
                        if (root != null) {
                            val inputNodes = root.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/chat_input_text")
                                .ifEmpty { root.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/input_box") }
                                .ifEmpty { root.findAccessibilityNodeInfosByText("Ask Gemini") }
                                .ifEmpty { root.findAccessibilityNodeInfosByText("जेमिनी से पूछें") }

                            var textInputNode: AccessibilityNodeInfo? = inputNodes.firstOrNull()
                            if (textInputNode == null) {
                                textInputNode = findFirstEditableNode(root)
                            }

                            if (textInputNode != null) {
                                val args = Bundle().apply {
                                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, prompt)
                                }
                                textInputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                                textInputNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)

                                mainHandler.postDelayed({
                                    val activeRoot = inst.rootInActiveWindow ?: return@postDelayed
                                    val sendButtons = activeRoot.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/send_button")
                                        .ifEmpty { activeRoot.findAccessibilityNodeInfosByViewId("com.google.android.apps.bard:id/submit_button") }
                                        .ifEmpty { activeRoot.findAccessibilityNodeInfosByText("Send") }
                                        .ifEmpty { activeRoot.findAccessibilityNodeInfosByText("भेजें") }

                                    for (btn in sendButtons) {
                                        if (inst.performSafeClick(btn)) {
                                            Log.d(TAG, "Gemini Send button clicked!")
                                            shouldPasteGeminiPrompt = false
                                            return@postDelayed
                                        }
                                    }
                                    if (inst.searchAndClickSendButton(activeRoot)) {
                                        shouldPasteGeminiPrompt = false
                                    }
                                }, 350)
                                return@postDelayed
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in scheduleGeminiPromptRetry", e)
                    }
                    scheduleGeminiPromptRetry(attempt + 1)
                }
            }, 300)
        }

        private fun scheduleInstagramSendRetry(attempt: Int) {
            if (!shouldAutoClickInstagram || attempt > 30) return
            mainHandler.postDelayed({
                val inst = instance
                val message = targetInstagramMessage
                if (inst != null && shouldAutoClickInstagram) {
                    try {
                        val root = inst.rootInActiveWindow
                        if (root != null) {
                            if (!message.isNullOrBlank()) {
                                val editBoxes = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/row_thread_composer_edittext")
                                for (box in editBoxes) {
                                    val currentText = box.text?.toString() ?: ""
                                    if (currentText.isBlank()) {
                                        val args = Bundle().apply {
                                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                                        }
                                        box.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                                    }
                                }
                            }

                            val clicked = inst.searchAndClickInstagramSend(root)
                            if (clicked) {
                                Log.d(TAG, "Instagram send button clicked on retry $attempt")
                                shouldAutoClickInstagram = false
                                return@postDelayed
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in scheduleInstagramSendRetry", e)
                    }
                    scheduleInstagramSendRetry(attempt + 1)
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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        try {
            // Programmatically set serviceInfo to guarantee compatibility with ColorOS, OxygenOS, and stock Android
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                notificationTimeout = 50
            }
            serviceInfo = info
            Log.i(TAG, "Maya Automation Accessibility Service Connected and Configured successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onServiceConnected", e)
        }
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            val packageName = event.packageName?.toString() ?: ""

            // WhatsApp auto-click send
            if (shouldAutoClickWhatsApp && packageName.contains("whatsapp")) {
                val root = rootInActiveWindow
                if (root != null) {
                    val clicked = searchAndClickSendButton(root)
                    if (clicked) {
                        Log.d(TAG, "WhatsApp Send button clicked from event!")
                        shouldAutoClickWhatsApp = false
                    }
                }
            }

            // WhatsApp search & send flow
            if (shouldSearchAndSendWhatsApp && packageName.contains("whatsapp")) {
                scheduleWhatsAppSearchRetry(0)
            }

            // Gemini prompt flow
            if (shouldPasteGeminiPrompt && (packageName.contains("bard") || packageName.contains("gemini"))) {
                scheduleGeminiPromptRetry(0)
            }

            // Instagram auto-click send
            if (shouldAutoClickInstagram && packageName.contains("instagram")) {
                val root = rootInActiveWindow
                if (root != null) {
                    val clicked = searchAndClickInstagramSend(root)
                    if (clicked) {
                        Log.d(TAG, "Instagram send clicked successfully!")
                        shouldAutoClickInstagram = false
                    }
                }
            }
        } catch (e: Throwable) {
            // Never let any exception bubble up to Android OS to prevent "malfunctioning" state!
            Log.e(TAG, "Handled exception in onAccessibilityEvent", e)
        }
    }

    // --- WhatsApp Automation ---

    fun searchAndClickSendButton(node: AccessibilityNodeInfo): Boolean {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchAndClickSendButton", e)
            return false
        }
    }

    private fun recursiveSearchAndClickSend(node: AccessibilityNodeInfo): Boolean {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in recursiveSearchAndClickSend", e)
        }
        return false
    }

    // --- Instagram Automation ---

    fun searchAndClickInstagramSend(root: AccessibilityNodeInfo): Boolean {
        try {
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

            val descNodes = root.findAccessibilityNodeInfosByText("Send")
            for (node in descNodes) {
                if (performSafeClick(node)) return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchAndClickInstagramSend", e)
        }
        return false
    }

    fun performSafeClick(node: AccessibilityNodeInfo): Boolean {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in performSafeClick", e)
        }
        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "Maya Automation Accessibility Service Interrupted")
    }
}
