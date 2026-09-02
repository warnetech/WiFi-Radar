package com.example.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.export.LogExporter
import com.example.data.local.NokiaDeviceDao
import com.example.data.local.NokiaDeviceEntity
import com.example.data.local.ScanSightingEntity
import com.example.data.preferences.RadarPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

data class RadarScanState(
    val isScanning: Boolean = false,
    val lastScanTime: Long = 0L,
    val currentActiveThreats: List<NokiaDeviceEntity> = emptyList(),
    val totalThreatsCount: Int = 0,
    val totalEncountersCount: Int = 0,
    val newThreatDetectedPulse: Boolean = false,
    val scanMessage: String = "Radar standby"
)

class WifiRadarManager(
    private val context: Context,
    private val dao: NokiaDeviceDao,
    private val preferences: RadarPreferences,
    private val coroutineScope: CoroutineScope
) {
    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val _scanState = MutableStateFlow(RadarScanState())
    val scanState: StateFlow<RadarScanState> = _scanState.asStateFlow()

    private var scanLoopJob: Job? = null
    private var isReceiverRegistered = false

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                processWifiScanResults(success)
            }
        }
    }

    init {
        // Collect DB stats to keep HUD count accurate
        coroutineScope.launch {
            dao.getNokiaThreatCountFlow().collect { count ->
                _scanState.value = _scanState.value.copy(totalThreatsCount = count)
            }
        }
        coroutineScope.launch {
            dao.getTotalNokiaEncountersFlow().collect { encounters ->
                _scanState.value = _scanState.value.copy(totalEncountersCount = encounters ?: 0)
            }
        }
    }

    fun startRadar() {
        if (scanLoopJob?.isActive == true) return

        registerReceiver()

        scanLoopJob = coroutineScope.launch(Dispatchers.Default) {
            _scanState.value = _scanState.value.copy(isScanning = true, scanMessage = "Radar sweep active")
            while (isActive) {
                triggerSingleScan()
                val intervalSec = preferences.settingsFlow.value.scanIntervalSeconds
                delay(intervalSec * 1000L)
            }
        }
    }

    fun stopRadar() {
        scanLoopJob?.cancel()
        scanLoopJob = null
        unregisterReceiver()
        _scanState.value = _scanState.value.copy(isScanning = false, scanMessage = "Radar paused")
    }

    fun triggerSingleScan() {
        try {
            if (wifiManager?.isWifiEnabled == true) {
                @Suppress("DEPRECATION")
                val scanInitiated = wifiManager.startScan()
                if (!scanInitiated) {
                    // System scan throttled, read available scan results immediately
                    processWifiScanResults(false)
                }
            } else {
                // If Wi-Fi disabled in settings, still poll results or update message
                processWifiScanResults(false)
            }
        } catch (e: SecurityException) {
            _scanState.value = _scanState.value.copy(
                scanMessage = "Wi-Fi permission needed to scan live APs"
            )
        } catch (e: Exception) {
            processWifiScanResults(false)
        }
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(wifiScanReceiver, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(wifiScanReceiver)
            } catch (e: Exception) {
                // Ignore unregister exceptions
            }
            isReceiverRegistered = false
        }
    }

    private fun processWifiScanResults(wasFreshScan: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val rawResults: List<ScanResult> = try {
                wifiManager?.scanResults ?: emptyList()
            } catch (e: SecurityException) {
                emptyList()
            }

            val prefix = preferences.settingsFlow.value.targetPrefix

            val activeThreats = mutableListOf<NokiaDeviceEntity>()
            var hadNokiaMatch = false

            for (res in rawResults) {
                val ssid = res.SSID ?: ""
                val bssid = res.BSSID ?: continue
                if (ssid.isBlank() && bssid.isBlank()) continue

                val isNokia = ssid.startsWith(prefix)
                if (isNokia) hadNokiaMatch = true

                val existing = dao.getDeviceByBssid(bssid)
                val angle = calculateRadarAngle(bssid)

                val updated = if (existing != null) {
                    existing.copy(
                        ssid = if (ssid.isNotBlank()) ssid else existing.ssid,
                        lastSeenEpoch = now,
                        encounterCount = existing.encounterCount + 1,
                        lastRssi = res.level,
                        frequency = res.frequency,
                        capabilities = res.capabilities ?: existing.capabilities,
                        isNokiaTarget = isNokia,
                        radarAngleDeg = angle
                    )
                } else {
                    NokiaDeviceEntity(
                        bssid = bssid,
                        ssid = if (ssid.isNotBlank()) ssid else "[Hidden BSSID]",
                        firstSeenEpoch = now,
                        lastSeenEpoch = now,
                        encounterCount = 1,
                        lastRssi = res.level,
                        frequency = res.frequency,
                        capabilities = res.capabilities ?: "Unknown",
                        isNokiaTarget = isNokia,
                        radarAngleDeg = angle
                    )
                }

                dao.upsert(updated)
                dao.insertSighting(
                    ScanSightingEntity(
                        bssid = bssid,
                        ssid = updated.ssid,
                        timestamp = now,
                        rssi = res.level,
                        frequency = res.frequency,
                        isNokiaTarget = isNokia
                    )
                )

                if (isNokia) {
                    activeThreats.add(updated)
                }
            }

            if (hadNokiaMatch) {
                triggerThreatAlert()
            }

            _scanState.value = _scanState.value.copy(
                lastScanTime = now,
                currentActiveThreats = activeThreats,
                newThreatDetectedPulse = hadNokiaMatch,
                scanMessage = if (hadNokiaMatch) "ALERT: ${activeThreats.size} NOKIA device(s) in range!"
                else if (rawResults.isNotEmpty()) "Scanned ${rawResults.size} Wi-Fi device(s)"
                else "Scanned area (no devices detected)"
            )

            // Auto upload / export if enabled and folder configured
            triggerAutoUploadIfEligible()
        }
    }

    /**
     * Injects a simulated NOKIA or test device for emulator / safety verification.
     * Guaranteed to match user prefix exactly (e.g. NOKIA-L6D8, NOKIA-P9E7).
     */
    fun injectTestDevice(
        ssid: String,
        bssid: String,
        rssi: Int = -58,
        frequency: Int = 2437
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val prefix = preferences.settingsFlow.value.targetPrefix
            val isNokia = ssid.startsWith(prefix)

            val existing = dao.getDeviceByBssid(bssid)
            val angle = calculateRadarAngle(bssid)

            val updated = if (existing != null) {
                existing.copy(
                    ssid = ssid,
                    lastSeenEpoch = now,
                    encounterCount = existing.encounterCount + 1,
                    lastRssi = rssi,
                    frequency = frequency,
                    isNokiaTarget = isNokia,
                    radarAngleDeg = angle
                )
            } else {
                NokiaDeviceEntity(
                    bssid = bssid,
                    ssid = ssid,
                    firstSeenEpoch = now,
                    lastSeenEpoch = now,
                    encounterCount = 1,
                    lastRssi = rssi,
                    frequency = frequency,
                    capabilities = "[WPA2-PSK-CCMP][ESS]",
                    isNokiaTarget = isNokia,
                    radarAngleDeg = angle
                )
            }

            dao.upsert(updated)
            dao.insertSighting(
                ScanSightingEntity(
                    bssid = bssid,
                    ssid = ssid,
                    timestamp = now,
                    rssi = rssi,
                    frequency = frequency,
                    isNokiaTarget = isNokia
                )
            )

            if (isNokia) {
                triggerThreatAlert()
                val currentThreats = _scanState.value.currentActiveThreats.filter { it.bssid != bssid } + updated
                _scanState.value = _scanState.value.copy(
                    lastScanTime = now,
                    currentActiveThreats = currentThreats,
                    newThreatDetectedPulse = true,
                    scanMessage = "ALERT: Target identified: $ssid"
                )
            }

            triggerAutoUploadIfEligible()
        }
    }

    private fun triggerThreatAlert() {
        if (!preferences.settingsFlow.value.alertVibration) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 250)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 150, 100, 250), -1)
            }
        } catch (e: Exception) {
            Log.e("WifiRadarManager", "Vibration alert error: ${e.message}")
        }
    }

    private suspend fun triggerAutoUploadIfEligible() {
        val settings = preferences.settingsFlow.value
        if (!settings.autoUploadEnabled || settings.selectedFolderUri.isNullOrBlank()) return

        try {
            val uri = Uri.parse(settings.selectedFolderUri)
            val allDevices = dao.getAllDevicesList()
            if (allDevices.isEmpty()) return

            val result = LogExporter.exportLogsToFolder(context, uri, allDevices)
            preferences.updateSyncStatus(
                timestamp = System.currentTimeMillis(),
                status = if (result.success) "Auto-synced: ${result.message}" else "Auto-sync failed: ${result.message}"
            )
        } catch (e: Exception) {
            preferences.updateSyncStatus(
                timestamp = System.currentTimeMillis(),
                status = "Auto-sync error: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    suspend fun manualSyncNow(): LogExporter.ExportResult {
        val settings = preferences.settingsFlow.value
        val uriStr = settings.selectedFolderUri
        if (uriStr.isNullOrBlank()) {
            return LogExporter.ExportResult(
                success = false,
                message = "No destination folder selected in Settings"
            )
        }
        val uri = Uri.parse(uriStr)
        val allDevices = dao.getAllDevicesList()
        val result = LogExporter.exportLogsToFolder(context, uri, allDevices)
        preferences.updateSyncStatus(
            timestamp = System.currentTimeMillis(),
            status = if (result.success) result.message else "Sync failed: ${result.message}"
        )
        return result
    }

    private fun calculateRadarAngle(bssid: String): Float {
        val hash = abs(bssid.hashCode())
        return (hash % 360).toFloat()
    }
}
