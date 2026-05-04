package com.example.crowdalert.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.crowdalert.ui.auth.AuthViewModel
import com.example.crowdalert.ui.auth.LoginRoute
import com.example.crowdalert.ui.auth.RegisterRoute
import com.example.crowdalert.ui.incidents.IncidentsListRoute
import com.example.crowdalert.ui.incidents.MyIncidentsRoute
import com.example.crowdalert.ui.incidents.MyIncidentsViewModel
import com.example.crowdalert.ui.map.MapRoute
import com.example.crowdalert.ui.map.MapViewModel
import com.example.crowdalert.ui.report.ReportRoute
import com.example.crowdalert.ui.report.ReportViewModel
import com.example.crowdalert.ui.settings.AppThemeMode
import com.example.crowdalert.ui.settings.SettingsRoute

/**
 * Single-activity navigation graph: auth flow and main app shell (map + report).
 */
@Composable
fun CrowdAlertNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
    currentThemeMode: AppThemeMode,
    onThemeModeSelected: (AppThemeMode) -> Unit,
) {
    val isSignedIn by authViewModel.isSignedIn.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val start =
        if (isSignedIn) CrowdAlertRoute.Map.route else CrowdAlertRoute.Login.route

    LaunchedEffect(isSignedIn, currentRoute) {
        when {
            isSignedIn && currentRoute in authRoutes -> {
                navController.navigate(CrowdAlertRoute.Map.route) {
                    popUpTo(CrowdAlertRoute.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }

            !isSignedIn && currentRoute in mainRoutes -> {
                navController.navigate(CrowdAlertRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = start,
        modifier = modifier,
    ) {
        composable(CrowdAlertRoute.Login.route) {
            LoginRoute(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(CrowdAlertRoute.Register.route) },
            )
        }
        composable(CrowdAlertRoute.Register.route) {
            RegisterRoute(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(CrowdAlertRoute.Map.route) {
            val mapViewModel: MapViewModel = hiltViewModel()
            MapRoute(
                viewModel = mapViewModel,
                onOpenReport = { navController.navigate(CrowdAlertRoute.Report.route) },
                onOpenIncidents = { navController.navigate(CrowdAlertRoute.IncidentsList.route) },
                onOpenMyIncidents = { navController.navigate(CrowdAlertRoute.MyIncidents.route) },
                onOpenSettings = { navController.navigate(CrowdAlertRoute.Settings.route) },
            )
        }
        composable(CrowdAlertRoute.Settings.route) {
            val mapBackStackEntry =
                remember(navController) {
                    navController.getBackStackEntry(CrowdAlertRoute.Map.route)
                }
            val mapViewModel: MapViewModel = hiltViewModel(mapBackStackEntry)
            SettingsRoute(
                currentThemeMode = currentThemeMode,
                onThemeModeSelected = onThemeModeSelected,
                onBack = { navController.popBackStack() },
                onSignOut = {
                    mapViewModel.onSignedOut()
                    authViewModel.signOut {
                        navController.navigate(CrowdAlertRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable(CrowdAlertRoute.IncidentsList.route) {
            val mapBackStackEntry =
                remember(navController) {
                    navController.getBackStackEntry(CrowdAlertRoute.Map.route)
                }
            val mapViewModel: MapViewModel = hiltViewModel(mapBackStackEntry)
            IncidentsListRoute(
                viewModel = mapViewModel,
                onBack = { navController.popBackStack() },
                onIncidentSelected = { incident ->
                    mapViewModel.focusIncident(incident)
                    navController.popBackStack()
                },
            )
        }
        composable(CrowdAlertRoute.MyIncidents.route) {
            val mapBackStackEntry =
                remember(navController) {
                    navController.getBackStackEntry(CrowdAlertRoute.Map.route)
                }
            val mapViewModel: MapViewModel = hiltViewModel(mapBackStackEntry)
            val myIncidentsViewModel: MyIncidentsViewModel = hiltViewModel()
            MyIncidentsRoute(
                viewModel = myIncidentsViewModel,
                onBack = { navController.popBackStack() },
                onIncidentSelected = { incident ->
                    mapViewModel.focusIncident(incident)
                    navController.popBackStack()
                },
            )
        }
        composable(CrowdAlertRoute.Report.route) {
            val reportViewModel: ReportViewModel = hiltViewModel()
            ReportRoute(
                viewModel = reportViewModel,
                onSubmitted = { navController.popBackStack() },
                onBackToMap = {
                    val returnedToMap = navController.popBackStack(CrowdAlertRoute.Map.route, false)
                    if (!returnedToMap) {
                        navController.navigate(CrowdAlertRoute.Map.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}

private val authRoutes =
    setOf(
        CrowdAlertRoute.Login.route,
        CrowdAlertRoute.Register.route,
    )

private val mainRoutes =
    setOf(
        CrowdAlertRoute.Map.route,
        CrowdAlertRoute.IncidentsList.route,
        CrowdAlertRoute.MyIncidents.route,
        CrowdAlertRoute.Report.route,
        CrowdAlertRoute.Settings.route,
    )
