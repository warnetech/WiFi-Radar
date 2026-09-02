package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NokiaDeviceDao {

    @Query("SELECT * FROM nokia_devices ORDER BY isNokiaTarget DESC, lastSeenEpoch DESC")
    fun getAllDevicesFlow(): Flow<List<NokiaDeviceEntity>>

    @Query("SELECT * FROM nokia_devices WHERE isNokiaTarget = 1 ORDER BY lastSeenEpoch DESC")
    fun getNokiaThreatsFlow(): Flow<List<NokiaDeviceEntity>>

    @Query("SELECT * FROM nokia_devices ORDER BY isNokiaTarget DESC, lastSeenEpoch DESC")
    suspend fun getAllDevicesList(): List<NokiaDeviceEntity>

    @Query("SELECT * FROM nokia_devices WHERE isNokiaTarget = 1 ORDER BY lastSeenEpoch DESC")
    suspend fun getNokiaThreatsList(): List<NokiaDeviceEntity>

    @Query("SELECT * FROM nokia_devices WHERE bssid = :bssid LIMIT 1")
    suspend fun getDeviceByBssid(bssid: String): NokiaDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: NokiaDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<NokiaDeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSighting(sighting: ScanSightingEntity)

    @Query("SELECT * FROM scan_sightings WHERE isNokiaTarget = 1 ORDER BY timestamp DESC LIMIT 200")
    fun getNokiaSightingsFlow(): Flow<List<ScanSightingEntity>>

    @Query("SELECT COUNT(*) FROM nokia_devices WHERE isNokiaTarget = 1")
    fun getNokiaThreatCountFlow(): Flow<Int>

    @Query("SELECT SUM(encounterCount) FROM nokia_devices WHERE isNokiaTarget = 1")
    fun getTotalNokiaEncountersFlow(): Flow<Int?>

    @Query("DELETE FROM nokia_devices WHERE bssid = :bssid")
    suspend fun deleteDevice(bssid: String)

    @Query("DELETE FROM nokia_devices")
    suspend fun clearAllDevices()

    @Query("DELETE FROM scan_sightings")
    suspend fun clearAllSightings()
}
