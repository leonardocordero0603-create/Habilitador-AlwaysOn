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
        isRunning = true
        AodScreenOffController.start(applicationContext)

        serviceScope.launch {
            SamsungAodManager(applicationContext).applyMaximumPerformanceOptimizations(
                enableAod = true,
                tapToShow = false
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            serviceScope.launch {
                AodPreferencesRepository(applicationContext).updateAutoAodOnScreenOff(false)
            }
            AodScreenOffController.stop(applicationContext)
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        AodScreenOffController.start(applicationContext)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (AodPreferencesRepository(applicationContext).getCachedAutoAodEnabled()) {
            try {
                applicationContext.startService(Intent(applicationContext, AODAutoService::class.java))
            } catch (_: Exception) {
                // System-bound NotificationListenerService keeps AodScreenOffController active silently
            }
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
