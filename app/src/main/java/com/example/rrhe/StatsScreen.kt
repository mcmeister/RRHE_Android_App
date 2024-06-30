package com.example.rrhe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

class StatsScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: StatsViewModel = viewModel()
            StatsScreenComposable(viewModel)
        }
    }
}

@Composable
fun StatsScreenComposable(viewModel: StatsViewModel = viewModel()) {
    val stats by viewModel.stats.collectAsState()

    AndroidView(factory = { context ->
        LayoutInflater.from(context).inflate(R.layout.screen_stats, null)
    }, update = { view ->
        stats?.let {
            view.findViewById<TextView>(R.id.totalRowsTextView).text = "Total Rows: ${it.totalRows}"
            view.findViewById<TextView>(R.id.totalPlantsTextView).text = "Total Plants: ${it.totalPlants}"
            view.findViewById<TextView>(R.id.totalNonMTextView).text = "Total Non-Mother: ${it.totalNonM}"
            view.findViewById<TextView>(R.id.totalMTextView).text = "Total Mother: ${it.totalM}"
            view.findViewById<TextView>(R.id.nonMValueTextView).text = "Non-Mother Value: THB ${formatValue(it.nonMValue)}"
            view.findViewById<TextView>(R.id.mValueTextView).text = "Mother Value: THB ${formatValue(it.mValue)}"
            view.findViewById<TextView>(R.id.totalValueTextView).text = "Total Value: THB ${formatValue(it.totalValue)}"
            view.findViewById<TextView>(R.id.webPlantsTextView).text = "Web Plants: ${it.webPlants}"
            view.findViewById<TextView>(R.id.webQtyTextView).text = "Web Quantity: ${it.webQty}"
            view.findViewById<TextView>(R.id.webValueTextView).text = "Web Value: THB ${formatValue(it.webValue)}"
            view.findViewById<TextView>(R.id.usdTextView).text = "USD: ${it.usd}"
            view.findViewById<TextView>(R.id.eurTextView).text = "EUR: ${it.eur}"
        }
    })
}

fun formatValue(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format("%,.0f", value) // No decimal part
    } else {
        String.format("%,.2f", value) // Keep two decimal places if necessary
    }
}
