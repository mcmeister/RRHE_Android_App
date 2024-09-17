package com.example.rrhe

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.rrhe.ui.theme.RRHETheme

class MainActivity : AppCompatActivity() {

    private val stockViewModel: StockViewModel by viewModels {
        ViewModelFactory(application)
    }
    private lateinit var inactivityDetector: InactivityDetector
    private lateinit var screenLockReceiver: Utils.ScreenLockReceiver

    private val requestCodePostNotifications = 1001

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

            // Check and request POST_NOTIFICATIONS permission for Android 13+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    requestCodePostNotifications
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == requestCodePostNotifications) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Log permission granted
                Log.d("MainActivity", "POST_NOTIFICATIONS permission granted. Notifications will be shown.")
            } else {
                // Log permission denied
                Log.d("MainActivity", "POST_NOTIFICATIONS permission denied. User will not receive notifications.")

                // Permission denied, handle accordingly
                val alertDialog = android.app.AlertDialog.Builder(this)
                    .setTitle("Notifications Disabled")
                    .setMessage("You have denied the notification permission. As a result, you will not receive any important notifications from the app.")
                    .setPositiveButton("OK", null)
                    .create()
                alertDialog.show()

                // Log that the alert dialog was shown
                Log.d("MainActivity", "AlertDialog shown informing the user that notifications are disabled.")
            }
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