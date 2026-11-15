package com.example.aod

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color as AndroidColor
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Translucent lock-screen wake handover Activity used when the user presses the physical
 * Power button while Always On Display is active.
 *
 * Uses a lightweight native black veil View (no Compose startup overhead) on top of a
 * translucent window so the system Lock Screen is illuminated smoothly underneath as the
 * display powers on, avoiding any long black gap or abrupt snap.
 */
class AodWakeScreenActivity : ComponentActivity() {

    companion object {
        private const val MAX_INTERACTIVE_WAIT_MS = 180L
        private const val INTERACTIVE_POLL_MS = 16L
        private const val LOCKSCREEN_SURFACE_SETTLE_MS = 35L
        private const val REVEAL_DURATION_MS = 260L
        private const val HARD_FINISH_TIMEOUT_MS = 620L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var veilView: View? = null
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

        val view = View(this).apply {
            setBackgroundColor(AndroidColor.BLACK)
            alpha = 1f
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

    private fun scheduleSynchronizedReveal(view: View) {
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

    private fun waitForInteractiveAndReveal(view: View) {
        if (finished || revealStarted) return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = pm?.isInteractive == true
        val elapsed = SystemClock.elapsedRealtime() - createdElapsedMs
        if (isInteractive || elapsed >= MAX_INTERACTIVE_WAIT_MS) {
            revealStarted = true
            handler.postDelayed(
                { startVeilFadeOut(view) },
                LOCKSCREEN_SURFACE_SETTLE_MS
            )
        } else {
            handler.postDelayed(
                { waitForInteractiveAndReveal(view) },
                INTERACTIVE_POLL_MS
            )
        }
    }

    private fun startVeilFadeOut(view: View) {
        if (finished) return
        view.animate()
            .alpha(0f)
            .setDuration(REVEAL_DURATION_MS)
            .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f))
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishWakeHandover()
                }
            })
            .start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !finished) {
            hideSystemBars()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        veilView?.let { v ->
            v.animate().cancel()
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
        veilView?.animate()?.cancel()

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
