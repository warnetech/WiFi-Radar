package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.LogExporter
import com.example.data.local.NokiaDeviceEntity
import com.example.data.local.RadarDatabase
import com.example.data.preferences.RadarPreferences
import com.example.data.preferences.RadarSettingsState
import com.example.scanner.RadarScanState
import com.example.scanner.WifiRadarManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RadarDatabase.getInstance(application)
    private val dao = db.nokiaDeviceDao()
    val preferences = RadarPreferences(application)

    val radarManager = WifiRadarManager(
        context = application,
        dao = dao,
        preferences = preferences,
        coroutineScope = viewModelScope
    )

    val allDevicesFlow: StateFlow<List<NokiaDeviceEntity>> =
        dao.getAllDevicesFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val nokiaThreatsFlow: StateFlow<List<NokiaDeviceEntity>> =
        dao.getNokiaThreatsFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settingsFlow: StateFlow<RadarSettingsState> = preferences.settingsFlow
    val scanState: StateFlow<RadarScanState> = radarManager.scanState

    private val _selectedDeviceForDetail = MutableStateFlow<NokiaDeviceEntity?>(null)
    val selectedDeviceForDetail: StateFlow<NokiaDeviceEntity?> = _selectedDeviceForDetail.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        // Auto-start radar sweep loop
        radarManager.startRadar()
    }

    fun startScanning() {
        radarManager.startRadar()
    }

    fun stopScanning() {
        radarManager.stopRadar()
    }

    fun triggerSingleScan() {
        radarManager.triggerSingleScan()
    }

    fun selectDeviceForDetail(device: NokiaDeviceEntity?) {
        _selectedDeviceForDetail.value = device
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    fun onFolderSelected(treeUri: Uri, folderName: String) {
        viewModelScope.launch {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                getApplication<Application>().contentResolver.takePersistableUriPermission(treeUri, flags)

                preferences.updateSelectedFolder(treeUri.toString(), folderName)
                _userFeedbackMessage.value = "Target folder configured: $folderName"

                // Automatically run first upload sync if there are devices
                val syncResult = radarManager.manualSyncNow()
                if (syncResult.success) {
                    _userFeedbackMessage.value = "Connected to '$folderName' & synced initial logs!"
                }
            } catch (e: Exception) {
                preferences.updateSelectedFolder(treeUri.toString(), folderName)
                _userFeedbackMessage.value = "Selected folder: $folderName (Persistent permission noted)"
            }
        }
    }

    fun setScanIntervalSeconds(seconds: Int) {
        preferences.updateScanInterval(seconds)
    }

    fun setAutoUploadEnabled(enabled: Boolean) {
        preferences.updateAutoUpload(enabled)
    }

    fun setTargetPrefix(prefix: String) {
        preferences.updateTargetPrefix(prefix)
    }

    fun setVibrationAlert(enabled: Boolean) {
        preferences.updateAlertVibration(enabled)
    }

    fun manualSyncToFolder() {
        viewModelScope.launch {
            val result = radarManager.manualSyncNow()
            _userFeedbackMessage.value = result.message
        }
    }

    fun injectTestSampleNokia(preset: String = "NOKIA-L6D8") {
        val bssid = when (preset) {
            "NOKIA-L6D8" -> "C4:EA:1D:A3:L6:D8"
            "NOKIA-P9E7" -> "E8:48:B8:F1:P9:E7"
            "NOKIA-K2M9" -> "14:CC:20:98:K2:M9"
            else -> "70:3A:0E:12:44:00"
        }
        val rssi = (-78..-42).random()
        radarManager.injectTestDevice(preset, bssid, rssi = rssi)
        _userFeedbackMessage.value = "Identified & logged security target: $preset"
    }

    fun injectAmbientWifi(ssid: String = "Public_WiFi_Hotspot") {
        val bssid = "00:1A:2B:3C:4D:${(10..99).random()}"
        radarManager.injectTestDevice(ssid, bssid, rssi = -72, frequency = 5180)
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            dao.clearAllDevices()
            dao.clearAllSightings()
            _userFeedbackMessage.value = "All logs and security sightings cleared"
        }
    }

    override fun onCleared() {
        super.onCleared()
        radarManager.stopRadar()
    }
}
