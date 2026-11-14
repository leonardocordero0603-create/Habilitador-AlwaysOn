package com.example.aod

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.AodPreferencesRepository

class AodNotificationListenerService : NotificationListenerService() {

    companion object {
        @Volatile
        var isConnected: Boolean = false
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            isConnected = true
            AodLiveStateHolder.startMediaMonitoring(applicationContext)
            refreshNotifications()

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

    override fun onListenerDisconnected() {
        try {
            isConnected = false
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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            refreshNotifications()
            AodLiveStateHolder.refreshMediaSessions(applicationContext)
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        try {
            refreshNotifications()
            AodLiveStateHolder.refreshMediaSessions(applicationContext)
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    private fun refreshNotifications() {
        try {
            AodLiveStateHolder.updateNotifications(applicationContext, activeNotifications)
        } catch (_: Throwable) {
            // Service may not yet be fully bound
        }
    }
}
