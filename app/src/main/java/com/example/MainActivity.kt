package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aod.AodScreenOffController
import com.example.ui.AodViewModel
import com.example.ui.screens.ActivateScreen
import com.example.ui.screens.CompatibilityScreen
import com.example.ui.screens.StylesScreen
import com.example.ui.theme.AlwaysOnTheme
import com.example.ui.theme.ElectricSapphire
import com.example.ui.theme.IceCyan
import com.example.ui.theme.OledObsidian
import com.example.ui.theme.OledSurface
import com.example.ui.theme.TextSecondaryDark

const val ROUTE_ACTIVATE = "activate"
const val ROUTE_STYLES = "styles"
const val ROUTE_COMPATIBILITY = "compatibility"

private data class AodDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    AodDestination(
        route = ROUTE_ACTIVATE,
        labelRes = R.string.nav_activate,
        selectedIcon = Icons.Filled.PowerSettingsNew,
        unselectedIcon = Icons.Outlined.PowerSettingsNew,
        testTag = "nav_tab_activate"
    ),
    AodDestination(
        route = ROUTE_STYLES,
        labelRes = R.string.nav_styles,
        selectedIcon = Icons.Filled.Palette,
        unselectedIcon = Icons.Outlined.Palette,
        testTag = "nav_tab_styles"
    ),
    AodDestination(
        route = ROUTE_COMPATIBILITY,
        labelRes = R.string.nav_compatibility,
        selectedIcon = Icons.Filled.Devices,
        unselectedIcon = Icons.Outlined.Devices,
        testTag = "nav_tab_compatibility"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            AlwaysOnTheme(darkTheme = true) {
                AlwaysOnApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AodScreenOffController.restoreSystemBrightnessIfNeeded(applicationContext)
    }
}

@Composable
fun AlwaysOnApp(viewModel: AodViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = OledObsidian,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(containerColor = OledSurface) {
                        Destinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = IceCyan,
                                    selectedTextColor = IceCyan,
                                    indicatorColor = ElectricSapphire.copy(alpha = 0.22f),
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = OledSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Destinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = IceCyan,
                                    selectedTextColor = IceCyan,
                                    indicatorColor = ElectricSapphire.copy(alpha = 0.22f),
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_ACTIVATE,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_ACTIVATE) {
                        ActivateScreen(
                            uiState = uiState,
                            onRefreshStatus = viewModel::refreshStatus,
                            onLaunchUniversalAod = viewModel::launchUniversalAod,
                            onToggleAutoAod = viewModel::setAutoAodOnScreenOff,
                            onSetAutoAodOnlyCharging = viewModel::setAutoAodOnlyCharging,
                            onOpenOverlayPermissionSettings = viewModel::openOverlayPermissionSettings,
                            onOpenFullScreenIntentSettings = viewModel::openFullScreenIntentSettings,
                            onOpenNotificationListenerSettings = viewModel::openNotificationListenerSettings,
                            onOpenAppBatterySettings = viewModel::openAppBatterySettings,
                            onOpenSamsungAodSettings = viewModel::openSamsungAodSettings,
                            onOpenDreamSettings = viewModel::openDreamSettings,
                            onRequestAddTile = viewModel::requestAddQuickTile,
                            onShareApp = viewModel::shareApp,
                            onDismissQrDialog = viewModel::dismissQrDialog,
                            onShareAppViaSystem = viewModel::shareAppViaSystem,
                            onShowFeedback = viewModel::showFeedback
                        )
                    }
                    composable(ROUTE_STYLES) {
                        StylesScreen(
                            uiState = uiState,
                            onApplyThemePreset = viewModel::applyThemePreset,
                            onSelectClockStyle = viewModel::setClockStyle,
                            onSelectClockFont = viewModel::setClockFont,
                            onSelectAccentColor = viewModel::setAccentColor,
                            onClockScaleChange = viewModel::setClockScale,
                            onToggleAlignLeft = viewModel::setAlignLeft,
                            onBrightnessChange = viewModel::setBrightness,
                            onToggleBurnIn = viewModel::setBurnInProtection,
                            onTogglePocketMode = viewModel::setPocketModeEnabled,
                            onToggle24Hour = viewModel::setUse24HourFormat,
                            onToggleShowBattery = viewModel::setShowBattery,
                            onToggleShowDate = viewModel::setShowDate,
                            onToggleShowSeconds = viewModel::setShowSeconds,
                            onToggleShowNotifications = viewModel::setShowNotifications,
                            onToggleShowMusicControls = viewModel::setShowMusicControls,
                            onSelectTransitionStyle = viewModel::setTransitionStyle,
                            onCustomLabelChange = viewModel::setCustomLabel,
                            onLaunchPreviewFullscreen = viewModel::launchUniversalAod,
                            onUnlockPro = viewModel::setProUnlocked,
                            onRestorePurchases = viewModel::restorePurchases
                        )
                    }
                    composable(ROUTE_COMPATIBILITY) {
                        CompatibilityScreen(
                            uiState = uiState,
                            onToggleNativeAod = viewModel::toggleNativeAod,
                            onSetTapToShowMode = viewModel::setTapToShowMode,
                            onOpenSamsungAodSettings = viewModel::openSamsungAodSettings,
                            onOpenWriteSettingsPermission = viewModel::openWriteSettingsPermission,
                            onDismissWriteSettingsDialog = viewModel::dismissWriteSettingsDialog,
                            onOpenOverlayPermissionSettings = viewModel::openOverlayPermissionSettings,
                            onOpenFullScreenIntentSettings = viewModel::openFullScreenIntentSettings,
                            onOpenNotificationListenerSettings = viewModel::openNotificationListenerSettings,
                            onOpenAppBatterySettings = viewModel::openAppBatterySettings,
                            onRunNextOptimization = viewModel::runNextOptimizationStep
                        )
                    }
                }
            }
        }
    }
}
