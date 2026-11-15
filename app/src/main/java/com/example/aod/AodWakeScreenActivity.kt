package com.example.aod

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.NotificationManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.AodPreferencesRepository
import com.example.model.AodTransitionStyle
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Translucent lock-screen wake handover Activity used when the user presses the physical
 * Power button while Always On Display is active.
 *
 * Renders an instant pure-black edge-to-edge veil (<16ms, zero Compose overhead), waits until
 * the display power rail is interactive and the Keyguard surface has composed its first frame,
 * and then reveals the system Lock Screen using an OLED S-curve choreography matched to the
 * user's selected AOD transition style and accent color.
 */
class AodWakeScreenActivity : ComponentActivity() {

    companion object {
        private const val MAX_INTERACTIVE_WAIT_MS = 220L
        private const val INTERACTIVE_POLL_MS = 16L
        private const val LOCKSCREEN_SURFACE_SETTLE_MS = 55L
        private const val REVEAL_DURATION_MS = 320L
        private const val HARD_FINISH_TIMEOUT_MS = 760L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var veilView: WakeVeilView? = null
    private var revealAnimator: ValueAnimator? = null
    private var preDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    private var createdElapsedMs: Long = 0L
    private var revealStarted: Boolean = false
    private var finished: Boolean = false
    private val hardTimeoutRunnable = Runnable { finishWakeHandover() }

    override fun onCreate(savedInstanceState: Bundle?) {
        disableSystemTransition()
        super.onCreate(savedInstanceState)
        createdElapsedMs = SystemClock.elapsedRealtime()

        try {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            )
        } catch (_: Throwable) {
            // Safe fallback
        }

        try {
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            val lp = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            window.attributes = lp

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Throwable) {
            // Safe fallback
        }

        val repo = AodPreferencesRepository(applicationContext)
        val transitionStyle = repo.getCachedTransitionStyle()
        val accentArgb = repo.getCachedAccentColor().color.toArgb()

        val view = WakeVeilView(
            context = this,
            transitionStyle = transitionStyle,
            accentColorArgb = accentArgb
        ).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        veilView = view
        setContentView(view)
        hideSystemBars()

        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(AodScreenOffController.TRIGGER_NOTIFICATION_ID)
            nm?.cancel(AodScreenOffController.TRIGGER_NOTIFICATION_ID + 1)
        } catch (_: Throwable) {
            // Safe fallback
        }

        handler.postDelayed(hardTimeoutRunnable, HARD_FINISH_TIMEOUT_MS)
        scheduleSynchronizedReveal(view)
    }

    private fun scheduleSynchronizedReveal(view: WakeVeilView) {
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (view.viewTreeObserver.isAlive) {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                }
                preDrawListener = null
                waitForInteractiveAndReveal(view)
                return true
            }
        }
        preDrawListener = listener
        view.viewTreeObserver.addOnPreDrawListener(listener)
    }

    private fun waitForInteractiveAndReveal(view: WakeVeilView) {
        if (finished || revealStarted) return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = pm?.isInteractive == true
        val elapsed = SystemClock.elapsedRealtime() - createdElapsedMs
        if (isInteractive || elapsed >= MAX_INTERACTIVE_WAIT_MS) {
            revealStarted = true
            handler.postDelayed(
                { startVeilReveal(view) },
                LOCKSCREEN_SURFACE_SETTLE_MS
            )
        } else {
            handler.postDelayed(
                { waitForInteractiveAndReveal(view) },
                INTERACTIVE_POLL_MS
            )
        }
    }

    private fun startVeilReveal(view: WakeVeilView) {
        if (finished) return
        revealAnimator?.cancel()
        var wasCancelled = false
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = REVEAL_DURATION_MS
            interpolator = PathInterpolator(0.38f, 0.0f, 0.16f, 1.0f)
            addUpdateListener { anim ->
                view.setProgress(anim.animatedValue as Float)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    wasCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!wasCancelled) {
                        finishWakeHandover()
                    }
                }
            })
        }
        revealAnimator = animator
        animator.start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !finished) {
            hideSystemBars()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        revealAnimator?.cancel()
        revealAnimator = null
        veilView?.let { v ->
            preDrawListener?.let { l ->
                if (v.viewTreeObserver.isAlive) {
                    v.viewTreeObserver.removeOnPreDrawListener(l)
                }
            }
        }
        preDrawListener = null
        veilView = null
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        disableSystemTransition()
    }

    private fun finishWakeHandover() {
        if (finished) return
        finished = true
        handler.removeCallbacksAndMessages(null)
        revealAnimator?.cancel()
        revealAnimator = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false)
                setTurnScreenOn(false)
            } else {
                @Suppress("DEPRECATION")
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Throwable) {
            // Safe fallback
        }

        finishAndRemoveTask()
        disableSystemTransition()
    }

    private fun hideSystemBars() {
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    private fun disableSystemTransition() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }
}

