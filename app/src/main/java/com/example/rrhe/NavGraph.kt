package com.example.rrhe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    inactivityDetector: InactivityDetector // Pass the inactivityDetector here
) {
    NavHost(
        navController = navController,
        startDestination = Screen.StockScreen.route,
        modifier = modifier
    ) {
        composable(Screen.StockScreen.route) {
            val stockViewModel: StockViewModel = viewModel()
            StockScreenComposable(stockViewModel, inactivityDetector) // Pass it to StockScreenComposable
        }
        composable(Screen.StatsScreen.route) {
            val statsViewModel: StatsViewModel = viewModel()
            StatsScreenComposable(statsViewModel, inactivityDetector) // Pass it to StatsScreenComposable
        }
    }
}

sealed class Screen(val route: String) {
    data object StockScreen : Screen("Stock")
    data object StatsScreen : Screen("Stats")
}