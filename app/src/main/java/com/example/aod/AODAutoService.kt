package com.example.aod

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.data.AodPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Silent background service (without foreground notification) that delegates screen-off detection
 * to AodScreenOffController. When NotificationListenerService is enabled, Android binds that
 * system listener persistently with zero notifications as well.
 */
class AODAutoService : Service() {

    companion object {
        const val TRIGGER_NOTIFICATION_ID = AodScreenOffController.TRIGGER_NOTIFICATION_ID
        const val ACTION_STOP_SERVICE = "com.example.aod.ACTION_STOP_AUTO_SERVICE"

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        try {
            isRunning = true
            AodScreenOffController.start(applicationContext)

            serviceScope.launch {
                try {
                    SamsungAodManager(applicationContext).applyMaximumPerformanceOptimizations(
                        enableAod = true,
                        tapToShow = false
                    )
                } catch (_: Throwable) {
                    // Safe fallback
                }
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent?.action == ACTION_STOP_SERVICE) {
                serviceScope.launch {
                    try {
                        AodPreferencesRepository(applicationContext).updateAutoAodOnScreenOff(false)
                    } catch (_: Throwable) {
                        // Safe fallback
                    }
                }
                AodScreenOffController.stop(applicationContext)
                stopSelf()
                return START_NOT_STICKY
            }

            isRunning = true
            AodScreenOffController.start(applicationContext)
        } catch (_: Throwable) {
            // Safe fallback
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            if (AodPreferencesRepository(applicationContext).getCachedAutoAodEnabled()) {
                try {
                    applicationContext.startService(Intent(applicationContext, AODAutoService::class.java))
                } catch (_: Throwable) {
                    // System-bound NotificationListenerService keeps AodScreenOffController active silently
                }
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    override fun onDestroy() {
        isRunning = false
        if (!AodNotificationListenerService.isConnected ||
            !AodPreferencesRepository(applicationContext).getCachedAutoAodEnabled()
        ) {
            AodScreenOffController.stop(applicationContext)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
