package com.example.aod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AodPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AodBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = AodPreferencesRepository(appContext)
                val prefs = repo.preferencesFlow.first()
                if (prefs.autoAodOnScreenOff) {
                    val manager = SamsungAodManager(appContext)
                    manager.applyMaximumPerformanceOptimizations(
                        enableAod = true,
                        tapToShow = false
                    )
                    manager.startAutoAodService()
                }
            } catch (_: Exception) {
                // Safely ignore on OEMs restricting background FGS boot start until next app open
            } finally {
                pendingResult.finish()
            }
        }
    }
}
