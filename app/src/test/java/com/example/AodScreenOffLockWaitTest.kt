package com.example

import android.app.AlarmManager
import android.app.Application
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.aod.AODDisplayActivity
import com.example.aod.AodLockCheckReceiver
import com.example.aod.AodScreenOffController
import com.example.data.AodPreferencesRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowKeyguardManager
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPowerManager
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AodScreenOffLockWaitTest {

    private lateinit var app: Application
    private lateinit var km: KeyguardManager
    private lateinit var shadowKm: ShadowKeyguardManager
    private lateinit var pm: PowerManager
    private lateinit var shadowPm: ShadowPowerManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        km = app.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        shadowKm = Shadows.shadowOf(km)
        pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowPm = Shadows.shadowOf(pm)

        ShadowSettings.setCanDrawOverlays(true)

        val repo = AodPreferencesRepository(app)
        runBlocking {
            repo.updateAutoAodOnScreenOff(true)
            repo.updateAutoAodOnlyCharging(false)
        }
        AodScreenOffController.setOnlyChargingMode(false)
        AodScreenOffController.start(app)
        Shadows.shadowOf(app).clearNextStartedActivities()
    }

    @After
    fun tearDown() {
        AodScreenOffController.stop(app)
        Shadows.shadowOf(app).clearNextStartedActivities()
        ShadowSettings.setCanDrawOverlays(false)
    }

    @Test
    fun `power button instant lock launches AOD after initial 850ms settle`() {
        shadowPm.setIsInteractive(false)
        shadowKm.setIsKeyguardSecure(true)
        shadowKm.setKeyguardLocked(true)

        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        ShadowLooper.idleMainLooper(900, TimeUnit.MILLISECONDS)

        val started = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull("Expected AODDisplayActivity to launch when already locked", started)
        assertEquals(AODDisplayActivity::class.java.name, started?.component?.className)
        assertTrue(started?.getBooleanExtra(AODDisplayActivity.EXTRA_REQUIRE_KEYGUARD_LOCKED, false) == true)
    }

    @Test
    fun `auto screen timeout waits until keyguard actually locks before launching AOD`() {
        Settings.Secure.putLong(app.contentResolver, "lock_screen_lock_after_timeout", 5000L)
        shadowPm.setIsInteractive(false)
        shadowKm.setIsKeyguardSecure(true)
        shadowKm.setKeyguardLocked(false)

        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))

        // Must NOT launch before Keyguard locks (900ms, 2900ms, 4500ms)
        ShadowLooper.idleMainLooper(900, TimeUnit.MILLISECONDS)
        assertNull("Must not launch AOD at 900ms while Keyguard is unlocked", Shadows.shadowOf(app).nextStartedActivity)

        ShadowLooper.idleMainLooper(2000, TimeUnit.MILLISECONDS)
        assertNull("Must not launch AOD at 2900ms while Keyguard is unlocked", Shadows.shadowOf(app).nextStartedActivity)

        ShadowLooper.idleMainLooper(1600, TimeUnit.MILLISECONDS)
        assertNull("Must not launch AOD at 4500ms while Keyguard is unlocked", Shadows.shadowOf(app).nextStartedActivity)

        // Now Android's 5s lock-after-timeout fires and locks the Keyguard
        shadowKm.setKeyguardLocked(true)
        ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)

        val started = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull("Expected AODDisplayActivity to launch once Keyguard locked", started)
        assertEquals(AODDisplayActivity::class.java.name, started?.component?.className)
    }

    @Test
    fun `waking screen before delayed lock cancels AOD launch`() {
        Settings.Secure.putLong(app.contentResolver, "lock_screen_lock_after_timeout", 5000L)
        shadowPm.setIsInteractive(false)
        shadowKm.setIsKeyguardSecure(true)
        shadowKm.setKeyguardLocked(false)

        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)
        assertNull(Shadows.shadowOf(app).nextStartedActivity)

        // User wakes the phone before the 5s auto-lock timer expires
        shadowPm.setIsInteractive(true)
        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_ON))
        ShadowLooper.idleMainLooper()

        // Even if time passes and keyguard flag is toggled later, pending session must be cancelled
        shadowKm.setKeyguardLocked(true)
        ShadowLooper.idleMainLooper(5000, TimeUnit.MILLISECONDS)
        assertNull("Cancelled session must not launch AOD", Shadows.shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `long lock timeout hands off to AlarmManager and launches when alarm triggers after lock`() {
        Settings.Secure.putLong(app.contentResolver, "lock_screen_lock_after_timeout", 180_000L)
        shadowPm.setIsInteractive(false)
        shadowKm.setIsKeyguardSecure(true)
        shadowKm.setKeyguardLocked(false)

        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        ShadowLooper.idleMainLooper(1000, TimeUnit.MILLISECONDS)
        assertNull(Shadows.shadowOf(app).nextStartedActivity)

        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val scheduledAlarm = Shadows.shadowOf(am).nextScheduledAlarm
        assertNotNull("Expected AlarmManager wakeup for >65s lock timeout", scheduledAlarm)

        // Simulate alarm firing after device locks
        shadowKm.setKeyguardLocked(true)
        val alarmIntent = Shadows.shadowOf(scheduledAlarm!!.operation).savedIntent
        AodLockCheckReceiver().onReceive(app, alarmIntent)
        ShadowLooper.idleMainLooper(600, TimeUnit.MILLISECONDS)

        val started = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull("Expected AODDisplayActivity to launch when alarm sees locked keyguard", started)
        assertEquals(AODDisplayActivity::class.java.name, started?.component?.className)
    }

    @Test
    fun `without overlay permission full screen notification waits until keyguard locks`() {
        ShadowSettings.setCanDrawOverlays(false)
        Settings.Secure.putLong(app.contentResolver, "lock_screen_lock_after_timeout", 5000L)
        shadowPm.setIsInteractive(false)
        shadowKm.setIsKeyguardSecure(true)
        shadowKm.setKeyguardLocked(false)

        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)

        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        ShadowLooper.idleMainLooper(1200, TimeUnit.MILLISECONDS)
        assertNull("Full-screen trigger notification must not post while unlocked", shadowNm.getNotification(AodScreenOffController.TRIGGER_NOTIFICATION_ID))

        shadowKm.setKeyguardLocked(true)
        ShadowLooper.idleMainLooper(5000, TimeUnit.MILLISECONDS)
        assertNotNull("Full-screen trigger notification must post once locked", shadowNm.getNotification(AodScreenOffController.TRIGGER_NOTIFICATION_ID))
    }

    @Test
    fun `AODDisplayActivity finishes immediately if launched automatically while secured device is unlocked`() {
        shadowKm.setIsKeyguardSecure(true)
        shadowKm.setKeyguardLocked(false)

        val intent = Intent(app, AODDisplayActivity::class.java).apply {
            putExtra(AODDisplayActivity.EXTRA_REQUIRE_KEYGUARD_LOCKED, true)
        }
        val controller = Robolectric.buildActivity(AODDisplayActivity::class.java, intent).create()
        assertTrue("Activity must finish immediately if launched while secured and unlocked", controller.get().isFinishing)
    }

    @Test
    fun `power button wake handover launches AodWakeScreenActivity within 150ms when screen is off`() {
        shadowPm.setIsInteractive(false)
        AodScreenOffController.wakeLockScreenFromAod(app)

        ShadowLooper.idleMainLooper(150, TimeUnit.MILLISECONDS)
        val started = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull("Expected AodWakeScreenActivity to launch promptly after 120ms sleep settle", started)
        assertEquals(com.example.aod.AodWakeScreenActivity::class.java.name, started?.component?.className)
    }

    @Test
    fun `AodWakeScreenActivity completes reveal and finishes cleanly within 700ms`() {
        shadowPm.setIsInteractive(true)
        val controller = Robolectric.buildActivity(com.example.aod.AodWakeScreenActivity::class.java).setup()
        val activity = controller.get()

        ShadowLooper.idleMainLooper(700, TimeUnit.MILLISECONDS)
        assertTrue("AodWakeScreenActivity must finish cleanly after veil fade-out", activity.isFinishing)
    }
}
