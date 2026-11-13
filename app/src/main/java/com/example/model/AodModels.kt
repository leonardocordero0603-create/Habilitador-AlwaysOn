package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlayfairFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

enum class AodClockStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val supportsAlignment: Boolean = true
) {
    STACKED_ONE_UI(
        id = "stacked_one_ui",
        title = "Apilado Clásico",
        subtitle = "Hora vertical fluida"
    ),
    DIGITAL_MINIMAL(
        id = "digital_minimal",
        title = "Digital",
        subtitle = "Clásico horizontal"
    ),
    TYPOGRAPHIC_BOLD(
        id = "typographic_bold",
        title = "Editorial Dinámico",
        subtitle = "Números grandes dinámicos"
    ),
    POSTER_TALL(
        id = "poster_tall",
        title = "Póster Alto",
        subtitle = "Números altos de impacto"
    ),
    DUO_CAPSULE(
        id = "duo_capsule",
        title = "Cápsula Dúo",
        subtitle = "Bloques contrastados OLED"
    ),
    ORBIT_CHRONO(
        id = "orbit_chrono",
        title = "Orbital",
        subtitle = "Anillos de progreso en vivo",
        supportsAlignment = false
    ),
    DOT_MATRIX(
        id = "dot_matrix",
        title = "Matriz LED",
        subtitle = "Estilo puntos retro OLED"
    ),
    RETRO_FLIP(
        id = "retro_flip",
        title = "Tarjetas Flip",
        subtitle = "Reloj de solapas clásico"
    ),
    EDGE_NEON(
        id = "edge_neon",
        title = "Borde Edge",
        subtitle = "Barra luminosa lateral"
    ),
    ANALOG_GALAXY(
        id = "analog_galaxy",
        title = "Analógico",
        subtitle = "Esfera de precisión",
        supportsAlignment = false
    ),
    ANALOG_NUMBERS(
        id = "analog_numbers",
        title = "Esfera 12·6",
        subtitle = "Analógico minimalista",
        supportsAlignment = false
    ),
    ROMAN_CLASSIC(
        id = "roman_classic",
        title = "Romano",
        subtitle = "Esfera clásica XII·VI",
        supportsAlignment = false
    );

    companion object {
        fun fromId(id: String?): AodClockStyle =
            entries.firstOrNull { it.id == id } ?: STACKED_ONE_UI
    }
}

enum class AodClockFont(
    val id: String,
    val label: String,
    val fontFamily: FontFamily
) {
    ONE_UI_ROUNDED("one_ui_rounded", "Redondeada", OutfitFontFamily),
    GEOMETRIC("geometric", "Moderna", SpaceGroteskFontFamily),
    ELEGANT_SERIF("elegant_serif", "Elegante", PlayfairFontFamily),
    TECH_MONO("tech_mono", "Técnica", JetBrainsMonoFontFamily),
    POSTER_CONDENSED("poster_condensed", "Póster", BebasNeueFontFamily),
    CYBER_FUTURA("cyber_futura", "Futurista", OrbitronFontFamily);

    companion object {
        fun fromId(id: String?): AodClockFont =
            entries.firstOrNull { it.id == id } ?: ONE_UI_ROUNDED
    }
}

enum class AodAccentColor(
    val id: String,
    val label: String,
    val color: Color
) {
    PURE_WHITE("pure_white", "Blanco Puro", Color(0xFFF8FAFC)),
    CYAN_ICE("cyan_ice", "Cian Hielo", Color(0xFF38BDF8)),
    SAPPHIRE("sapphire", "Zafiro", Color(0xFF60A5FA)),
    AQUA_TEAL("aqua_teal", "Turquesa", Color(0xFF2DD4BF)),
    MINT_GREEN("emerald", "Menta", Color(0xFF34D399)),
    LIME_NEON("lime_neon", "Lima Neón", Color(0xFFA3E635)),
    AMBER_GOLD("amber_gold", "Ámbar", Color(0xFFFBBF24)),
    SOLAR_ORANGE("solar_orange", "Naranja", Color(0xFFFB923C)),
    CORAL_ROSE("coral_rose", "Coral", Color(0xFFFB7185)),
    CRIMSON_RED("crimson_red", "Carmesí", Color(0xFFF87171)),
    LAVENDER("lavender", "Lavanda", Color(0xFFC4B5FD)),
    AURORA_MAGENTA("aurora_magenta", "Magenta", Color(0xFFE879F9));

    companion object {
        fun fromId(id: String?): AodAccentColor =
            entries.firstOrNull { it.id == id } ?: CYAN_ICE
    }
}

enum class AodTransitionStyle(
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String
) {
    ONE_UI_ZOOM(
        id = "one_ui_zoom",
        title = "One UI Elevación",
        shortTitle = "Elevación",
        subtitle = "Zoom y elevación suave estilo Good Lock"
    ),
    WONDERLAND_HALO(
        id = "wonderland_halo",
        title = "Halo Wonderland",
        shortTitle = "Wonderland",
        subtitle = "Aura radial OLED al entrar y salir"
    ),
    IRIS_PORTAL(
        id = "iris_portal",
        title = "Portal Iris",
        shortTitle = "Iris",
        subtitle = "Apertura y cierre circular luminoso"
    ),
    CURTAIN_SWEEP(
        id = "curtain_sweep",
        title = "Cortina Luminosa",
        shortTitle = "Cortina",
        subtitle = "Barrido vertical con haz de acento"
    ),
    SOFT_FADE(
        id = "soft_fade",
        title = "Desvanecimiento",
        shortTitle = "Suave",
        subtitle = "Fundido discreto sobre negro puro"
    );

    companion object {
        fun fromId(id: String?): AodTransitionStyle =
            entries.firstOrNull { it.id == id } ?: ONE_UI_ZOOM
    }
}

