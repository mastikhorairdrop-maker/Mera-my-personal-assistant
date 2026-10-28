package com.example.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import com.example.live.ZoyaState

/**
 * EdgeLightOverlayManager displays the continuous multicolor/rainbow
 * edge lighting strip along the 4 borders of the device screen while
 * Maya is running in background or foreground.
 */
object EdgeLightOverlayManager {
    private const val TAG = "EdgeLightOverlay"
    private var windowManager: WindowManager? = null
    private var overlayView: EdgeLightView? = null
    private var isShowing = false

    enum class EdgeLightMode {
        SMART_ACTIVE, // Only lights up when speaking, listening, or thinking (saves screen & battery!)
        ALWAYS_ON,    // Continuous glow
        OFF           // Disabled
    }

    private var currentMode = EdgeLightMode.OFF
    private var isInitialized = false

    fun getMode(): EdgeLightMode = currentMode

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("edge_light_mode", "OFF") ?: "OFF"
        currentMode = when (saved) {
            "ALWAYS" -> EdgeLightMode.ALWAYS_ON
            "SMART" -> EdgeLightMode.SMART_ACTIVE
            else -> EdgeLightMode.OFF
        }
        isInitialized = true
        if (currentMode == EdgeLightMode.OFF) {
            hide()
        }
    }

    fun saveMode(context: Context, mode: EdgeLightMode) {
        currentMode = mode
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("edge_light_mode", when (mode) {
            EdgeLightMode.ALWAYS_ON -> "ALWAYS"
            EdgeLightMode.OFF -> "OFF"
            EdgeLightMode.SMART_ACTIVE -> "SMART"
        }).apply()

        if (mode == EdgeLightMode.OFF) {
            hide()
        } else {
            val state = com.example.ZoyaForegroundService.currentState
            updateState(state, context)
        }
    }

    fun isOverlayPermitted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @Synchronized
    fun show(context: Context, state: ZoyaState = ZoyaState.IDLE) {
        if (!isOverlayPermitted(context)) return

        init(context)

        // Battery saver: If OFF, don't show
        if (currentMode == EdgeLightMode.OFF) {
            hide()
            return
        }

        // Battery saver: In SMART_ACTIVE mode, do NOT show light during IDLE (standby)
        if (currentMode == EdgeLightMode.SMART_ACTIVE && state == ZoyaState.IDLE) {
            hide()
            return
        }

        if (isShowing && overlayView != null) {
            overlayView?.updateState(state)
            return
        }

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            val view = EdgeLightView(context).apply {
                updateState(state)
            }
            wm.addView(view, params)
            overlayView = view
            isShowing = true
            Log.d(TAG, "Multicolor Edge Light Overlay attached to screen")
        } catch (e: Exception) {
            Log.e(TAG, "Could not show edge light overlay: ${e.message}")
        }
    }

    @Synchronized
    fun updateState(state: ZoyaState, context: Context? = null) {
        if (currentMode == EdgeLightMode.OFF) {
            hide()
            return
        }

        if (currentMode == EdgeLightMode.SMART_ACTIVE) {
            if (state == ZoyaState.IDLE) {
                // Instantly turn off edge light when Maya is silent to save battery
                hide()
                return
            } else {
                if (!isShowing && context != null) {
                    show(context, state)
                    return
                }
            }
        }

        overlayView?.updateState(state)
    }

    @Synchronized
    fun hide() {
        if (!isShowing) return
        try {
            overlayView?.let { view ->
                view.stopAnimation()
                windowManager?.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing edge light overlay: ${e.message}")
        } finally {
            overlayView = null
            windowManager = null
            isShowing = false
            Log.d(TAG, "Edge light overlay removed")
        }
    }

    class EdgeLightView(context: Context) : View(context) {
        private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = Color.WHITE
        }

        private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val fullPath = Path()
        private val segmentPath = Path()
        private val pathMeasure = PathMeasure()
        private var animator: ValueAnimator? = null
        private var progress = 0f
        private var colorHue = 0f
        private val pos = FloatArray(2)
        private val tan = FloatArray(2)
        private var currentState = ZoyaState.IDLE

        init {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 3400L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val value = it.animatedValue as Float
                    progress = value
                    // Dynamic color morphing across all colors
                    colorHue = (colorHue + 0.8f) % 360f
                    invalidate()
                }
                start()
            }
        }

        fun updateState(state: ZoyaState) {
            currentState = state
            val isHighIntensity = state != ZoyaState.IDLE
            animator?.duration = if (isHighIntensity) 2200L else 3400L
            invalidate()
        }

        fun stopAnimation() {
            animator?.cancel()
            animator = null
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            fullPath.reset()
            val strokeHalf = 6f
            val cornerRadius = 72f // Gentle curved corners of modern phone display
            val rect = RectF(strokeHalf, strokeHalf, w.toFloat() - strokeHalf, h.toFloat() - strokeHalf)
            fullPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            pathMeasure.setPath(fullPath, false)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val totalLength = pathMeasure.length
            if (totalLength <= 0f) return

            val isHighIntensity = currentState != ZoyaState.IDLE

            // 1. Subtle perimeter boundary track that dynamically shifts hue
            val baseColor = Color.HSVToColor(if (isHighIntensity) 50 else 28, floatArrayOf(colorHue, 0.9f, 0.95f))
            basePaint.color = baseColor
            canvas.drawPath(fullPath, basePaint)

            // Multiple harmonized rainbow beams (Beam 1, Beam 2, Beam 3, Beam 4)
            val beamLength = totalLength * if (isHighIntensity) 0.28f else 0.22f

            // Beam 1: Primary Hue (Cyan -> Blue -> Violet)
            drawDynamicRainbowBeam(
                canvas = canvas,
                totalLength = totalLength,
                headOffset = progress,
                beamLength = beamLength,
                hueDegree = colorHue,
                isHighIntensity = isHighIntensity
            )

            // Beam 2: Secondary Hue (Magenta / Hot Pink / Red)
            drawDynamicRainbowBeam(
                canvas = canvas,
                totalLength = totalLength,
                headOffset = (progress + 0.25f) % 1f,
                beamLength = beamLength,
                hueDegree = (colorHue + 90f) % 360f,
                isHighIntensity = isHighIntensity
            )

            // Beam 3: Tertiary Hue (Gold / Amber / Coral)
            drawDynamicRainbowBeam(
                canvas = canvas,
                totalLength = totalLength,
                headOffset = (progress + 0.50f) % 1f,
                beamLength = beamLength,
                hueDegree = (colorHue + 180f) % 360f,
                isHighIntensity = isHighIntensity
            )

            // Beam 4: Quaternary Hue (Emerald Green / Aquamarine)
            drawDynamicRainbowBeam(
                canvas = canvas,
                totalLength = totalLength,
                headOffset = (progress + 0.75f) % 1f,
                beamLength = beamLength,
                hueDegree = (colorHue + 270f) % 360f,
                isHighIntensity = isHighIntensity
            )
        }

        private fun drawDynamicRainbowBeam(
            canvas: Canvas,
            totalLength: Float,
            headOffset: Float,
            beamLength: Float,
            hueDegree: Float,
            isHighIntensity: Boolean
        ) {
            val head = headOffset * totalLength
            val start = head - beamLength

            segmentPath.reset()
            if (start >= 0) {
                pathMeasure.getSegment(start, head, segmentPath, true)
            } else {
                pathMeasure.getSegment(totalLength + start, totalLength, segmentPath, true)
                pathMeasure.getSegment(0f, head, segmentPath, false)
            }

            val mainColor = Color.HSVToColor(255, floatArrayOf(hueDegree, 0.95f, 1f))
            val glowColor = Color.HSVToColor(if (isHighIntensity) 110 else 65, floatArrayOf(hueDegree, 0.95f, 1f))

            // Outer diffuse halo
            glowPaint.color = glowColor
            glowPaint.strokeWidth = if (isHighIntensity) 24f else 18f
            canvas.drawPath(segmentPath, glowPaint)

            // Vibrant beam body
            beamPaint.color = mainColor
            beamPaint.strokeWidth = if (isHighIntensity) 10f else 7.5f
            canvas.drawPath(segmentPath, beamPaint)

            // Intense pure white core on leading front 35% of the beam
            val coreLen = beamLength * 0.35f
            val coreStart = head - coreLen
            segmentPath.reset()
            if (coreStart >= 0) {
                pathMeasure.getSegment(coreStart, head, segmentPath, true)
            } else {
                pathMeasure.getSegment(totalLength + coreStart, totalLength, segmentPath, true)
                pathMeasure.getSegment(0f, head, segmentPath, false)
            }
            corePaint.strokeWidth = if (isHighIntensity) 4.5f else 3.5f
            canvas.drawPath(segmentPath, corePaint)

            // Glowing comet head spark
            if (pathMeasure.getPosTan(head, pos, tan)) {
                sparkPaint.color = glowColor
                canvas.drawCircle(pos[0], pos[1], if (isHighIntensity) 16f else 12f, sparkPaint)
                sparkPaint.color = Color.WHITE
                canvas.drawCircle(pos[0], pos[1], if (isHighIntensity) 7f else 5.5f, sparkPaint)
            }
        }
    }
}
