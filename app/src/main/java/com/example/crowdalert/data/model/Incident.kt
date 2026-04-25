package com.example.crowdalert.data.model

/**
 * Domain model for a user-reported incident on the map.
 * Firestore field mapping is implemented when integrating with the backend.
 */
data class Incident(
    val id: String,
    val title: String,
    val type: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
)
