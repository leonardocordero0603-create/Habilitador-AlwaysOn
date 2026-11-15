package com.example.aod

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.model.AodBackgroundStyle
import com.example.model.AodClockStyle
import com.example.model.AodPreferences
import com.example.model.AodThemePreset
import com.example.model.AodVerticalPosition

private data class StarPoint(val xFrac: Float, val yFrac: Float, val radiusScale: Float, val isAccent: Boolean)

private val AMBIENT_STARS = listOf(
    StarPoint(0.14f, 0.12f, 1.0f, true),
    StarPoint(0.28f, 0.08f, 0.75f, false),
    StarPoint(0.76f, 0.11f, 0.9f, false),
    StarPoint(0.88f, 0.19f, 1.1f, true),
    StarPoint(0.10f, 0.34f, 0.7f, false),
    StarPoint(0.91f, 0.37f, 0.8f, false),
    StarPoint(0.22f, 0.56f, 0.85f, true),
    StarPoint(0.82f, 0.61f, 0.75f, false),
    StarPoint(0.15f, 0.76f, 0.95f, false),
    StarPoint(0.69f, 0.74f, 0.8f, true),
    StarPoint(0.46f, 0.06f, 0.7f, false),
    StarPoint(0.55f, 0.83f, 0.85f, false)
)

@Composable
fun AodAmbientBackgroundLayer(
    backgroundStyle: AodBackgroundStyle,
    accent: Color,
    neonGlow: Boolean,
    verticalPosition: AodVerticalPosition,
    isPreview: Boolean,
    contentAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (size.minDimension <= 1f) return@Canvas
        val intensity = contentAlpha.coerceIn(0.45f, 1f)
        val clockYFrac = when (verticalPosition) {
            AodVerticalPosition.TOP -> if (isPreview) 0.31f else 0.27f
            AodVerticalPosition.CENTER -> 0.46f
            AodVerticalPosition.LOWER -> if (isPreview) 0.62f else 0.60f
        }

        when (backgroundStyle) {
            AodBackgroundStyle.PURE_BLACK -> Unit

            AodBackgroundStyle.TOP_AURORA -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to accent.copy(alpha = 0.24f * intensity),
                        0.28f to accent.copy(alpha = 0.09f * intensity),
                        0.62f to Color.Transparent
                    )
                )
            }

            AodBackgroundStyle.RADIAL_HALO -> {
                val haloCenter = Offset(size.width * 0.5f, size.height * clockYFrac)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.24f * intensity),
                            accent.copy(alpha = 0.07f * intensity),
                            Color.Transparent
                        ),
                        center = haloCenter,
                        radius = size.minDimension * 0.62f
                    ),
                    radius = size.minDimension * 0.62f,
                    center = haloCenter
                )
            }

            AodBackgroundStyle.EDGE_GLOW -> {
                val edgeWidth = if (isPreview) 16.dp.toPx() else 28.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.24f * intensity),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = edgeWidth
                    )
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.24f * intensity)
                        ),
                        startX = size.width - edgeWidth,
                        endX = size.width
                    )
                )
                val lineStroke = if (isPreview) 1.4.dp.toPx() else 2.2.dp.toPx()
                drawLine(
                    brush = Brush.verticalGradient(
                        0.08f to Color.Transparent,
                        0.45f to accent.copy(alpha = 0.72f * intensity),
                        0.92f to Color.Transparent
                    ),
                    start = Offset(lineStroke * 0.6f, 0f),
                    end = Offset(lineStroke * 0.6f, size.height),
                    strokeWidth = lineStroke
                )
                drawLine(
                    brush = Brush.verticalGradient(
                        0.08f to Color.Transparent,
                        0.45f to accent.copy(alpha = 0.72f * intensity),
                        0.92f to Color.Transparent
                    ),
                    start = Offset(size.width - lineStroke * 0.6f, 0f),
                    end = Offset(size.width - lineStroke * 0.6f, size.height),
                    strokeWidth = lineStroke
                )
            }

            AodBackgroundStyle.STARFIELD_MIST -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f * intensity),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = size.minDimension * 0.7f
                    )
                )
                val baseDot = if (isPreview) 1.2.dp.toPx() else 1.8.dp.toPx()
                AMBIENT_STARS.forEach { star ->
                    val center = Offset(size.width * star.xFrac, size.height * star.yFrac)
                    val color = if (star.isAccent) {
                        accent.copy(alpha = 0.62f * intensity)
                    } else {
                        Color.White.copy(alpha = 0.42f * intensity)
                    }
                    drawCircle(
                        color = color,
                        radius = baseDot * star.radiusScale,
                        center = center
                    )
                }
            }

            AodBackgroundStyle.HORIZON_WAVE -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        0.82f to accent.copy(alpha = 0.12f * intensity),
                        1.0f to accent.copy(alpha = 0.26f * intensity)
                    )
                )
                val horizonY = size.height * 0.80f
                drawLine(
                    brush = Brush.horizontalGradient(
                        0.0f to Color.Transparent,
                        0.5f to accent.copy(alpha = 0.55f * intensity),
                        1.0f to Color.Transparent
                    ),
                    start = Offset(0f, horizonY),
                    end = Offset(size.width, horizonY),
                    strokeWidth = if (isPreview) 1.dp.toPx() else 1.6.dp.toPx()
                )
            }
        }

        if (neonGlow) {
            val glowCenter = Offset(size.width * 0.5f, size.height * clockYFrac)
            val glowRadius = size.minDimension * 0.44f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.20f * intensity),
                        accent.copy(alpha = 0.05f * intensity),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = glowCenter
            )
        }
    }
}

@Composable
fun AodBackgroundThumbnail(
    backgroundStyle: AodBackgroundStyle,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AodAmbientBackgroundLayer(
            backgroundStyle = backgroundStyle,
            accent = accent,
            neonGlow = false,
            verticalPosition = AodVerticalPosition.CENTER,
            isPreview = true,
            contentAlpha = 1f,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AodThemePresetThumbnail(
    preset: AodThemePreset,
    modifier: Modifier = Modifier
) {
    AodLiveThemeThumbnail(
        clockStyle = preset.style,
        backgroundStyle = preset.background,
        accentColor = preset.accent.color,
        fontFamily = preset.font.fontFamily,
        neonGlow = preset.neonGlow,
        modifier = modifier
    )
}

@Composable
fun AodLiveThemeMiniPreview(
    preferences: AodPreferences,
    modifier: Modifier = Modifier
) {
    AodLiveThemeThumbnail(
        clockStyle = preferences.clockStyle,
        backgroundStyle = preferences.backgroundStyle,
        accentColor = preferences.accentColor.color,
        fontFamily = preferences.clockFont.fontFamily,
        neonGlow = preferences.neonGlowEnabled,
        modifier = modifier
    )
}

@Composable
private fun AodLiveThemeThumbnail(
    clockStyle: AodClockStyle,
    backgroundStyle: AodBackgroundStyle,
    accentColor: Color,
    fontFamily: FontFamily,
    neonGlow: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AodAmbientBackgroundLayer(
            backgroundStyle = backgroundStyle,
            accent = accentColor,
            neonGlow = neonGlow,
            verticalPosition = AodVerticalPosition.CENTER,
            isPreview = true,
            contentAlpha = 1f,
            modifier = Modifier.fillMaxSize()
        )
        AodStyleThumbnail(
            style = clockStyle,
            accentColor = accentColor,
            fontFamily = fontFamily,
            backgroundColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        )
    }
}
