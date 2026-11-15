package com.example.aod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Wakeup receiver used by AlarmManager when the device's auto-lock timeout
 * ("lock_screen_lock_after_timeout") exceeds the short CPU-polling window.
 */
class AodLockCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val sessionId = intent?.getLongExtra(AodScreenOffController.EXTRA_LOCK_WAIT_SESSION_ID, -1L) ?: -1L
        if (sessionId <= 0L) return
        AodScreenOffController.onLockWaitAlarm(context.applicationContext, sessionId)
    }
}
