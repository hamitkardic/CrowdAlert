package com.example.crowdalert.ui.navigation

/**
 * Type-safe route strings for [androidx.navigation.compose.NavHost].
 */
sealed class CrowdAlertRoute(val route: String) {
    data object Login : CrowdAlertRoute("login")

    data object Register : CrowdAlertRoute("register")

    data object Map : CrowdAlertRoute("map")

    data object Report : CrowdAlertRoute("report")
}
