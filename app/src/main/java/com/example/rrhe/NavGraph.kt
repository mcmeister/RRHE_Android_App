package com.example.rrhe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.StockScreen.route,
        modifier = modifier
    ) {
        composable(Screen.StockScreen.route) {
            StockScreenComposable()
        }
        composable(Screen.StatsScreen.route) {
            StatsScreenComposable()
        }
    }
}

sealed class Screen(val route: String) {
    data object StockScreen : Screen("Stock")
    data object StatsScreen : Screen("Stats")
}