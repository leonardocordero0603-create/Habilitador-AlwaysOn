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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
fun AodExtraClockFaceContent(
    preferences: AodPreferences,
    displayHour: String,
    displayMinute: String,
    displaySecond: String,
    hourInt: Int,
    minuteInt: Int,
    amPm: String,
    accent: Color,
    clockFont: FontFamily,
    scale: Float,
    isPreview: Boolean,
    horizontalAlign: Alignment.Horizontal
) {
    when (preferences.clockStyle) {
        AodClockStyle.OUTLINE_NEON -> {
            val numSize = ((if (isPreview) 52f else 88f) * scale).sp
            val lineH = ((if (isPreview) 48f else 82f) * scale).sp
            val strokePx = if (isPreview) 3.2f else 5.5f
            Column(horizontalAlignment = horizontalAlign) {
                Text(
                    text = displayHour,
                    style = TextStyle(
                        fontFamily = clockFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = numSize,
                        lineHeight = lineH,
                        letterSpacing = (-2).sp,
                        drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)
                    ),
                    color = accent
                )
                Text(
                    text = displayMinute,
                    fontFamily = clockFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = numSize,
                    lineHeight = lineH,
                    letterSpacing = (-2).sp,
                    color = Color.White
                )
                if (preferences.showSeconds || amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listOfNotNull(
                            if (preferences.showSeconds) ":$displaySecond" else null,
                            amPm.ifEmpty { null }
                        ).joinToString(" · "),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isPreview) 10.sp else 14.sp,
                        color = accent.copy(alpha = 0.9f)
                    )
                }
            }
        }

        AodClockStyle.BOLD_SPLIT -> {
            val hourSize = ((if (isPreview) 52f else 86f) * scale).sp
            val minSize = ((if (isPreview) 24f else 38f) * scale).sp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isPreview) 8.dp else 14.dp)
            ) {
                Text(
                    text = displayHour,
                    fontFamily = clockFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = hourSize,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )
                Surface(
                    color = accent.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(if (isPreview) 14.dp else 22.dp),
                    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.85f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(
                            horizontal = if (isPreview) 10.dp else 16.dp,
                            vertical = if (isPreview) 6.dp else 10.dp
                        )
                    ) {
                        Text(
                            text = displayMinute,
                            fontFamily = clockFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = minSize,
                            color = accent
                        )
                        if (preferences.showSeconds || amPm.isNotEmpty()) {
                            Text(
                                text = listOfNotNull(
                                    if (preferences.showSeconds) ":$displaySecond" else null,
                                    amPm.ifEmpty { null }
                                ).joinToString(" "),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = if (isPreview) 8.5.sp else 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        AodClockStyle.WORD_ESSENCE -> {
            val numSize = ((if (isPreview) 40f else 66f) * scale).sp
            Column(horizontalAlignment = horizontalAlign) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isPreview) 8.dp else 14.dp)
                ) {
                    Text(
                        text = displayHour,
                        fontFamily = clockFont,
                        fontWeight = FontWeight.Light,
                        fontSize = numSize,
                        color = Color.White.copy(alpha = 0.92f),
                        letterSpacing = (-1).sp
                    )
                    Box(
                        modifier = Modifier
                            .width(if (isPreview) 2.dp else 3.dp)
                            .height(((if (isPreview) 32f else 54f) * scale).dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent)
                    )
                    Text(
                        text = displayMinute,
                        fontFamily = clockFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = numSize,
                        color = accent,
                        letterSpacing = (-1).sp
                    )
                }
                if (preferences.showSeconds || amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listOfNotNull(
                            if (preferences.showSeconds) "${displaySecond}s" else null,
                            amPm.ifEmpty { null }
                        ).joinToString(" · "),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isPreview) 10.sp else 13.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
        }

        AodClockStyle.HORIZON_ARC -> {
            val dialSize = ((if (isPreview) 130f else 214f) * scale).dp
            HorizonArcClockFace(
                displayHour = displayHour,
                displayMinute = displayMinute,
                displaySecond = displaySecond,
                hourInt = hourInt,
                minuteInt = minuteInt,
                showSeconds = preferences.showSeconds,
                amPm = amPm,
                accentColor = accent,
                fontFamily = clockFont,
                isPreview = isPreview,
                modifier = Modifier.size(dialSize)
            )
        }

        AodClockStyle.CYBER_HUD -> {
            val numSize = ((if (isPreview) 34f else 56f) * scale).sp
            val hudPadH = if (isPreview) 12.dp else 20.dp
            val hudPadV = if (isPreview) 8.dp else 14.dp
            Box(
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val bracketLen = if (isPreview) 10.dp.toPx() else 16.dp.toPx()
                    val stroke = if (isPreview) 1.5.dp.toPx() else 2.2.dp.toPx()
                    val c = accent.copy(alpha = 0.9f)
                    // Top-left
                    drawLine(c, Offset(0f, 0f), Offset(bracketLen, 0f), stroke)
                    drawLine(c, Offset(0f, 0f), Offset(0f, bracketLen), stroke)
                    // Top-right
                    drawLine(c, Offset(size.width - bracketLen, 0f), Offset(size.width, 0f), stroke)
                    drawLine(c, Offset(size.width, 0f), Offset(size.width, bracketLen), stroke)
                    // Bottom-left
                    drawLine(c, Offset(0f, size.height), Offset(bracketLen, size.height), stroke)
                    drawLine(c, Offset(0f, size.height - bracketLen), Offset(0f, size.height), stroke)
                    // Bottom-right
                    drawLine(c, Offset(size.width - bracketLen, size.height), Offset(size.width, size.height), stroke)
                    drawLine(c, Offset(size.width, size.height - bracketLen), Offset(size.width, size.height), stroke)
                }
                Column(
                    horizontalAlignment = horizontalAlign,
                    modifier = Modifier.padding(horizontal = hudPadH, vertical = hudPadV)
                ) {
                    Text(
                        text = if (preferences.use24HourFormat) "AOD // 24H" else "AOD // $amPm",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isPreview) 7.5.sp else 10.5.sp,
                        letterSpacing = 1.6.sp,
                        color = accent.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "$displayHour:$displayMinute",
                        fontFamily = clockFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = numSize,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    if (preferences.showSeconds) {
                        Text(
                            text = "SEC · $displaySecond",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = if (isPreview) 9.sp else 12.5.sp,
                            letterSpacing = 1.4.sp,
                            color = accent
                        )
                    }
                }
            }
        }

        AodClockStyle.SEGMENT_RETRO -> {
            val segW = ((if (isPreview) 140f else 240f) * scale).dp
            val segH = ((if (isPreview) 46f else 80f) * scale).dp
            Column(horizontalAlignment = horizontalAlign) {
                SevenSegmentClockCanvas(
                    timeText = "$displayHour:$displayMinute",
                    accentColor = accent,
                    modifier = Modifier.size(width = segW, height = segH)
                )
                if (preferences.showSeconds || amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(if (isPreview) 4.dp else 8.dp))
                    Text(
                        text = listOfNotNull(
                            if (preferences.showSeconds) ":$displaySecond" else null,
                            amPm.ifEmpty { null }
                        ).joinToString("  ·  "),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (isPreview) 10.sp else 14.sp,
                        letterSpacing = 1.4.sp,
                        color = accent.copy(alpha = 0.9f)
                    )
                }
            }
        }

        else -> Unit
    }
}

