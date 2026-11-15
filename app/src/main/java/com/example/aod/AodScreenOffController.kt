package com.example.aod

import android.app.ActivityOptions
import android.app.AlarmManager
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
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.AodPreferencesRepository
import com.example.model.AodPreferences
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared silent screen-off & charging detector used by both AodNotificationListenerService
 * (zero notification, persistent system binding) and AODAutoService without ongoing notifications.
 *
 * Crucial behavior: When the screen turns off automatically due to screen timeout, Android does not
 * lock the Keyguard immediately; it waits for Settings.Secure "lock_screen_lock_after_timeout"
 * (default 5s on One UI / AOSP). Launching AODDisplayActivity before the Keyguard locks wakes the
 * screen (FLAG_KEEP_SCREEN_ON / setTurnScreenOn) and cancels Android's delayed lock timer.
 * This controller therefore strictly waits until KeyguardManager.isKeyguardLocked is true before
 * activating AOD on any secured device.
 */
object AodScreenOffController {

    const val CHANNEL_TRIGGER_ID = "aod_trigger_channel"
    const val TRIGGER_NOTIFICATION_ID = 2002
    const val EXTRA_LOCK_WAIT_SESSION_ID = "com.example.aod.EXTRA_LOCK_WAIT_SESSION_ID"
    private const val ACTION_LOCK_CHECK_ALARM = "com.example.aod.ACTION_LOCK_CHECK_ALARM"
    private const val ALARM_REQUEST_CODE = 2009

    private const val INITIAL_CHECK_DELAY_MS = 850L
    private const val SWIPE_OR_NONE_GRACE_MS = 2200L
    private const val LOCK_SETTLE_DELAY_MS = 300L
    private const val POLL_INTERVAL_FAST_MS = 350L
    private const val POLL_INTERVAL_COARSE_MS = 1000L
    private const val SHORT_WAIT_WAKELOCK_LIMIT_MS = 65_000L
    private const val MAX_ALARM_WAIT_MS = 35L * 60_000L

    private const val PREFS_BRIGHTNESS_BACKUP = "aod_brightness_backup"
    private const val KEY_HAS_BACKUP = "has_backup"
    private const val KEY_SAVED_BRIGHTNESS = "saved_brightness"
    private const val KEY_SAVED_MODE = "saved_mode"

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeSessionId = AtomicLong(0L)

    @Volatile
    var isListening: Boolean = false
        private set

    @Volatile
    private var cachedAppContext: Context? = null

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

    @Volatile
    private var waitStartedElapsedMs: Long = 0L

    @Volatile
    private var waitConfiguredLockTimeoutMs: Long = -1L

    @Volatile
    private var waitStartedAutoCharging: Boolean = false

    @Volatile
    private var waitAllowInteractiveWhileLocked: Boolean = false

    @Volatile
    private var sawUnlockedDuringWait: Boolean = false

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

    fun onLockWaitAlarm(context: Context, sessionId: Long) {
        val appContext = context.applicationContext
        cachedAppContext = appContext
        if (sessionId <= 0L || sessionId != activeSessionId.get()) {
            return
        }
        if (!isListening || !isAutoAodEnabled(appContext) || AODDisplayActivity.isCurrentlyVisible) {
            cancelPendingLaunch(appContext)
            return
        }
        acquireCpuHoldWakeLock(appContext, 5000L)
        mainHandler.post {
            evaluateLockAndMaybeLaunch(
                appContext = appContext,
                sessionId = sessionId,
                isSettleFollowUp = false
            )
        }
    }

    fun wakeLockScreenFromAod(context: Context) {
        val appContext = context.applicationContext
        cachedAppContext = appContext
        lastWokeUpFromAodAtMs = System.currentTimeMillis()
        cancelPendingLaunch(appContext)
        cancelTriggerNotification(appContext)
        restoreSystemBrightnessIfNeeded(appContext)
        acquireCpuHoldWakeLock(appContext, 3000L)

        wakeLockRunnable?.let { mainHandler.removeCallbacks(it) }
        scheduleWakeAttempt(appContext, attempt = 0, delayMs = 120L)
    }

