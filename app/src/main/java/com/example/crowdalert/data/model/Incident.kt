package com.example.crowdalert.data.model

/** Domain model for a user-reported incident on the map. */
data class Incident(
    val id: String,
    val title: String,
    val type: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val severity: String? = null,
    val reporterId: String? = null,
    val reporterEmail: String? = null,
    val reporterName: String? = null,
    /** Millis since epoch from the Firestore `createdAt` field, if present. */
    val createdAtMillis: Long? = null,
)
