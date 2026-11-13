package com.example.aod

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.AodActionResult
import com.example.model.DeviceCompatibilityTier
import com.example.model.SamsungDeviceStatus

enum class OptimizationStep(
    val title: String,
    val description: String,
    val actionLabel: String
) {
    OVERLAY(
        title = "Mostrar reloj al apagar la pantalla",
        description = "Permite abrir Always On automáticamente al bloquear.",
        actionLabel = "Permitir"
    ),
    NOTIFICATION_LISTENER(
        title = "Modo silencioso, notificaciones y reproductor",
        description = "Mantiene Always On activo sin notificación molesta, muestra iconos y controla el reproductor multimedia.",
        actionLabel = "Permitir"
    ),
    FULL_SCREEN_INTENT(
        title = "Mostrar sobre la pantalla de bloqueo",
        description = "Evita que el bloqueo del sistema tape el reloj.",
        actionLabel = "Permitir"
    ),
    UNRESTRICTED_BATTERY(
        title = "Batería sin restricciones",
        description = "Evita que el sistema detenga Always On tras unos minutos.",
        actionLabel = "Configurar"
    ),
    WRITE_SETTINGS(
        title = "Sincronizar ajustes de pantalla del sistema",
        description = "Permite aplicar brillo y opciones nativas automáticamente.",
        actionLabel = "Permitir"
    )
}

data class OptimizationSummary(
    val completedSteps: Int,
    val totalSteps: Int,
    val nextPendingStep: OptimizationStep?
) {
    val isFullyOptimized: Boolean get() = nextPendingStep == null
}

class SamsungAodManager(private val context: Context) {

    companion object {
        const val SAMSUNG_AOD_PACKAGE = "com.samsung.android.app.aodservice"
        const val KEY_AOD_MODE = "aod_mode"
        const val KEY_AOD_TAP_TO_SHOW = "aod_tap_to_show_mode"
        const val KEY_AOD_START_TIME = "aod_mode_start_time"
        const val KEY_AOD_END_TIME = "aod_mode_end_time"
        const val KEY_DOZE_ALWAYS_ON = "doze_always_on"
        const val APP_SHARE_URL = "https://ais-pre-jrxzfwmq343cdhxug3bhs7-537917905441.us-west2.run.app"
    }

    fun inspectDeviceStatus(): SamsungDeviceStatus {
        val manufacturer = Build.MANUFACTURER ?: "Desconocido"
        val brand = Build.BRAND ?: manufacturer
        val model = Build.MODEL ?: "Dispositivo Android"
        val androidVersion = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()
        val sdkInt = Build.VERSION.SDK_INT

        val hasSamsungAod = isPackageInstalled(SAMSUNG_AOD_PACKAGE)
        val oneUiVersion = detectOneUiVersion()

        val nativeAodMode = readSystemInt(KEY_AOD_MODE)
        val tapToShowMode = readSystemInt(KEY_AOD_TAP_TO_SHOW)
        val dozeAlwaysOn = readSecureInt(KEY_DOZE_ALWAYS_ON)

        val canWriteSettings = Settings.System.canWrite(context)
        val hasWriteSecure = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        val hasPostNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasFullScreenIntent = hasFullScreenIntentPermission()
        val hasProximitySensor = detectProximitySensor()

        val isSamsung = manufacturer.equals("samsung", ignoreCase = true) ||
            brand.equals("samsung", ignoreCase = true)

        val tier = when {
            isSamsung && hasSamsungAod -> DeviceCompatibilityTier.SAMSUNG_NATIVE_READY
            isSamsung -> DeviceCompatibilityTier.SAMSUNG_CORE_UNIVERSAL
            else -> DeviceCompatibilityTier.ANDROID_UNIVERSAL
        }

        val (batteryPercent, isCharging) = readBatterySnapshot()

        return SamsungDeviceStatus(
            manufacturer = manufacturer,
            brand = brand,
            model = model,
            androidVersion = androidVersion,
            sdkInt = sdkInt,
            oneUiVersion = oneUiVersion,
            hasSamsungAodPackage = hasSamsungAod,
            nativeAodMode = nativeAodMode,
            tapToShowMode = tapToShowMode,
            dozeAlwaysOn = dozeAlwaysOn,
            canWriteSettings = canWriteSettings,
            hasWriteSecureSettings = hasWriteSecure,
            hasPostNotificationsPermission = hasPostNotifications,
            hasNotificationListenerPermission = hasNotificationListenerPermission(),
            hasFullScreenIntentPermission = hasFullScreenIntent,
            hasOverlayPermission = hasOverlayPermission(),
            hasProximitySensor = hasProximitySensor,
            autoServiceRunning = AodScreenOffController.isListening || AODAutoService.isRunning,
            isBatteryUnrestricted = isBatteryUnrestricted(),
            tier = tier,
            batteryPercent = batteryPercent,
            isCharging = isCharging
        )
    }

