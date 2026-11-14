package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AodAccentColor
import com.example.model.AodClockFont
import com.example.model.AodClockStyle
import com.example.model.AodPreferences
import com.example.model.AodThemePreset
import com.example.model.AodTransitionStyle
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.aodDataStore: DataStore<Preferences> by preferencesDataStore(name = "aod_preferences")

class AodPreferencesRepository(private val context: Context) {

    private object Keys {
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val CLOCK_FONT = stringPreferencesKey("clock_font")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val CLOCK_SCALE = floatPreferencesKey("clock_scale")
        val ALIGN_LEFT = booleanPreferencesKey("align_left")
        val BRIGHTNESS_LEVEL = floatPreferencesKey("brightness_level")
        val BURN_IN_PROTECTION = booleanPreferencesKey("burn_in_protection")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val CUSTOM_LABEL = stringPreferencesKey("custom_label")
        val AUTO_AOD_ON_SCREEN_OFF = booleanPreferencesKey("auto_aod_on_screen_off")
        val AUTO_AOD_ONLY_CHARGING = booleanPreferencesKey("auto_aod_only_charging")
        val POCKET_MODE_ENABLED = booleanPreferencesKey("pocket_mode_enabled")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val SHOW_MUSIC_CONTROLS = booleanPreferencesKey("show_music_controls")
        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
        val IS_PRO_UNLOCKED = booleanPreferencesKey("is_pro_unlocked")
    }

    private val quickCache by lazy {
        runCatching {
            context.applicationContext.getSharedPreferences("aod_quick_cache", Context.MODE_PRIVATE)
        }.getOrNull()
    }

