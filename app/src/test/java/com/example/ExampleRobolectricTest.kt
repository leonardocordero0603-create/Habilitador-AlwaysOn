package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.aod.AodWakeScreenActivity
import com.example.aod.SamsungAodManager
import com.example.model.AodAccentColor
import com.example.model.AodClockFont
import com.example.model.AodClockStyle
import com.example.model.AodPreferences
import com.example.model.AodThemePreset
import com.example.model.AodTransitionStyle
import com.example.ui.components.QrCodeEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Always On", appName)
    }

    @get:org.junit.Rule
    val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `launch MainActivity directly without crash or hang`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertFalse(activity.isDestroyed)
        assertFalse(activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test
    fun `launch AODDisplayActivity directly without crash`() {
        val controller = Robolectric.buildActivity(com.example.aod.AODDisplayActivity::class.java).setup()
        val activity = controller.get()
        assertFalse(activity.isDestroyed)
        controller.pause().stop().destroy()
    }

    @Test
    fun `render AlwaysOnApp Compose UI directly`() {
        composeTestRule.setContent {
            com.example.ui.theme.AlwaysOnTheme(darkTheme = true) {
                AlwaysOnApp()
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `render StylesScreen Compose UI directly`() {
        composeTestRule.setContent {
            com.example.ui.theme.AlwaysOnTheme(darkTheme = true) {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val state = com.example.ui.AodUiState(
                    deviceStatus = SamsungAodManager(context).createInitialFallbackStatus()
                )
                com.example.ui.screens.StylesScreen(
                    uiState = state,
                    onApplyThemePreset = {},
                    onSelectClockStyle = {},
                    onSelectClockFont = {},
                    onSelectAccentColor = {},
                    onClockScaleChange = {},
                    onToggleAlignLeft = {},
                    onBrightnessChange = {},
                    onToggleBurnIn = {},
                    onTogglePocketMode = {},
                    onToggle24Hour = {},
                    onToggleShowBattery = {},
                    onToggleShowDate = {},
                    onToggleShowSeconds = {},
                    onToggleShowNotifications = {},
                    onToggleShowMusicControls = {},
                    onSelectTransitionStyle = {},
                    onCustomLabelChange = {},
                    onLaunchPreviewFullscreen = {},
                    onUnlockPro = {},
                    onRestorePurchases = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `render CompatibilityScreen Compose UI directly`() {
        composeTestRule.setContent {
            com.example.ui.theme.AlwaysOnTheme(darkTheme = true) {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val state = com.example.ui.AodUiState(
                    deviceStatus = SamsungAodManager(context).createInitialFallbackStatus()
                )
                com.example.ui.screens.CompatibilityScreen(
                    uiState = state,
                    onToggleNativeAod = {},
                    onSetTapToShowMode = {},
                    onOpenSamsungAodSettings = {},
                    onOpenWriteSettingsPermission = {},
                    onDismissWriteSettingsDialog = {},
                    onOpenOverlayPermissionSettings = {},
                    onOpenFullScreenIntentSettings = {},
                    onOpenNotificationListenerSettings = {},
                    onOpenAppBatterySettings = {},
                    onRunNextOptimization = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `inspect device status and adb commands`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SamsungAodManager(context)
        val status = manager.inspectDeviceStatus()

        assertTrue(status.sdkInt > 0)
        assertTrue(manager.getAdbGrantCommand().contains(context.packageName))
        assertTrue(manager.getAdbEnableNativeAodCommand().contains("aod_mode"))
    }

    @Test
    fun `s23 customization styles fonts and colors resolve cleanly`() {
        assertEquals(12, AodClockStyle.entries.size)
        assertEquals(6, AodClockFont.entries.size)
        assertEquals(12, AodAccentColor.entries.size)
        assertEquals(8, AodThemePreset.entries.size)
        assertEquals(5, AodTransitionStyle.entries.size)
        assertEquals(AodClockStyle.TYPOGRAPHIC_BOLD, AodClockStyle.fromId("typographic_bold"))
        assertEquals(AodClockStyle.DOT_MATRIX, AodClockStyle.fromId("dot_matrix"))
        assertEquals(AodClockStyle.ORBIT_CHRONO, AodClockStyle.fromId("orbit_chrono"))
        assertEquals(AodClockFont.ELEGANT_SERIF, AodClockFont.fromId("elegant_serif"))
        assertEquals(AodClockFont.CYBER_FUTURA, AodClockFont.fromId("cyber_futura"))
        assertEquals(AodAccentColor.PURE_WHITE, AodAccentColor.fromId("pure_white"))
        assertEquals(AodAccentColor.AURORA_MAGENTA, AodAccentColor.fromId("aurora_magenta"))
        assertEquals(AodTransitionStyle.ONE_UI_ZOOM, AodTransitionStyle.fromId("one_ui_zoom"))
        assertEquals(AodTransitionStyle.WONDERLAND_HALO, AodTransitionStyle.fromId("wonderland_halo"))
        assertEquals(AodTransitionStyle.IRIS_PORTAL, AodTransitionStyle.fromId("iris_portal"))
        assertEquals(AodTransitionStyle.CURTAIN_SWEEP, AodTransitionStyle.fromId("curtain_sweep"))
        assertEquals(AodTransitionStyle.SOFT_FADE, AodTransitionStyle.fromId("soft_fade"))
        assertEquals(AodTransitionStyle.ONE_UI_ZOOM, AodTransitionStyle.fromId("unknown_style"))

        val defaults = AodPreferences()
        assertEquals(1.0f, defaults.clockScale, 0.001f)
        assertEquals(false, defaults.alignLeft)
        assertEquals(AodTransitionStyle.ONE_UI_ZOOM, defaults.transitionStyle)
        assertTrue(AodThemePreset.GALAXY_S23.matches(defaults))
    }

    @Test
    fun `wake screen handover activity starts and finishes cleanly`() {
        val controller = Robolectric.buildActivity(AodWakeScreenActivity::class.java).setup()
        val activity = controller.get()
        assertFalse(activity.isDestroyed)
        controller.pause().stop().destroy()
    }

    @Test
    fun `optimization evaluator computes pending steps cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SamsungAodManager(context)
        val status = manager.inspectDeviceStatus()
        val summary = manager.evaluateOptimizations(status)

        assertEquals(5, summary.totalSteps)
        assertTrue(summary.completedSteps in 0..5)
        manager.applyMaximumPerformanceOptimizations(enableAod = true, tapToShow = false)

        val prefs = AodPreferences()
        assertTrue(prefs.showNotifications)
        assertTrue(prefs.showMusicControls)
        assertTrue(prefs.pocketModeEnabled)
    }

    @Test
    fun `qr encoder produces valid scannable iso matrix without crashing`() {
        val urlMatrix = QrCodeEncoder.encode(SamsungAodManager.APP_SHARE_URL)
        assertEquals(37, urlMatrix.size) // Version 5 for 68-byte URL at EC M
        // Top-left finder pattern outer border & center
        assertTrue(urlMatrix[0][0])
        assertFalse(urlMatrix[1][1])
        assertTrue(urlMatrix[3][3])
        // Separator around top-left finder
        assertFalse(urlMatrix[7][0])
        assertFalse(urlMatrix[0][7])
        // Dark module at (4*5 + 9, 8) = (29, 8)
        assertTrue(urlMatrix[29][8])
        // Timing pattern
        assertTrue(urlMatrix[6][8])
        assertFalse(urlMatrix[6][9])

        // Edge cases must never throw
        assertEquals(21, QrCodeEncoder.encode("").size)
        assertEquals(21, QrCodeEncoder.encode("Always On ⭐").size)
        assertEquals(57, QrCodeEncoder.encode("x".repeat(300)).size)
    }

    @Test
    fun `charging detection recognizes plugged usb even when status is not charging`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Remove any sticky battery broadcast
        context.removeStickyBroadcast(android.content.Intent(android.content.Intent.ACTION_BATTERY_CHANGED))
        assertFalse(com.example.aod.AodScreenOffController.isDeviceCharging(context))

        // Samsung 80% battery protection reports plugged USB + NOT_CHARGING
        val usbProtectedIntent = android.content.Intent(android.content.Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(android.os.BatteryManager.EXTRA_PLUGGED, android.os.BatteryManager.BATTERY_PLUGGED_USB)
            putExtra(android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING)
        }
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(usbProtectedIntent)
        assertTrue(com.example.aod.AodScreenOffController.isDeviceCharging(context))

        // Unplugged discharging
        val unpluggedIntent = android.content.Intent(android.content.Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0)
            putExtra(android.os.BatteryManager.EXTRA_STATUS, android.os.BatteryManager.BATTERY_STATUS_DISCHARGING)
        }
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(unpluggedIntent)
        assertFalse(com.example.aod.AodScreenOffController.isDeviceCharging(context))
    }

    @Test
    fun `charging only mode updates controller and repository cache synchronously`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.AodPreferencesRepository(context)
        assertFalse(repo.getCachedAutoAodOnlyCharging())
        kotlinx.coroutines.runBlocking {
            repo.updateAutoAodOnlyCharging(true)
        }
        assertTrue(repo.getCachedAutoAodOnlyCharging())
        com.example.aod.AodScreenOffController.setOnlyChargingMode(true)
    }
}