private class WakeVeilView(
    context: Context,
    private val transitionStyle: AodTransitionStyle,
    private val accentColorArgb: Int
) : View(context) {

    private var progress: Float = 0f
    private val veilPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
    }
    private val irisPath = Path().apply {
        fillType = Path.FillType.EVEN_ODD
    }
    private val stops2 = floatArrayOf(0f, 1f)
    private val stops3 = floatArrayOf(0f, 0.55f, 1f)

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val p = progress
        if (p <= 0.001f) {
            canvas.drawColor(AndroidColor.BLACK)
            return
        }
        if (p >= 0.999f) {
            return
        }

        when (transitionStyle) {
            AodTransitionStyle.ONE_UI_ZOOM,
            AodTransitionStyle.WONDERLAND_HALO -> drawRadialBloomVeil(canvas, w, h, p)
            AodTransitionStyle.IRIS_PORTAL -> drawIrisVeil(canvas, w, h, p)
            AodTransitionStyle.CURTAIN_SWEEP -> drawCurtainVeil(canvas, w, h, p)
            AodTransitionStyle.SOFT_FADE -> {
                veilPaint.shader = null
                veilPaint.color = AndroidColor.argb(((1f - p) * 255f).toInt().coerceIn(0, 255), 0, 0, 0)
                canvas.drawRect(0f, 0f, w, h, veilPaint)
            }
        }
    }

    private fun drawRadialBloomVeil(canvas: Canvas, w: Float, h: Float, p: Float) {
        val cx = w * 0.5f
        val cy = h * 0.42f
        val maxR = hypot(w, h) * 0.72f

        val centerAlpha = ((1f - p * 1.35f).coerceIn(0f, 1f) * 255f).toInt()
        val midAlpha = ((1f - p * 1.10f).coerceIn(0f, 1f) * 255f).toInt()
        val edgeAlpha = ((1f - p).coerceIn(0f, 1f) * 255f).toInt()

        veilPaint.shader = RadialGradient(
            cx,
            cy,
            maxR.coerceAtLeast(1f),
            intArrayOf(
                AndroidColor.argb(centerAlpha, 0, 0, 0),
                AndroidColor.argb(midAlpha, 0, 0, 0),
                AndroidColor.argb(edgeAlpha, 0, 0, 0)
            ),
            stops3,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, veilPaint)

        // Subtle OLED accent light halo peaking gently mid-bloom
        val wave = sin(p * Math.PI).toFloat().coerceIn(0f, 1f)
        val glowAlpha = (wave * 0.14f * 255f).toInt().coerceIn(0, 255)
        if (glowAlpha > 1) {
            val r = (maxR * (0.18f + 0.72f * p)).coerceAtLeast(1f)
            val accentRgb = accentColorArgb and 0x00FFFFFF
            glowPaint.shader = RadialGradient(
                cx,
                cy,
                r,
                intArrayOf(
                    (glowAlpha shl 24) or accentRgb,
                    AndroidColor.TRANSPARENT
                ),
                stops2,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, glowPaint)
        }
    }

    private fun drawIrisVeil(canvas: Canvas, w: Float, h: Float, p: Float) {
        val cx = w * 0.5f
        val cy = h * 0.46f
        val maxR = hypot(w * 0.5f, h * 0.55f)
        val radius = (maxR * p).coerceAtLeast(1f)
        val fadeEdge = ((1f - p * 0.85f).coerceIn(0f, 1f) * 255f).toInt()

        irisPath.reset()
        irisPath.addRect(0f, 0f, w, h, Path.Direction.CW)
        irisPath.addCircle(cx, cy, radius, Path.Direction.CCW)
        veilPaint.shader = null
        veilPaint.color = AndroidColor.argb(fadeEdge, 0, 0, 0)
        canvas.drawPath(irisPath, veilPaint)

        val ringAlpha = (sin(p * Math.PI).toFloat().coerceIn(0f, 1f) * 0.36f * 255f).toInt()
        if (ringAlpha > 1) {
            ringPaint.color = (ringAlpha shl 24) or (accentColorArgb and 0x00FFFFFF)
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }
    }

    private fun drawCurtainVeil(canvas: Canvas, w: Float, h: Float, p: Float) {
        val feather = 140f * resources.displayMetrics.density
        val sweepY = (h + feather) * p
        val topClear = (sweepY - feather).coerceAtLeast(0f)

        val topAlpha = ((1f - p * 1.25f).coerceIn(0f, 1f) * 255f).toInt()
        val bottomAlpha = ((1f - p * 0.92f).coerceIn(0f, 1f) * 255f).toInt()

        veilPaint.shader = LinearGradient(
            0f,
            topClear,
            0f,
            sweepY.coerceAtLeast(topClear + 1f),
            intArrayOf(
                AndroidColor.argb(topAlpha, 0, 0, 0),
                AndroidColor.argb(bottomAlpha, 0, 0, 0)
            ),
            stops2,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, veilPaint)

        val lineAlpha = (sin(p * Math.PI).toFloat().coerceIn(0f, 1f) * 0.42f * 255f).toInt()
        if (lineAlpha > 1 && sweepY in 0f..h) {
            ringPaint.color = (lineAlpha shl 24) or (accentColorArgb and 0x00FFFFFF)
            canvas.drawLine(0f, sweepY, w, sweepY, ringPaint)
        }
    }
}
