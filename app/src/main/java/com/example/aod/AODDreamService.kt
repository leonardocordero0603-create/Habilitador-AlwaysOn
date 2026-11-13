package com.example.aod

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.service.dreams.DreamService
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AodPreferencesRepository
import com.example.model.AodPreferences
import com.example.ui.theme.AlwaysOnTheme

class AODDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false

        val repo = AodPreferencesRepository(applicationContext)
        val (batteryPercent, isCharging) = readBattery()

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AODDreamService)
            setViewTreeSavedStateRegistryOwner(this@AODDreamService)
            setContent {
                val prefs by repo.preferencesFlow.collectAsStateWithLifecycle(
                    initialValue = AodPreferences()
                )
                val notifications by AodLiveStateHolder.notifications.collectAsStateWithLifecycle()
                val samsungMusic by AodLiveStateHolder.samsungMusic.collectAsStateWithLifecycle()
                AlwaysOnTheme(darkTheme = true) {
                    AodClockCanvas(
                        preferences = prefs,
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
            }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDetachedFromWindow()
    }

    private fun readBattery(): Pair<Int?, Boolean> {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val prop = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (prop in 0..100) prop else null
        }
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return percent to charging
    }
}
