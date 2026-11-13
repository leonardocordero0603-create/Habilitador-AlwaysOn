package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.aod.SamsungAodManager
import com.example.model.AodAccentColor
import com.example.model.AodClockFont
import com.example.model.AodClockStyle
import com.example.model.AodPreferences
import com.example.model.AodThemePreset
import com.example.ui.components.QrCodeEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
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
        assertEquals(AodClockStyle.TYPOGRAPHIC_BOLD, AodClockStyle.fromId("typographic_bold"))
        assertEquals(AodClockStyle.DOT_MATRIX, AodClockStyle.fromId("dot_matrix"))
        assertEquals(AodClockStyle.ORBIT_CHRONO, AodClockStyle.fromId("orbit_chrono"))
        assertEquals(AodClockFont.ELEGANT_SERIF, AodClockFont.fromId("elegant_serif"))
        assertEquals(AodClockFont.CYBER_FUTURA, AodClockFont.fromId("cyber_futura"))
        assertEquals(AodAccentColor.PURE_WHITE, AodAccentColor.fromId("pure_white"))
        assertEquals(AodAccentColor.AURORA_MAGENTA, AodAccentColor.fromId("aurora_magenta"))

        val defaults = AodPreferences()
        assertEquals(1.0f, defaults.clockScale, 0.001f)
        assertEquals(false, defaults.alignLeft)
        assertTrue(AodThemePreset.GALAXY_S23.matches(defaults))
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
}
