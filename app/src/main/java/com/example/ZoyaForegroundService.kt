package com.example

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.live.LiveSessionManager
import com.example.live.MayaWakeWordManager
import com.example.live.ZoyaState
import com.example.tools.ToolExecutionEngine
import com.example.ui.EdgeLightOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ServiceStatus {
    ACTIVE,
    SLEEPING,
    STOPPED
}

class ZoyaForegroundService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    lateinit var liveSessionManager: LiveSessionManager
    private lateinit var toolEngine: ToolExecutionEngine
    lateinit var fastIntentEngine: com.example.live.FastIntentEngine
    private var wakeWordManager: MayaWakeWordManager? = null

    // WakeLock to prevent the CPU from entering deep sleep and stopping background listening
    private var wakeLock: PowerManager.WakeLock? = null

    private var isRecording = false

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val outputSampleRate = 24000
    private val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "MAYA_FOREGROUND_CHANNEL"

        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_SLEEP = "com.example.ACTION_SLEEP"
        const val ACTION_WAKE = "com.example.ACTION_WAKE"

        var currentState: ZoyaState = ZoyaState.IDLE
            private set
        var onStateChange: ((ZoyaState) -> Unit)? = null

        var isSleeping: Boolean = false
            private set

        private val _serviceStatus = kotlinx.coroutines.flow.MutableStateFlow(ServiceStatus.ACTIVE)
        val serviceStatus: kotlinx.coroutines.flow.StateFlow<ServiceStatus> = _serviceStatus.asStateFlow()

        private val _messages = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
        val messages: kotlinx.coroutines.flow.StateFlow<List<String>> = _messages.asStateFlow()

        var activeService: ZoyaForegroundService? = null

        fun isBatteryOptimizationIgnored(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            activeService = this
            toolEngine = ToolExecutionEngine(this)

            fastIntentEngine = com.example.live.FastIntentEngine(
                context = this,
                toolEngine = toolEngine,
                onStateChange = { state ->
                    currentState = state
                    onStateChange?.invoke(state)
                },
                onMessageLog = { user, maya ->
                    liveSessionManager.addMessage(user)
                    liveSessionManager.addMessage(maya)
                }
            )

            // Acquire Partial WakeLock so background listening doesn't get throttled by Doze
            acquireWakeLock()

            val onAudioOut: (ByteArray) -> Unit = { audioData ->
                playAudio(audioData)
            }

            val onInterruptOut: () -> Unit = {
                try {
                    audioOutputQueue.clear()
                    if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack?.pause()
                        audioTrack?.flush()
                        audioTrack?.play()
                    }
                } catch (e: Exception) {
                    Log.e("MayaService", "Error flushing track on interrupt", e)
                }
            }

            liveSessionManager = LiveSessionManager(this, toolEngine, onAudioOut, onInterruptOut)

            createNotificationChannel()
            val initialNotification = buildNotification("Maya is listening for 'Ok Maya'...")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(NOTIFICATION_ID, initialNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } catch (e: Exception) {
                    try { startForeground(NOTIFICATION_ID, initialNotification) } catch (_: Exception) {}
                }
            } else {
                try { startForeground(NOTIFICATION_ID, initialNotification) } catch (_: Exception) {}
            }

            // Initialize EdgeLightOverlayManager with user preferences
            EdgeLightOverlayManager.init(applicationContext)
            if (EdgeLightOverlayManager.isOverlayPermitted(applicationContext)) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    EdgeLightOverlayManager.updateState(ZoyaState.IDLE, applicationContext)
                }
            }

            scope.launch {
                liveSessionManager.zoyaState.collect { state ->
                    currentState = state
                    onStateChange?.invoke(state)

                    if (!isSleeping) {
                        // Update persistent notification with current state
                        val stateText = when (state) {
                            ZoyaState.SPEAKING -> "बोल रही हूँ ❤️..."
                            ZoyaState.LISTENING -> "आपकी बात सुन रही हूँ..."
                            ZoyaState.THINKING -> "कमांड पूरा कर रही हूँ..."
                            ZoyaState.IDLE -> "Maya is listening for 'Ok Maya'..."
                        }
                        updateNotification(stateText)

                        // Edge lighting overlay along display borders (hidden if OFF to save battery)
                        if (EdgeLightOverlayManager.getMode() != EdgeLightOverlayManager.EdgeLightMode.OFF &&
                            EdgeLightOverlayManager.isOverlayPermitted(applicationContext)
                        ) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                EdgeLightOverlayManager.updateState(state, applicationContext)
                            }
                        } else {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                EdgeLightOverlayManager.hide()
                            }
                        }
                    }

                    // Coordinate wake word manager: only pause when Maya is speaking so she doesn't hear herself
                    if (state == ZoyaState.SPEAKING) {
                        wakeWordManager?.pauseListening()
                    } else {
                        wakeWordManager?.resumeListening()
                    }
                }
            }

            scope.launch {
                liveSessionManager.messages.collect { msgList ->
                    _messages.value = msgList
                }
            }

            initAudioTrack()

            // Setup Wake Word Manager for "Ok Maya" (Zero latency on-device STT & instant fast intents)
            initWakeWordManager()

            // Connect initial session
            liveSessionManager.startSession()
        } catch (e: Exception) {
            Log.e("MayaService", "Error in onCreate", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Maya::BackgroundListeningWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24 hours lock
            }
            Log.d("MayaService", "Acquired partial WakeLock for background service.")
        } catch (e: Exception) {
            Log.e("MayaService", "Failed to acquire WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("MayaService", "Released WakeLock.")
            }
        } catch (e: Exception) {
            Log.e("MayaService", "Error releasing WakeLock: ${e.message}")
        }
    }

    private fun initWakeWordManager() {
        val prefs = getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val wakeWordEnabled = prefs.getBoolean("handsfree_wake_word", true)

        if (wakeWordEnabled) {
            wakeWordManager = MayaWakeWordManager(this) { heardPhrase ->
                Log.i("MayaService", "Wake word recognized: '$heardPhrase'. Waking up Maya!")
                triggerHapticFeedback()

                val wasSleeping = isSleeping
                val cleanCommand = heardPhrase
                    .replace(Regex("(?i)^(ok|okay|hey|hello|hi|ओए|सुनो)?\\s*(maya|माया|zoya|जोया|joya)\\s*"), "")
                    .trim()

                // Check fast intent locally FIRST (< 15ms instant execution without network lag!)
                val handled = (cleanCommand.isNotEmpty() && fastIntentEngine.processFastIntent(cleanCommand)) ||
                              fastIntentEngine.processFastIntent(heardPhrase)
                if (handled) {
                    Log.i("MayaService", "Command executed INSTANTLY locally!")
                    return@MayaWakeWordManager
                }

                if (wasSleeping) {
                    wakeUp()
                } else if (currentState == ZoyaState.IDLE) {
                    liveSessionManager.startSession()
                }

                if (cleanCommand.isNotEmpty()) {
                    liveSessionManager.sendTextMessage(cleanCommand)
                } else if (wasSleeping) {
                    fastIntentEngine.processFastIntent("kaise ho")
                } else {
                    // Instant sweet response locally without server lag!
                    val userName = com.example.settings.UserSettingsManager.getUserName(this)
                    val sweetReply = "हाँ $userName बोलो, मैं सुन रही हूँ! ❤️"
                    liveSessionManager.addMessage("You: Ok Maya")
                    liveSessionManager.addMessage("Maya: $sweetReply")
                    currentState = ZoyaState.SPEAKING
                    onStateChange?.invoke(ZoyaState.SPEAKING)
                    com.example.live.MayaSpeaker.speak(sweetReply) {
                        currentState = ZoyaState.LISTENING
                        onStateChange?.invoke(ZoyaState.LISTENING)
                    }
                }
            }
            wakeWordManager?.startListening()
        }
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(100)
                }
            }
        } catch (_: Exception) {}
    }

    private val audioOutputQueue = java.util.concurrent.LinkedBlockingQueue<ByteArray>()
    private var isAudioPlaybackActive = false

    private fun startAudioPlaybackLoop() {
        isAudioPlaybackActive = true
        scope.launch(Dispatchers.IO) {
            while (isActive && isAudioPlaybackActive) {
                try {
                    val data = audioOutputQueue.poll(50, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (data != null) {
                        if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack?.play()
                        }
                        audioTrack?.write(data, 0, data.size)
                    }
                } catch (e: Exception) {
                    Log.e("MayaService", "Playback loop error", e)
                }
            }
        }
    }

    private fun initAudioTrack() {
        try {
            val minBuf = AudioTrack.getMinBufferSize(outputSampleRate, outChannelConfig, audioFormat)
            val finalBuf = if (minBuf > 0) minBuf * 4 else 8192

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(outputSampleRate)
                        .setChannelMask(outChannelConfig)
                        .build()
                )
                .setBufferSizeInBytes(finalBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            startAudioPlaybackLoop()
        } catch (e: Exception) {
            Log.e("MayaService", "Error initializing AudioTrack", e)
        }
    }

    private fun playAudio(data: ByteArray) {
        try {
            audioOutputQueue.offer(data)
        } catch (e: Exception) {
            Log.e("MayaService", "Error queueing audio", e)
        }
    }

    private fun startMicrophoneLoop() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("MayaService", "Missing RECORD_AUDIO permission")
            return
        }

        try {
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val finalBuf = if (minBuf > 0) minBuf * 4 else 8192

            val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                createAttributionContext("maya_audio")
            } else {
                this
            }

            audioRecord = AudioRecord.Builder()
                .setContext(ctx)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(finalBuf)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("MayaService", "AudioRecord initialization failed!")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            scope.launch(Dispatchers.IO) {
                val chunkSize = 1600
                val audioBuffer = ShortArray(chunkSize)
                while (isActive && isRecording) {
                    try {
                        val readResult = audioRecord?.read(audioBuffer, 0, chunkSize) ?: 0
                        if (readResult > 0) {
                            processAudio(audioBuffer, readResult)
                        }
                    } catch (e: Exception) {
                        Log.e("MayaService", "Error reading audio", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MayaService", "Error starting microphone", e)
        }
    }

    private fun processAudio(buffer: ShortArray, length: Int) {
        val state = liveSessionManager.zoyaState.value
        if (state != ZoyaState.IDLE) {
            liveSessionManager.sendAudioData(buffer, length)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP", ACTION_STOP -> {
                Log.i("MayaService", "Received STOP action. Stopping Maya completely.")
                stopMayaCompletely()
                return START_NOT_STICKY
            }
            ACTION_SLEEP -> {
                Log.i("MayaService", "Received SLEEP action. Putting Maya to sleep.")
                putToSleep()
                return START_STICKY
            }
            ACTION_WAKE -> {
                Log.i("MayaService", "Received WAKE action. Waking Maya up.")
                wakeUp()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun sendTextMessage(text: String) {
        val handled = fastIntentEngine.processFastIntent(text)
        if (handled) {
            Log.i("MayaService", "Command '$text' executed INSTANTLY locally via sendTextMessage!")
            return
        }
        if (isSleeping) {
            wakeUp()
        }
        liveSessionManager.sendTextMessage(text)
    }

    fun reconnectSession() {
        if (isSleeping) {
            wakeUp()
        } else {
            liveSessionManager.startSession()
        }
    }

    fun putToSleep() {
        if (isSleeping) return
        isSleeping = true
        _serviceStatus.value = ServiceStatus.SLEEPING
        try {
            // Disconnect heavy audio socket to save data & battery
            liveSessionManager.stopSession()
            // Stop mic loop to save battery
            isRecording = false
            try { audioRecord?.stop() } catch (_: Exception) {}
            // Stop and hide edge lights immediately to protect screen & battery
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                EdgeLightOverlayManager.hide()
            }
            // Pause microphone listener so the green dot disappears and battery is 100% saved
            wakeWordManager?.pauseListening()
            triggerHapticFeedback()
            updateNotification("😴 Maya सो रही है (स्लीप मोड) | ऐप में टैप करके या 'जगाएं' बटन दबाएं")
            Log.i("MayaService", "Maya is now asleep. Mic released, green dot off, battery saved.")
        } catch (e: Exception) {
            Log.e("MayaService", "Error putting Maya to sleep", e)
        }
    }

    fun wakeUp() {
        if (!isSleeping && _serviceStatus.value == ServiceStatus.ACTIVE) return
        isSleeping = false
        _serviceStatus.value = ServiceStatus.ACTIVE
        try {
            triggerHapticFeedback()
            // Restart mic loop and live session
            initAudioTrack()
            startMicrophoneLoop()
            liveSessionManager.startSession()
            updateNotification("Maya is listening for 'Ok Maya'...")
            Log.i("MayaService", "Maya has AWOKEN from sleep mode!")
        } catch (e: Exception) {
            Log.e("MayaService", "Error waking up Maya", e)
        }
    }

    fun stopMayaCompletely() {
        try {
            val prefs = getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("maya_explicitly_stopped", true).apply()
            _serviceStatus.value = ServiceStatus.STOPPED
            releaseWakeLock()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                EdgeLightOverlayManager.hide()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            Log.i("MayaService", "Maya stopped completely by user.")
        } catch (e: Exception) {
            Log.e("MayaService", "Error stopping service completely: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val prefs = getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val explicitlyStopped = prefs.getBoolean("maya_explicitly_stopped", false)
        if (explicitlyStopped) {
            Log.i("MayaService", "Maya was explicitly stopped. Will not restart onTaskRemoved.")
            return
        }

        Log.i("MayaService", "App task swiped away from recent apps. Keeping Maya alive in background!")

        // Schedule immediate restart alarm only if not explicitly stopped
        try {
            val restartServiceIntent = Intent(applicationContext, ZoyaForegroundService::class.java).apply {
                setPackage(packageName)
            }
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                101,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(
                AlarmManager.RTC,
                System.currentTimeMillis() + 1000,
                restartPendingIntent
            )
        } catch (e: Exception) {
            Log.e("MayaService", "Error scheduling restart alarm onTaskRemoved", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeService = null
        isRecording = false
        isAudioPlaybackActive = false
        currentState = ZoyaState.IDLE
        onStateChange?.invoke(currentState)
        audioOutputQueue.clear()

        releaseWakeLock()

        try { wakeWordManager?.destroy() } catch (_: Exception) {}
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        try { audioTrack?.stop() } catch (_: Exception) {}
        try { audioTrack?.release() } catch (_: Exception) {}
        try { liveSessionManager.stopSession() } catch (_: Exception) {}
        try { EdgeLightOverlayManager.hide() } catch (_: Exception) {}
        job.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Maya Assistant Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Maya voice assistant running in the background and listening for 'Ok Maya'"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Stop service completely
        val stopIntent = Intent(this, ZoyaForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Sleep
        val sleepIntent = Intent(this, ZoyaForegroundService::class.java).apply {
            action = ACTION_SLEEP
        }
        val sleepPendingIntent = PendingIntent.getService(
            this,
            2,
            sleepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Wake
        val wakeIntent = Intent(this, ZoyaForegroundService::class.java).apply {
            action = ACTION_WAKE
        }
        val wakePendingIntent = PendingIntent.getService(
            this,
            3,
            wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSleeping) "Maya (स्लीप मोड 😴 - बैटरी सेवर)" else "Maya AI Assistant ❤️"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (isSleeping) {
            builder.addAction(android.R.drawable.ic_lock_power_off, "⚡ जगाएं", wakePendingIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "🛑 बंद करें", stopPendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_pause, "😴 सुलाएं", sleepPendingIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "🛑 बंद करें", stopPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification(statusText: String) {
        val notification = buildNotification(statusText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
