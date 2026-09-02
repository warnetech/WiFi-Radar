package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.RadarViewModel
import com.example.ui.components.DeviceDetailSheet
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.RadarScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBackground
import com.example.ui.theme.RadarDarkBorder
import com.example.ui.theme.RadarDarkSurface
import com.example.ui.theme.RadarDarkSurfaceVariant
import com.example.ui.theme.RadarThreatRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WiFiRadarTheme

enum class RadarTab {
    RADAR,
    LOGS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: RadarViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WiFiRadarTheme {
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }
                var currentTab by remember { mutableStateOf(RadarTab.RADAR) }

                val allDevices by viewModel.allDevicesFlow.collectAsStateWithLifecycle()
                val nokiaThreats by viewModel.nokiaThreatsFlow.collectAsStateWithLifecycle()
                val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
                val scanState by viewModel.scanState.collectAsStateWithLifecycle()
                val selectedDevice by viewModel.selectedDeviceForDetail.collectAsStateWithLifecycle()
                val userFeedback by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

                // Request Wi-Fi permissions on launch
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.triggerSingleScan()
                }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                    }

                    val notGranted = permissions.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (notGranted.isNotEmpty()) {
                        permissionLauncher.launch(notGranted.toTypedArray())
                    }
                }

                // Show feedback messages in snackbar
                LaunchedEffect(userFeedback) {
                    userFeedback?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearFeedbackMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = RadarDarkBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = null,
                                        tint = RadarCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "WIFI RADAR",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.5.sp
                                        ),
                                        color = TextPrimary
                                    )
                                }
                            },
                            actions = {
                                if (nokiaThreats.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(RadarThreatRed.copy(alpha = 0.2f))
                                            .border(1.dp, RadarThreatRed, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = RadarThreatRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${nokiaThreats.size} NOKIA",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = RadarThreatRed
                                            )
                                        }
                                    }
                                }

                                if (settings.selectedFolderUri != null) {
                                    IconButton(
                                        onClick = { viewModel.manualSyncToFolder() },
                                        modifier = Modifier.testTag("app_bar_sync_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "Sync to folder",
                                            tint = RadarCyan
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = RadarDarkBackground
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = RadarDarkSurface,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .border(1.dp, RadarDarkBorder)
                                .testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == RadarTab.RADAR,
                                onClick = { currentTab = RadarTab.RADAR },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = "Radar"
                                    )
                                },
                                label = { Text("Radar") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RadarDarkSurface,
                                    selectedTextColor = RadarCyan,
                                    indicatorColor = RadarCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("tab_radar")
                            )

                            NavigationBarItem(
                                selected = currentTab == RadarTab.LOGS,
                                onClick = { currentTab = RadarTab.LOGS },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (nokiaThreats.isNotEmpty()) {
                                                Badge(
                                                    containerColor = RadarThreatRed,
                                                    contentColor = TextPrimary
                                                ) {
                                                    Text("${nokiaThreats.size}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ListAlt,
                                            contentDescription = "Threat Logs"
                                        )
                                    }
                                },
                                label = { Text("Logs") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RadarDarkSurface,
                                    selectedTextColor = RadarThreatRed,
                                    indicatorColor = RadarThreatRed,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("tab_logs")
                            )

                            NavigationBarItem(
                                selected = currentTab == RadarTab.SETTINGS,
                                onClick = { currentTab = RadarTab.SETTINGS },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (settings.selectedFolderUri == null) {
                                                Badge(
                                                    containerColor = RadarCyan,
                                                    contentColor = RadarDarkSurface
                                                ) {
                                                    Text("!")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings"
                                        )
                                    }
                                },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RadarDarkSurface,
                                    selectedTextColor = RadarCyan,
                                    indicatorColor = RadarCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("tab_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "ScreenTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                RadarTab.RADAR -> {
                                    RadarScreen(
                                        devices = allDevices,
                                        nokiaThreats = nokiaThreats,
                                        scanState = scanState,
                                        onTriggerScan = { viewModel.triggerSingleScan() },
                                        onToggleScanLoop = { isScanning ->
                                            if (isScanning) viewModel.startScanning()
                                            else viewModel.stopScanning()
                                        },
                                        onInjectSample = { preset ->
                                            viewModel.injectTestSampleNokia(preset)
                                        },
                                        onSelectDevice = { dev ->
                                            viewModel.selectDeviceForDetail(dev)
                                        }
                                    )
                                }
                                RadarTab.LOGS -> {
                                    LogsScreen(
                                        devices = allDevices,
                                        nokiaThreats = nokiaThreats,
                                        settings = settings,
                                        onManualSync = { viewModel.manualSyncToFolder() },
                                        onClearLogs = { viewModel.clearAllLogs() },
                                        onSelectDevice = { dev ->
                                            viewModel.selectDeviceForDetail(dev)
                                        },
                                        onNavigateToSettings = { currentTab = RadarTab.SETTINGS }
                                    )
                                }
                                RadarTab.SETTINGS -> {
                                    SettingsScreen(
                                        settings = settings,
                                        onFolderSelected = { uri, name ->
                                            viewModel.onFolderSelected(uri, name)
                                        },
                                        onUpdateInterval = { sec ->
                                            viewModel.setScanIntervalSeconds(sec)
                                        },
                                        onToggleAutoUpload = { enabled ->
                                            viewModel.setAutoUploadEnabled(enabled)
                                        },
                                        onUpdatePrefix = { prefix ->
                                            viewModel.setTargetPrefix(prefix)
                                        },
                                        onToggleVibration = { vibrate ->
                                            viewModel.setVibrationAlert(vibrate)
                                        },
                                        onManualSync = { viewModel.manualSyncToFolder() },
                                        onInjectTestNokia = { preset ->
                                            viewModel.injectTestSampleNokia(preset)
                                        },
                                        onClearAllLogs = { viewModel.clearAllLogs() }
                                    )
                                }
                            }
                        }

                        // Detail BottomSheet / Dialog
                        selectedDevice?.let { dev ->
                            DeviceDetailSheet(
                                device = dev,
                                onDismiss = { viewModel.selectDeviceForDetail(null) }
                            )
                        }
                    }
                }
            }
        }
    }
}