@Composable
fun AodExtraStyleThumbnail(
    style: AodClockStyle,
    accentColor: Color,
    fontFamily: FontFamily
) {
    when (style) {
        AodClockStyle.OUTLINE_NEON -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "12",
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 17.sp,
                        drawStyle = Stroke(width = 2.4f, join = StrokeJoin.Round)
                    ),
                    color = accentColor
                )
                Text(
                    text = "45",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 17.sp,
                    color = Color.White
                )
            }
        }

        AodClockStyle.BOLD_SPLIT -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "12",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.24f))
                        .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "45",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = accentColor
                    )
                }
            }
        }

        AodClockStyle.WORD_ESSENCE -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "12",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(accentColor, RoundedCornerShape(50))
                )
                Text(
                    text = "45",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = accentColor
                )
            }
        }

        AodClockStyle.HORIZON_ARC -> {
            HorizonArcClockFace(
                displayHour = "12",
                displayMinute = "45",
                displaySecond = "00",
                hourInt = 14,
                minuteInt = 45,
                showSeconds = false,
                amPm = "",
                accentColor = accentColor,
                fontFamily = fontFamily,
                isPreview = true,
                isThumbnail = true,
                modifier = Modifier.size(48.dp)
            )
        }

        AodClockStyle.CYBER_HUD -> {
            Box(
                modifier = Modifier
                    .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "12:45",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        AodClockStyle.SEGMENT_RETRO -> {
            SevenSegmentClockCanvas(
                timeText = "12:45",
                accentColor = accentColor,
                modifier = Modifier.size(width = 58.dp, height = 22.dp)
            )
        }

        else -> Unit
    }
}

