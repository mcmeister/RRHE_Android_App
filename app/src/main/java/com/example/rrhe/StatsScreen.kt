package com.example.rrhe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rrhe.ui.theme.RRHETheme

class StatsScreen : ComponentActivity() {

    // private lateinit var inactivityDetector: InactivityDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // inactivityDetector = InactivityDetector(this)
        setContent {
            val viewModel: StatsViewModel = viewModel()
            StatsScreenComposable(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // inactivityDetector.reset()
    }

    override fun onPause() {
        super.onPause()
        // inactivityDetector.stop()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // inactivityDetector.reset()
        return super.onTouchEvent(event)
    }
}

@Composable
fun StatsScreenComposable(viewModel: StatsViewModel = viewModel()) {
    RRHETheme {
        val stats by viewModel.stats.collectAsState()

        // Get the colors from the theme
        val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
        val textColor = MaterialTheme.colorScheme.onBackground.toArgb()

        LaunchedEffect(Unit) {
            viewModel.syncWithMainDatabase()
        }

        AndroidView(factory = { context ->
            val parent = FrameLayout(context)
            LayoutInflater.from(context).inflate(R.layout.screen_stats, parent, false)
        }, update = { view ->
            // Set background color for the parent layout
            view.setBackgroundColor(backgroundColor)

            stats?.let {
                view.findViewById<TextView>(R.id.statsTitle).apply {
                    text = context.getString(R.string.stats)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.totalRowsTextView).apply {
                    text = context.getString(R.string.total_rows, it.totalRows)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.totalPlantsTextView).apply {
                    text = context.getString(R.string.total_plants, it.totalPlants)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.totalNonMTextView).apply {
                    text = context.getString(R.string.total_non_m, it.totalNonM)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.totalMTextView).apply {
                    text = context.getString(R.string.total_m, it.totalM)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.nonMValueTextView).apply {
                    text = context.getString(R.string.non_m_value, it.nonMValue)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.mValueTextView).apply {
                    text = context.getString(R.string.m_value, it.mValue)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.totalValueTextView).apply {
                    text = context.getString(R.string.total_value, it.totalValue)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.webPlantsTextView).apply {
                    text = context.getString(R.string.web_plants, it.webPlants)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.webQtyTextView).apply {
                    text = context.getString(R.string.web_qty, it.webQty)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.webValueTextView).apply {
                    text = context.getString(R.string.web_value, it.webValue)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.ratesTextView).apply {
                    text = context.getString(R.string.rates_text)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.usdTextView).apply {
                    text = context.getString(R.string.usd, it.usd)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.eurTextView).apply {
                    text = context.getString(R.string.eur, it.eur)
                    setTextColor(textColor)
                }
                view.findViewById<TextView>(R.id.stampTextView).apply {
                    text = context.getString(R.string.stats_stamp, it.stamp)
                    setTextColor(textColor)
                }
            }

            // Reset inactivity timer when user interacts with the screen
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    // inactivityDetector.reset()
                    v.performClick()
                }
                false
            }
        })
    }
}
