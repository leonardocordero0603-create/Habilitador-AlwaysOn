package com.example.aod

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AodPreferencesRepository
import com.example.model.AodPreferences
import com.example.ui.theme.AlwaysOnTheme
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.delay

class AODDisplayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_STARTED_AUTO_CHARGING = "com.example.aod.EXTRA_STARTED_AUTO_CHARGING"

        @Volatile
        var isCurrentlyVisible: Boolean = false
            private set

        @Volatile
        var lastPausedAtMs: Long = 0L
            private set
    }

    private enum class AodExitPhase { NONE, DOUBLE_TAP, UNLOCK, DISCONNECT }

    private lateinit var preferencesRepository: AodPreferencesRepository
    private val mainHandler = Handler(Looper.getMainLooper())
    private var createdAtMs: Long = 0L
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var isResumedState: Boolean = false
    private var wantProximityScreenOff: Boolean = true
    private var observedKeyguardLocked: Boolean = false
    private var isFinishingSmartly: Boolean = false
    private var startedInAutoChargingMode by mutableStateOf(false)
    private var exitPhase by mutableStateOf(AodExitPhase.NONE)
    private var transitionGeneration by mutableIntStateOf(0)
    private val forceFinishRunnable = Runnable { completeAnimatedExit() }

    override fun onCreate(savedInstanceState: Bundle?) {
        disableSystemTransition()
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            )
        } catch (_: Throwable) {
            // Safe fallback
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                splashScreen.setOnExitAnimationListener { splashScreenView ->
                    splashScreenView.remove()
                }
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
        createdAtMs = System.currentTimeMillis()
        isFinishingSmartly = false
        startedInAutoChargingMode = intent?.getBooleanExtra(EXTRA_STARTED_AUTO_CHARGING, false) == true
        exitPhase = AodExitPhase.NONE
        observedKeyguardLocked = isKeyguardCurrentlyLocked()
        preferencesRepository = AodPreferencesRepository(applicationContext)

        try {
            // Configure pure black OLED window, cutout coverage, keep-screen-on, and 1% pre-reveal brightness BEFORE any draw
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
            window.decorView.setBackgroundColor(AndroidColor.BLACK)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            val lp = window.attributes
            lp.screenBrightness = 0.01f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            window.attributes = lp

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            }
        } catch (_: Throwable) {
            // Safe fallback
        }

        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(AODAutoService.TRIGGER_NOTIFICATION_ID)
        } catch (_: Throwable) {
            // Safe fallback
        }

        // Synchronously attach a pure-black native root container before Compose runs
        val rootContainer = FrameLayout(this).apply {
            setBackgroundColor(AndroidColor.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val composeView = ComposeView(this).apply {
            setBackgroundColor(AndroidColor.BLACK)
            setViewTreeLifecycleOwner(this@AODDisplayActivity)
            setViewTreeSavedStateRegistryOwner(this@AODDisplayActivity)
            setContent {
                val prefs by preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
                    initialValue = AodPreferences(brightnessLevel = preferencesRepository.getCachedBrightness())
                )
                var brightnessStageReady by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(80L)
                    brightnessStageReady = true
                }

                SideEffect {
                    hideSystemBars()
                }

                LaunchedEffect(brightnessStageReady, prefs.brightnessLevel, prefs.pocketModeEnabled) {
                    wantProximityScreenOff = prefs.pocketModeEnabled
                    syncProximityWakeLock()
                }

                AlwaysOnTheme(darkTheme = true) {
                    AodFullscreenScreen(
                        preferences = prefs,
                        brightnessStageReady = brightnessStageReady,
                        startedInAutoChargingMode = startedInAutoChargingMode,
                        isExiting = exitPhase != AodExitPhase.NONE,
                        exitDurationMs = when (exitPhase) {
                            AodExitPhase.UNLOCK -> 200
                            AodExitPhase.DISCONNECT -> 260
                            else -> 320
                        },
                        transitionGeneration = transitionGeneration,
                        onProximityPowerStateChanged = { covered ->
                            applyWindowPowerState(
                                covered = covered,
                                brightnessStageReady = brightnessStageReady,
                                targetBrightness = prefs.brightnessLevel
                            )
                        },
                        onCheckSmartUnlock = { checkSmartKeyguardUnlock() },
                        onUnlockDismiss = { beginAnimatedExit(AodExitPhase.UNLOCK) },
                        onPowerDisconnectedDismiss = { beginAnimatedExit(AodExitPhase.DISCONNECT) },
                        onPowerPressedWhileAod = { pocketCovered ->
                            handlePowerPressedWhileAod(pocketCovered)
                        },
                        onExit = { beginAnimatedExit(AodExitPhase.DOUBLE_TAP) },
                        onExitAnimationFinished = { completeAnimatedExit() }
                    )
                }
            }
        }

        rootContainer.addView(composeView)
        setContentView(rootContainer)
        hideSystemBars()

        // Enable turnScreenOn AFTER the pure-black root surface and black splash theme are configured
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        hideSystemBars()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        createdAtMs = System.currentTimeMillis()
        isFinishingSmartly = false
        startedInAutoChargingMode = intent.getBooleanExtra(EXTRA_STARTED_AUTO_CHARGING, false)
        exitPhase = AodExitPhase.NONE
        transitionGeneration += 1
        mainHandler.removeCallbacks(forceFinishRunnable)
        observedKeyguardLocked = observedKeyguardLocked || isKeyguardCurrentlyLocked()
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
        disableSystemTransition()
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        isResumedState = true
        isCurrentlyVisible = true
        if (checkSmartKeyguardUnlock()) return
        AodLiveStateHolder.refreshMediaSessions(applicationContext)
        hideSystemBars()
        syncProximityWakeLock()
    }

    override fun onPause() {
        isResumedState = false
        isCurrentlyVisible = false
        lastPausedAtMs = System.currentTimeMillis()
        releaseProximityWakeLock()
        AodScreenOffController.restoreSystemBrightnessIfNeeded(applicationContext)
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(forceFinishRunnable)
        releaseProximityWakeLock()
        AodScreenOffController.restoreSystemBrightnessIfNeeded(applicationContext)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (checkSmartKeyguardUnlock()) return
        if (hasFocus) hideSystemBars()
    }

    private fun isKeyguardCurrentlyLocked(): Boolean {
        return try {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.isKeyguardLocked == true
        } catch (_: Throwable) {
            false
        }
    }

    private fun checkSmartKeyguardUnlock(): Boolean {
        if (isFinishingSmartly) return true
        if (exitPhase == AodExitPhase.UNLOCK) return true
        val lockedNow = isKeyguardCurrentlyLocked()
        if (lockedNow) {
            observedKeyguardLocked = true
            return false
        }
        if (observedKeyguardLocked) {
            beginAnimatedExit(AodExitPhase.UNLOCK)
            return true
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun syncProximityWakeLock() {
        if (!isResumedState || !wantProximityScreenOff) {
            releaseProximityWakeLock()
            return
        }
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        if (!pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            return
        }
        if (proximityWakeLock == null) {
            proximityWakeLock = pm.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "AlwaysOn:ProximityScreenOff"
            ).apply {
                setReferenceCounted(false)
            }
        }
        val wl = proximityWakeLock ?: return
        if (!wl.isHeld) {
            try {
                wl.acquire()
            } catch (_: Exception) {
                // Ignored if restricted on device
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun releaseProximityWakeLock() {
        proximityWakeLock?.let { wl ->
            if (wl.isHeld) {
                try {
                    wl.release()
                } catch (_: Exception) {
                    // Ignored
                }
            }
        }
    }

    private fun applyWindowPowerState(
        covered: Boolean,
        brightnessStageReady: Boolean,
        targetBrightness: Float
    ) {
        try {
            if (covered) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val updatedLp = window.attributes
                updatedLp.screenBrightness = 0.0f
                window.attributes = updatedLp
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val updatedLp = window.attributes
                updatedLp.screenBrightness = if (brightnessStageReady) {
                    targetBrightness.coerceIn(0.03f, 0.45f)
                } else {
                    0.01f
                }
                window.attributes = updatedLp
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    override fun finish() {
        super.finish()
        disableSystemTransition()
    }

    private fun beginAnimatedExit(phase: AodExitPhase) {
        if (isFinishingSmartly || phase == AodExitPhase.NONE) return
        if (exitPhase == AodExitPhase.UNLOCK) return
        if (exitPhase == phase) return
        exitPhase = phase
        mainHandler.removeCallbacks(forceFinishRunnable)
        val timeoutMs = when (phase) {
            AodExitPhase.UNLOCK -> 280L
            AodExitPhase.DISCONNECT -> 360L
            else -> 430L
        }
        mainHandler.postDelayed(forceFinishRunnable, timeoutMs)
    }

    private fun completeAnimatedExit() {
        mainHandler.removeCallbacks(forceFinishRunnable)
        when (exitPhase) {
            AodExitPhase.UNLOCK -> finishUnlockedSmartly()
            AodExitPhase.DISCONNECT -> finishDismissedOnDisconnect()
            else -> finishCleanly()
        }
    }

    private fun finishDismissedOnDisconnect() {
        if (isFinishingSmartly) return
        isFinishingSmartly = true
        mainHandler.removeCallbacks(forceFinishRunnable)
        releaseProximityWakeLock()
        clearLockScreenWindowFlags()
        AodScreenOffController.restoreSystemBrightnessIfNeeded(applicationContext)
        finishAndRemoveTask()
        disableSystemTransition()
    }

    private fun finishCleanly() {
        if (isFinishingSmartly) return
        isFinishingSmartly = true
        mainHandler.removeCallbacks(forceFinishRunnable)
        releaseProximityWakeLock()
        AodScreenOffController.restoreSystemBrightnessIfNeeded(applicationContext)
        finish()
        disableSystemTransition()
    }

    private fun handlePowerPressedWhileAod(pocketCovered: Boolean) {
        if (isFinishingSmartly) return
        if (System.currentTimeMillis() - createdAtMs <= 400L) return
        mainHandler.removeCallbacks(forceFinishRunnable)
        if (pocketCovered) {
            finishCleanly()
            return
        }
        isFinishingSmartly = true
        releaseProximityWakeLock()
        clearLockScreenWindowFlags()
        AodScreenOffController.wakeLockScreenFromAod(applicationContext)
        finishAndRemoveTask()
        disableSystemTransition()
    }

    private fun finishUnlockedSmartly() {
        if (isFinishingSmartly) return
        isFinishingSmartly = true
        mainHandler.removeCallbacks(forceFinishRunnable)
        AodScreenOffController.notifyUnlocked()
        releaseProximityWakeLock()
        clearLockScreenWindowFlags()
        AodScreenOffController.restoreSystemBrightnessIfNeeded(applicationContext)

        finishAndRemoveTask()
        disableSystemTransition()
    }

    private fun clearLockScreenWindowFlags() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false)
                setTurnScreenOn(false)
            } else {
                @Suppress("DEPRECATION")
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            val updatedLp = window.attributes
            updatedLp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = updatedLp
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    private fun disableSystemTransition() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    private fun hideSystemBars() {
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } catch (_: Throwable) {
            // Safe fallback
        }
    }
}

@Composable
private fun AodFullscreenScreen(
    preferences: AodPreferences,
    brightnessStageReady: Boolean,
    startedInAutoChargingMode: Boolean,
    isExiting: Boolean,
    exitDurationMs: Int,
    transitionGeneration: Int,
    onProximityPowerStateChanged: (Boolean) -> Unit,
    onCheckSmartUnlock: () -> Boolean,
    onUnlockDismiss: () -> Unit,
    onPowerDisconnectedDismiss: () -> Unit,
    onPowerPressedWhileAod: (Boolean) -> Unit,
    onExit: () -> Unit,
    onExitAnimationFinished: () -> Unit
) {
    val context = LocalContext.current
    val notifications by AodLiveStateHolder.notifications.collectAsStateWithLifecycle()
    val samsungMusic by AodLiveStateHolder.samsungMusic.collectAsStateWithLifecycle()
    var batteryPercent by remember { mutableStateOf<Int?>(null) }
    var isCharging by remember { mutableStateOf(false) }
    var wasChargingDuringSession by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(true) }
    var isPocketCovered by remember { mutableStateOf(false) }
    val latestOnlyCharging by androidx.compose.runtime.rememberUpdatedState(preferences.autoAodOnlyCharging)
    val latestAutoAodEnabled by androidx.compose.runtime.rememberUpdatedState(preferences.autoAodOnScreenOff)
    val latestStartedAutoCharging by androidx.compose.runtime.rememberUpdatedState(startedInAutoChargingMode)

    LaunchedEffect(isPocketCovered, brightnessStageReady, preferences.brightnessLevel) {
        onProximityPowerStateChanged(isPocketCovered)
    }

    // Watch Keyguard lock->unlock transitions (e.g., side or under-display fingerprint unlock)
    LaunchedEffect(Unit) {
        delay(40L)
        while (true) {
            if (onCheckSmartUnlock()) break
            delay(250L)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                        if (level >= 0 && scale > 0) {
                            batteryPercent = ((level * 100f) / scale).toInt().coerceIn(0, 100)
                        }
                        val nowCharging = plugged > 0 ||
                            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                        isCharging = nowCharging
                        if (nowCharging) {
                            wasChargingDuringSession = true
                        } else if (wasChargingDuringSession &&
                            latestOnlyCharging &&
                            (latestStartedAutoCharging || latestAutoAodEnabled)
                        ) {
                            onPowerDisconnectedDismiss()
                        }
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        isCharging = false
                        if (latestOnlyCharging &&
                            (latestStartedAutoCharging || (latestAutoAodEnabled && wasChargingDuringSession))
                        ) {
                            onPowerDisconnectedDismiss()
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> onUnlockDismiss()
                    Intent.ACTION_SCREEN_ON -> onCheckSmartUnlock()
                    Intent.ACTION_SCREEN_OFF -> {
                        // Pressing Power while AOD is displayed wakes the normal lock screen
                        onPowerPressedWhileAod(isPocketCovered)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        val sticky = try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Throwable) {
            try {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            } catch (_: Throwable) {
                null
            }
        }
        sticky?.let { receiver.onReceive(context, it) }

        if (batteryPercent == null) {
            try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val prop = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                if (prop in 0..100) {
                    batteryPercent = prop
                }
            } catch (_: Throwable) {
                // Safe fallback
            }
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Throwable) {
                // Already unregistered
            }
        }
    }

    DisposableEffect(context, preferences.pocketModeEnabled) {
        if (!preferences.pocketModeEnabled) {
            isPocketCovered = false
            return@DisposableEffect onDispose {}
        }

        val sensorManager = try {
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        } catch (_: Throwable) {
            null
        }
        val proximitySensor = try {
            sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        } catch (_: Throwable) {
            null
        }
        if (sensorManager == null || proximitySensor == null) {
            isPocketCovered = false
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val distance = event?.values?.firstOrNull() ?: return
                val threshold = if (proximitySensor.maximumRange > 0f) proximitySensor.maximumRange else 1f
                isPocketCovered = distance < threshold
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        try {
            sensorManager.registerListener(listener, proximitySensor, SensorManager.SENSOR_DELAY_UI)
        } catch (_: Throwable) {
            isPocketCovered = false
        }

        onDispose {
            try {
                sensorManager.unregisterListener(listener)
            } catch (_: Throwable) {
                // Safe fallback
            }
        }
    }

    LaunchedEffect(showHint) {
        if (showHint) {
            delay(3500)
            showHint = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onExit() },
                    onTap = { showHint = true }
                )
            }
    ) {
        if (!isPocketCovered) {
            AodTransitionContainer(
                visible = !isExiting,
                transitionStyle = preferences.transitionStyle,
                accentColor = preferences.accentColor.color,
                replayTrigger = transitionGeneration,
                animateOnInitialShow = true,
                exitDurationMs = exitDurationMs,
                onExitAnimationFinished = onExitAnimationFinished,
                modifier = Modifier.fillMaxSize()
            ) {
                AodClockCanvas(
                    preferences = preferences,
                    batteryPercent = batteryPercent,
                    isCharging = isCharging,
                    isPreview = false,
                    notifications = notifications,
                    samsungMusic = samsungMusic,
                    onMusicPrevious = AodLiveStateHolder::skipToPrevious,
                    onMusicPlayPause = AodLiveStateHolder::togglePlayPause,
                    onMusicNext = AodLiveStateHolder::skipToNext
                )
            }

            AnimatedVisibility(
                visible = showHint && !isExiting,
                enter = fadeIn(tween(450)),
                exit = fadeOut(tween(240)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 22.dp)
            ) {
                Text(
                    text = "Toca dos veces para salir de Always On",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }
    }
}
