package com.example

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.aod.AodWakeScreenActivity
import com.example.aod.SamsungAodManager
import com.example.model.AodAccentColor
import com.example.model.AodBackgroundStyle
import com.example.model.AodClockFont
import com.example.model.AodClockStyle
import com.example.model.AodDateStyle
import com.example.model.AodPreferences
import com.example.model.AodThemeCategory
import com.example.model.AodThemePreset
import com.example.model.AodTransitionStyle
import com.example.model.AodVerticalPosition
import com.example.ui.components.QrCodeEncoder
import com.example.ui.components.ThemeGalleryBottomSheet
import kotlinx.coroutines.flow.first
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
                    onSelectBackgroundStyle = {},
                    onSelectDateStyle = {},
                    onSelectVerticalPosition = {},
                    onToggleNeonGlow = {},
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
                    onLaunchPreviewFullscreen = {}
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
        assertEquals(18, AodClockStyle.entries.size)
        assertEquals(6, AodClockFont.entries.size)
        assertEquals(16, AodAccentColor.entries.size)
        assertEquals(12, AodThemePreset.entries.size)
        assertEquals(5, AodTransitionStyle.entries.size)
        assertEquals(6, AodBackgroundStyle.entries.size)
        assertEquals(5, AodDateStyle.entries.size)
        assertEquals(3, AodVerticalPosition.entries.size)
        assertEquals(AodClockStyle.TYPOGRAPHIC_BOLD, AodClockStyle.fromId("typographic_bold"))
        assertEquals(AodClockStyle.DOT_MATRIX, AodClockStyle.fromId("dot_matrix"))
        assertEquals(AodClockStyle.ORBIT_CHRONO, AodClockStyle.fromId("orbit_chrono"))
        assertEquals(AodClockStyle.OUTLINE_NEON, AodClockStyle.fromId("outline_neon"))
        assertEquals(AodClockStyle.SEGMENT_RETRO, AodClockStyle.fromId("segment_retro"))
        assertEquals(AodClockStyle.CYBER_HUD, AodClockStyle.fromId("cyber_hud"))
        assertEquals(AodClockStyle.HORIZON_ARC, AodClockStyle.fromId("horizon_arc"))
        assertEquals(AodClockStyle.BOLD_SPLIT, AodClockStyle.fromId("bold_split"))
        assertEquals(AodClockStyle.WORD_ESSENCE, AodClockStyle.fromId("word_essence"))
        assertEquals(AodClockFont.ELEGANT_SERIF, AodClockFont.fromId("elegant_serif"))
        assertEquals(AodClockFont.CYBER_FUTURA, AodClockFont.fromId("cyber_futura"))
        assertEquals(AodAccentColor.PURE_WHITE, AodAccentColor.fromId("pure_white"))
        assertEquals(AodAccentColor.AURORA_MAGENTA, AodAccentColor.fromId("aurora_magenta"))
        assertEquals(AodAccentColor.ELECTRIC_INDIGO, AodAccentColor.fromId("electric_indigo"))
        assertEquals(AodAccentColor.ROSE_GOLD, AodAccentColor.fromId("rose_gold"))
        assertEquals(AodBackgroundStyle.EDGE_GLOW, AodBackgroundStyle.fromId("edge_glow"))
        assertEquals(AodBackgroundStyle.STARFIELD_MIST, AodBackgroundStyle.fromId("starfield_mist"))
        assertEquals(AodDateStyle.COMPACT_PILL, AodDateStyle.fromId("compact_pill"))
        assertEquals(AodDateStyle.WEEK_PROGRESS, AodDateStyle.fromId("week_progress"))
        assertEquals(AodVerticalPosition.CENTER, AodVerticalPosition.fromId("center"))
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
        assertEquals(AodBackgroundStyle.PURE_BLACK, defaults.backgroundStyle)
        assertEquals(AodDateStyle.CLASSIC, defaults.dateStyle)
        assertEquals(AodVerticalPosition.TOP, defaults.verticalPosition)
        assertFalse(defaults.neonGlowEnabled)
        assertTrue(AodThemePreset.GALAXY_S23.matches(defaults))
    }

    @Test
    fun `render all new clock faces backgrounds and date styles without crash`() {
        val samplePrefs = listOf(
            AodPreferences(
                clockStyle = AodClockStyle.OUTLINE_NEON,
                backgroundStyle = AodBackgroundStyle.RADIAL_HALO,
                dateStyle = AodDateStyle.COMPACT_PILL,
                verticalPosition = AodVerticalPosition.TOP,
                neonGlowEnabled = true
            ),
            AodPreferences(
                clockStyle = AodClockStyle.SEGMENT_RETRO,
                backgroundStyle = AodBackgroundStyle.EDGE_GLOW,
                dateStyle = AodDateStyle.NUMERIC_TECH,
                verticalPosition = AodVerticalPosition.CENTER,
                showSeconds = true
            ),
            AodPreferences(
                clockStyle = AodClockStyle.HORIZON_ARC,
                backgroundStyle = AodBackgroundStyle.STARFIELD_MIST,
                dateStyle = AodDateStyle.WEEK_PROGRESS,
                verticalPosition = AodVerticalPosition.CENTER
            ),
            AodPreferences(
                clockStyle = AodClockStyle.CYBER_HUD,
                backgroundStyle = AodBackgroundStyle.TOP_AURORA,
                dateStyle = AodDateStyle.WEEKDAY_STACK,
                verticalPosition = AodVerticalPosition.LOWER,
                showSeconds = true
            ),
            AodPreferences(
                clockStyle = AodClockStyle.BOLD_SPLIT,
                backgroundStyle = AodBackgroundStyle.HORIZON_WAVE,
                dateStyle = AodDateStyle.CLASSIC,
                verticalPosition = AodVerticalPosition.TOP
            ),
            AodPreferences(
                clockStyle = AodClockStyle.WORD_ESSENCE,
                backgroundStyle = AodBackgroundStyle.PURE_BLACK,
                dateStyle = AodDateStyle.COMPACT_PILL,
                verticalPosition = AodVerticalPosition.CENTER
            )
        )
        composeTestRule.setContent {
            com.example.ui.theme.AlwaysOnTheme(darkTheme = true) {
                androidx.compose.foundation.layout.Column {
                    samplePrefs.forEach { prefs ->
                        com.example.aod.AodClockCanvas(
                            preferences = prefs,
                            batteryPercent = 84,
                            isCharging = true,
                            isPreview = true,
                            modifier = androidx.compose.ui.Modifier.size(180.dp)
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `predefined theme selection system resolves cycles and persists cleanly`() {
        assertEquals(5, AodThemeCategory.entries.size)
        assertEquals(AodThemePreset.GALAXY_S23, AodThemePreset.findMatching(AodPreferences()))
        assertEquals(AodThemePreset.CYBER_ORBIT, AodThemePreset.nextAfter(AodThemePreset.GALAXY_S23))
        assertEquals(AodThemePreset.GALAXY_S23, AodThemePreset.nextAfter(AodThemePreset.entries.last()))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.AodPreferencesRepository(context)
        kotlinx.coroutines.runBlocking {
            repo.applyThemePreset(AodThemePreset.CYBER_HUD_PRO)
            val applied = repo.preferencesFlow.first()
            assertEquals(AodClockStyle.CYBER_HUD, applied.clockStyle)
            assertEquals(AodAccentColor.LIME_NEON, applied.accentColor)
            assertEquals(AodBackgroundStyle.EDGE_GLOW, applied.backgroundStyle)
            assertEquals(AodDateStyle.NUMERIC_TECH, applied.dateStyle)
            assertTrue(applied.neonGlowEnabled)
            assertEquals(AodThemePreset.CYBER_HUD_PRO, AodThemePreset.findMatching(applied))

            // Changing accent color customizes without breaking style
            repo.updateAccentColor(AodAccentColor.ROSE_GOLD)
            val customized = repo.preferencesFlow.first()
            assertEquals(AodClockStyle.CYBER_HUD, customized.clockStyle)
            assertEquals(AodAccentColor.ROSE_GOLD, customized.accentColor)
            assertEquals(null, AodThemePreset.findMatching(customized))
        }
    }

    @Test
    fun `theme gallery bottom sheet filters categories and selects preset`() {
        var appliedPreset: AodThemePreset? = null
        var appliedColor: AodAccentColor? = null
        composeTestRule.setContent {
            com.example.ui.theme.AlwaysOnTheme(darkTheme = true) {
                ThemeGalleryBottomSheet(
                    preferences = AodPreferences(),
                    onSelectPreset = { appliedPreset = it },
                    onSelectAccentColor = { appliedColor = it },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_category_${AodThemeCategory.NEON.id}").performClick()
        composeTestRule.waitForIdle()
        val presetTag = "gallery_theme_preset_${AodThemePreset.NEON_OUTLINE_GLOW.id}"
        composeTestRule.onNodeWithTag("gallery_theme_grid").performScrollToNode(hasTestTag(presetTag))
        composeTestRule.onNodeWithTag(presetTag).performClick()
        composeTestRule.waitForIdle()
        assertEquals(AodThemePreset.NEON_OUTLINE_GLOW, appliedPreset)

        val accentTag = "gallery_accent_${AodAccentColor.ROSE_GOLD.id}"
        composeTestRule.onNodeWithTag("gallery_accent_row").performScrollToNode(hasTestTag(accentTag))
        composeTestRule.onNodeWithTag(accentTag).performClick()
        composeTestRule.waitForIdle()
        assertEquals(AodAccentColor.ROSE_GOLD, appliedColor)
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

    @Test
    fun `navigate across all tabs in AlwaysOnApp without freezing or crashing`() {
        composeTestRule.setContent {
            com.example.ui.theme.AlwaysOnTheme(darkTheme = true) {
                AlwaysOnApp()
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_styles").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_compatibility").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_activate").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `auto service and boot receiver handle lifecycle cleanly`() {
        val serviceController = Robolectric.buildService(com.example.aod.AODAutoService::class.java)
            .create()
            .startCommand(0, 1)
        assertTrue(com.example.aod.AODAutoService.isRunning)
        serviceController.destroy()
        assertFalse(com.example.aod.AODAutoService.isRunning)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val bootReceiver = com.example.aod.AodBootReceiver()
        bootReceiver.onReceive(
            context,
            android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)
        )
    }
}
