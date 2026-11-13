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
    }

    override fun onListenerDisconnected() {
        isConnected = false
        AodLiveStateHolder.stopMediaMonitoring()
        AodLiveStateHolder.clearAll()
        if (!AODAutoService.isRunning) {
            AodScreenOffController.stop(applicationContext)
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
        AodLiveStateHolder.refreshMediaSessions(applicationContext)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
        AodLiveStateHolder.refreshMediaSessions(applicationContext)
    }

    private fun refreshNotifications() {
        try {
            AodLiveStateHolder.updateNotifications(applicationContext, activeNotifications)
        } catch (_: Exception) {
            // Service may not yet be fully bound
        }
    }
}
