package com.example.crowdalert.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [IncidentEntity::class], version = 2, exportSchema = false)
abstract class CrowdAlertDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
}
