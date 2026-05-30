package com.example.crowdalert.ui.navigation

sealed class CrowdAlertRoute(val route: String) {
    data object Login : CrowdAlertRoute("signin")

    data object Register : CrowdAlertRoute("register")

    data object Map : CrowdAlertRoute("map")

    data object IncidentsList : CrowdAlertRoute("incidents")

    data object MyIncidents : CrowdAlertRoute("my_incidents")

    data object Report : CrowdAlertRoute("report")

    data object Settings : CrowdAlertRoute("settings")
}
