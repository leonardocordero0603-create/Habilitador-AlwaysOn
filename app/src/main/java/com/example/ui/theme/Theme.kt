package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = ElectricSapphire,
    onPrimary = Color.White,
    primaryContainer = ElectricSapphireContainer,
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = IceCyan,
    onSecondary = Color(0xFF042230),
    secondaryContainer = IceCyanContainer,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = EmeraldActive,
    onTertiary = Color(0xFF032117),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = OledObsidian,
    onBackground = TextPrimaryDark,
    surface = OledSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = OledSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = OledBlack,
    surfaceContainerLow = OledObsidian,
    surfaceContainer = OledSurface,
    surfaceContainerHigh = OledSurfaceElevated,
    surfaceContainerHighest = OledSurfaceBright,
    outline = OledOutline,
    outlineVariant = OledOutlineSubtle,
    error = CoralAlert,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFFFECDD3)
)

private val DaylightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF059669),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun AlwaysOnTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ObsidianDarkColorScheme
        else -> DaylightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
