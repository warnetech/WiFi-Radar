package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RadarSettingsState(
    val scanIntervalSeconds: Int = 10,
    val selectedFolderUri: String? = null,
    val selectedFolderName: String? = null,
    val autoUploadEnabled: Boolean = true,
    val targetPrefix: String = "NOKIA-",
    val alertVibration: Boolean = true,
    val alertAudio: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncStatus: String = "No export yet"
)

class RadarPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wifi_radar_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<RadarSettingsState> = _settingsFlow.asStateFlow()

    private fun loadSettings(): RadarSettingsState {
        return RadarSettingsState(
            scanIntervalSeconds = prefs.getInt("scan_interval_sec", 10),
            selectedFolderUri = prefs.getString("selected_folder_uri", null),
            selectedFolderName = prefs.getString("selected_folder_name", null),
            autoUploadEnabled = prefs.getBoolean("auto_upload_enabled", true),
            targetPrefix = prefs.getString("target_prefix", "NOKIA-") ?: "NOKIA-",
            alertVibration = prefs.getBoolean("alert_vibration", true),
            alertAudio = prefs.getBoolean("alert_audio", false),
            lastSyncTimestamp = prefs.getLong("last_sync_timestamp", 0L),
            lastSyncStatus = prefs.getString("last_sync_status", "No export yet") ?: "No export yet"
        )
    }

    fun updateScanInterval(seconds: Int) {
        val clamped = seconds.coerceIn(3, 120)
        prefs.edit().putInt("scan_interval_sec", clamped).apply()
        _settingsFlow.value = _settingsFlow.value.copy(scanIntervalSeconds = clamped)
    }

    fun updateSelectedFolder(uriString: String?, folderName: String?) {
        prefs.edit()
            .putString("selected_folder_uri", uriString)
            .putString("selected_folder_name", folderName)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            selectedFolderUri = uriString,
            selectedFolderName = folderName
        )
    }

    fun updateAutoUpload(enabled: Boolean) {
        prefs.edit().putBoolean("auto_upload_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoUploadEnabled = enabled)
    }

    fun updateTargetPrefix(prefix: String) {
        val trimmed = prefix.ifBlank { "NOKIA-" }
        prefs.edit().putString("target_prefix", trimmed).apply()
        _settingsFlow.value = _settingsFlow.value.copy(targetPrefix = trimmed)
    }

    fun updateAlertVibration(enabled: Boolean) {
        prefs.edit().putBoolean("alert_vibration", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(alertVibration = enabled)
    }

    fun updateAlertAudio(enabled: Boolean) {
        prefs.edit().putBoolean("alert_audio", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(alertAudio = enabled)
    }

    fun updateSyncStatus(timestamp: Long, status: String) {
        prefs.edit()
            .putLong("last_sync_timestamp", timestamp)
            .putString("last_sync_status", status)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            lastSyncTimestamp = timestamp,
            lastSyncStatus = status
        )
    }
}
