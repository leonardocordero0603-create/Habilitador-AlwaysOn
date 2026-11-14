package com.example.aod

import android.app.ActivityOptions
import android.app.KeyguardManager
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
 * Shared silent screen-off & charging detector used by both AodNotificationListenerService
 * (zero notification, persistent system binding) and AODAutoService without ongoing notifications.
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
    private var cachedAutoEnabled: Boolean? = null

    @Volatile
    private var cachedOnlyCharging: Boolean? = null

    @Volatile
    private var lastUnlockedAtMs: Long = 0L

    @Volatile
    private var lastWokeUpFromAodAtMs: Long = 0L

    @Volatile
    private var lastScreenOnAtMs: Long = 0L

    @Volatile
    private var lastScreenOffAtMs: Long = 0L

    @Volatile
    private var lastPowerConnectedAtMs: Long = 0L

    @Volatile
    private var pendingLaunchIsPowerConnect: Boolean = false

    private var screenReceiver: BroadcastReceiver? = null
    private var prefsJob: Job? = null
    private var pendingLaunchRunnable: Runnable? = null
    private var wakeLockRunnable: Runnable? = null
    private var cpuHoldWakeLock: PowerManager.WakeLock? = null

    fun setOnlyChargingMode(enabled: Boolean) {
        cachedOnlyCharging = enabled
        currentPrefs = currentPrefs.copy(autoAodOnlyCharging = enabled)
    }

    fun notifyUnlocked() {
        lastUnlockedAtMs = System.currentTimeMillis()
        cancelPendingLaunch()
    }

    fun wakeLockScreenFromAod(context: Context) {
        val appContext = context.applicationContext
        lastWokeUpFromAodAtMs = System.currentTimeMillis()
        cancelPendingLaunch()
        cancelTriggerNotification(appContext)
        restoreSystemBrightnessIfNeeded(appContext)
        acquireCpuHoldWakeLock(appContext)

        wakeLockRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            try {
                val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm == null || !pm.isInteractive) {
                    // Legacy wakeLock nudge + modern setTurnScreenOn Activity handover
                    if (pm != null) {
                        @Suppress("DEPRECATION")
                        val wakeLock = pm.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                                PowerManager.ON_AFTER_RELEASE,
                            "AlwaysOn:WakeToLockScreen"
                        ).apply {
                            setReferenceCounted(false)
                        }
                        try {
                            wakeLock.acquire(800L)
                            mainHandler.postDelayed({
                                try {
                                    if (wakeLock.isHeld) wakeLock.release()
                                } catch (_: Exception) {
                                    // Ignored
                                }
                            }, 350L)
                        } catch (_: Exception) {
                            // Ignored if restricted
                        }
                    }
                    launchAodWakeHandover(appContext)
                }
            } finally {
                releaseCpuHoldWakeLock()
            }
        }
        wakeLockRunnable = runnable
        // Wait ~380ms so Android / One UI finishes its power-button goToSleep transition cleanly
        mainHandler.postDelayed(runnable, 380L)
    }

    private fun launchAodWakeHandover(appContext: Context) {
        val wakeIntent = Intent(appContext, AodWakeScreenActivity::class.java).apply {
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
                val options = ActivityOptions.makeCustomAnimation(appContext, 0, 0)
                appContext.startActivity(wakeIntent, options.toBundle())
                launchedDirectly = true
            } catch (_: Exception) {
                launchedDirectly = false
            }
        }

        if (!launchedDirectly) {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ensureTriggerChannel(nm)
            val canUseFsi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                nm?.canUseFullScreenIntent() ?: true
            } else {
                true
            }
            if (canUseFsi) {
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    appContext,
                    TRIGGER_NOTIFICATION_ID + 1,
                    wakeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val wakeNotification = NotificationCompat.Builder(appContext, CHANNEL_TRIGGER_ID)
                    .setSmallIcon(R.drawable.ic_tile_aod)
                    .setContentTitle("Always On Display")
                    .setContentText("Despertando pantalla")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSilent(true)
                    .setAutoCancel(true)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .build()
                try {
                    nm?.notify(TRIGGER_NOTIFICATION_ID, wakeNotification)
                } catch (_: Throwable) {
                    // Safe fallback
                }
            }
        }
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        val repo = AodPreferencesRepository(appContext)
        if (cachedAutoEnabled == null) {
            cachedAutoEnabled = repo.getCachedAutoAodEnabled()
        }
        if (cachedOnlyCharging == null) {
            cachedOnlyCharging = repo.getCachedAutoAodOnlyCharging()
        }
        synchronized(lock) {
            if (prefsJob == null) {
                prefsJob = scope.launch(Dispatchers.IO) {
                    try {
                        repo.preferencesFlow.collect { prefs ->
                            currentPrefs = prefs
                            cachedAutoEnabled = prefs.autoAodOnScreenOff
                            cachedOnlyCharging = prefs.autoAodOnlyCharging
                        }
                    } catch (_: Throwable) {
                        // Safe fallback
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
                        Intent.ACTION_SCREEN_OFF -> {
                            lastScreenOffAtMs = System.currentTimeMillis()
                            scheduleAodOnScreenOff(appContext)
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            val now = System.currentTimeMillis()
                            lastScreenOnAtMs = now
                            // On Samsung / One UI, plugging a charger while screen-off briefly wakes the
                            // locked screen to show the charging pill; don't abort a charger launch for that.
                            val isChargerWakeWhileLocked = pendingLaunchIsPowerConnect &&
                                (now - lastPowerConnectedAtMs < 3000L) &&
                                isKeyguardLocked(appContext)
                            if (!isChargerWakeWhileLocked) {
                                cancelPendingLaunch()
                            }
                            if (!AODDisplayActivity.isCurrentlyVisible) {
                                restoreSystemBrightnessIfNeeded(appContext)
                            }
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            notifyUnlocked()
                            restoreSystemBrightnessIfNeeded(appContext)
                            cancelTriggerNotification(appContext)
                        }
                        Intent.ACTION_POWER_CONNECTED -> {
                            lastPowerConnectedAtMs = System.currentTimeMillis()
                            scheduleAodOnPowerConnected(appContext)
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            if (pendingLaunchIsPowerConnect) {
                                cancelPendingLaunch()
                            }
                        }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } catch (_: SecurityException) {
                        @Suppress("UnspecifiedRegisterReceiverFlag")
                        appContext.registerReceiver(receiver, filter)
                    }
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    appContext.registerReceiver(receiver, filter)
                }
                screenReceiver = receiver
                isListening = true
            } catch (_: Throwable) {
                isListening = false
            }
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            isListening = false
            cancelPendingLaunch()
            wakeLockRunnable?.let { mainHandler.removeCallbacks(it) }
            wakeLockRunnable = null
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

    private fun isAutoAodEnabled(appContext: Context): Boolean {
        return cachedAutoEnabled
            ?: AodPreferencesRepository(appContext).getCachedAutoAodEnabled().also {
                cachedAutoEnabled = it
            }
    }

    private fun isOnlyChargingMode(appContext: Context): Boolean {
        return cachedOnlyCharging
            ?: AodPreferencesRepository(appContext).getCachedAutoAodOnlyCharging().also {
                cachedOnlyCharging = it
            }
    }

    private fun isKeyguardLocked(appContext: Context): Boolean {
        val km = appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked == true
    }

    private fun scheduleAodOnScreenOff(appContext: Context) {
        // Avoid re-opening AOD if the user just woke the screen from AOD or recently unlocked
        val now = System.currentTimeMillis()
        val recentlyDismissed = now - AODDisplayActivity.lastPausedAtMs < 1100L
        val recentlyWokeFromAod = now - lastWokeUpFromAodAtMs < 1200L
        val recentlyUnlocked = now - lastUnlockedAtMs < 1000L
        if (AODDisplayActivity.isCurrentlyVisible ||
            recentlyDismissed ||
            recentlyWokeFromAod ||
            recentlyUnlocked
        ) {
            return
        }

        val onlyCharging = isOnlyChargingMode(appContext)
        if (onlyCharging && !isDeviceCharging(appContext)) {
            return
        }

        cancelPendingLaunch()
        pendingLaunchIsPowerConnect = false
        acquireCpuHoldWakeLock(appContext)

        val runnable = Runnable {
            try {
                val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm?.isInteractive == true || AODDisplayActivity.isCurrentlyVisible) {
                    return@Runnable
                }
                val stillOnlyCharging = isOnlyChargingMode(appContext)
                if (stillOnlyCharging && !isDeviceCharging(appContext)) {
                    return@Runnable
                }
                launchAodOverLockScreen(appContext, startedAutoCharging = stillOnlyCharging)
            } finally {
                releaseCpuHoldWakeLock()
            }
        }
        pendingLaunchRunnable = runnable
        // Wait 450ms so One UI / Android finishes locking the Keyguard cleanly
        mainHandler.postDelayed(runnable, 450L)
    }

    private fun scheduleAodOnPowerConnected(appContext: Context) {
        val now = System.currentTimeMillis()
        if (!isAutoAodEnabled(appContext) || !isOnlyChargingMode(appContext)) {
            return
        }
        if (AODDisplayActivity.isCurrentlyVisible || now - lastUnlockedAtMs < 1500L) {
            return
        }

        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = pm?.isInteractive == true
        val isLocked = isKeyguardLocked(appContext)
        val screenOffOrJustWokeForCharger = !isInteractive ||
            lastScreenOffAtMs >= lastScreenOnAtMs ||
            (now - lastScreenOnAtMs < 3500L)

        // Only auto-activate when screen is off or Samsung's charging indicator just woke a locked screen
        if (isInteractive && !(isLocked && screenOffOrJustWokeForCharger)) {
            return
        }

        cancelPendingLaunch()
        pendingLaunchIsPowerConnect = true
        acquireCpuHoldWakeLock(appContext)

        val runnable = Runnable {
            try {
                if (!isAutoAodEnabled(appContext) || !isOnlyChargingMode(appContext)) {
                    return@Runnable
                }
                if (AODDisplayActivity.isCurrentlyVisible || !isDeviceCharging(appContext)) {
                    return@Runnable
                }
                val currentPm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val interactiveNow = currentPm?.isInteractive == true
                val lockedNow = isKeyguardLocked(appContext)
                if (interactiveNow && !lockedNow) {
                    return@Runnable
                }
                val canDirectStart = Settings.canDrawOverlays(appContext) ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                if (interactiveNow && !canDirectStart) {
                    return@Runnable
                }
                launchAodOverLockScreen(appContext, startedAutoCharging = true)
            } finally {
                pendingLaunchIsPowerConnect = false
                releaseCpuHoldWakeLock()
            }
        }
        pendingLaunchRunnable = runnable
        mainHandler.postDelayed(runnable, 950L)
    }

    private fun launchAodOverLockScreen(appContext: Context, startedAutoCharging: Boolean) {
        val launchIntent = Intent(appContext, AODDisplayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
            putExtra(AODDisplayActivity.EXTRA_STARTED_AUTO_CHARGING, startedAutoCharging)
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

                try {
                    nm?.notify(TRIGGER_NOTIFICATION_ID, triggerNotification)
                } catch (_: Throwable) {
                    // Safe fallback
                }
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
        pendingLaunchIsPowerConnect = false
        releaseCpuHoldWakeLock()
    }

    private fun acquireCpuHoldWakeLock(appContext: Context) {
        releaseCpuHoldWakeLock()
        try {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            cpuHoldWakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AlwaysOn:ScreenOffWait"
            )?.apply {
                acquire(3000L)
            }
        } catch (_: Throwable) {
            cpuHoldWakeLock = null
        }
    }

    private fun releaseCpuHoldWakeLock() {
        cpuHoldWakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                } catch (_: Throwable) {
                    // Ignored
                }
            }
        }
        cpuHoldWakeLock = null
    }

    private fun cancelTriggerNotification(appContext: Context) {
        try {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(TRIGGER_NOTIFICATION_ID)
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    fun isDeviceCharging(appContext: Context): Boolean {
        return try {
            val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            plugged > 0 ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Throwable) {
            false
        }
    }

    fun dimSystemBrightnessBeforeWake(context: Context) {
        // Window brightness in AODDisplayActivity already controls OLED panel brightness safely
        // without modifying or corrupting the user's Android system brightness settings.
    }

    fun restoreSystemBrightnessIfNeeded(context: Context) {
        val appContext = context.applicationContext
        scope.launch(Dispatchers.IO) {
            try {
                val prefs = appContext.getSharedPreferences(PREFS_BRIGHTNESS_BACKUP, Context.MODE_PRIVATE)
                if (!prefs.getBoolean(KEY_HAS_BACKUP, false)) return@launch

                val savedBri = prefs.getInt(KEY_SAVED_BRIGHTNESS, 128).coerceAtLeast(30)
                val savedMode = prefs.getInt(KEY_SAVED_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                prefs.edit().putBoolean(KEY_HAS_BACKUP, false).apply()

                if (!Settings.System.canWrite(appContext)) return@launch
                val cr = appContext.contentResolver
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, savedBri)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, savedMode)
            } catch (_: Throwable) {
                // Safe fallback
            }
        }
    }
}
