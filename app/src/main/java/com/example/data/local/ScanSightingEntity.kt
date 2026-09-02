package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Historical sighting log for each encounter of a Wi-Fi device.
 */
@Entity(tableName = "scan_sightings")
data class ScanSightingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bssid: String,
    val ssid: String,
    val timestamp: Long,
    val rssi: Int,
    val frequency: Int,
    val isNokiaTarget: Boolean
)
