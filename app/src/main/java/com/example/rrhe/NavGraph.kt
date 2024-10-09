package com.example.rrhe

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // inactivityDetector: InactivityDetector
) {
    // Get Application from LocalContext
    val application = LocalContext.current.applicationContext as Application
    val mainViewModelFactory = remember { ViewModelFactory(application) } // Pass Application, not Context

    // Define NavHost for navigation with the correct routes
    NavHost(
        navController = navController,
        startDestination = Screen.StockScreen.route, // Start with StockScreen as default
        modifier = modifier
    ) {
        // Define StockScreen Composable route
        composable(Screen.StockScreen.route) {
            val stockViewModel: StockViewModel = viewModel(factory = mainViewModelFactory)
            StockScreenComposable(stockViewModel)
            // inactivityDetector.reset()
        }

        // Define StatsScreen Composable route
        composable(Screen.StatsScreen.route) {
            val statsViewModel: StatsViewModel = viewModel(factory = mainViewModelFactory)
            StatsScreenComposable(statsViewModel)
            // inactivityDetector.reset()
        }

        // Define WebsiteScreen Composable route
        composable(Screen.WebsiteScreen.route) { // Add WebsiteScreen to the NavGraph
            val websiteViewModel: WebsiteViewModel = viewModel(factory = mainViewModelFactory)
            WebsiteScreenComposable(websiteViewModel)
            // inactivityDetector.reset()
        }
    }
}

// Define the Screen routes for navigation
sealed class Screen(val route: String) {
    data object StockScreen : Screen("Stock")
    data object StatsScreen : Screen("Stats")
    data object WebsiteScreen : Screen("Website") // WebsiteScreen route
}
