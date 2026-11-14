package com.example.aod

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Short-lived lock-screen wake handover Activity used when the user presses the physical
 * Power button while Always On Display is active. Using Activity.setTurnScreenOn(true)
 * reliably wakes the hardware display across Android 8.1–16 and Samsung One UI, then exits
 * cleanly to reveal the user's normal lock screen.
 */
class AodWakeScreenActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var finished = false
    private val finishRunnable = Runnable { finishWakeHandover() }

    override fun onCreate(savedInstanceState: Bundle?) {
        disableSystemTransition()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        try {
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

        val blackView = View(this).apply {
            setBackgroundColor(AndroidColor.BLACK)
        }
        setContentView(blackView)

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(AodScreenOffController.TRIGGER_NOTIFICATION_ID)

        // Fallback finish in case focus callback is delayed on an OEM lock screen
        handler.postDelayed(finishRunnable, 480L)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !finished) {
            handler.removeCallbacks(finishRunnable)
            handler.postDelayed(finishRunnable, 90L)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(finishRunnable)
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        disableSystemTransition()
    }

    private fun finishWakeHandover() {
        if (finished) return
        finished = true
        handler.removeCallbacks(finishRunnable)

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

        finishAndRemoveTask()
        disableSystemTransition()
    }

    private fun disableSystemTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
