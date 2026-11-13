package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.aod.AodClockCanvas
import com.example.aod.AodLiveStateHolder
import com.example.aod.AodTransitionContainer
import com.example.model.AodPreferences
import com.example.model.DeviceCompatibilityTier
import com.example.model.SamsungDeviceStatus
import kotlinx.coroutines.delay
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricSapphire
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.IceCyan
import com.example.ui.theme.IceCyanContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledOutline
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceElevated

@Composable
fun DeviceTelemetryHeroCard(
    status: SamsungDeviceStatus,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeLabel, badgeBg, badgeFg) = when {
        status.isNativeAodEnabled -> Triple("AOD Nativo Activo", EmeraldContainer, EmeraldActive)
        status.autoServiceRunning -> Triple("Auto-AOD Activo", EmeraldContainer, EmeraldActive)
        status.tier == DeviceCompatibilityTier.SAMSUNG_NATIVE_READY ->
            Triple("Samsung AOD Disponible", IceCyanContainer, IceCyan)
        status.tier == DeviceCompatibilityTier.SAMSUNG_CORE_UNIVERSAL ->
            Triple("One UI Core · Universal", AmberContainer, AmberWarning)
        else -> Triple("Modo Universal AMOLED", IceCyanContainer, IceCyan)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledOutline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricSapphire.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(badgeFg)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeFg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh_status),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(OledSurfaceElevated)
                            .border(1.dp, OledOutline, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = IceCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${status.manufacturer.replaceFirstChar { it.uppercase() }} ${status.model}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val uiLabel = status.oneUiVersion?.let { "$it · " } ?: ""
                        Text(
                            text = "${uiLabel}Android ${status.androidVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryMiniPill(
                        label = "Modo compatible",
                        value = if (status.hasSamsungAodPackage) "Samsung Nativo" else "Universal AMOLED",
                        highlight = status.hasSamsungAodPackage || status.autoServiceRunning,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniPill(
                        label = "Estado actual",
                        value = when {
                            status.autoServiceRunning -> "Activo"
                            status.isNativeAodEnabled -> "Activo en sistema"
                            else -> "Listo para activar"
                        },
                        highlight = status.isNativeAodEnabled || status.autoServiceRunning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniPill(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = OledSurfaceElevated,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, OledOutline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (highlight) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = if (highlight) EmeraldActive else IceCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ActionControlCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = IceCyan,
    trailingBadge: String? = null,
    tag: String
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailingBadge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = trailingBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PhoneMockupPreview(
    preferences: AodPreferences,
    batteryPercent: Int?,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    transitionReplayTrigger: Int = 0
) {
    val previewCd = stringResource(R.string.cd_clock_preview)
    val notifications by AodLiveStateHolder.notifications.collectAsStateWithLifecycle()
    val samsungMusic by AodLiveStateHolder.samsungMusic.collectAsStateWithLifecycle()
    var previewVisible by remember { mutableStateOf(true) }

    LaunchedEffect(transitionReplayTrigger) {
        if (transitionReplayTrigger > 0) {
            delay(40L)
            previewVisible = false
            delay(360L)
            previewVisible = true
        }
    }

    Box(
        modifier = modifier
            .width(196.dp)
            .height(372.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(OledBlack)
            .border(2.5.dp, Color(0xFF2B3447), RoundedCornerShape(30.dp))
            .semantics { contentDescription = previewCd },
        contentAlignment = Alignment.Center
    ) {
        AodTransitionContainer(
            visible = previewVisible,
            transitionStyle = preferences.transitionStyle,
            accentColor = preferences.accentColor.color,
            replayTrigger = 0,
            animateOnInitialShow = false,
            exitDurationMs = 310,
            modifier = Modifier.fillMaxSize()
        ) {
            AodClockCanvas(
                preferences = preferences,
                batteryPercent = batteryPercent,
                isCharging = isCharging,
                isPreview = true,
                notifications = notifications,
                samsungMusic = samsungMusic,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 11.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF141821))
        )
    }
}

@Composable
fun AdbCommandCard(
    title: String,
    description: String,
    command: String,
    onCopy: () -> Unit,
    copyTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledOutline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = OledBlack,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, OledOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = command,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = IceCyan,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag(copyTag)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy_command),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
