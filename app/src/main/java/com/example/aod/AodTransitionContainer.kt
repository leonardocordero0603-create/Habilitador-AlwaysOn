package com.example.aod

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.AodTransitionStyle
import kotlin.math.hypot
import kotlin.math.sin

private class CircularRevealShape(private val progress: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val p = progress.coerceIn(0f, 1f)
        if (p >= 0.999f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val maxRadius = hypot(size.width / 2f, size.height / 2f)
        val radius = (maxRadius * p).coerceAtLeast(0.5f)
        val path = Path().apply {
            addOval(
                Rect(
                    left = size.width / 2f - radius,
                    top = size.height / 2f - radius,
                    right = size.width / 2f + radius,
                    bottom = size.height / 2f + radius
                )
            )
        }
        return Outline.Generic(path)
    }
}

private class VerticalCurtainShape(private val progress: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val p = progress.coerceIn(0f, 1f)
        if (p >= 0.999f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val h = (size.height * p).coerceAtLeast(0.5f)
        return Outline.Rectangle(Rect(0f, 0f, size.width, h))
    }
}

/**
 * Choreography wrapper delivering Samsung Good Lock / One UI inspired OLED entry & exit
 * transitions for both the full-screen Always On display and the live mockup preview.
 */
@Composable
fun AodTransitionContainer(
    visible: Boolean,
    transitionStyle: AodTransitionStyle,
    accentColor: Color,
    replayTrigger: Int = 0,
    animateOnInitialShow: Boolean = true,
    exitDurationMs: Int = 320,
    onExitAnimationFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val startProgress = if (visible && !animateOnInitialShow && replayTrigger == 0) 1f else 0f
    val progress = remember { Animatable(startProgress) }
    val glowPulse = remember { Animatable(0f) }
    var hasBeenVisible by remember { mutableStateOf(visible) }
    var skipInitialEntry by remember { mutableStateOf(visible && !animateOnInitialShow && replayTrigger == 0) }
    val currentStyle by rememberUpdatedState(transitionStyle)

    LaunchedEffect(visible, replayTrigger) {
        if (visible) {
            hasBeenVisible = true
            if (skipInitialEntry) {
                skipInitialEntry = false
                return@LaunchedEffect
            }
            progress.snapTo(0f)
            glowPulse.snapTo(0f)
            when (currentStyle) {
                AodTransitionStyle.ONE_UI_ZOOM -> {
                    glowPulse.animateTo(1f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                    progress.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    glowPulse.animateTo(0f, animationSpec = tween(260, easing = LinearOutSlowInEasing))
                }
                AodTransitionStyle.WONDERLAND_HALO -> {
                    glowPulse.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                    progress.animateTo(1f, animationSpec = tween(480, easing = FastOutSlowInEasing))
                    glowPulse.animateTo(0f, animationSpec = tween(320, easing = LinearOutSlowInEasing))
                }
                AodTransitionStyle.IRIS_PORTAL -> {
                    glowPulse.snapTo(1f)
                    progress.animateTo(1f, animationSpec = tween(460, easing = FastOutSlowInEasing))
                    glowPulse.animateTo(0f, animationSpec = tween(200, easing = LinearOutSlowInEasing))
                }
                AodTransitionStyle.CURTAIN_SWEEP -> {
                    glowPulse.snapTo(1f)
                    progress.animateTo(1f, animationSpec = tween(470, easing = FastOutSlowInEasing))
                    glowPulse.animateTo(0f, animationSpec = tween(180, easing = LinearOutSlowInEasing))
                }
                AodTransitionStyle.SOFT_FADE -> {
                    progress.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
                }
            }
        } else if (hasBeenVisible) {
            val dur = exitDurationMs.coerceIn(180, 420)
            when (currentStyle) {
                AodTransitionStyle.ONE_UI_ZOOM -> {
                    glowPulse.snapTo(0.85f)
                    progress.animateTo(0f, animationSpec = tween(dur, easing = FastOutLinearInEasing))
                    glowPulse.snapTo(0f)
                }
                AodTransitionStyle.WONDERLAND_HALO -> {
                    glowPulse.snapTo(1f)
                    progress.animateTo(0f, animationSpec = tween(dur, easing = FastOutSlowInEasing))
                    glowPulse.snapTo(0f)
                }
                AodTransitionStyle.IRIS_PORTAL -> {
                    glowPulse.snapTo(1f)
                    progress.animateTo(0f, animationSpec = tween(dur, easing = FastOutLinearInEasing))
                    glowPulse.snapTo(0f)
                }
                AodTransitionStyle.CURTAIN_SWEEP -> {
                    glowPulse.snapTo(1f)
                    progress.animateTo(0f, animationSpec = tween(dur, easing = FastOutLinearInEasing))
                    glowPulse.snapTo(0f)
                }
                AodTransitionStyle.SOFT_FADE -> {
                    progress.animateTo(0f, animationSpec = tween(dur, easing = LinearOutSlowInEasing))
                }
            }
            onExitAnimationFinished()
        }
    }

    val p = progress.value.coerceIn(0f, 1.08f)
    val pClamped = p.coerceIn(0f, 1f)
    val glow = glowPulse.value.coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        val layerModifier = when (transitionStyle) {
            AodTransitionStyle.ONE_UI_ZOOM -> {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = if (visible) {
                            0.84f + 0.16f * p
                        } else {
                            1.12f - 0.12f * pClamped
                        }
                        val shiftDp = if (visible) {
                            (1f - p) * 28.dp.toPx()
                        } else {
                            -(1f - pClamped) * 34.dp.toPx()
                        }
                        scaleX = scale
                        scaleY = scale
                        translationY = shiftDp
                        alpha = pClamped
                    }
            }
            AodTransitionStyle.WONDERLAND_HALO -> {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = if (visible) {
                            0.90f + 0.10f * pClamped
                        } else {
                            0.86f + 0.14f * pClamped
                        }
                        scaleX = scale
                        scaleY = scale
                        alpha = pClamped
                    }
            }
            AodTransitionStyle.IRIS_PORTAL -> {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 0.94f + 0.06f * pClamped
                        scaleX = scale
                        scaleY = scale
                        alpha = (pClamped * 1.35f).coerceIn(0f, 1f)
                        clip = true
                        shape = CircularRevealShape(pClamped)
                    }
            }
            AodTransitionStyle.CURTAIN_SWEEP -> {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = if (visible) {
                            -(1f - pClamped) * 14.dp.toPx()
                        } else {
                            -(1f - pClamped) * 18.dp.toPx()
                        }
                        alpha = (pClamped * 1.25f).coerceIn(0f, 1f)
                        clip = true
                        shape = VerticalCurtainShape(pClamped)
                    }
            }
            AodTransitionStyle.SOFT_FADE -> {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = pClamped
                    }
            }
        }

        Box(modifier = layerModifier) {
            content()
        }

        // Good Lock / Wonderland OLED light accent overlay (pure GPU draw, zero background bleed)
        if (transitionStyle != AodTransitionStyle.SOFT_FADE && (glow > 0.01f || (pClamped > 0.02f && pClamped < 0.98f))) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.42f)
                val maxR = hypot(size.width, size.height) * 0.55f

                when (transitionStyle) {
                    AodTransitionStyle.ONE_UI_ZOOM -> {
                        val auraAlpha = (0.20f * glow * sin(pClamped * Math.PI).toFloat().coerceAtLeast(0.25f)).coerceIn(0f, 0.24f)
                        if (auraAlpha > 0.005f) {
                            val r = (maxR * (0.35f + 0.45f * pClamped)).coerceAtLeast(1f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(accentColor.copy(alpha = auraAlpha), Color.Transparent),
                                    center = center,
                                    radius = r
                                ),
                                radius = r,
                                center = center
                            )
                        }
                    }
                    AodTransitionStyle.WONDERLAND_HALO -> {
                        val wave = sin(pClamped * Math.PI).toFloat().coerceIn(0f, 1f)
                        val rOuter = (maxR * (0.18f + 0.92f * pClamped)).coerceAtLeast(1f)
                        val rInner = (maxR * (0.08f + 0.58f * pClamped)).coerceAtLeast(1f)
                        if (wave > 0.01f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.28f * wave),
                                        accentColor.copy(alpha = 0.10f * wave),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = rOuter
                                ),
                                radius = rOuter,
                                center = center
                            )
                            drawCircle(
                                color = accentColor.copy(alpha = 0.45f * wave),
                                radius = rInner,
                                center = center,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    }
                    AodTransitionStyle.IRIS_PORTAL -> {
                        if (pClamped in 0.02f..0.98f) {
                            val r = (hypot(size.width / 2f, size.height / 2f) * pClamped).coerceAtLeast(1f)
                            drawCircle(
                                color = accentColor.copy(alpha = 0.58f * (1f - pClamped * 0.3f)),
                                radius = r,
                                center = Offset(size.width / 2f, size.height / 2f),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    }
                    AodTransitionStyle.CURTAIN_SWEEP -> {
                        if (pClamped in 0.02f..0.98f) {
                            val sweepY = size.height * pClamped
                            val beamHeight = 28.dp.toPx()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        accentColor.copy(alpha = 0.34f),
                                        Color.Transparent
                                    ),
                                    startY = sweepY - beamHeight,
                                    endY = sweepY + 4.dp.toPx()
                                ),
                                topLeft = Offset(0f, (sweepY - beamHeight).coerceAtLeast(0f)),
                                size = Size(size.width, beamHeight + 4.dp.toPx())
                            )
                            drawLine(
                                color = accentColor.copy(alpha = 0.72f),
                                start = Offset(0f, sweepY),
                                end = Offset(size.width, sweepY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                    AodTransitionStyle.SOFT_FADE -> Unit
                }
            }
        }
    }
}
