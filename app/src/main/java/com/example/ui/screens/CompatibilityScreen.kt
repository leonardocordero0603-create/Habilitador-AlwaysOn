package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.AodUiState
import com.example.ui.components.ActionControlCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricSapphire
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.IceCyan
import com.example.ui.theme.OledOutline
import com.example.ui.theme.OledOutlineSubtle
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceElevated

@Composable
fun CompatibilityScreen(
    uiState: AodUiState,
    onToggleNativeAod: (Boolean) -> Unit,
    onSetTapToShowMode: (Boolean) -> Unit,
    onOpenSamsungAodSettings: () -> Unit,
    onOpenWriteSettingsPermission: () -> Unit,
    onDismissWriteSettingsDialog: () -> Unit,
    onOpenOverlayPermissionSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onOpenAppBatterySettings: () -> Unit,
    onRunNextOptimization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = uiState.deviceStatus
    val opt = uiState.optimizationSummary
    val oneUiShape = RoundedCornerShape(24.dp)

    if (uiState.showWriteSettingsDialog) {
        AlertDialog(
            onDismissRequest = onDismissWriteSettingsDialog,
            containerColor = OledSurfaceElevated,
            title = {
                Text(
                    text = "Permiso de ajustes de pantalla",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Activa «Modificar ajustes del sistema» para que Always On pueda sincronizar automáticamente el brillo y las opciones de pantalla de tu Samsung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = onOpenWriteSettingsPermission,
                    modifier = Modifier.testTag("grant_write_settings_confirm")
                ) {
                    Text("Permitir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissWriteSettingsDialog,
                    modifier = Modifier.testTag("grant_write_settings_dismiss")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = "Ajustes y Rendimiento",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Todo lo necesario para que Always On funcione fluido y sin cierres",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1-Tap Automatic Performance Card (replaces technical jargon & ADB boxes)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(
                        width = if (opt.isFullyOptimized) 1.5.dp else 1.dp,
                        color = if (opt.isFullyOptimized) EmeraldActive else OledOutline
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Speed,
                                    contentDescription = null,
                                    tint = if (opt.isFullyOptimized) EmeraldActive else IceCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (opt.isFullyOptimized) {
                                        "Rendimiento máximo activo"
                                    } else {
                                        "Optimización recomendada"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                color = if (opt.isFullyOptimized) EmeraldContainer else ElectricSapphire.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${opt.completedSteps}/${opt.totalSteps}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (opt.isFullyOptimized) EmeraldActive else IceCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { opt.completedSteps.toFloat() / opt.totalSteps.coerceAtLeast(1) },
                            color = if (opt.isFullyOptimized) EmeraldActive else IceCyan,
                            trackColor = OledSurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val nextStep = opt.nextPendingStep
                        Text(
                            text = if (nextStep == null) {
                                "La app aplicó automáticamente los ajustes del sistema, arranque tras reinicio y protección contra ahorro de batería."
                            } else {
                                "Siguiente ajuste: ${nextStep.title}. ${nextStep.description}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (nextStep != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRunNextOptimization,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricSapphire,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("run_next_optimization_button")
                            ) {
                                Text("Optimizar ahora (${nextStep.actionLabel})")
                            }
                        }
                    }
                }
            }

            // Native Samsung controls only shown plainly when device includes native Samsung AOD
            if (status.hasSamsungAodPackage) {
                item {
                    val targetEnable = !status.isNativeAodEnabled
                    ActionControlCard(
                        title = if (targetEnable) "Activar Always On de Samsung" else "Desactivar Always On de Samsung",
                        subtitle = "Sincroniza el interruptor oficial de tu teléfono Galaxy",
                        icon = Icons.Filled.PowerSettingsNew,
                        accentColor = if (status.isNativeAodEnabled) EmeraldActive else ElectricSapphire,
                        trailingBadge = if (status.isNativeAodEnabled) "Activo" else "Inactivo",
                        onClick = { onToggleNativeAod(targetEnable) },
                        tag = "toggle_native_aod_button"
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = oneUiShape,
                        colors = CardDefaults.cardColors(containerColor = OledSurface),
                        border = BorderStroke(1.dp, OledOutline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = IceCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Modo de visualización en Samsung",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = status.tapToShowMode == 0,
                                    onClick = { onSetTapToShowMode(false) },
                                    label = { Text("Mostrar siempre") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricSapphire.copy(alpha = 0.25f),
                                        selectedLabelColor = IceCyan
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("mode_always_on_chip")
                                )
                                FilterChip(
                                    selected = status.tapToShowMode == 1,
                                    onClick = { onSetTapToShowMode(true) },
                                    label = { Text("Tocar pantalla") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.TouchApp,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricSapphire.copy(alpha = 0.25f),
                                        selectedLabelColor = IceCyan
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("mode_tap_to_show_chip")
                                )
                            }
                        }
                    }
                }
            }

            item {
                ActionControlCard(
                    title = "Ajustes de pantalla de Samsung",
                    subtitle = "Abre la configuración de pantalla y bloqueo de tu teléfono",
                    icon = Icons.Filled.DisplaySettings,
                    accentColor = IceCyan,
                    trailingBadge = "Abrir",
                    onClick = onOpenSamsungAodSettings,
                    tag = "compat_open_samsung_settings_button"
                )
            }

            // Friendly Permission & Auto-Optimization Checklist (no ADB/technical strings)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LockOpen,
                                contentDescription = null,
                                tint = ElectricSapphire,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Optimización automática y permisos",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        FriendlyPermissionRow(
                            name = "Mostrar reloj al apagar la pantalla",
                            granted = status.hasOverlayPermission,
                            onFixClick = onOpenOverlayPermissionSettings,
                            tag = "compat_open_overlay_button"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        FriendlyPermissionRow(
                            name = "Modo silencioso, iconos y Samsung Music",
                            granted = status.hasNotificationListenerPermission,
                            onFixClick = onOpenNotificationListenerSettings,
                            tag = "compat_open_notification_listener_button"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        FriendlyPermissionRow(
                            name = "Mostrar sobre la pantalla de bloqueo",
                            granted = status.hasFullScreenIntentPermission,
                            onFixClick = onOpenFullScreenIntentSettings,
                            tag = "compat_open_fullscreen_intent_button"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        FriendlyPermissionRow(
                            name = "Batería sin restricciones",
                            granted = status.isBatteryUnrestricted,
                            onFixClick = onOpenAppBatterySettings,
                            tag = "compat_open_battery_settings_button"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        FriendlyPermissionRow(
                            name = "Sincronización automática de pantalla",
                            granted = status.canWriteSettings,
                            onFixClick = onOpenWriteSettingsPermission,
                            tag = "compat_open_write_settings_button"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        FriendlyPermissionRow(
                            name = "Inicio automático al reiniciar el móvil",
                            granted = true,
                            onFixClick = null,
                            tag = "boot_persistence_status"
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Devices,
                                contentDescription = null,
                                tint = IceCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Compatibilidad Galaxy",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        SeriesCompatibilityRow(
                            series = "Galaxy S · Note · Z Fold / Flip",
                            method = "Compatible con Always On oficial, Panel Rápido y estilos S23.",
                            badge = "100% Nativo",
                            badgeColor = EmeraldActive
                        )
                        SeriesCompatibilityRow(
                            series = "Galaxy A3x · A5x · A7x",
                            method = "Compatible con Always On de Samsung, estilos extra e inicio rápido.",
                            badge = "Nativo + Extra",
                            badgeColor = IceCyan
                        )
                        SeriesCompatibilityRow(
                            series = "Galaxy A0x · A1x · A2x · M · F · Tab",
                            method = "Activa Always On automáticamente al apagar la pantalla con ahorro OLED.",
                            badge = "Universal AMOLED",
                            badgeColor = AmberWarning
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SeriesCompatibilityRow(
    series: String,
    method: String,
    badge: String,
    badgeColor: Color
) {
    Surface(
        color = OledSurfaceElevated,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, OledOutline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = series,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = method,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FriendlyPermissionRow(
    name: String,
    granted: Boolean,
    onFixClick: (() -> Unit)?,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (!granted && onFixClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onFixClick)
                        .testTag(tag)
                } else {
                    Modifier.testTag(tag)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (granted) EmeraldActive else AmberWarning,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = if (granted) EmeraldContainer else AmberWarning.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = if (granted) "Activo" else "Activar",
                style = MaterialTheme.typography.labelMedium,
                color = if (granted) EmeraldActive else AmberWarning,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