    private fun scheduleWakeAttempt(appContext: Context, attempt: Int, delayMs: Long) {
        val runnable = Runnable {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && pm.isInteractive) {
                if (attempt < 3) {
                    scheduleWakeAttempt(appContext, attempt + 1, 75L)
                } else {
                    releaseCpuHoldWakeLock()
                }
                return@Runnable
            }
            try {
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
            } finally {
                releaseCpuHoldWakeLock()
            }
        }
        wakeLockRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
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

        var launched = false
        if (Settings.canDrawOverlays(appContext) || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                val options = ActivityOptions.makeCustomAnimation(appContext, 0, 0)
                appContext.startActivity(wakeIntent, options.toBundle())
                launched = true
            } catch (_: Exception) {
                launched = false
            }
        }

        if (!launched) {
            try {
                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ensureTriggerChannel(nm)
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
                nm?.notify(TRIGGER_NOTIFICATION_ID + 1, wakeNotification)
            } catch (_: Throwable) {
                // Safe fallback
            }
        }
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        cachedAppContext = appContext
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
                                cancelPendingLaunch(appContext)
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
                                cancelPendingLaunch(appContext)
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
        cachedAppContext = appContext
        synchronized(lock) {
            isListening = false
            cancelPendingLaunch(appContext)
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
        return try {
            val km = appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.isKeyguardLocked == true
        } catch (_: Throwable) {
            false
        }
    }

    private fun isKeyguardSecure(appContext: Context): Boolean {
        return try {
            val km = appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.isKeyguardSecure == true || km?.isDeviceSecure == true
        } catch (_: Throwable) {
            false
        }
    }

    private fun readLockAfterTimeoutMs(appContext: Context): Long {
        return try {
            Settings.Secure.getLong(
                appContext.contentResolver,
                "lock_screen_lock_after_timeout",
                -1L
            )
        } catch (_: Throwable) {
            -1L
        }
    }

    private fun startLockWaitSession(
        appContext: Context,
        isPowerConnect: Boolean,
        startedAutoCharging: Boolean,
        allowInteractiveWhileLocked: Boolean
    ): Long {
        cancelPendingLaunch(appContext)
        val sessionId = activeSessionId.incrementAndGet()
        pendingLaunchIsPowerConnect = isPowerConnect
        waitStartedElapsedMs = SystemClock.elapsedRealtime()
        waitConfiguredLockTimeoutMs = readLockAfterTimeoutMs(appContext)
        waitStartedAutoCharging = startedAutoCharging
        waitAllowInteractiveWhileLocked = allowInteractiveWhileLocked
        sawUnlockedDuringWait = false
        return sessionId
    }

    private fun scheduleAodOnScreenOff(appContext: Context) {
        if (!isAutoAodEnabled(appContext)) {
            return
        }
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

        val sessionId = startLockWaitSession(
            appContext = appContext,
            isPowerConnect = false,
            startedAutoCharging = onlyCharging,
            allowInteractiveWhileLocked = false
        )
        acquireCpuHoldWakeLock(appContext, 3000L)

        val runnable = Runnable {
            evaluateLockAndMaybeLaunch(
                appContext = appContext,
                sessionId = sessionId,
                isSettleFollowUp = false
            )
        }
        pendingLaunchRunnable = runnable
        // Initial 850ms settle covers Power-button instant lock & screen-off animation cleanly
        mainHandler.postDelayed(runnable, INITIAL_CHECK_DELAY_MS)
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

        val sessionId = startLockWaitSession(
            appContext = appContext,
            isPowerConnect = true,
            startedAutoCharging = true,
            allowInteractiveWhileLocked = true
        )
        acquireCpuHoldWakeLock(appContext, 6500L)

        val runnable = Runnable {
            evaluateLockAndMaybeLaunch(
                appContext = appContext,
                sessionId = sessionId,
                isSettleFollowUp = false
            )
        }
        pendingLaunchRunnable = runnable
        mainHandler.postDelayed(runnable, 4800L)
    }

    private fun evaluateLockAndMaybeLaunch(
        appContext: Context,
        sessionId: Long,
        isSettleFollowUp: Boolean
    ) {
        if (sessionId != activeSessionId.get()) {
            return
        }
        if (!isAutoAodEnabled(appContext) || AODDisplayActivity.isCurrentlyVisible) {
            cancelPendingLaunch(appContext)
            return
        }

        val onlyCharging = isOnlyChargingMode(appContext)
        if (waitStartedAutoCharging && !onlyCharging) {
            // Mode changed to always; still respect charging=false only if started solely for power-connect while interactive
            if (pendingLaunchIsPowerConnect) {
                cancelPendingLaunch(appContext)
                return
            }
        }
        if (onlyCharging && !isDeviceCharging(appContext)) {
            cancelPendingLaunch(appContext)
            return
        }

        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val interactiveNow = pm?.isInteractive == true
        val lockedNow = isKeyguardLocked(appContext)
        val secureNow = isKeyguardSecure(appContext)

        if (interactiveNow) {
            val canDirectStart = Settings.canDrawOverlays(appContext) ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            val canLaunchInteractiveChargingPill = waitAllowInteractiveWhileLocked &&
                lockedNow &&
                canDirectStart
            if (!canLaunchInteractiveChargingPill) {
                cancelPendingLaunch(appContext)
                return
            }
        } else {
            // Once screen is off, require non-interactive + locked cleanly
            waitAllowInteractiveWhileLocked = false
        }

        if (lockedNow) {
            if (sawUnlockedDuringWait && !isSettleFollowUp && !interactiveNow) {
                // Delayed lock just engaged; wait 300ms for Keyguard / One UI lock screen to settle
                acquireCpuHoldWakeLock(appContext, 2000L)
                scheduleNextHandlerCheck(appContext, sessionId, LOCK_SETTLE_DELAY_MS, isSettleFollowUp = true)
                return
            }

            val autoCharging = waitStartedAutoCharging || onlyCharging
            cancelPendingLaunch(appContext)
            launchAodOverLockScreen(
                appContext = appContext,
                startedAutoCharging = autoCharging,
                requireKeyguardLocked = secureNow || lockedNow
            )
            return
        }

        // Not locked yet (lockedNow == false)
        sawUnlockedDuringWait = true
        val elapsedMs = (SystemClock.elapsedRealtime() - waitStartedElapsedMs).coerceAtLeast(0L)

        if (!secureNow) {
            // Device uses either Swipe (locks shortly after screen off) or None (never locks).
            if (elapsedMs < SWIPE_OR_NONE_GRACE_MS) {
                acquireCpuHoldWakeLock(appContext, 3000L)
                scheduleNextHandlerCheck(appContext, sessionId, POLL_INTERVAL_FAST_MS, isSettleFollowUp = false)
            } else {
                // Lock screen is set to "None"; safe to launch AOD without blocking any lock.
                val autoCharging = waitStartedAutoCharging || onlyCharging
                cancelPendingLaunch(appContext)
                launchAodOverLockScreen(
                    appContext = appContext,
                    startedAutoCharging = autoCharging,
                    requireKeyguardLocked = false
                )
            }
            return
        }

        // Device IS secured (PIN / Pattern / Password / Biometrics) and is NOT locked yet.
        // We MUST NOT wake the screen or launch AOD until isKeyguardLocked becomes true.
        val configuredTimeout = waitConfiguredLockTimeoutMs
        val timeoutKnownLong = configuredTimeout > SHORT_WAIT_WAKELOCK_LIMIT_MS
        val exceededShortWait = elapsedMs >= SHORT_WAIT_WAKELOCK_LIMIT_MS

        if (timeoutKnownLong || exceededShortWait) {
            if (elapsedMs >= MAX_ALARM_WAIT_MS) {
                cancelPendingLaunch(appContext)
                return
            }
            // Hand off to AlarmManager so CPU can deep-sleep cleanly with zero battery drain
            releaseCpuHoldWakeLock()
            val delayMs = if (configuredTimeout > elapsedMs) {
                (configuredTimeout - elapsedMs + LOCK_SETTLE_DELAY_MS).coerceIn(1000L, MAX_ALARM_WAIT_MS)
            } else {
                15_000L
            }
            scheduleLockCheckAlarm(appContext, sessionId, delayMs)
            return
        }

        // Short-wait window (covers 5s / 15s / 30s / 60s auto-lock timers accurately)
        val remainingBudgetMs = if (configuredTimeout in 1L..SHORT_WAIT_WAKELOCK_LIMIT_MS) {
            (configuredTimeout - elapsedMs + 6000L).coerceIn(3000L, 70_000L)
        } else {
            (SHORT_WAIT_WAKELOCK_LIMIT_MS - elapsedMs + 5000L).coerceIn(3000L, 70_000L)
        }
        acquireCpuHoldWakeLock(appContext, remainingBudgetMs)

        val nextPollDelayMs = if (configuredTimeout > 1500L && elapsedMs < configuredTimeout - 800L) {
            ((configuredTimeout - elapsedMs) - 400L).coerceIn(POLL_INTERVAL_FAST_MS, POLL_INTERVAL_COARSE_MS)
        } else {
            POLL_INTERVAL_FAST_MS
        }
        scheduleNextHandlerCheck(appContext, sessionId, nextPollDelayMs, isSettleFollowUp = false)
    }

    private fun scheduleNextHandlerCheck(
        appContext: Context,
        sessionId: Long,
        delayMs: Long,
        isSettleFollowUp: Boolean
    ) {
        pendingLaunchRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            evaluateLockAndMaybeLaunch(
                appContext = appContext,
                sessionId = sessionId,
                isSettleFollowUp = isSettleFollowUp
            )
        }
        pendingLaunchRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun createLockCheckPendingIntent(
        appContext: Context,
        sessionId: Long,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(appContext, AodLockCheckReceiver::class.java).apply {
            action = ACTION_LOCK_CHECK_ALARM
            setPackage(appContext.packageName)
            putExtra(EXTRA_LOCK_WAIT_SESSION_ID, sessionId)
        }
        return PendingIntent.getBroadcast(appContext, ALARM_REQUEST_CODE, intent, flags)
    }

    private fun scheduleLockCheckAlarm(appContext: Context, sessionId: Long, delayMs: Long) {
        try {
            val am = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = createLockCheckPendingIntent(
                appContext = appContext,
                sessionId = sessionId,
                flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
            val triggerAt = SystemClock.elapsedRealtime() + delayMs.coerceAtLeast(1000L)
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                try {
                    am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
                    return
                } catch (_: SecurityException) {
                    // Fallback to inexact allowWhileIdle
                }
            }
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    private fun cancelLockCheckAlarm(appContext: Context?) {
        val ctx = appContext ?: cachedAppContext ?: return
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = createLockCheckPendingIntent(
                appContext = ctx,
                sessionId = 0L,
                flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
            am.cancel(pi)
            pi.cancel()
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    private fun launchAodOverLockScreen(
        appContext: Context,
        startedAutoCharging: Boolean,
        requireKeyguardLocked: Boolean
    ) {
        val launchIntent = Intent(appContext, AODDisplayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
            putExtra(AODDisplayActivity.EXTRA_STARTED_AUTO_CHARGING, startedAutoCharging)
            putExtra(AODDisplayActivity.EXTRA_REQUIRE_KEYGUARD_LOCKED, requireKeyguardLocked)
        }

        var launched = false
        if (Settings.canDrawOverlays(appContext) || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                dimSystemBrightnessBeforeWake(appContext)
                val options = ActivityOptions.makeCustomAnimation(appContext, 0, 0)
                appContext.startActivity(launchIntent, options.toBundle())
                launched = true
            } catch (_: Exception) {
                restoreSystemBrightnessIfNeeded(appContext)
                launched = false
            }
        }

        if (!launched) {
            try {
                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(TRIGGER_NOTIFICATION_ID)
                ensureTriggerChannel(nm)

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
            } catch (_: Throwable) {
                // Safe fallback
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

    private fun cancelPendingLaunch(appContext: Context? = cachedAppContext) {
        activeSessionId.incrementAndGet()
        pendingLaunchRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingLaunchRunnable = null
        pendingLaunchIsPowerConnect = false
        waitStartedAutoCharging = false
        waitAllowInteractiveWhileLocked = false
        sawUnlockedDuringWait = false
        cancelLockCheckAlarm(appContext)
        releaseCpuHoldWakeLock()
    }

    private fun acquireCpuHoldWakeLock(appContext: Context, timeoutMs: Long) {
        try {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val existing = cpuHoldWakeLock
            val wl = existing ?: pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AlwaysOn:ScreenOffWait"
            ).apply {
                setReferenceCounted(false)
            }.also {
                cpuHoldWakeLock = it
            }
            // Re-acquiring a non-reference-counted WakeLock extends its timeout cleanly without dropping it
            wl.acquire(timeoutMs.coerceIn(1000L, 75_000L))
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
