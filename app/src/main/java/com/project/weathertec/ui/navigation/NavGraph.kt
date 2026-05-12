package com.project.weathertec.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.project.weathertec.ui.comparison.ComparisonScreen
import com.project.weathertec.ui.dashboard.DashboardScreen
import com.project.weathertec.ui.export.ExportScreen
import com.project.weathertec.ui.historical.HistoricalScreen
import com.project.weathertec.ui.hourly.HourlyAverageScreen
import com.project.weathertec.ui.range.RangeAverageScreen
import com.project.weathertec.ui.shared.WeatherViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Inicio", Icons.Default.Home)
    object Hourly : Screen("hourly", "Por Hora", Icons.Default.AccessTime)
    object Range : Screen("range", "Rango", Icons.Default.DateRange)
    object Historical : Screen("historical", "Histórico", Icons.Default.BarChart)
    object Comparison : Screen("comparison", "Comparar", Icons.Default.CompareArrows)
    object Export : Screen("export", "Exportar", Icons.Default.Download)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Hourly,
    Screen.Range,
    Screen.Historical,
    Screen.Comparison,
    Screen.Export
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Shared ViewModel — single instance across all screens
    val sharedVm: WeatherViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(sharedVm) }
            composable(Screen.Hourly.route) { HourlyAverageScreen(sharedVm) }
            composable(Screen.Range.route) { RangeAverageScreen(sharedVm) }
            composable(Screen.Historical.route) { HistoricalScreen(sharedVm) }
            composable(Screen.Comparison.route) { ComparisonScreen() }
            composable(Screen.Export.route) { ExportScreen() }
        }
    }
}
