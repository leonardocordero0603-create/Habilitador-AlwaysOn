package com.example.aod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AodClockStyle
import com.example.model.AodPreferences
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AodClockFaceContent(
    preferences: AodPreferences,
    displayHour: String,
    displayMinute: String,
    displaySecond: String,
    hourInt: Int,
    minuteInt: Int,
    secondInt: Int,
    amPm: String,
    accent: Color,
    clockFont: FontFamily,
    scale: Float,
    isPreview: Boolean,
    horizontalAlign: Alignment.Horizontal
) {
    when (preferences.clockStyle) {
        AodClockStyle.STACKED_ONE_UI -> {
            val numSize = ((if (isPreview) 50f else 82f) * scale).sp
            val lineH = ((if (isPreview) 50f else 82f) * scale).sp
            Text(
                text = displayHour,
                fontFamily = clockFont,
                fontWeight = FontWeight.Bold,
                fontSize = numSize,
                lineHeight = lineH,
                color = accent,
                letterSpacing = (-2).sp
            )
            Text(
                text = displayMinute,
                fontFamily = clockFont,
                fontWeight = FontWeight.Bold,
                fontSize = numSize,
                lineHeight = lineH,
                color = Color.White,
                letterSpacing = (-2).sp
            )
            if (preferences.showSeconds || amPm.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        if (preferences.showSeconds) ":$displaySecond" else null,
                        amPm.ifEmpty { null }
                    ).joinToString(" "),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (isPreview) 11.sp else 15.sp,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }

        AodClockStyle.DIGITAL_MINIMAL -> {
            val numSize = ((if (isPreview) 42f else 68f) * scale).sp
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$displayHour:$displayMinute",
                    fontFamily = clockFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = numSize,
                    color = accent,
                    letterSpacing = (-1.5).sp
                )
                if (preferences.showSeconds) {
                    Text(
                        text = ":$displaySecond",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = ((if (isPreview) 14f else 22f) * scale).sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = if (isPreview) 6.dp else 10.dp, start = 4.dp)
                    )
                }
                if (amPm.isNotEmpty()) {
                    Text(
                        text = amPm,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isPreview) 11.sp else 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = if (isPreview) 7.dp else 12.dp, start = 5.dp)
                    )
                }
            }
        }

        AodClockStyle.TYPOGRAPHIC_BOLD -> {
            val numSize = ((if (isPreview) 54f else 90f) * scale).sp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayHour,
                    fontFamily = clockFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = numSize,
                    color = Color.White,
                    letterSpacing = (-3).sp
                )
                Text(
                    text = ":",
                    fontFamily = clockFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = ((if (isPreview) 44f else 74f) * scale).sp,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Text(
                    text = displayMinute,
                    fontFamily = clockFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = numSize,
                    color = accent,
                    letterSpacing = (-3).sp
                )
            }
            if (preferences.showSeconds || amPm.isNotEmpty()) {
                Text(
                    text = listOfNotNull(
                        if (preferences.showSeconds) "${displaySecond}s" else null,
                        amPm.ifEmpty { null }
                    ).joinToString(" · "),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (isPreview) 11.sp else 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        AodClockStyle.POSTER_TALL -> {
            val numSize = ((if (isPreview) 58f else 98f) * scale).sp
            val lineH = ((if (isPreview) 52f else 88f) * scale).sp
            Column(horizontalAlignment = horizontalAlign) {
                Text(
                    text = displayHour,
                    fontFamily = clockFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = numSize,
                    lineHeight = lineH,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .padding(vertical = if (isPreview) 2.dp else 4.dp)
                        .width(((if (isPreview) 44f else 74f) * scale).dp)
                        .height(if (isPreview) 2.5.dp else 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                )
                Text(
                    text = displayMinute,
                    fontFamily = clockFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = numSize,
                    lineHeight = lineH,
                    color = accent,
                    letterSpacing = 1.sp
                )
                if (preferences.showSeconds || amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listOfNotNull(
                            if (preferences.showSeconds) "$displaySecond SEG" else null,
                            amPm.ifEmpty { null }
                        ).joinToString(" · "),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isPreview) 10.sp else 13.sp,
                        color = Color.White.copy(alpha = 0.74f)
                    )
                }
            }
        }

        AodClockStyle.DUO_CAPSULE -> {
            val numSize = ((if (isPreview) 30f else 48f) * scale).sp
            val pillW = ((if (isPreview) 84f else 138f) * scale).dp
            val corner = (if (isPreview) 18.dp else 26.dp)
            Column(
                horizontalAlignment = horizontalAlign,
                verticalArrangement = Arrangement.spacedBy(if (isPreview) 5.dp else 8.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(corner),
                    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.75f)),
                    modifier = Modifier.width(pillW)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = if (isPreview) 4.dp else 8.dp)
                    ) {
                        Text(
                            text = displayHour,
                            fontFamily = clockFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = numSize,
                            color = accent
                        )
                    }
                }
                Surface(
                    color = Color(0xFF0B0E14),
                    shape = RoundedCornerShape(corner),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                    modifier = Modifier.width(pillW)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = if (isPreview) 4.dp else 8.dp)
                    ) {
                        Text(
                            text = displayMinute,
                            fontFamily = clockFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = numSize,
                            color = Color.White
                        )
                    }
                }
                if (preferences.showSeconds || amPm.isNotEmpty()) {
                    Text(
                        text = listOfNotNull(
                            if (preferences.showSeconds) ":$displaySecond" else null,
                            amPm.ifEmpty { null }
                        ).joinToString(" "),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isPreview) 10.sp else 13.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
        }

        AodClockStyle.ORBIT_CHRONO -> {
            val dialSize = ((if (isPreview) 132f else 220f) * scale).dp
            OrbitChronoClockFace(
                displayHour = displayHour,
                displayMinute = displayMinute,
                displaySecond = displaySecond,
                minuteInt = minuteInt,
                secondInt = secondInt,
                showSeconds = preferences.showSeconds,
                amPm = amPm,
                accentColor = accent,
                fontFamily = clockFont,
                isPreview = isPreview,
                modifier = Modifier.size(dialSize)
            )
        }

        AodClockStyle.DOT_MATRIX -> {
            val matrixW = ((if (isPreview) 144f else 248f) * scale).dp
            val matrixH = ((if (isPreview) 44f else 76f) * scale).dp
            Column(horizontalAlignment = horizontalAlign) {
                DotMatrixClockFace(
                    timeText = "$displayHour:$displayMinute",
                    accentColor = accent,
                    modifier = Modifier.size(width = matrixW, height = matrixH)
                )
                if (preferences.showSeconds || amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(if (isPreview) 5.dp else 10.dp))
                    Text(
                        text = listOfNotNull(
                            if (preferences.showSeconds) "SEC $displaySecond" else null,
                            amPm.ifEmpty { null }
                        ).joinToString("  ·  "),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isPreview) 10.sp else 13.5.sp,
                        letterSpacing = 1.5.sp,
                        color = accent.copy(alpha = 0.85f)
                    )
                }
            }
        }

        AodClockStyle.RETRO_FLIP -> {
            val cardW = ((if (isPreview) 58f else 98f) * scale).dp
            val cardH = ((if (isPreview) 60f else 102f) * scale).dp
            val numSize = ((if (isPreview) 32f else 54f) * scale).sp
            Column(horizontalAlignment = horizontalAlign) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isPreview) 8.dp else 14.dp)
                ) {
                    FlipClockTile(
                        digits = displayHour,
                        digitColor = Color.White,
                        accentColor = accent,
                        fontFamily = clockFont,
                        fontSize = numSize,
                        width = cardW,
                        height = cardH,
                        cornerRadius = if (isPreview) 12.dp else 18.dp
                    )
                    FlipClockTile(
                        digits = displayMinute,
                        digitColor = accent,
                        accentColor = accent,
                        fontFamily = clockFont,
                        fontSize = numSize,
                        width = cardW,
                        height = cardH,
                        cornerRadius = if (isPreview) 12.dp else 18.dp
                    )
                }
                if (preferences.showSeconds || amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(if (isPreview) 5.dp else 8.dp))
                    Text(
                        text = listOfNotNull(
                            if (preferences.showSeconds) ":$displaySecond" else null,
                            amPm.ifEmpty { null }
                        ).joinToString(" · "),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isPreview) 10.sp else 13.5.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
        }

        AodClockStyle.ANALOG_GALAXY -> {
            val dialSize = ((if (isPreview) 118f else 196f) * scale).dp
            AnalogOledClockFace(
                hour = hourInt,
                minute = minuteInt,
                second = secondInt,
                showSeconds = preferences.showSeconds,
                labelMode = AnalogLabelMode.NONE,
                accentColor = accent,
                fontFamily = clockFont,
                modifier = Modifier.size(dialSize)
            )
        }

        AodClockStyle.ANALOG_NUMBERS -> {
            val dialSize = ((if (isPreview) 122f else 204f) * scale).dp
            AnalogOledClockFace(
                hour = hourInt,
                minute = minuteInt,
                second = secondInt,
                showSeconds = preferences.showSeconds,
                labelMode = AnalogLabelMode.ARABIC,
                accentColor = accent,
                fontFamily = clockFont,
                modifier = Modifier.size(dialSize)
            )
        }

        AodClockStyle.ROMAN_CLASSIC -> {
            val dialSize = ((if (isPreview) 122f else 206f) * scale).dp
            AnalogOledClockFace(
                hour = hourInt,
                minute = minuteInt,
                second = secondInt,
                showSeconds = preferences.showSeconds,
                labelMode = AnalogLabelMode.ROMAN,
                accentColor = accent,
                fontFamily = clockFont,
                modifier = Modifier.size(dialSize)
            )
        }

        AodClockStyle.EDGE_NEON -> {
            val numSize = ((if (isPreview) 36f else 58f) * scale).sp
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(((if (isPreview) 50f else 84f) * scale).dp)
                        .background(accent, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "$displayHour:$displayMinute" + if (preferences.showSeconds) ":$displaySecond" else "",
                        fontFamily = clockFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = numSize,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    if (amPm.isNotEmpty()) {
                        Text(
                            text = amPm,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = if (isPreview) 10.sp else 14.sp,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AodStyleThumbnail(
    style: AodClockStyle,
    accentColor: Color,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            AodClockStyle.STACKED_ONE_UI -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "12",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        color = accentColor
                    )
                    Text(
                        text = "45",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        color = Color.White
                    )
                }
            }

            AodClockStyle.DIGITAL_MINIMAL -> {
                Text(
                    text = "12:45",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = accentColor
                )
            }

            AodClockStyle.TYPOGRAPHIC_BOLD -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "12",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                    Text(
                        text = ":",
                        fontFamily = fontFamily,
                        fontSize = 15.sp,
                        color = accentColor
                    )
                    Text(
                        text = "45",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = accentColor
                    )
                }
            }

            AodClockStyle.POSTER_TALL -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "12",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 18.sp,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .width(20.dp)
                            .height(2.dp)
                            .background(accentColor, RoundedCornerShape(50))
                    )
                    Text(
                        text = "45",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 18.sp,
                        color = accentColor
                    )
                }
            }

            AodClockStyle.DUO_CAPSULE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.24f))
                            .border(1.dp, accentColor.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "12",
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = accentColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0E121A))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "45",
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            AodClockStyle.ORBIT_CHRONO -> {
                OrbitChronoClockFace(
                    displayHour = "12",
                    displayMinute = "45",
                    displaySecond = "30",
                    minuteInt = 45,
                    secondInt = 30,
                    showSeconds = true,
                    amPm = "",
                    accentColor = accentColor,
                    fontFamily = fontFamily,
                    isPreview = true,
                    isThumbnail = true,
                    modifier = Modifier.size(48.dp)
                )
            }

            AodClockStyle.DOT_MATRIX -> {
                DotMatrixClockFace(
                    timeText = "12:45",
                    accentColor = accentColor,
                    modifier = Modifier.size(width = 58.dp, height = 20.dp)
                )
            }

            AodClockStyle.RETRO_FLIP -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlipClockTile(
                        digits = "12",
                        digitColor = Color.White,
                        accentColor = accentColor,
                        fontFamily = fontFamily,
                        fontSize = 13.sp,
                        width = 24.dp,
                        height = 26.dp,
                        cornerRadius = 6.dp
                    )
                    FlipClockTile(
                        digits = "45",
                        digitColor = accentColor,
                        accentColor = accentColor,
                        fontFamily = fontFamily,
                        fontSize = 13.sp,
                        width = 24.dp,
                        height = 26.dp,
                        cornerRadius = 6.dp
                    )
                }
            }

            AodClockStyle.ANALOG_GALAXY -> {
                AnalogOledClockFace(
                    hour = 10,
                    minute = 10,
                    second = 0,
                    showSeconds = false,
                    labelMode = AnalogLabelMode.NONE,
                    accentColor = accentColor,
                    fontFamily = fontFamily,
                    modifier = Modifier.size(42.dp)
                )
            }

            AodClockStyle.ANALOG_NUMBERS -> {
                Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    AnalogOledClockFace(
                        hour = 10,
                        minute = 10,
                        second = 0,
                        showSeconds = false,
                        labelMode = AnalogLabelMode.NONE,
                        accentColor = accentColor,
                        fontFamily = fontFamily,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "12",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                        lineHeight = 7.sp,
                        color = accentColor,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    Text(
                        text = "6",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                        lineHeight = 7.sp,
                        color = accentColor,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            AodClockStyle.ROMAN_CLASSIC -> {
                Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    AnalogOledClockFace(
                        hour = 10,
                        minute = 10,
                        second = 0,
                        showSeconds = false,
                        labelMode = AnalogLabelMode.NONE,
                        accentColor = accentColor,
                        fontFamily = fontFamily,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = "XII",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 6.5.sp,
                        lineHeight = 6.5.sp,
                        color = accentColor,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    Text(
                        text = "VI",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 6.5.sp,
                        lineHeight = 6.5.sp,
                        color = accentColor,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            AodClockStyle.EDGE_NEON -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(22.dp)
                            .background(accentColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "12:45",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private enum class AnalogLabelMode {
    NONE,
    ARABIC,
    ROMAN
}

@Composable
private fun AnalogOledClockFace(
    hour: Int,
    minute: Int,
    second: Int,
    showSeconds: Boolean,
    labelMode: AnalogLabelMode,
    accentColor: Color,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    val hideCardinalTicks = labelMode != AnalogLabelMode.NONE
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = accentColor.copy(alpha = if (labelMode == AnalogLabelMode.ROMAN) 0.42f else 0.32f),
                radius = radius - 2.dp.toPx(),
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )

            for (i in 0 until 12) {
                val isCardinal = i % 3 == 0
                if (hideCardinalTicks && isCardinal) continue
                val angleRad = (i * 30.0 - 90.0) * PI / 180.0
                val innerR = radius * (if (isCardinal) 0.78f else 0.85f)
                val outerR = radius * 0.93f
                drawLine(
                    color = if (isCardinal) accentColor else Color.White.copy(alpha = 0.45f),
                    start = Offset(
                        center.x + (innerR * cos(angleRad)).toFloat(),
                        center.y + (innerR * sin(angleRad)).toFloat()
                    ),
                    end = Offset(
                        center.x + (outerR * cos(angleRad)).toFloat(),
                        center.y + (outerR * sin(angleRad)).toFloat()
                    ),
                    strokeWidth = if (isCardinal) 2.2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val hourAngle = (((hour % 12) + minute / 60f) * 30f - 90f) * PI / 180.0
            val hourLength = radius * 0.48f
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(
                    center.x + (hourLength * cos(hourAngle)).toFloat(),
                    center.y + (hourLength * sin(hourAngle)).toFloat()
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            val minuteAngle = ((minute + second / 60f) * 6f - 90f) * PI / 180.0
            val minuteLength = radius * 0.70f
            drawLine(
                color = accentColor,
                start = center,
                end = Offset(
                    center.x + (minuteLength * cos(minuteAngle)).toFloat(),
                    center.y + (minuteLength * sin(minuteAngle)).toFloat()
                ),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            if (showSeconds) {
                val secondAngle = (second * 6f - 90f) * PI / 180.0
                val secondLength = radius * 0.76f
                drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = center,
                    end = Offset(
                        center.x + (secondLength * cos(secondAngle)).toFloat(),
                        center.y + (secondLength * sin(secondAngle)).toFloat()
                    ),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = accentColor,
                radius = 3.5.dp.toPx(),
                center = center
            )
        }

        if (labelMode != AnalogLabelMode.NONE) {
            val topLabel = if (labelMode == AnalogLabelMode.ROMAN) "XII" else "12"
            val bottomLabel = if (labelMode == AnalogLabelMode.ROMAN) "VI" else "6"
            val leftLabel = if (labelMode == AnalogLabelMode.ROMAN) "IX" else "9"
            val rightLabel = if (labelMode == AnalogLabelMode.ROMAN) "III" else "3"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                val labelColor = accentColor.copy(alpha = 0.92f)
                Text(
                    text = topLabel,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = labelColor,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                    text = bottomLabel,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = labelColor,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                Text(
                    text = leftLabel,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = labelColor,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = rightLabel,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = labelColor,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
private fun OrbitChronoClockFace(
    displayHour: String,
    displayMinute: String,
    displaySecond: String,
    minuteInt: Int,
    secondInt: Int,
    showSeconds: Boolean,
    amPm: String,
    accentColor: Color,
    fontFamily: FontFamily,
    isPreview: Boolean,
    isThumbnail: Boolean = false,
    modifier: Modifier = Modifier
) {
    val minuteSweep = ((minuteInt / 60f) * 360f).coerceIn(6f, 360f)
    val secondSweep = ((secondInt / 60f) * 360f).coerceIn(6f, 360f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerStroke = if (isThumbnail) 2.2.dp.toPx() else if (isPreview) 3.dp.toPx() else 5.dp.toPx()
            val innerStroke = if (isThumbnail) 1.5.dp.toPx() else if (isPreview) 2.dp.toPx() else 3.dp.toPx()
            val pad = outerStroke / 2f + 2.dp.toPx()
            val outerDiameter = size.minDimension - pad * 2f

            // Outer minute track + active sweep
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(outerDiameter, outerDiameter),
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = minuteSweep,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(outerDiameter, outerDiameter),
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )

            // Inner seconds ring
            if (showSeconds || isThumbnail) {
                val innerPad = pad + (if (isThumbnail) 5.dp.toPx() else if (isPreview) 8.dp.toPx() else 12.dp.toPx())
                val innerDiameter = size.minDimension - innerPad * 2f
                if (innerDiameter > 10f) {
                    drawArc(
                        color = accentColor.copy(alpha = 0.16f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(innerPad, innerPad),
                        size = Size(innerDiameter, innerDiameter),
                        style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.85f),
                        startAngle = -90f,
                        sweepAngle = secondSweep,
                        useCenter = false,
                        topLeft = Offset(innerPad, innerPad),
                        size = Size(innerDiameter, innerDiameter),
                        style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$displayHour:$displayMinute",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (isThumbnail) 10.sp else if (isPreview) 24.sp else 40.sp,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            if (!isThumbnail && (showSeconds || amPm.isNotEmpty())) {
                Text(
                    text = listOfNotNull(
                        if (showSeconds) "${displaySecond}s" else null,
                        amPm.ifEmpty { null }
                    ).joinToString(" · "),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (isPreview) 9.sp else 12.5.sp,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun FlipClockTile(
    digits: String,
    digitColor: Color,
    accentColor: Color,
    fontFamily: FontFamily,
    fontSize: androidx.compose.ui.unit.TextUnit,
    width: Dp,
    height: Dp,
    cornerRadius: Dp
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF0C1018))
            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digits,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = digitColor
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(1.dp)
                    .background(Color.Black.copy(alpha = 0.72f))
            )
        }
    }
}

private val DOT_FONT_5X7: Map<Char, List<String>> = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00110", "01000", "10000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
    ':' to listOf("000", "010", "010", "000", "010", "010", "000")
)

@Composable
private fun DotMatrixClockFace(
    timeText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val glyphs = timeText.mapNotNull { ch ->
            DOT_FONT_5X7[ch]?.let { ch to it }
        }
        if (glyphs.isEmpty()) return@Canvas

        val totalCols = glyphs.sumOf { it.second.first().length } + (glyphs.size - 1)
        val totalRows = 7
        val stepX = size.width / totalCols.toFloat()
        val stepY = size.height / totalRows.toFloat()
        val step = minOf(stepX, stepY)
        val dotRadius = step * 0.34f
        val startX = (size.width - totalCols * step) / 2f + step / 2f
        val startY = (size.height - totalRows * step) / 2f + step / 2f

        var colOffset = 0
        glyphs.forEachIndexed { index, (ch, rows) ->
            val cols = rows.first().length
            val isColon = ch == ':'
            val isMinuteDigit = index >= 3
            val activeColor = when {
                isColon -> accentColor
                isMinuteDigit -> accentColor
                else -> Color.White
            }
            for (r in 0 until totalRows) {
                val rowStr = rows[r]
                for (c in 0 until cols) {
                    val on = rowStr[c] == '1'
                    val cx = startX + (colOffset + c) * step
                    val cy = startY + r * step
                    drawCircle(
                        color = if (on) activeColor else Color.White.copy(alpha = 0.08f),
                        radius = dotRadius,
                        center = Offset(cx, cy)
                    )
                }
            }
            colOffset += cols + 1
        }
    }
}
