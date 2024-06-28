package com.example.rrhe

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.rrhe.databinding.ItemPlantDetailsBinding

class PlantDetailsAdapter(private val plants: List<Plant>) : RecyclerView.Adapter<PlantDetailsAdapter.PlantDetailsViewHolder>() {

    class PlantDetailsViewHolder(private val binding: ItemPlantDetailsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plant: Plant) {
            binding.plantName.text = plant.NameConcat
            binding.plantStockId.text = binding.root.context.getString(R.string.stock_id_text, plant.StockID)
            binding.plantStockQty.text = binding.root.context.getString(R.string.stock_text, plant.StockQty)

            val requestOptions = RequestOptions()
                .circleCrop()
                .placeholder(R.drawable.circle_shape)
                .error(R.drawable.circle_shape)

            Glide.with(binding.plantImage.context)
                .load(plant.PhotoLink1)
                .apply(requestOptions)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("Glide", "Image load failed", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(binding.plantImage)

            binding.backButton.setOnClickListener {
                (binding.root.context as? PlantDetailsActivity)?.finish()
            }

            binding.editButton.setOnClickListener {
                // Implement edit functionality later
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantDetailsViewHolder {
        val binding = ItemPlantDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantDetailsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantDetailsViewHolder, position: Int) {
        holder.bind(plants[position])
    }

    override fun getItemCount(): Int {
        return plants.size
    }
}
