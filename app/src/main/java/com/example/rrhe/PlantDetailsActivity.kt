package com.example.rrhe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.rrhe.databinding.ActivityPlantDetailsBinding

class PlantDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nameConcat = intent.getStringExtra("NameConcat") ?: ""
        val stockID = intent.getIntExtra("StockID", 0)
        val stockQty = intent.getIntExtra("StockQty", 0)
        val photoLink1 = intent.getStringExtra("PhotoLink1") ?: ""

        binding.plantName.text = nameConcat
        binding.plantStockId.text = getString(R.string.stock_id_text, stockID)
        binding.plantStockQty.text = getString(R.string.stock_text, stockQty)

        Glide.with(this)
            .load(photoLink1)
            .error(R.drawable.error_image)
            .into(binding.plantImage)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.editButton.setOnClickListener {
            // Implement edit functionality later
        }
    }
}