    val preferencesFlow: Flow<AodPreferences> = context.aodDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                emit(emptyPreferences())
            }
        }
        .map { prefs ->
            val autoEnabled = prefs[Keys.AUTO_AOD_ON_SCREEN_OFF] ?: false
            val onlyCharging = prefs[Keys.AUTO_AOD_ONLY_CHARGING] ?: false
            val brightness = (prefs[Keys.BRIGHTNESS_LEVEL] ?: 0.14f).coerceIn(0.03f, 0.45f)
            try {
                val cache = quickCache
                if (cache != null && (
                        cache.getBoolean("auto_aod_on_screen_off", false) != autoEnabled ||
                            cache.getBoolean("auto_aod_only_charging", false) != onlyCharging ||
                            cache.getFloat("brightness_level", 0.14f) != brightness
                        )
                ) {
                    cache.edit()
                        .putBoolean("auto_aod_on_screen_off", autoEnabled)
                        .putBoolean("auto_aod_only_charging", onlyCharging)
                        .putFloat("brightness_level", brightness)
                        .apply()
                }
            } catch (_: Throwable) {
                // Ignore cache update issues
            }
        AodPreferences(
            clockStyle = AodClockStyle.fromId(prefs[Keys.CLOCK_STYLE]),
            clockFont = AodClockFont.fromId(prefs[Keys.CLOCK_FONT]),
            accentColor = AodAccentColor.fromId(prefs[Keys.ACCENT_COLOR]),
            clockScale = (prefs[Keys.CLOCK_SCALE] ?: 1.0f).coerceIn(0.80f, 1.22f),
            alignLeft = prefs[Keys.ALIGN_LEFT] ?: false,
            brightnessLevel = brightness,
            burnInProtection = prefs[Keys.BURN_IN_PROTECTION] ?: true,
            use24HourFormat = prefs[Keys.USE_24_HOUR_FORMAT] ?: true,
            showBattery = prefs[Keys.SHOW_BATTERY] ?: true,
            showDate = prefs[Keys.SHOW_DATE] ?: true,
            showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
            customLabel = prefs[Keys.CUSTOM_LABEL] ?: "",
            autoAodOnScreenOff = autoEnabled,
            autoAodOnlyCharging = onlyCharging,
            pocketModeEnabled = prefs[Keys.POCKET_MODE_ENABLED] ?: true,
            showNotifications = prefs[Keys.SHOW_NOTIFICATIONS] ?: true,
            showMusicControls = prefs[Keys.SHOW_MUSIC_CONTROLS] ?: true,
            transitionStyle = AodTransitionStyle.fromId(prefs[Keys.TRANSITION_STYLE]),
            isProUnlocked = prefs[Keys.IS_PRO_UNLOCKED] ?: false
        )
    }.flowOn(Dispatchers.IO)

    fun getCachedBrightness(): Float = runCatching {
        quickCache?.getFloat("brightness_level", 0.14f)
    }.getOrNull()?.coerceIn(0.03f, 0.45f) ?: 0.14f

    fun getCachedAutoAodEnabled(): Boolean = runCatching {
        quickCache?.getBoolean("auto_aod_on_screen_off", false)
    }.getOrNull() ?: false

    fun getCachedAutoAodOnlyCharging(): Boolean = runCatching {
        quickCache?.getBoolean("auto_aod_only_charging", false)
    }.getOrNull() ?: false

    suspend fun updateClockStyle(style: AodClockStyle) {
        context.aodDataStore.edit { it[Keys.CLOCK_STYLE] = style.id }
    }

    suspend fun updateClockFont(font: AodClockFont) {
        context.aodDataStore.edit { it[Keys.CLOCK_FONT] = font.id }
    }

    suspend fun updateAccentColor(accentColor: AodAccentColor) {
        context.aodDataStore.edit { it[Keys.ACCENT_COLOR] = accentColor.id }
    }

    suspend fun applyThemePreset(preset: AodThemePreset) {
        context.aodDataStore.edit {
            it[Keys.CLOCK_STYLE] = preset.style.id
            it[Keys.CLOCK_FONT] = preset.font.id
            it[Keys.ACCENT_COLOR] = preset.accent.id
            if (preset.style.supportsAlignment) {
                it[Keys.ALIGN_LEFT] = preset.alignLeft
            }
            it[Keys.SHOW_SECONDS] = preset.showSeconds
        }
    }

    suspend fun updateClockScale(scale: Float) {
        context.aodDataStore.edit { it[Keys.CLOCK_SCALE] = scale.coerceIn(0.80f, 1.22f) }
    }

    suspend fun updateAlignLeft(alignLeft: Boolean) {
        context.aodDataStore.edit { it[Keys.ALIGN_LEFT] = alignLeft }
    }

    suspend fun updateBrightness(level: Float) {
        val clamped = level.coerceIn(0.03f, 0.45f)
        runCatching { quickCache?.edit()?.putFloat("brightness_level", clamped)?.apply() }
        context.aodDataStore.edit { it[Keys.BRIGHTNESS_LEVEL] = clamped }
    }

    suspend fun updateBurnInProtection(enabled: Boolean) {
        context.aodDataStore.edit { it[Keys.BURN_IN_PROTECTION] = enabled }
    }

    suspend fun updateUse24HourFormat(enabled: Boolean) {
        context.aodDataStore.edit { it[Keys.USE_24_HOUR_FORMAT] = enabled }
    }

    suspend fun updateShowBattery(enabled: Boolean) {
        context.aodDataStore.edit { it[Keys.SHOW_BATTERY] = enabled }
    }

    suspend fun updateShowDate(enabled: Boolean) {
        context.aodDataStore.edit { it[Keys.SHOW_DATE] = enabled }
    }

    suspend fun updateShowSeconds(enabled: Boolean) {
        context.aodDataStore.edit { it[Keys.SHOW_SECONDS] = enabled }
    }

    suspend fun updateCustomLabel(label: String) {
        context.aodDataStore.edit { it[Keys.CUSTOM_LABEL] = label.take(32) }
    }

    suspend fun updateAutoAodOnScreenOff(enabled: Boolean) {
        runCatching { quickCache?.edit()?.putBoolean("auto_aod_on_screen_off", enabled)?.apply() }
        context.aodDataStore.edit { it[Keys.AUTO_AOD_ON_SCREEN_OFF] = enabled }
    }

    suspend fun updateAutoAodOnlyCharging(enabled: Boolean) {
        runCatching { quickCache?.edit()?.putBoolean("auto_aod_only_charging", enabled)?.apply() }
        context.aodDataStore.edit { it[Keys.AUTO_AOD_ONLY_CHARGING] = enabled }
    }

    suspend fun updatePocketModeEnabled(enabled: Boolean) {
        context.aodDataStore.edit { it[Keys.POCKET_MODE_ENABLED] = enabled }
    }

    suspend fun updateShowNotifications(enabled: Boolean) {
        context.aodDataStore.edit { it[Keys.SHOW_NOTIFICATIONS] = enabled }
    }

    suspend fun updateShowMusicControls(enabled: Boolean) {
        context.aodDataStore.edit { it[Keys.SHOW_MUSIC_CONTROLS] = enabled }
    }

    suspend fun updateTransitionStyle(style: AodTransitionStyle) {
        context.aodDataStore.edit { it[Keys.TRANSITION_STYLE] = style.id }
    }

    suspend fun updateProUnlocked(unlocked: Boolean) {
        context.aodDataStore.edit { it[Keys.IS_PRO_UNLOCKED] = unlocked }
    }
}
