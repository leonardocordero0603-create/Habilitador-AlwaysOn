package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AodUiState
import com.example.ui.components.ActionControlCard
import com.example.ui.components.DeviceTelemetryHeroCard
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricSapphire
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.IceCyan
import com.example.ui.theme.OledOutline
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceElevated

@Composable
fun ActivateScreen(
    uiState: AodUiState,
    onRefreshStatus: () -> Unit,
    onLaunchUniversalAod: () -> Unit,
    onToggleAutoAod: (Boolean) -> Unit,
    onSetAutoAodOnlyCharging: (Boolean) -> Unit,
    onOpenOverlayPermissionSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onOpenAppBatterySettings: () -> Unit,
    onOpenSamsungAodSettings: () -> Unit,
    onOpenDreamSettings: () -> Unit,
    onRequestAddTile: () -> Unit,
    onShowFeedback: (String) -> Unit,
    onShareApp: () -> Unit,
    onDismissQrDialog: () -> Unit,
    onShareAppViaSystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = uiState.deviceStatus
    val prefs = uiState.preferences

    if (uiState.showQrDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissQrDialog,
            containerColor = OledSurfaceElevated,
            title = {
                Text(
                    text = "Código QR para compartir",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Escanea este código con cualquier cámara para instalar Always On",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = androidx.compose.ui.Modifier.size(210.dp)
                    ) {
                        Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.components.QrCodeCanvas(
                                url = com.example.aod.SamsungAodManager.APP_SHARE_URL,
                                modifier = androidx.compose.ui.Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onShareAppViaSystem,
                    modifier = androidx.compose.ui.Modifier.testTag("share_system_intent_button")
                ) {
                    Text("Compartir por WhatsApp / Redes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissQrDialog,
                    modifier = androidx.compose.ui.Modifier.testTag("dismiss_qr_dialog")
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    val isActive = status.autoServiceRunning || status.isNativeAodEnabled

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
                        text = "Always On",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Activa tu reloj AMOLED en un toque",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                DeviceTelemetryHeroCard(
                    status = status,
                    onRefresh = onRefreshStatus
                )
            }

            // Single Unified 1-Tap Activation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = if (isActive) EmeraldActive else OledOutline
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (status.autoServiceRunning) EmeraldContainer
                                        else ElectricSapphire.copy(alpha = 0.16f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (status.autoServiceRunning) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.PowerSettingsNew
                                    },
                                    contentDescription = null,
                                    tint = if (status.autoServiceRunning) EmeraldActive else IceCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (status.autoServiceRunning) {
                                        "Always On está activo"
                                    } else {
                                        "Activar Always On Display"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (status.autoServiceRunning) {
                                        "Bloquea tu teléfono para verlo. Pulsa encendido para despertar la pantalla o toca dos veces para salir."
                                    } else {
                                        "Pulsa el botón para mostrar el reloj al apagar la pantalla."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Main 1-tap button
                        if (!status.autoServiceRunning) {
                            Button(
                                onClick = { onToggleAutoAod(true) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .testTag("master_activate_button"),
                                shape = MaterialTheme.shapes.medium,
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(ElectricSapphire, IceCyan)
                                            ),
                                            shape = MaterialTheme.shapes.medium
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Activar ahora en 1 toque",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF041322)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onLaunchUniversalAod,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldActive,
                                        contentColor = Color(0xFF032117)
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 50.dp)
                                        .testTag("launch_universal_aod_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_launch_aod),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Probar ahora")
                                }

                                OutlinedButton(
                                    onClick = { onToggleAutoAod(false) },
                                    border = BorderStroke(1.dp, OledOutline),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .heightIn(min = 50.dp)
                                        .testTag("master_deactivate_button")
                                ) {
                                    Text("Desactivar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Guided Step 1: Appear on top (guarantees launch on screen-off on Android 10-16 / One UI)
                        AnimatedVisibility(
                            visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !status.hasOverlayPermission
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    color = AmberContainer,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable(onClick = onOpenOverlayPermissionSettings)
                                        .testTag("grant_overlay_permission_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.WarningAmber,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Paso necesario: Permitir «Aparecer encima»",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = AmberWarning
                                            )
                                            Text(
                                                text = "Toca aquí y activa el interruptor para que el reloj se abra automáticamente al apagar la pantalla.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = AmberWarning
                                        )
                                    }
                                }
                            }
                        }

                        // Guided Step 2 (fallback if overlay not granted and Android 14+ full-screen intent missing)
                        AnimatedVisibility(
                            visible = !status.hasOverlayPermission && !status.hasFullScreenIntentPermission
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = OledSurfaceElevated,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, OledOutline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable(onClick = onOpenFullScreenIntentSettings)
                                        .testTag("grant_fullscreen_intent_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.WarningAmber,
                                            contentDescription = null,
                                            tint = IceCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Alternativa: Pantalla completa (Android 14+)",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = IceCyan
                                            )
                                            Text(
                                                text = "También puedes activar «Notificaciones en pantalla completa».",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = IceCyan
                                        )
                                    }
                                }
                            }
                        }

                        // Optional 1-tap Samsung battery tip (auto-hides once Unrestricted)
                        AnimatedVisibility(
                            visible = status.autoServiceRunning &&
                                (status.hasOverlayPermission || status.hasFullScreenIntentPermission) &&
                                !status.isBatteryUnrestricted
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = OledSurfaceElevated,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, OledOutline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable(onClick = onOpenAppBatterySettings)
                                        .testTag("optimize_battery_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.BatterySaver,
                                            contentDescription = null,
                                            tint = IceCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Recomendado: Evitar que Samsung lo cierre",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = IceCyan
                                            )
                                            Text(
                                                text = "Toca aquí > Batería > elige «No restringido».",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = IceCyan
                                        )
                                    }
                                }
                            }
                        }

                        // Guided Step: Notification icons & Samsung Music player control
                        AnimatedVisibility(
                            visible = !status.hasNotificationListenerPermission
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = OledSurfaceElevated,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, ElectricSapphire.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable(onClick = onOpenNotificationListenerSettings)
                                        .testTag("grant_notification_listener_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = IceCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Modo silencioso, iconos y Samsung Music",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = IceCyan
                                            )
                                            Text(
                                                text = "Mantiene Always On activo sin notificación molesta, muestra iconos de avisos y activa la Now Bar de Samsung Music.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = IceCyan
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = !prefs.autoAodOnlyCharging,
                                onClick = { onSetAutoAodOnlyCharging(false) },
                                label = { Text("Siempre al bloquear") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ScreenLockPortrait,
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
                                    .testTag("auto_aod_always_chip")
                            )
                            FilterChip(
                                selected = prefs.autoAodOnlyCharging,
                                onClick = { onSetAutoAodOnlyCharging(true) },
                                label = { Text("Solo al cargar") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.BatteryChargingFull,
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
                                    .testTag("auto_aod_charging_chip")
                            )
                        }

                        if (!status.autoServiceRunning) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onLaunchUniversalAod,
                                border = BorderStroke(1.dp, OledOutline),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("launch_universal_aod_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_launch_aod),
                                    tint = IceCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Probar pantalla AMOLED ahora", color = IceCyan)
                            }
                        }
                    }
                }
            }

            // Show native Samsung shortcut prominently only if native AOD package exists
            if (status.hasSamsungAodPackage) {
                item {
                    ActionControlCard(
                        title = "Abrir Always On nativo de Samsung",
                        subtitle = "Tu teléfono incluye el AOD oficial de One UI. Tócalo para configurarlo directo.",
                        icon = Icons.Filled.DisplaySettings,
                        accentColor = IceCyan,
                        trailingBadge = "Nativo",
                        onClick = onOpenSamsungAodSettings,
                        tag = "open_samsung_aod_settings_button"
                    )
                }
            }

            item {
                Text(
                    text = "Atajos rápidos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            item {
                ActionControlCard(
                    title = "Añadir botón al Panel Rápido",
                    subtitle = "Pon el interruptor «Always On» junto al Wi-Fi de tu Samsung",
                    icon = Icons.Filled.AddToHomeScreen,
                    accentColor = EmeraldActive,
                    trailingBadge = "1 toque",
                    onClick = onRequestAddTile,
                    tag = "add_quick_tile_button"
                )
            }

            item {
                ActionControlCard(
                    title = "Protector de pantalla al cargar",
                    subtitle = "Usa «Always On AMOLED» automáticamente al conectar el cargador",
                    icon = Icons.Filled.Bedtime,
                    accentColor = AmberWarning,
                    trailingBadge = "Abrir",
                    onClick = onOpenDreamSettings,
                    tag = "open_dream_settings_button"
                )
            }

            item {
                ActionControlCard(
                    title = "Compartir app por QR y Enlace",
                    subtitle = "Muestra un código QR escaneable o comparte por WhatsApp y redes",
                    icon = Icons.Filled.QrCode2,
                    accentColor = IceCyan,
                    trailingBadge = "QR y Enlace",
                    onClick = onShareApp,
                    tag = "share_app_button"
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
