package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aod.AodStyleThumbnail
import com.example.model.AodAccentColor
import com.example.model.AodClockFont
import com.example.model.AodClockStyle
import com.example.model.AodThemePreset
import com.example.model.AodTransitionStyle
import com.example.ui.AodUiState
import com.example.ui.components.PhoneMockupPreview
import com.example.ui.theme.ElectricSapphire
import com.example.ui.theme.IceCyan
import com.example.ui.theme.OledOutline
import com.example.ui.theme.OledOutlineSubtle
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceElevated
import kotlin.math.roundToInt

@Composable
fun StylesScreen(
    uiState: AodUiState,
    onApplyThemePreset: (AodThemePreset) -> Unit,
    onSelectClockStyle: (AodClockStyle) -> Unit,
    onSelectClockFont: (AodClockFont) -> Unit,
    onSelectAccentColor: (AodAccentColor) -> Unit,
    onClockScaleChange: (Float) -> Unit,
    onToggleAlignLeft: (Boolean) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onToggleBurnIn: (Boolean) -> Unit,
    onTogglePocketMode: (Boolean) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onToggleShowBattery: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onToggleShowSeconds: (Boolean) -> Unit,
    onToggleShowNotifications: (Boolean) -> Unit,
    onToggleShowMusicControls: (Boolean) -> Unit,
    onSelectTransitionStyle: (AodTransitionStyle) -> Unit,
    onCustomLabelChange: (String) -> Unit,
    onLaunchPreviewFullscreen: () -> Unit,
    onUnlockPro: (Boolean) -> Unit,
    onRestorePurchases: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val status = uiState.deviceStatus
    val oneUiShape = RoundedCornerShape(24.dp)
    var transitionReplayTrigger by remember { mutableIntStateOf(0) }

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
                        text = "Estilo de reloj",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Personaliza tu Always On Display al estilo One UI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Monetization & Pro Upgrade / Ad Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (prefs.isProUnlocked) OledSurfaceElevated else ElectricSapphire.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, if (prefs.isProUnlocked) IceCyan else ElectricSapphire.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (prefs.isProUnlocked) "✨ Versión Pro Activada" else "⭐ Always On Pro & Sin Anuncios",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (prefs.isProUnlocked) IceCyan else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (prefs.isProUnlocked) "Disfrutas de estilos ilimitados y 0 anuncios." else "Desbloquea estilos avanzados y elimina todos los anuncios para siempre.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (!prefs.isProUnlocked) {
                            // Non-intrusive AdMob banner placeholder for free users
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(OledSurfaceElevated)
                                    .border(1.dp, OledOutlineSubtle, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Anuncio patrocinado (AdMob) — Se oculta al adquirir Pro",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onUnlockPro(true) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = IceCyan,
                                        contentColor = Color(0xFF041322)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("unlock_pro_button")
                                ) {
                                    Text("Desbloquear Pro ($2.99)", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onRestorePurchases,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OledSurfaceElevated,
                                        contentColor = IceCyan
                                    ),
                                    border = BorderStroke(1.dp, OledOutline),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("restore_purchases_button")
                                ) {
                                    Text("Restaurar")
                                }
                            }
                        } else {
                            Button(
                                onClick = { onUnlockPro(false) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OledSurfaceElevated,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, OledOutline),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("reset_pro_button")
                            ) {
                                Text("Restablecer modo gratuito (Prueba)")
                            }
                        }
                    }
                }
            }

            // Live Galaxy S23 Mockup Preview
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PhoneMockupPreview(
                        preferences = prefs,
                        batteryPercent = status.batteryPercent,
                        isCharging = status.isCharging,
                        transitionReplayTrigger = transitionReplayTrigger
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { transitionReplayTrigger += 1 },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, OledOutline),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("replay_transition_preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = IceCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Probar transición", color = IceCyan)
                        }
                        Button(
                            onClick = onLaunchPreviewFullscreen,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OledSurfaceElevated,
                                contentColor = IceCyan
                            ),
                            border = BorderStroke(1.dp, OledOutline),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("preview_launch_aod_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pantalla completa")
                        }
                    }
                }
            }

            // Good Lock Transition Style Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledOutline)
                ) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "Animación de entrada y salida",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Transiciones estilo Good Lock al iniciar o salir de Always On",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(AodTransitionStyle.entries) { style ->
                                val selected = prefs.transitionStyle == style
                                val accent = prefs.accentColor.color
                                val icon = when (style) {
                                    AodTransitionStyle.ONE_UI_ZOOM -> Icons.Filled.ZoomOutMap
                                    AodTransitionStyle.WONDERLAND_HALO -> Icons.Filled.AutoAwesome
                                    AodTransitionStyle.IRIS_PORTAL -> Icons.Filled.RadioButtonChecked
                                    AodTransitionStyle.CURTAIN_SWEEP -> Icons.Filled.SwipeVertical
                                    AodTransitionStyle.SOFT_FADE -> Icons.Filled.BlurOn
                                }
                                Card(
                                    onClick = {
                                        onSelectTransitionStyle(style)
                                        transitionReplayTrigger += 1
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) accent.copy(alpha = 0.14f) else OledSurfaceElevated
                                    ),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) accent else OledOutline
                                    ),
                                    modifier = Modifier
                                        .width(126.dp)
                                        .testTag("transition_style_${style.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (selected) accent.copy(alpha = 0.22f)
                                                    else Color.White.copy(alpha = 0.06f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = style.shortTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = style.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Curated 1-tap AOD Themes
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledOutline)
                ) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Temas listos en 1 toque",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Combinan reloj, tipografía y color automáticamente",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(AodThemePreset.entries) { preset ->
                                val selected = preset.matches(prefs)
                                val accentColor = preset.accent.color
                                Card(
                                    onClick = { onApplyThemePreset(preset) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) accentColor.copy(alpha = 0.10f) else OledSurfaceElevated
                                    ),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) accentColor else OledOutline
                                    ),
                                    modifier = Modifier
                                        .width(106.dp)
                                        .testTag("theme_preset_${preset.id}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(66.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(1.dp, OledOutlineSubtle, RoundedCornerShape(14.dp))
                                        ) {
                                            AodStyleThumbnail(
                                                style = preset.style,
                                                accentColor = accentColor,
                                                fontFamily = preset.font.fontFamily
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(7.dp))
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = 12.sp,
                                            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(accentColor.copy(alpha = 0.14f))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = preset.tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = accentColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // S23 One UI Horizontal Clock Style Thumbnail Strip + Font + Color Picker
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledOutline)
                ) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Diseño de reloj",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${AodClockStyle.entries.size} estilos",
                                style = MaterialTheme.typography.labelMedium,
                                color = IceCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(AodClockStyle.entries) { style ->
                                val selected = prefs.clockStyle == style
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(86.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onSelectClockStyle(style) }
                                        .testTag("clock_style_${style.id}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(82.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) IceCyan else OledOutline,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                    ) {
                                        AodStyleThumbnail(
                                            style = style,
                                            accentColor = prefs.accentColor.color,
                                            fontFamily = prefs.clockFont.fontFamily
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = style.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) IceCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = OledOutlineSubtle)
                        Spacer(modifier = Modifier.height(14.dp))

                        // One UI 6 Font Picker
                        Text(
                            text = "Tipografía",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(AodClockFont.entries) { font ->
                                val selected = prefs.clockFont == font
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectClockFont(font) },
                                    label = {
                                        Text(
                                            text = font.label,
                                            fontFamily = font.fontFamily
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricSapphire.copy(alpha = 0.25f),
                                        selectedLabelColor = IceCyan
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("clock_font_${font.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = OledOutlineSubtle)
                        Spacer(modifier = Modifier.height(14.dp))

                        // S23 Color Swatches
                        Text(
                            text = "Color",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(AodAccentColor.entries) { accent ->
                                val selected = prefs.accentColor == accent
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { onSelectAccentColor(accent) }
                                        .semantics { contentDescription = "Color ${accent.label}" }
                                        .testTag("accent_color_${accent.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(accent.color)
                                            .border(
                                                width = if (selected) 3.dp else 1.dp,
                                                color = if (selected) Color.White else OledOutline,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF07090E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // S23 Size & Alignment Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val sizePct = (prefs.clockScale * 100).roundToInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tamaño del reloj",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$sizePct%",
                                style = MaterialTheme.typography.labelLarge,
                                color = IceCyan
                            )
                        }
                        Slider(
                            value = prefs.clockScale,
                            onValueChange = onClockScaleChange,
                            valueRange = 0.80f..1.22f,
                            colors = SliderDefaults.colors(
                                thumbColor = IceCyan,
                                activeTrackColor = ElectricSapphire
                            ),
                            modifier = Modifier.testTag("clock_scale_slider")
                        )

                        val supportsAlignment = prefs.clockStyle.supportsAlignment
                        if (supportsAlignment) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Posición",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = !prefs.alignLeft,
                                    onClick = { onToggleAlignLeft(false) },
                                    label = { Text("Centrado") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.FormatAlignCenter,
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
                                        .testTag("align_center_chip")
                                )
                                FilterChip(
                                    selected = prefs.alignLeft,
                                    onClick = { onToggleAlignLeft(true) },
                                    label = { Text("Izquierda") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.FormatAlignLeft,
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
                                        .testTag("align_left_chip")
                                )
                            }
                        }
                    }
                }
            }

            // One UI Brightness & Information Toggles Group
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val pct = ((prefs.brightnessLevel / 0.45f) * 100).roundToInt().coerceIn(5, 100)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.BrightnessLow,
                                    contentDescription = null,
                                    tint = IceCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Brillo de Always On",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelLarge,
                                color = IceCyan
                            )
                        }
                        Slider(
                            value = prefs.brightnessLevel,
                            onValueChange = onBrightnessChange,
                            valueRange = 0.03f..0.45f,
                            colors = SliderDefaults.colors(
                                thumbColor = IceCyan,
                                activeTrackColor = ElectricSapphire
                            ),
                            modifier = Modifier.testTag("brightness_slider")
                        )

                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 10.dp))

                        AodSettingSwitchRow(
                            title = "Mostrar fecha",
                            subtitle = "Día y mes debajo del reloj",
                            checked = prefs.showDate,
                            onCheckedChange = onToggleShowDate,
                            tag = "show_date_switch"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        AodSettingSwitchRow(
                            title = "Mostrar batería y carga",
                            subtitle = "Indicador inferior estilo S23",
                            checked = prefs.showBattery,
                            onCheckedChange = onToggleShowBattery,
                            tag = "show_battery_switch"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        AodSettingSwitchRow(
                            title = "Formato 24 horas",
                            subtitle = "Muestra 18:57 en lugar de 06:57 PM",
                            checked = prefs.use24HourFormat,
                            onCheckedChange = onToggle24Hour,
                            tag = "format_24h_switch"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        AodSettingSwitchRow(
                            title = "Mostrar segundos",
                            subtitle = "Actualización continua segundo a segundo",
                            checked = prefs.showSeconds,
                            onCheckedChange = onToggleShowSeconds,
                            tag = "show_seconds_switch"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        AodSettingSwitchRow(
                            title = "Iconos de notificaciones",
                            subtitle = "Muestra los iconos reales de tus avisos en Always On",
                            checked = prefs.showNotifications,
                            onCheckedChange = onToggleShowNotifications,
                            tag = "show_notifications_switch"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        AodSettingSwitchRow(
                            title = "Now Bar de Samsung Music",
                            subtitle = "Reproductor estilo One UI 7 en píldora inferior solo con Samsung Music",
                            checked = prefs.showMusicControls,
                            onCheckedChange = onToggleShowMusicControls,
                            tag = "show_music_controls_switch"
                        )
                        HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        AodSettingSwitchRow(
                            title = "Protección anti-quemado (Pixel Shift)",
                            subtitle = "Desplaza suavemente el reloj cada minuto",
                            checked = prefs.burnInProtection,
                            onCheckedChange = onToggleBurnIn,
                            tag = "burn_in_switch"
                        )
                        if (status.hasProximitySensor) {
                            HorizontalDivider(color = OledOutlineSubtle, modifier = Modifier.padding(vertical = 8.dp))
                            AodSettingSwitchRow(
                                title = "Apagar pantalla boca abajo / bolsillo",
                                subtitle = "Usa el sensor de proximidad para apagar la pantalla al voltear el teléfono y ahorrar batería",
                                checked = prefs.pocketModeEnabled,
                                onCheckedChange = onTogglePocketMode,
                                tag = "pocket_mode_switch"
                            )
                        }
                    }
                }
            }

            // One UI Contact Information / Custom Signature
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = oneUiShape,
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Información de contacto o firma",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Se mostrará debajo de la fecha igual que en One UI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = prefs.customLabel,
                            onValueChange = onCustomLabelChange,
                            placeholder = { Text("Ej. Teléfono de contacto o tu nombre") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IceCyan,
                                unfocusedBorderColor = OledOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_label_input")
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
private fun AodSettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF041322),
                checkedTrackColor = IceCyan
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