    fun applyMaximumPerformanceOptimizations(
        enableAod: Boolean = true,
        tapToShow: Boolean = false
    ): Boolean {
        val targetValue = if (enableAod) 1 else 0
        val tapValue = if (tapToShow) 1 else 0
        val canWrite = Settings.System.canWrite(context)
        val hasSecure = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        var anyApplied = false

        if (canWrite || hasSecure) {
            try {
                if (Settings.System.putInt(context.contentResolver, KEY_AOD_MODE, targetValue)) {
                    anyApplied = true
                }
                if (enableAod) {
                    Settings.System.putInt(context.contentResolver, KEY_AOD_START_TIME, 0)
                    Settings.System.putInt(context.contentResolver, KEY_AOD_END_TIME, 0)
                }
                if (Settings.System.putInt(context.contentResolver, KEY_AOD_TAP_TO_SHOW, tapValue)) {
                    anyApplied = true
                }
            } catch (_: Exception) {
                // Firmware does not allow or support this system key; Universal AOD remains active
            }
        }

        if (hasSecure) {
            try {
                if (Settings.Secure.putInt(context.contentResolver, KEY_DOZE_ALWAYS_ON, targetValue)) {
                    anyApplied = true
                }
            } catch (_: Exception) {
                // Ignored when restricted by OEM
            }
        }

        return anyApplied
    }

    fun evaluateOptimizations(status: SamsungDeviceStatus): OptimizationSummary {
        val checks = listOf(
            OptimizationStep.OVERLAY to status.hasOverlayPermission,
            OptimizationStep.NOTIFICATION_LISTENER to status.hasNotificationListenerPermission,
            OptimizationStep.FULL_SCREEN_INTENT to status.hasFullScreenIntentPermission,
            OptimizationStep.UNRESTRICTED_BATTERY to status.isBatteryUnrestricted,
            OptimizationStep.WRITE_SETTINGS to status.canWriteSettings
        )
        val completed = checks.count { it.second }
        val next = checks.firstOrNull { !it.second }?.first
        return OptimizationSummary(
            completedSteps = completed,
            totalSteps = checks.size,
            nextPendingStep = next
        )
    }

    fun openOptimizationStep(step: OptimizationStep): Boolean {
        return when (step) {
            OptimizationStep.OVERLAY -> openOverlayPermissionSettings()
            OptimizationStep.NOTIFICATION_LISTENER -> openNotificationListenerSettings()
            OptimizationStep.FULL_SCREEN_INTENT -> openFullScreenIntentSettings()
            OptimizationStep.UNRESTRICTED_BATTERY -> openAppBatterySettings()
            OptimizationStep.WRITE_SETTINGS -> openWriteSettingsPermission()
        }
    }

