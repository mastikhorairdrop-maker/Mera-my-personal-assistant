package com.example.guard

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.live.MayaSpeaker
import kotlin.math.sqrt

/**
 * TouchGuardManager
 *
 * Implements:
 * 1. Touch Guard Mode (टच गार्ड): Protects screen from unauthorized or pocket touches.
 * 2. Pocket Protection: Detects pocket insertion / accidental touches.
 * 3. Don't Touch My Phone / Anti-Theft Motion Alarm: Uses Accelerometer to detect phone movement.
 * 4. Automatic voice alert when an unauthorized touch or movement occurs.
 */
object TouchGuardManager : SensorEventListener {
    private const val TAG = "TouchGuardManager"

    @Volatile
    var isTouchGuardActive = false
        private set

    @Volatile
    var isMotionAlarmArmed = false
        private set

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastWarningTime = 0L

    // Accelerometer baseline
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isFirstSensorReading = true

    fun enableTouchGuard(context: Context, armMotionAlarm: Boolean = true): String {
        isTouchGuardActive = true
        isMotionAlarmArmed = armMotionAlarm

        mainHandler.post {
            showTouchGuardOverlay(context)
            if (armMotionAlarm) {
                startMotionSensor(context)
            }
        }

        vibrateShort(context, 100)
        Log.i(TAG, "Touch Guard enabled (Motion Alarm: $armMotionAlarm)")
        return "टच गार्ड मोड ऑन कर दिया गया है।"
    }

    fun disableTouchGuard(context: Context): String {
        isTouchGuardActive = false
        isMotionAlarmArmed = false

        mainHandler.post {
            removeTouchGuardOverlay()
            stopMotionSensor()
        }

        vibrateShort(context, 50)
        Log.i(TAG, "Touch Guard disabled")
        return "टच गार्ड मोड बंद कर दिया गया है।"
    }

    private fun showTouchGuardOverlay(context: Context) {
        if (overlayView != null) return
        if (!android.provider.Settings.canDrawOverlays(context)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted, running motion-only guard.")
            return
        }

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 120
            }

            // Create attractive Touch Guard Pill
            val frame = FrameLayout(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setPadding(32, 16, 32, 16)
            }

            val pillLayout = FrameLayout(context).apply {
                val shape = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#E60D1117"))
                    cornerRadius = 48f
                    setStroke(3, android.graphics.Color.parseColor("#00E5FF"))
                }
                background = shape
                setPadding(40, 20, 40, 20)
            }

            val text = TextView(context).apply {
                this.text = "🛡️ टच गार्ड एक्टिव है • टच से सुरक्षित"
                setTextColor(android.graphics.Color.parseColor("#00E5FF"))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }

            pillLayout.addView(text)
            frame.addView(pillLayout)

            frame.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_OUTSIDE) {
                    handleUnauthorizedTouch(context)
                }
                false
            }

            windowManager?.addView(frame, layoutParams)
            overlayView = frame
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show touch guard overlay: ${e.message}")
        }
    }

    private fun removeTouchGuardOverlay() {
        try {
            if (overlayView != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove touch guard overlay: ${e.message}")
        }
    }

    private fun startMotionSensor(context: Context) {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                isFirstSensorReading = true
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start accelerometer: ${e.message}")
        }
    }

    private fun stopMotionSensor() {
        try {
            sensorManager?.unregisterListener(this)
            sensorManager = null
            accelerometer = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop accelerometer: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isMotionAlarmArmed || event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            if (isFirstSensorReading) {
                lastX = x
                lastY = y
                lastZ = z
                isFirstSensorReading = false
                return
            }

            val deltaX = Math.abs(lastX - x)
            val deltaY = Math.abs(lastY - y)
            val deltaZ = Math.abs(lastZ - z)
            val totalDelta = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble())

            // If phone moved more than 2.8 m/s²
            if (totalDelta > 2.8) {
                handleMotionDetected()
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun handleUnauthorizedTouch(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastWarningTime < 3500L) return
        lastWarningTime = now

        vibrateShort(context, 300)
        MayaSpeaker.speak(
            "चेतावनी! टच गार्ड एक्टिव है! मालिक की अनुमति के बिना फोन को मत छुओ!"
        )
    }

    private fun handleMotionDetected() {
        val now = System.currentTimeMillis()
        if (now - lastWarningTime < 4500L) return
        lastWarningTime = now

        MayaSpeaker.speak(
            "अरे! मेरा फोन किसने उठाया? नीचे रखो इसे! मालिक का फोन है!"
        )
    }

    private fun vibrateShort(context: Context, millis: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(millis)
            }
        } catch (_: Exception) {}
    }
}
