package com.example.aod

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.AodPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AodNotificationListenerService : NotificationListenerService() {

    companion object {
        @Volatile
        var isConnected: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            isConnected = true
            AodLiveStateHolder.startMediaMonitoring(applicationContext)
            scheduleRefresh()

            serviceScope.launch {
                try {
                    if (AodPreferencesRepository(applicationContext).getCachedAutoAodEnabled()) {
                        AodScreenOffController.start(applicationContext)
                        SamsungAodManager(applicationContext).applyMaximumPerformanceOptimizations(
                            enableAod = true,
                            tapToShow = false
                        )
                    }
                } catch (_: Throwable) {
                    // Safe fallback
                }
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    override fun onListenerDisconnected() {
        try {
            isConnected = false
            updateJob?.cancel()
            AodLiveStateHolder.stopMediaMonitoring()
            AodLiveStateHolder.clearAll()
            if (!AODAutoService.isRunning) {
                AodScreenOffController.stop(applicationContext)
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        updateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        scheduleRefresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        scheduleRefresh()
    }

    private fun scheduleRefresh() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            try {
                val snapshot = try {
                    activeNotifications
                } catch (_: Throwable) {
                    null
                }
                AodLiveStateHolder.updateNotifications(applicationContext, snapshot)
                AodLiveStateHolder.refreshMediaSessions(applicationContext)
            } catch (_: Throwable) {
                // Service may not yet be fully bound
            }
        }
    }
}