@Composable
private fun HorizonArcClockFace(
    displayHour: String,
    displayMinute: String,
    displaySecond: String,
    hourInt: Int,
    minuteInt: Int,
    showSeconds: Boolean,
    amPm: String,
    accentColor: Color,
    fontFamily: FontFamily,
    isPreview: Boolean,
    isThumbnail: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dayProgress = ((hourInt * 60 + minuteInt).coerceIn(0, 1440) / 1440f)
    val sweepDeg = (dayProgress * 260f).coerceIn(14f, 260f)
    val startDeg = 140f
    val phaseLabel = when (hourInt) {
        in 6..11 -> "MAÑANA"
        in 12..18 -> "TARDE"
        else -> "NOCHE"
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = if (isThumbnail) 2.2.dp.toPx() else if (isPreview) 3.dp.toPx() else 4.5.dp.toPx()
            val pad = stroke + 2.dp.toPx()
            val diameter = size.minDimension - pad * 2f
            if (diameter <= 4f) return@Canvas

            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = startDeg,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accentColor,
                startAngle = startDeg,
                sweepAngle = sweepDeg,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val tipRad = (startDeg + sweepDeg) * PI / 180.0
            val r = diameter / 2f
            val cx = size.width / 2f + (r * cos(tipRad)).toFloat()
            val cy = size.height / 2f + (r * sin(tipRad)).toFloat()
            drawCircle(
                color = Color.White,
                radius = stroke * 1.15f,
                center = Offset(cx, cy)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!isThumbnail) {
                Text(
                    text = phaseLabel,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isPreview) 7.sp else 10.sp,
                    letterSpacing = 1.6.sp,
                    color = accentColor
                )
            }
            Text(
                text = "$displayHour:$displayMinute",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (isThumbnail) 10.5.sp else if (isPreview) 24.sp else 40.sp,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            if (!isThumbnail && (showSeconds || amPm.isNotEmpty())) {
                Text(
                    text = listOfNotNull(
                        if (showSeconds) ":$displaySecond" else null,
                        amPm.ifEmpty { null }
                    ).joinToString(" "),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (isPreview) 8.5.sp else 12.sp,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

// Segment masks: bits 0..6 = Top, TopRight, BottomRight, Bottom, BottomLeft, TopLeft, Middle
private val SEVEN_SEGMENT_DIGITS = mapOf(
    '0' to intArrayOf(1, 1, 1, 1, 1, 1, 0),
    '1' to intArrayOf(0, 1, 1, 0, 0, 0, 0),
    '2' to intArrayOf(1, 1, 0, 1, 1, 0, 1),
    '3' to intArrayOf(1, 1, 1, 1, 0, 0, 1),
    '4' to intArrayOf(0, 1, 1, 0, 0, 1, 1),
    '5' to intArrayOf(1, 0, 1, 1, 0, 1, 1),
    '6' to intArrayOf(1, 0, 1, 1, 1, 1, 1),
    '7' to intArrayOf(1, 1, 1, 0, 0, 0, 0),
    '8' to intArrayOf(1, 1, 1, 1, 1, 1, 1),
    '9' to intArrayOf(1, 1, 1, 1, 0, 1, 1)
)

@Composable
private fun SevenSegmentClockCanvas(
    timeText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (size.minDimension <= 1f) return@Canvas
        val colonWidthRatio = 0.36f
        val gapRatio = 0.22f
        val units = timeText.sumOf { ch ->
            (if (ch == ':') colonWidthRatio else 1.0f).toDouble()
        }.toFloat() + gapRatio * (timeText.length - 1).coerceAtLeast(0)
        if (units <= 0f) return@Canvas

        val digitH = size.height * 0.92f
        val maxDigitW = (size.width / units)
        val digitW = minOf(maxDigitW, digitH * 0.56f)
        val gap = digitW * gapRatio
        val colonW = digitW * colonWidthRatio
        val totalW = timeText.sumOf { ch ->
            (if (ch == ':') colonW else digitW).toDouble()
        }.toFloat() + gap * (timeText.length - 1).coerceAtLeast(0)

        var x = (size.width - totalW) / 2f
        val y = (size.height - digitH) / 2f
        val t = (digitW * 0.19f).coerceAtLeast(1.4f)

        timeText.forEachIndexed { idx, ch ->
            if (ch == ':') {
                val cx = x + colonW / 2f
                drawCircle(
                    color = accentColor,
                    radius = t * 0.55f,
                    center = Offset(cx, y + digitH * 0.33f)
                )
                drawCircle(
                    color = accentColor,
                    radius = t * 0.55f,
                    center = Offset(cx, y + digitH * 0.67f)
                )
                x += colonW + gap
            } else {
                val mask = SEVEN_SEGMENT_DIGITS[ch] ?: SEVEN_SEGMENT_DIGITS['0']!!
                val activeColor = if (idx >= 3) accentColor else Color.White
                drawSevenSegmentDigit(
                    x = x,
                    y = y,
                    w = digitW,
                    h = digitH,
                    t = t,
                    mask = mask,
                    activeColor = activeColor,
                    inactiveColor = Color.White.copy(alpha = 0.06f)
                )
                x += digitW + gap
            }
        }
    }
}

private fun DrawScope.drawSevenSegmentDigit(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    t: Float,
    mask: IntArray,
    activeColor: Color,
    inactiveColor: Color
) {
    val halfH = h / 2f
    val inset = t * 0.65f
    val pad = t * 0.18f

    fun hBar(cx: Float, cy: Float, len: Float, color: Color) {
        val hw = len / 2f
        val ht = t / 2f
        val path = Path().apply {
            moveTo(cx - hw, cy)
            lineTo(cx - hw + ht, cy - ht)
            lineTo(cx + hw - ht, cy - ht)
            lineTo(cx + hw, cy)
            lineTo(cx + hw - ht, cy + ht)
            lineTo(cx - hw + ht, cy + ht)
            close()
        }
        drawPath(path, color)
    }

    fun vBar(cx: Float, cy: Float, len: Float, color: Color) {
        val hh = len / 2f
        val ht = t / 2f
        val path = Path().apply {
            moveTo(cx, cy - hh)
            lineTo(cx + ht, cy - hh + ht)
            lineTo(cx + ht, cy + hh - ht)
            lineTo(cx, cy + hh)
            lineTo(cx - ht, cy + hh - ht)
            lineTo(cx - ht, cy - hh + ht)
            close()
        }
        drawPath(path, color)
    }

    val hLen = (w - inset * 2f - pad * 2f).coerceAtLeast(2f)
    val vLen = (halfH - inset - pad * 2f).coerceAtLeast(2f)
    val cx = x + w / 2f
    val leftX = x + t / 2f
    val rightX = x + w - t / 2f
    val topY = y + t / 2f
    val midY = y + halfH
    val botY = y + h - t / 2f
    val upperMidY = (topY + midY) / 2f
    val lowerMidY = (midY + botY) / 2f

    // 0: Top, 1: TopRight, 2: BottomRight, 3: Bottom, 4: BottomLeft, 5: TopLeft, 6: Middle
    hBar(cx, topY, hLen, if (mask[0] == 1) activeColor else inactiveColor)
    vBar(rightX, upperMidY, vLen, if (mask[1] == 1) activeColor else inactiveColor)
    vBar(rightX, lowerMidY, vLen, if (mask[2] == 1) activeColor else inactiveColor)
    hBar(cx, botY, hLen, if (mask[3] == 1) activeColor else inactiveColor)
    vBar(leftX, lowerMidY, vLen, if (mask[4] == 1) activeColor else inactiveColor)
    vBar(leftX, upperMidY, vLen, if (mask[5] == 1) activeColor else inactiveColor)
    hBar(cx, midY, hLen, if (mask[6] == 1) activeColor else inactiveColor)
}
