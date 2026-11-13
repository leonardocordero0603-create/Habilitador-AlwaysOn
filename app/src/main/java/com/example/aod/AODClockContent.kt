package com.example.aod

import android.os.SystemClock
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AodPreferences
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AodClockCanvas(
    preferences: AodPreferences,
    batteryPercent: Int?,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    notifications: AodNotificationsSnapshot = AodNotificationsSnapshot(),
    samsungMusic: SamsungMusicState? = null,
    onMusicPrevious: () -> Unit = {},
    onMusicPlayPause: () -> Unit = {},
    onMusicNext: () -> Unit = {}
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(preferences.showSeconds) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeMillis = now
            val delayMs = if (preferences.showSeconds) {
                (1000L - (now % 1000L) + 16L).coerceIn(200L, 1050L)
            } else {
                (60_000L - (now % 60_000L) + 50L).coerceIn(500L, 60_100L)
            }
            delay(delayMs)
        }
    }

    val minuteTick = (currentTimeMillis / 60_000L).toInt()
    val targetOffsetX: Dp = if (preferences.burnInProtection && !isPreview) {
        val pattern = listOf(-10, -4, 6, 12, 5, -7, 0, 9)
        pattern[minuteTick % pattern.size].dp
    } else 0.dp

    val targetOffsetY: Dp = if (preferences.burnInProtection && !isPreview) {
        val pattern = listOf(-14, 8, -6, 14, 2, -10, 12, -4)
        pattern[(minuteTick / 2) % pattern.size].dp
    } else 0.dp

    val animatedX by animateDpAsState(targetValue = targetOffsetX, animationSpec = tween(1200), label = "burnInX")
    val animatedY by animateDpAsState(targetValue = targetOffsetY, animationSpec = tween(1200), label = "burnInY")

    val contentAlpha = if (isPreview) {
        (0.65f + preferences.brightnessLevel * 0.8f).coerceIn(0.65f, 1.0f)
    } else {
        (0.55f + preferences.brightnessLevel * 1.1f).coerceIn(0.55f, 1.0f)
    }

    val accent = preferences.accentColor.color
    val clockFont = preferences.clockFont.fontFamily
    val scale = preferences.clockScale.coerceIn(0.80f, 1.22f)

    val calendar = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }
    val localeEs = remember { Locale("es", "ES") }
    val dateString = remember(currentTimeMillis) {
        val sdf = SimpleDateFormat("EEE, d 'de' MMMM", localeEs)
        sdf.format(Date(currentTimeMillis)).replaceFirstChar { it.uppercase(localeEs) }
    }

    val hourInt = calendar.get(Calendar.HOUR_OF_DAY)
    val minuteInt = calendar.get(Calendar.MINUTE)
    val secondInt = calendar.get(Calendar.SECOND)

    val displayHour = if (preferences.use24HourFormat) {
        String.format(Locale.US, "%02d", hourInt)
    } else {
        val h12 = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        String.format(Locale.US, "%02d", h12)
    }
    val displayMinute = String.format(Locale.US, "%02d", minuteInt)
    val displaySecond = String.format(Locale.US, "%02d", secondInt)
    val amPm = if (!preferences.use24HourFormat) {
        if (hourInt < 12) "AM" else "PM"
    } else ""

    val horizontalAlign = if (preferences.alignLeft && preferences.clockStyle.supportsAlignment) {
        Alignment.Start
    } else {
        Alignment.CenterHorizontally
    }

    val textAlign = if (horizontalAlign == Alignment.Start) TextAlign.Start else TextAlign.Center

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // S23 upper-middle clock + date + contact info block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(
                    top = if (isPreview) 46.dp else 112.dp,
                    start = if (isPreview) 20.dp else 32.dp,
                    end = if (isPreview) 20.dp else 32.dp
                )
                .offset(x = animatedX, y = animatedY)
                .alpha(contentAlpha),
            horizontalAlignment = horizontalAlign
        ) {
            AodClockFaceContent(
                preferences = preferences,
                displayHour = displayHour,
                displayMinute = displayMinute,
                displaySecond = displaySecond,
                hourInt = hourInt,
                minuteInt = minuteInt,
                secondInt = secondInt,
                amPm = amPm,
                accent = accent,
                clockFont = clockFont,
                scale = scale,
                isPreview = isPreview,
                horizontalAlign = horizontalAlign
            )

            if (preferences.showDate) {
                Spacer(modifier = Modifier.height(if (isPreview) 8.dp else 14.dp))
                Text(
                    text = dateString,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (isPreview) 11.5.sp else 16.sp,
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = textAlign
                )
            }

            if (preferences.customLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(if (isPreview) 4.dp else 8.dp))
                Text(
                    text = preferences.customLabel,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = if (isPreview) 10.sp else 14.sp,
                    color = accent.copy(alpha = 0.90f),
                    textAlign = textAlign
                )
            }

            // Real Notification Icons Row (One UI S23 style)
            if (preferences.showNotifications && notifications.icons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (isPreview) 8.dp else 16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isPreview) 6.dp else 12.dp)
                ) {
                    notifications.icons.forEach { item ->
                        val iconSize = if (isPreview) 13.dp else 20.dp
                        if (item.isMonochromeSmallIcon) {
                            Icon(
                                bitmap = item.iconBitmap,
                                contentDescription = item.appName,
                                tint = Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(iconSize)
                            )
                        } else {
                            Image(
                                bitmap = item.iconBitmap,
                                contentDescription = item.appName,
                                modifier = Modifier
                                    .size(iconSize)
                                    .clip(CircleShape)
                            )
                        }
                    }
                    val extraCount = notifications.totalCount - notifications.icons.size
                    if (extraCount > 0) {
                        Surface(
                            color = accent.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "+$extraCount",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (isPreview) 8.5.sp else 11.5.sp,
                                color = accent,
                                modifier = Modifier.padding(
                                    horizontal = if (isPreview) 5.dp else 7.dp,
                                    vertical = 1.dp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Authentic Samsung One UI 7 Now Bar + Bottom S23 Battery
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(if (!isPreview) Modifier.navigationBarsPadding() else Modifier)
                .padding(
                    start = if (isPreview) 10.dp else 16.dp,
                    end = if (isPreview) 10.dp else 16.dp,
                    bottom = if (isPreview) 16.dp else 46.dp
                )
                .offset(x = animatedX / 2, y = animatedY / 2)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (preferences.showMusicControls && samsungMusic != null) {
                SamsungNowBar(
                    music = samsungMusic,
                    accentColor = accent,
                    isPreview = isPreview,
                    onPrevious = onMusicPrevious,
                    onPlayPause = onMusicPlayPause,
                    onNext = onMusicNext
                )
                Spacer(modifier = Modifier.height(if (isPreview) 8.dp else 14.dp))
            } else if (preferences.showMusicControls && isPreview) {
                SamsungNowBarPreviewPlaceholder(accentColor = accent)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (preferences.showBattery && batteryPercent != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val batteryIcon = when {
                        isCharging -> Icons.Filled.BatteryChargingFull
                        batteryPercent >= 90 -> Icons.Filled.BatteryFull
                        else -> Icons.Filled.BatteryStd
                    }
                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = stringResource(R.string.cd_battery_icon),
                        tint = if (isCharging) Color(0xFF34D399) else accent,
                        modifier = Modifier.size(if (isPreview) 13.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isCharging) "$batteryPercent% · Carga rápida" else "$batteryPercent%",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isPreview) 10.5.sp else 13.5.sp,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SamsungNowBar(
    music: SamsungMusicState,
    accentColor: Color,
    isPreview: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val barTint = remember(music.accentArgb, accentColor) {
        music.accentArgb?.let { Color(it) } ?: accentColor
    }
    var isExpanded by remember { mutableStateOf(false) }

    var livePositionMs by remember(music) {
        mutableLongStateOf(computeCurrentPositionMs(music))
    }

    LaunchedEffect(music) {
        livePositionMs = computeCurrentPositionMs(music)
        while (music.isPlaying && music.durationMs > 0L) {
            delay(1000L)
            livePositionMs = computeCurrentPositionMs(music)
        }
    }

    val progressFraction = if (music.durationMs > 0L) {
        (livePositionMs.toFloat() / music.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val pillGradient = Brush.horizontalGradient(
        colors = listOf(
            barTint.copy(alpha = 0.24f),
            Color(0xFF151923).copy(alpha = 0.94f),
            Color(0xFF0E1118).copy(alpha = 0.96f)
        )
    )
    val borderBrush = Brush.horizontalGradient(
        colors = listOf(
            barTint.copy(alpha = 0.50f),
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.10f)
        )
    )

    if (isPreview) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, borderBrush),
            modifier = Modifier
                .widthIn(max = 176.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(pillGradient)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NowBarArtwork(
                    music = music,
                    barTint = barTint,
                    size = 20.dp,
                    cornerRadius = 10.dp
                )
                Text(
                    text = music.title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                NowBarEqualizer(
                    isPlaying = music.isPlaying,
                    tint = barTint,
                    height = 10.dp
                )
                Icon(
                    imageVector = if (music.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        return
    }

    val shape = if (isExpanded) RoundedCornerShape(28.dp) else RoundedCornerShape(50)
    val expandCd = stringResource(if (isExpanded) R.string.cd_music_collapse else R.string.cd_music_expand)

    Box(
        modifier = Modifier
            .widthIn(max = 388.dp)
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(shape)
            .background(pillGradient)
            .border(1.dp, borderBrush, shape)
            .testTag("samsung_now_bar")
    ) {
        if (!isExpanded) {
            // Compact One UI 7 Now Bar Capsule
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { isExpanded = true }
                            .semantics { contentDescription = expandCd }
                            .padding(end = 6.dp)
                            .testTag("now_bar_expand_area"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NowBarArtwork(
                            music = music,
                            barTint = barTint,
                            size = 40.dp,
                            cornerRadius = 20.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = music.title,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                NowBarEqualizer(
                                    isPlaying = music.isPlaying,
                                    tint = barTint,
                                    height = 11.dp
                                )
                            }
                            Text(
                                text = music.artist.ifBlank { "Samsung Music" },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("aod_music_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_music_previous),
                            tint = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("aod_music_play_pause_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(barTint.copy(alpha = 0.24f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (music.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_music_play_pause),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("aod_music_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.cd_music_next),
                            tint = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (music.durationMs > 0L) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.10f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .background(barTint.copy(alpha = 0.90f))
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        } else {
            // Expanded One UI 7 Now Bar Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isExpanded = false }
                        .semantics { contentDescription = expandCd },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NowBarArtwork(
                        music = music,
                        barTint = barTint,
                        size = 52.dp,
                        cornerRadius = 14.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NowBarEqualizer(
                                isPlaying = music.isPlaying,
                                tint = barTint,
                                height = 11.dp
                            )
                            Text(
                                text = "Samsung Music",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = barTint
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = music.title,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (music.artist.isNotBlank()) {
                            Text(
                                text = music.artist,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = { isExpanded = false },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_bar_collapse_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_music_collapse),
                            tint = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                if (music.durationMs > 0L) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(barTint)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationMs(livePositionMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                        Text(
                            text = formatDurationMs(music.durationMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("aod_music_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_music_previous),
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(barTint.copy(alpha = 0.24f))
                            .testTag("aod_music_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (music.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_music_play_pause),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("aod_music_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.cd_music_next),
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SamsungNowBarPreviewPlaceholder(accentColor: Color) {
    Surface(
        color = Color(0xFF131720).copy(alpha = 0.92f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .widthIn(max = 166.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = "Now Bar · Samsung Music",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 8.5.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NowBarArtwork(
    music: SamsungMusicState,
    barTint: Color,
    size: Dp,
    cornerRadius: Dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (music.albumArt != null) {
        Image(
            bitmap = music.albumArt,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(barTint.copy(alpha = 0.22f))
                .border(1.dp, barTint.copy(alpha = 0.45f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = barTint,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@Composable
private fun NowBarEqualizer(
    isPlaying: Boolean,
    tint: Color,
    height: Dp
) {
    if (!isPlaying) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(height)
        ) {
            listOf(0.35f, 0.55f, 0.35f).forEach { ratio ->
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(ratio)
                        .clip(RoundedCornerShape(50))
                        .background(tint.copy(alpha = 0.65f))
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "nowBarEq")
    val b1 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(540, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(height)
    ) {
        listOf(b1, b2, b3).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(50))
                    .background(tint)
            )
        }
    }
}

private fun computeCurrentPositionMs(music: SamsungMusicState): Long {
    if (music.durationMs <= 0L) return 0L
    if (!music.isPlaying) return music.positionMs.coerceIn(0L, music.durationMs)
    val elapsed = (SystemClock.elapsedRealtime() - music.positionUpdateTimeMs).coerceAtLeast(0L)
    val estimated = music.positionMs + (elapsed * music.playbackSpeed).toLong()
    return estimated.coerceIn(0L, music.durationMs)
}

private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
