package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aod.AodLiveStateHolder
import com.example.aod.AodScreenOffController
import com.example.aod.OptimizationSummary
import com.example.aod.SamsungAodManager
import com.example.data.AodPreferencesRepository
import com.example.model.AodAccentColor
import com.example.model.AodActionResult
import com.example.model.AodClockStyle
import com.example.model.AodPreferences
import com.example.model.AodThemePreset
import com.example.model.SamsungDeviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AodUiState(
    val deviceStatus: SamsungDeviceStatus,
    val preferences: AodPreferences = AodPreferences(),
    val optimizationSummary: OptimizationSummary = OptimizationSummary(0, 5, null),
    val feedbackMessage: String? = null,
    val showWriteSettingsDialog: Boolean = false,
    val adbGrantCommand: String = "",
    val adbEnableCommand: String = ""
)

class AodViewModel(application: Application) : AndroidViewModel(application) {

    private val aodManager = SamsungAodManager(application.applicationContext)
    private val prefsRepo = AodPreferencesRepository(application.applicationContext)

    private val _deviceStatus = MutableStateFlow(aodManager.inspectDeviceStatus())
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _showWriteSettingsDialog = MutableStateFlow(false)

    val uiState: StateFlow<AodUiState> = combine(
        _deviceStatus,
        prefsRepo.preferencesFlow,
        _feedbackMessage,
        _showWriteSettingsDialog
    ) { status, prefs, feedback, showDialog ->
        AodUiState(
            deviceStatus = status,
            preferences = prefs,
            optimizationSummary = aodManager.evaluateOptimizations(status),
            feedbackMessage = feedback,
            showWriteSettingsDialog = showDialog,
            adbGrantCommand = aodManager.getAdbGrantCommand(),
            adbEnableCommand = aodManager.getAdbEnableNativeAodCommand()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AodUiState(
            deviceStatus = _deviceStatus.value,
            optimizationSummary = aodManager.evaluateOptimizations(_deviceStatus.value),
            adbGrantCommand = aodManager.getAdbGrantCommand(),
            adbEnableCommand = aodManager.getAdbEnableNativeAodCommand()
        )
    )

    init {
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                if (prefs.autoAodOnScreenOff) {
                    aodManager.applyMaximumPerformanceOptimizations(
                        enableAod = true,
                        tapToShow = false
                    )
                    if (!AodScreenOffController.isListening) {
                        aodManager.startAutoAodService()
                        refreshStatus()
                    }
                }
            }
        }
    }

    fun refreshStatus() {
        val current = aodManager.inspectDeviceStatus()
        if (current.hasNotificationListenerPermission) {
            AodLiveStateHolder.refreshMediaSessions(getApplication())
        }
        if (current.autoServiceRunning || uiState.value.preferences.autoAodOnScreenOff) {
            aodManager.applyMaximumPerformanceOptimizations(enableAod = true, tapToShow = false)
            _deviceStatus.value = aodManager.inspectDeviceStatus()
        } else {
            _deviceStatus.value = current
        }
    }

    fun runNextOptimizationStep() {
        val status = aodManager.inspectDeviceStatus()
        val summary = aodManager.evaluateOptimizations(status)
        val next = summary.nextPendingStep
        if (next == null) {
            aodManager.applyMaximumPerformanceOptimizations(enableAod = true, tapToShow = false)
            refreshStatus()
            _feedbackMessage.value = "Rendimiento máximo activo: todos los ajustes están optimizados."
        } else {
            val opened = aodManager.openOptimizationStep(next)
            if (!opened) {
                _feedbackMessage.value = "Abre los ajustes de la app para activar: ${next.title}."
            }
        }
    }

    fun setAutoAodOnScreenOff(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val status = aodManager.inspectDeviceStatus()
                prefsRepo.updateAutoAodOnScreenOff(true)
                aodManager.applyMaximumPerformanceOptimizations(enableAod = true, tapToShow = false)
                aodManager.startAutoAodService()
                _feedbackMessage.value = if (status.hasOverlayPermission || status.hasFullScreenIntentPermission) {
                    "Always On activo sin notificaciones molestas."
                } else {
                    "Always On iniciado. Activa «Aparecer encima» para mostrarlo al apagar la pantalla."
                }
            } else {
                prefsRepo.updateAutoAodOnScreenOff(false)
                aodManager.stopAutoAodService()
                _feedbackMessage.value = "Always On automático desactivado."
            }
            refreshStatus()
        }
    }

    fun setAutoAodOnlyCharging(onlyCharging: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateAutoAodOnlyCharging(onlyCharging)
            _feedbackMessage.value = if (onlyCharging) {
                "AOD automático configurado: solo al cargar."
            } else {
                "AOD automático configurado: siempre al bloquear."
            }
        }
    }

    fun setPocketModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updatePocketModeEnabled(enabled)
        }
    }

    fun openFullScreenIntentSettings() {
        val opened = aodManager.openFullScreenIntentSettings()
        if (!opened) {
            _feedbackMessage.value = "Abre Ajustes > Aplicaciones > Always On > Notificaciones en pantalla completa."
        }
    }

    fun openAppBatterySettings() {
        val opened = aodManager.openAppBatterySettings()
        if (!opened) {
            _feedbackMessage.value = "Abre Ajustes > Aplicaciones > Always On > Batería > No restringido."
        }
    }

    fun openOverlayPermissionSettings() {
        val opened = aodManager.openOverlayPermissionSettings()
        if (!opened) {
            _feedbackMessage.value = "Abre Ajustes > Aplicaciones > Always On > Aparecer encima."
        }
    }

    fun openNotificationListenerSettings() {
        val opened = aodManager.openNotificationListenerSettings()
        if (!opened) {
            _feedbackMessage.value = "Abre Ajustes > Aplicaciones > Acceso especial > Acceso a notificaciones y activa Always On."
        }
    }

    fun toggleNativeAod(enable: Boolean) {
        when (val result = aodManager.setNativeAodEnabled(enable)) {
            is AodActionResult.Success -> {
                _feedbackMessage.value = result.message
                refreshStatus()
            }
            AodActionResult.RequiresWriteSettingsPermission -> {
                _showWriteSettingsDialog.value = true
            }
            is AodActionResult.RequiresNativeOrAdb -> {
                _feedbackMessage.value = result.reason
            }
            is AodActionResult.Error -> {
                _feedbackMessage.value = result.message
            }
        }
    }

    fun setTapToShowMode(tapToShow: Boolean) {
        when (val result = aodManager.setTapToShowMode(tapToShow)) {
            is AodActionResult.Success -> {
                _feedbackMessage.value = result.message
                refreshStatus()
            }
            AodActionResult.RequiresWriteSettingsPermission -> {
                _showWriteSettingsDialog.value = true
            }
            is AodActionResult.RequiresNativeOrAdb -> {
                _feedbackMessage.value = result.reason
            }
            is AodActionResult.Error -> {
                _feedbackMessage.value = result.message
            }
        }
    }

    fun openSamsungAodSettings() {
        val opened = aodManager.openSamsungAodSettings()
        if (!opened) {
            _feedbackMessage.value = "No se encontró el menú AOD nativo. Usa el Modo AOD Automático."
        }
    }

    fun openWriteSettingsPermission() {
        _showWriteSettingsDialog.value = false
        val opened = aodManager.openWriteSettingsPermission()
        if (!opened) {
            _feedbackMessage.value = "Abre Ajustes > Aplicaciones > Always On > Cambiar ajustes del sistema."
        }
    }

    fun dismissWriteSettingsDialog() {
        _showWriteSettingsDialog.value = false
    }

    fun openDreamSettings() {
        val opened = aodManager.openDreamSettings()
        if (!opened) {
            _feedbackMessage.value = "Busca «Protector de pantalla» dentro de Ajustes > Pantalla."
        }
    }

    fun requestAddQuickTile() {
        aodManager.requestAddQuickTile { message ->
            _feedbackMessage.value = message
        }
    }

    fun launchUniversalAod() {
        aodManager.launchUniversalAod()
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun setClockStyle(style: AodClockStyle) {
        viewModelScope.launch { prefsRepo.updateClockStyle(style) }
    }

    fun setClockFont(font: com.example.model.AodClockFont) {
        viewModelScope.launch { prefsRepo.updateClockFont(font) }
    }

    fun setAccentColor(color: AodAccentColor) {
        viewModelScope.launch { prefsRepo.updateAccentColor(color) }
    }

    fun applyThemePreset(preset: AodThemePreset) {
        viewModelScope.launch {
            prefsRepo.applyThemePreset(preset)
            _feedbackMessage.value = "Tema «${preset.title}» aplicado."
        }
    }

    fun setClockScale(scale: Float) {
        viewModelScope.launch { prefsRepo.updateClockScale(scale) }
    }

    fun setAlignLeft(alignLeft: Boolean) {
        viewModelScope.launch { prefsRepo.updateAlignLeft(alignLeft) }
    }

    fun setBrightness(level: Float) {
        viewModelScope.launch { prefsRepo.updateBrightness(level) }
    }

    fun setBurnInProtection(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateBurnInProtection(enabled) }
    }

    fun setUse24HourFormat(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateUse24HourFormat(enabled) }
    }

    fun setShowBattery(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateShowBattery(enabled) }
    }

    fun setShowDate(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateShowDate(enabled) }
    }

    fun setShowSeconds(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateShowSeconds(enabled) }
    }

    fun setShowNotifications(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateShowNotifications(enabled) }
    }

    fun setShowMusicControls(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateShowMusicControls(enabled) }
    }

    fun setCustomLabel(label: String) {
        viewModelScope.launch { prefsRepo.updateCustomLabel(label) }
    }

    fun setProUnlocked(unlocked: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateProUnlocked(unlocked)
            _feedbackMessage.value = if (unlocked) {
                "¡Versión Pro desbloqueada! Todos los anuncios han sido eliminados."
            } else {
                "Estado Pro restablecido."
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            // Simulando consulta a Google Play Billing para restaurar compras previas
            prefsRepo.updateProUnlocked(true)
            _feedbackMessage.value = "Compras restauradas correctamente desde Google Play."
        }
    }
}
