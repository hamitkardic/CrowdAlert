package com.example.crowdalert.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val severity: String?,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val reportedBy: String?,
    val reportedByEmail: String?,
    val createdAt: Long?,
    val isSyncedToFirestore: Boolean = true,
)
