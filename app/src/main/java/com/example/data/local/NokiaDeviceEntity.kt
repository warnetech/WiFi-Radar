package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a logged Wi-Fi device.
 * Special security threat tracking applied to devices starting with "NOKIA-".
 */
@Entity(tableName = "nokia_devices")
data class NokiaDeviceEntity(
    @PrimaryKey
    val bssid: String,
    val ssid: String,
    val firstSeenEpoch: Long,
    val lastSeenEpoch: Long,
    val encounterCount: Int,
    val lastRssi: Int,
    val frequency: Int,
    val capabilities: String,
    val isNokiaTarget: Boolean,
    val threatLevel: String = if (isNokiaTarget) "CRITICAL SECURITY RISK" else "NORMAL",
    val radarAngleDeg: Float = 0f,
    val notes: String = ""
)