enum class AodThemePreset(
    val id: String,
    val title: String,
    val tag: String,
    val style: AodClockStyle,
    val font: AodClockFont,
    val accent: AodAccentColor,
    val alignLeft: Boolean = false,
    val showSeconds: Boolean = false
) {
    GALAXY_S23(
        id = "galaxy_s23",
        title = "Estilo Estelar",
        tag = "Moderna",
        style = AodClockStyle.STACKED_ONE_UI,
        font = AodClockFont.ONE_UI_ROUNDED,
        accent = AodAccentColor.CYAN_ICE
    ),
    CYBER_ORBIT(
        id = "cyber_orbit",
        title = "Orbital Pro",
        tag = "Futurista",
        style = AodClockStyle.ORBIT_CHRONO,
        font = AodClockFont.CYBER_FUTURA,
        accent = AodAccentColor.AQUA_TEAL,
        showSeconds = true
    ),
    NOTHING_LED(
        id = "nothing_led",
        title = "Matriz LED",
        tag = "Retro Dot",
        style = AodClockStyle.DOT_MATRIX,
        font = AodClockFont.TECH_MONO,
        accent = AodAccentColor.PURE_WHITE
    ),
    POSTER_EDITORIAL(
        id = "poster_editorial",
        title = "Póster Oro",
        tag = "Editorial",
        style = AodClockStyle.POSTER_TALL,
        font = AodClockFont.POSTER_CONDENSED,
        accent = AodAccentColor.AMBER_GOLD,
        alignLeft = true
    ),
    LUXURY_ROMAN(
        id = "luxury_roman",
        title = "Clásico XIX",
        tag = "Lujo",
        style = AodClockStyle.ROMAN_CLASSIC,
        font = AodClockFont.ELEGANT_SERIF,
        accent = AodAccentColor.SOLAR_ORANGE
    ),
    RETRO_STUDIO(
        id = "retro_studio",
        title = "Estudio Flip",
        tag = "Clásico",
        style = AodClockStyle.RETRO_FLIP,
        font = AodClockFont.GEOMETRIC,
        accent = AodAccentColor.CORAL_ROSE
    ),
    NEON_NIGHT(
        id = "neon_night",
        title = "Neón Night",
        tag = "Cyber",
        style = AodClockStyle.EDGE_NEON,
        font = AodClockFont.CYBER_FUTURA,
        accent = AodAccentColor.AURORA_MAGENTA,
        alignLeft = true,
        showSeconds = true
    ),
    DUO_MINIMAL(
        id = "duo_minimal",
        title = "Cápsula Zen",
        tag = "Minimal",
        style = AodClockStyle.DUO_CAPSULE,
        font = AodClockFont.ONE_UI_ROUNDED,
        accent = AodAccentColor.MINT_GREEN
    );

    fun matches(prefs: AodPreferences): Boolean =
        prefs.clockStyle == style &&
            prefs.clockFont == font &&
            prefs.accentColor == accent &&
            (!style.supportsAlignment || prefs.alignLeft == alignLeft)
}

data class AodPreferences(
    val clockStyle: AodClockStyle = AodClockStyle.STACKED_ONE_UI,
    val clockFont: AodClockFont = AodClockFont.ONE_UI_ROUNDED,
    val accentColor: AodAccentColor = AodAccentColor.CYAN_ICE,
    val clockScale: Float = 1.0f,
    val alignLeft: Boolean = false,
    val brightnessLevel: Float = 0.14f,
    val burnInProtection: Boolean = true,
    val use24HourFormat: Boolean = true,
    val showBattery: Boolean = true,
    val showDate: Boolean = true,
    val showSeconds: Boolean = false,
    val customLabel: String = "",
    val autoAodOnScreenOff: Boolean = false,
    val autoAodOnlyCharging: Boolean = false,
    val pocketModeEnabled: Boolean = true,
    val showNotifications: Boolean = true,
    val showMusicControls: Boolean = true,
    val transitionStyle: AodTransitionStyle = AodTransitionStyle.ONE_UI_ZOOM,
    val isProUnlocked: Boolean = false
)

enum class DeviceCompatibilityTier {
    SAMSUNG_NATIVE_READY,
    SAMSUNG_CORE_UNIVERSAL,
    ANDROID_UNIVERSAL
}

data class SamsungDeviceStatus(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val oneUiVersion: String?,
    val hasSamsungAodPackage: Boolean,
    val nativeAodMode: Int,
    val tapToShowMode: Int,
    val dozeAlwaysOn: Int,
    val canWriteSettings: Boolean,
    val hasWriteSecureSettings: Boolean,
    val hasPostNotificationsPermission: Boolean,
    val hasNotificationListenerPermission: Boolean,
    val hasFullScreenIntentPermission: Boolean,
    val hasOverlayPermission: Boolean,
    val hasProximitySensor: Boolean,
    val autoServiceRunning: Boolean,
    val isBatteryUnrestricted: Boolean,
    val tier: DeviceCompatibilityTier,
    val batteryPercent: Int?,
    val isCharging: Boolean
) {
    val isSamsungDevice: Boolean
        get() = manufacturer.equals("samsung", ignoreCase = true) ||
            brand.equals("samsung", ignoreCase = true)

    val isNativeAodEnabled: Boolean
        get() = nativeAodMode == 1 || dozeAlwaysOn == 1
}

sealed interface AodActionResult {
    data class Success(val message: String) : AodActionResult
    data object RequiresWriteSettingsPermission : AodActionResult
    data class RequiresNativeOrAdb(val reason: String) : AodActionResult
    data class Error(val message: String) : AodActionResult
}
