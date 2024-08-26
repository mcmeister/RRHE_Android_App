package com.example.rrhe

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.rrhe.ui.theme.RRHETheme

class MainActivity : AppCompatActivity() {

    private val stockViewModel: StockViewModel by viewModels()
    private lateinit var inactivityDetector: InactivityDetector
    private lateinit var screenLockReceiver: Utils.ScreenLockReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize InactivityDetector
        inactivityDetector = InactivityDetector(this)
        screenLockReceiver = Utils.ScreenLockReceiver(inactivityDetector)
        registerReceiver(screenLockReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        setContent {
            RRHETheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(navController = navController)
                    }
                ) {
                    NavGraph(navController = navController, inactivityDetector = inactivityDetector, modifier = Modifier.padding(it))
                }
            }
        }

        // Initialize and check database connection
        stockViewModel.checkDatabaseConnectionOnLaunch()

        // Enable OnBackInvokedCallback for API level 33 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Handle the back press
                    finish()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        inactivityDetector.reset() // Reset inactivity timer when resuming
    }

    override fun onPause() {
        super.onPause()
        inactivityDetector.stop() // Stop inactivity timer when pausing
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        inactivityDetector.reset() // Reset inactivity timer on user interaction
        return super.onTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenLockReceiver)
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.StockScreen,
        Screen.StatsScreen
    )
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Black
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    when (screen.route) {
                        Screen.StockScreen.route -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = screen.route)
                        Screen.StatsScreen.route -> Icon(Icons.Default.BarChart, contentDescription = screen.route)
                    }
                },
                label = { Text(screen.route) },
                selected = false, // Update this as needed
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}