package com.example.aod

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.AodPreferencesRepository
import com.example.model.AodPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared silent screen-off detector used by both AodNotificationListenerService (zero notification,
 * persistent system binding) and AODAutoService without showing any ongoing notification.
 */
object AodScreenOffController {

    const val CHANNEL_TRIGGER_ID = "aod_trigger_channel"
    const val TRIGGER_NOTIFICATION_ID = 2002
    private const val PREFS_BRIGHTNESS_BACKUP = "aod_brightness_backup"
    private const val KEY_HAS_BACKUP = "has_backup"
    private const val KEY_SAVED_BRIGHTNESS = "saved_brightness"
    private const val KEY_SAVED_MODE = "saved_mode"

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    var isListening: Boolean = false
        private set

    @Volatile
    private var currentPrefs: AodPreferences = AodPreferences()

    @Volatile
    private var lastUnlockedAtMs: Long = 0L

    private var screenReceiver: BroadcastReceiver? = null
    private var prefsJob: Job? = null
    private var pendingLaunchRunnable: Runnable? = null
    private var cpuHoldWakeLock: PowerManager.WakeLock? = null

    fun notifyUnlocked() {
        lastUnlockedAtMs = System.currentTimeMillis()
        cancelPendingLaunch()
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (prefsJob == null) {
                val repo = AodPreferencesRepository(appContext)
                prefsJob = scope.launch {
                    repo.preferencesFlow.collect { prefs ->
                        currentPrefs = prefs
                    }
                }
            }

            if (screenReceiver != null) {
                isListening = true
                return
            }

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_SCREEN_OFF -> scheduleAodOnScreenOff(appContext)
                        Intent.ACTION_SCREEN_ON -> {
                            cancelPendingLaunch()
                            if (!AODDisplayActivity.isCurrentlyVisible) {
                                restoreSystemBrightnessIfNeeded(appContext)
                            }
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            notifyUnlocked()
                            restoreSystemBrightnessIfNeeded(appContext)
                            cancelTriggerNotification(appContext)
                        }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }

