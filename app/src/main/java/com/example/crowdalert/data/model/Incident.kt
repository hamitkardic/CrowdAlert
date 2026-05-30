package com.example.crowdalert.data.model

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
    val createdAtMillis: Long? = null,
)
