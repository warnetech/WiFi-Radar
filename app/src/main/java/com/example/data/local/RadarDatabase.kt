package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NokiaDeviceEntity::class, ScanSightingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RadarDatabase : RoomDatabase() {

    abstract fun nokiaDeviceDao(): NokiaDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: RadarDatabase? = null

        fun getInstance(context: Context): RadarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RadarDatabase::class.java,
                    "wifi_radar_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