            screenReceiver = receiver
            isListening = true
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            isListening = false
            cancelPendingLaunch()
            screenReceiver?.let {
                try {
                    appContext.unregisterReceiver(it)
                } catch (_: Exception) {
                    // Already unregistered
                }
            }
            screenReceiver = null
            prefsJob?.cancel()
            prefsJob = null
            cancelTriggerNotification(appContext)
        }
    }

    private fun scheduleAodOnScreenOff(appContext: Context) {
        // Avoid re-opening AOD if the user pressed Power while AOD was already visible or just unlocked
        val now = System.currentTimeMillis()
        val recentlyDismissed = now - AODDisplayActivity.lastPausedAtMs < 1500L
        val recentlyUnlocked = now - lastUnlockedAtMs < 1200L
        if (AODDisplayActivity.isCurrentlyVisible || recentlyDismissed || recentlyUnlocked) {
            return
        }

        if (currentPrefs.autoAodOnlyCharging && !isDeviceCharging(appContext)) {
            return
        }

        cancelPendingLaunch()
        acquireCpuHoldWakeLock(appContext)

        val runnable = Runnable {
            try {
                val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm?.isInteractive == true || AODDisplayActivity.isCurrentlyVisible) {
                    return@Runnable
                }
                if (currentPrefs.autoAodOnlyCharging && !isDeviceCharging(appContext)) {
                    return@Runnable
                }
                launchAodOverLockScreen(appContext)
            } finally {
                releaseCpuHoldWakeLock()
            }
        }
        pendingLaunchRunnable = runnable
        // Wait 520ms so Samsung One UI completes its full screen-off fade and keyguard transition
        mainHandler.postDelayed(runnable, 520L)
    }

    private fun launchAodOverLockScreen(appContext: Context) {
        val launchIntent = Intent(appContext, AODDisplayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }

        val canDirectStart = Settings.canDrawOverlays(appContext) ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

        var launchedDirectly = false
        if (canDirectStart) {
            try {
                // Dim system brightness to minimum before waking the panel so StartingWindow never flashes bright
                dimSystemBrightnessBeforeWake(appContext)
                val options = ActivityOptions.makeCustomAnimation(appContext, 0, 0)
                appContext.startActivity(launchIntent, options.toBundle())
                launchedDirectly = true
            } catch (_: Exception) {
                restoreSystemBrightnessIfNeeded(appContext)
                launchedDirectly = false
            }
        }

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(TRIGGER_NOTIFICATION_ID)

        if (!launchedDirectly) {
            ensureTriggerChannel(nm)
            val canUseFsi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                nm?.canUseFullScreenIntent() ?: true
            } else {
                true
            }

            if (canUseFsi) {
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    appContext,
                    TRIGGER_NOTIFICATION_ID,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerNotification = NotificationCompat.Builder(appContext, CHANNEL_TRIGGER_ID)
                    .setSmallIcon(R.drawable.ic_tile_aod)
                    .setContentTitle("Always On Display")
                    .setContentText("Reloj AMOLED activo")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSilent(true)
                    .setAutoCancel(true)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .build()

                nm?.notify(TRIGGER_NOTIFICATION_ID, triggerNotification)
            }
        }
    }

    private fun ensureTriggerChannel(nm: NotificationManager?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            if (nm.getNotificationChannel(CHANNEL_TRIGGER_ID) == null) {
                val triggerChannel = NotificationChannel(
                    CHANNEL_TRIGGER_ID,
                    "Activación pantalla Always On",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Permite mostrar el reloj AMOLED automáticamente al apagar la pantalla"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(triggerChannel)
            }
        }
    }

    private fun cancelPendingLaunch() {
        pendingLaunchRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingLaunchRunnable = null
        releaseCpuHoldWakeLock()
    }

    private fun acquireCpuHoldWakeLock(appContext: Context) {
        releaseCpuHoldWakeLock()
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        cpuHoldWakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlwaysOn:ScreenOffWait"
        )?.apply {
            acquire(3000L)
        }
    }

    private fun releaseCpuHoldWakeLock() {
        cpuHoldWakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                } catch (_: Exception) {
                    // Ignored
                }
            }
        }
        cpuHoldWakeLock = null
    }

    private fun cancelTriggerNotification(appContext: Context) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(TRIGGER_NOTIFICATION_ID)
    }

    private fun isDeviceCharging(appContext: Context): Boolean {
        val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun dimSystemBrightnessBeforeWake(context: Context) {
        val appContext = context.applicationContext
        if (!Settings.System.canWrite(appContext)) return
        try {
            val prefs = appContext.getSharedPreferences(PREFS_BRIGHTNESS_BACKUP, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_HAS_BACKUP, false)) {
                val cr = appContext.contentResolver
                val currentMode = Settings.System.getInt(
                    cr,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                val currentBri = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128)
                prefs.edit()
                    .putBoolean(KEY_HAS_BACKUP, true)
                    .putInt(KEY_SAVED_BRIGHTNESS, currentBri)
                    .putInt(KEY_SAVED_MODE, currentMode)
                    .commit()

                if (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(
                        cr,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                }
            }
            Settings.System.putInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 1)
        } catch (_: Exception) {
            // Safe fallback on restricted OEM keys
        }
    }

    fun restoreSystemBrightnessIfNeeded(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_BRIGHTNESS_BACKUP, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_BACKUP, false)) return

        val savedBri = prefs.getInt(KEY_SAVED_BRIGHTNESS, 128)
        val savedMode = prefs.getInt(KEY_SAVED_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        prefs.edit().putBoolean(KEY_HAS_BACKUP, false).commit()

        if (!Settings.System.canWrite(appContext)) return
        try {
            val cr = appContext.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, savedBri)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, savedMode)
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
