package com.example.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ZoyaForegroundService
import com.example.live.ZoyaState
import kotlin.math.abs

/**
 * FloatingMayaOverlayManager displays an elegant, draggable floating Maya Orb
 * on the screen edge (like Google Assistant / Messenger Chat Head).
 * A single tap lets the user speak to Maya from ANY screen (Free Fire, Home, WhatsApp)
 * without requiring the microphone indicator to stay permanently open in the status bar!
 */
object FloatingMayaOverlayManager {
    private const val TAG = "FloatingMayaOverlay"
    private var windowManager: WindowManager? = null
    private var floatingView: MayaFloatingBubbleView? = null
    private var isShowing = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isOverlayPermitted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @Synchronized
    fun show(context: Context) {
        if (!isOverlayPermitted(context)) return
        if (isShowing && floatingView != null) return

        mainHandler.post {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager = wm

                val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val sizePx = (56 * context.resources.displayMetrics.density).toInt()
                val displayMetrics = context.resources.displayMetrics
                val initialX = displayMetrics.widthPixels - sizePx - 20
                val initialY = (displayMetrics.heightPixels * 0.45f).toInt()

                val params = WindowManager.LayoutParams(
                    sizePx,
                    sizePx,
                    layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = initialX
                    y = initialY
                }

                val bubbleView = MayaFloatingBubbleView(context, params, wm) {
                    // Tap action: Wake Maya up or trigger voice command immediately
                    Log.d(TAG, "Floating Maya bubble tapped! Activating Maya voice listening...")
                    val service = ZoyaForegroundService.activeService
                    if (service != null) {
                        service.wakeUp()
                        // Announce readiness and start active listening
                        val userName = com.example.settings.UserSettingsManager.getUserName(context)
                        val sweetGreeting = "हाँ $userName, बोलिए! मैं सुन रही हूँ ❤️"
                        com.example.live.MayaSpeaker.speak(sweetGreeting) {
                            ZoyaForegroundService.currentState = ZoyaState.LISTENING
                            ZoyaForegroundService.onStateChange?.invoke(ZoyaState.LISTENING)
                        }
                    } else {
                        // Launch app main screen if service isn't alive
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (launchIntent != null) context.startActivity(launchIntent)
                    }
                }

                wm.addView(bubbleView, params)
                floatingView = bubbleView
                isShowing = true
                Log.d(TAG, "Floating Maya assistant bubble added to screen successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show floating bubble: ${e.message}")
            }
        }
    }

    @Synchronized
    fun hide() {
        if (!isShowing || floatingView == null) return
        mainHandler.post {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating bubble: ${e.message}")
            } finally {
                floatingView = null
                isShowing = false
            }
        }
    }

    fun updateState(state: ZoyaState) {
        floatingView?.updateState(state)
    }

    @SuppressLint("ViewConstructor")
    private class MayaFloatingBubbleView(
        context: Context,
        private val params: WindowManager.LayoutParams,
        private val wm: WindowManager,
        private val onTap: () -> Unit
    ) : View(context) {

        private var avatarBitmap: Bitmap? = null
        private var pulseFraction = 0f
        private var currentState = ZoyaState.IDLE

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f * resources.displayMetrics.density
        }

        private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                pulseFraction = it.animatedValue as Float
                invalidate()
            }
        }

        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var initialX = 0
        private var initialY = 0
        private var isClick = false

        init {
            loadAvatar()
            pulseAnimator.start()
        }

        private fun loadAvatar() {
            try {
                val drawable = ContextCompat.getDrawable(context, R.drawable.maya_app_logo)
                    ?: ContextCompat.getDrawable(context, R.drawable.maya_hd_logo)
                if (drawable is BitmapDrawable) {
                    val raw = drawable.bitmap
                    val size = (52 * resources.displayMetrics.density).toInt()
                    val scaled = Bitmap.createScaledBitmap(raw, size, size, true)
                    avatarBitmap = getCircularBitmap(scaled)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading avatar bitmap: ${e.message}")
            }
        }

        private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val radius = bitmap.width / 2f
            canvas.drawCircle(radius, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }

        fun updateState(state: ZoyaState) {
            currentState = state
            postInvalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = (width / 2f) - (4f * resources.displayMetrics.density)

            // Dynamic glow color depending on state
            val glowColor = when (currentState) {
                ZoyaState.SPEAKING -> Color.parseColor("#FF007F") // Romantic pink glow
                ZoyaState.LISTENING -> Color.parseColor("#00E5FF") // Cyan listening glow
                ZoyaState.THINKING -> Color.parseColor("#FFD700") // Gold thinking glow
                ZoyaState.IDLE -> Color.parseColor("#7C4DFF")    // Deep violet idle glow
            }

            glowPaint.color = glowColor
            val glowAlpha = (140 + (pulseFraction * 115)).toInt().coerceIn(0, 255)
            glowPaint.alpha = glowAlpha

            // Draw glowing halo
            canvas.drawCircle(cx, cy, radius, glowPaint)

            // Draw avatar bitmap inside
            avatarBitmap?.let { bmp ->
                val left = (width - bmp.width) / 2f
                val top = (height - bmp.height) / 2f
                canvas.drawBitmap(bmp, left, top, null)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = (initialX + dx).toInt()
                    params.y = (initialY + dy).toInt()
                    wm.updateViewLayout(this, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        onTap()
                    } else {
                        // Snap smoothly to closest screen edge (left or right)
                        val displayWidth = resources.displayMetrics.widthPixels
                        val targetX = if (params.x + width / 2 < displayWidth / 2) 20 else displayWidth - width - 20
                        params.x = targetX
                        wm.updateViewLayout(this, params)
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            pulseAnimator.cancel()
        }
    }
}