    fun hasNotificationListenerPermission(): Boolean {
        return try {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    fun openNotificationListenerSettings(): Boolean {
        val component = ComponentName(context, AodNotificationListenerService::class.java)
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                        putExtra(
                            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            component.flattenToString()
                        )
                    }
                )
            }
            add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {
                // Try fallback
            }
        }
        return false
    }

    fun setNativeAodEnabled(enabled: Boolean): AodActionResult {
        val canWrite = Settings.System.canWrite(context)
        val hasSecure = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        if (!canWrite && !hasSecure) {
            return AodActionResult.RequiresWriteSettingsPermission
        }

        val anyWritten = applyMaximumPerformanceOptimizations(enableAod = enabled, tapToShow = false)

        return if (anyWritten) {
            val label = if (enabled) {
                "Always On del sistema activado y sincronizado."
            } else {
                "Always On nativo del sistema desactivado."
            }
            AodActionResult.Success(label)
        } else {
            AodActionResult.RequiresNativeOrAdb(
                "Tu modelo usa el motor Always On Universal automático incluido en la app."
            )
        }
    }

    fun setTapToShowMode(tapToShow: Boolean): AodActionResult {
        val targetValue = if (tapToShow) 1 else 0
        if (!Settings.System.canWrite(context) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED
        ) {
            return AodActionResult.RequiresWriteSettingsPermission
        }

        return try {
            val written = Settings.System.putInt(context.contentResolver, KEY_AOD_TAP_TO_SHOW, targetValue)
            if (written) {
                val modeName = if (tapToShow) "Tocar para mostrar" else "Mostrar siempre"
                AodActionResult.Success("Modo cambiado a: $modeName")
            } else {
                AodActionResult.RequiresNativeOrAdb("Tu modelo usa el modo continuo del motor Always On Universal.")
            }
        } catch (_: SecurityException) {
            AodActionResult.RequiresNativeOrAdb(
                "Puedes cambiar este modo directamente desde los ajustes de pantalla de tu dispositivo."
            )
        } catch (e: Exception) {
            AodActionResult.Error(e.message ?: "Error al cambiar modo de visualización.")
        }
    }

    fun startAutoAodService() {
        AodScreenOffController.start(context)
        try {
            val intent = Intent(context, AODAutoService::class.java)
            context.startService(intent)
        } catch (_: Exception) {
            // Silently rely on NotificationListenerService + AodScreenOffController when in background
        }
    }

    fun stopAutoAodService() {
        AodScreenOffController.stop(context)
        try {
            val intent = Intent(context, AODAutoService::class.java)
            context.stopService(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }

    fun hasFullScreenIntentPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }

    fun openFullScreenIntentSettings(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    fun openOverlayPermissionSettings(): Boolean {
        return try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isBatteryUnrestricted(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun openAppBatterySettings(): Boolean {
        return try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSamsungAodSettings(): Boolean {
        val candidateIntents = listOf(
            Intent().apply {
                component = ComponentName(
                    SAMSUNG_AOD_PACKAGE,
                    "com.samsung.android.app.aodservice.settings.AODSettingsMainActivity"
                )
            },
            Intent().apply {
                component = ComponentName(
                    SAMSUNG_AOD_PACKAGE,
                    "com.samsung.android.app.aodservice.settings.AlwaysOnDisplaySettings"
                )
            },
            Intent("com.samsung.android.app.aodservice.action.AOD_SETTINGS"),
            Intent().apply {
                component = ComponentName(
                    "com.android.settings",
                    "com.samsung.android.settings.display.AlwaysOnDisplaySettings"
                )
            },
            Intent().apply {
                component = ComponentName(
                    "com.android.settings",
                    "com.samsung.android.settings.lockscreen.LockScreenSettings"
                )
            },
            Intent().apply {
                component = ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$AlwaysOnDisplaySettingsActivity"
                )
            },
            Intent(Settings.ACTION_DISPLAY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in candidateIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {
                // Try next fallback candidate
            }
        }
        return false
    }

    fun openWriteSettingsPermission(): Boolean {
        return try {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openDreamSettings(): Boolean {
        val candidates = listOf(
            Intent(Settings.ACTION_DREAM_SETTINGS),
            Intent(Settings.ACTION_DISPLAY_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {
                // Try fallback
            }
        }
        return false
    }

    fun launchUniversalAod() {
        val intent = Intent(context, AODDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }

    fun shareApp() {
        try {
            val shareText = "¡Hola! Te recomiendo «Always On», una app excelente para tener Always On Display estilo Samsung en cualquier pantalla AMOLED con relojes personalizados, Now Bar de música y ahorro inteligente de batería. Pruébala: $APP_SHARE_URL"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Always On Display AMOLED")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Compartir Always On").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Ignored if activity context unavailable
        }
    }

    fun requestAddQuickTile(onResult: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            if (statusBarManager != null) {
                val componentName = ComponentName(context, AODTileService::class.java)
                val label = context.getString(R.string.tile_aod_label)
                val icon = Icon.createWithResource(context, R.drawable.ic_tile_aod)
                statusBarManager.requestAddTileService(
                    componentName,
                    label,
                    icon,
                    ContextCompat.getMainExecutor(context)
                ) { resultCode ->
                    val message = when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            "Botón rápido añadido al panel de control."
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            "El botón rápido Always On ya está en tu panel."
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                            "No se añadió el botón rápido."
                        else ->
                            "Puedes añadir el botón «Always On» editando tu panel rápido."
                    }
                    onResult(message)
                }
                return
            }
        }
        onResult("Desliza el panel rápido, pulsa el icono de edición y arrastra «Always On».")
    }

    fun getAdbGrantCommand(): String =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    fun getAdbEnableNativeAodCommand(): String =
        "adb shell settings put system aod_mode 1 && adb shell settings put system aod_tap_to_show_mode 0 && adb shell settings put secure doze_always_on 1"

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun detectProximitySensor(): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        return sm?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
    }

    private fun readSystemInt(key: String): Int {
        return try {
            Settings.System.getInt(context.contentResolver, key, -1)
        } catch (_: Exception) {
            -1
        }
    }

    private fun readSecureInt(key: String): Int {
        return try {
            Settings.Secure.getInt(context.contentResolver, key, -1)
        } catch (_: Exception) {
            -1
        }
    }

    private fun readBatterySnapshot(): Pair<Int?, Boolean> {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val percent: Int? = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val prop = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (prop in 0..100) prop else null
        }

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return percent to charging
    }

    @SuppressLint("PrivateApi")
    private fun detectOneUiVersion(): String? {
        try {
            val sysProps = Class.forName("android.os.SystemProperties")
            val getProp = sysProps.getMethod("get", String::class.java, String::class.java)
            val oneUiRaw = getProp.invoke(null, "ro.build.version.oneui", "") as? String
            if (!oneUiRaw.isNullOrBlank()) {
                val numeric = oneUiRaw.toIntOrNull()
                if (numeric != null && numeric >= 10000) {
                    val major = numeric / 10000
                    val minor = (numeric % 10000) / 100
                    return "One UI $major.$minor"
                }
            }
        } catch (_: Exception) {
            // Fallback below
        }

        try {
            val field = Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
            val semInt = field.getInt(null)
            if (semInt >= 100000) {
                val oneUiMajor = (semInt - 90000) / 10000
                val oneUiMinor = ((semInt - 90000) % 10000) / 100
                if (oneUiMajor >= 1) {
                    return "One UI $oneUiMajor.$oneUiMinor"
                }
            }
        } catch (_: Exception) {
            // Not a Samsung One UI ROM
        }
        return null
    }
}
