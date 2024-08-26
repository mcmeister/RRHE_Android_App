package com.example.rrhe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rrhe.ui.theme.RRHETheme

class StatsScreen : ComponentActivity() {

    private lateinit var inactivityDetector: InactivityDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inactivityDetector = InactivityDetector(this)
        setContent {
            val viewModel: StatsViewModel = viewModel()
            StatsScreenComposable(viewModel, inactivityDetector)  // Pass inactivityDetector to Composable
        }
    }

    override fun onResume() {
        super.onResume()
        inactivityDetector.reset()
    }

    override fun onPause() {
        super.onPause()
        inactivityDetector.stop()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        inactivityDetector.reset()
        return super.onTouchEvent(event)
    }
}

@Composable
fun StatsScreenComposable(viewModel: StatsViewModel = viewModel(), inactivityDetector: InactivityDetector) {
    RRHETheme {
        val stats by viewModel.stats.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.syncWithMainDatabase()
        }

        AndroidView(factory = { context ->
            val parent = FrameLayout(context)
            LayoutInflater.from(context).inflate(R.layout.screen_stats, parent, false)
        }, update = { view ->
            val context = view.context
            stats?.let {
                view.findViewById<TextView>(R.id.totalRowsTextView).text =
                    context.getString(R.string.total_rows, it.totalRows)
                view.findViewById<TextView>(R.id.totalPlantsTextView).text =
                    context.getString(R.string.total_plants, it.totalPlants)
                view.findViewById<TextView>(R.id.totalNonMTextView).text =
                    context.getString(R.string.total_non_m, it.totalNonM)
                view.findViewById<TextView>(R.id.totalMTextView).text =
                    context.getString(R.string.total_m, it.totalM)
                view.findViewById<TextView>(R.id.nonMValueTextView).text =
                    context.getString(R.string.non_m_value, it.nonMValue)
                view.findViewById<TextView>(R.id.mValueTextView).text =
                    context.getString(R.string.m_value, it.mValue)
                view.findViewById<TextView>(R.id.totalValueTextView).text =
                    context.getString(R.string.total_value, it.totalValue)
                view.findViewById<TextView>(R.id.webPlantsTextView).text =
                    context.getString(R.string.web_plants, it.webPlants)
                view.findViewById<TextView>(R.id.webQtyTextView).text =
                    context.getString(R.string.web_qty, it.webQty)
                view.findViewById<TextView>(R.id.webValueTextView).text =
                    context.getString(R.string.web_value, it.webValue)
                view.findViewById<TextView>(R.id.ratesTextView).text =
                    context.getString(R.string.rates_text)
                view.findViewById<TextView>(R.id.usdTextView).text =
                    context.getString(R.string.usd, it.usd)
                view.findViewById<TextView>(R.id.eurTextView).text =
                    context.getString(R.string.eur, it.eur)
                view.findViewById<TextView>(R.id.stampTextView).text =
                    context.getString(R.string.stats_stamp, it.stamp)
            }

            // Reset inactivity timer when user interacts with the screen (e.g., scrolls)
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    inactivityDetector.reset()
                    v.performClick()  // Perform click action if needed
                }
                false
            }
        })
    }
}