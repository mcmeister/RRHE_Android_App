package com.example.rrhe

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.rrhe.databinding.ItemPlantBinding

class PlantAdapter(private val plants: List<Plant>) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(private val binding: ItemPlantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plant: Plant) {
            binding.plantName.text = plant.NameConcat
            binding.plantStock.text = binding.root.context.getString(R.string.stock_text, plant.StockQty)
            binding.plantStockId.text = binding.root.context.getString(R.string.stock_id_text, plant.StockID)

            val requestOptions = RequestOptions()
                .circleCrop()
                .placeholder(R.drawable.circle_shape)  // Use circle_shape as the placeholder
                .error(R.drawable.circle_shape)        // Use circle_shape as the error image

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

            binding.plantImage.setOnClickListener {
                val activity = it.context as FragmentActivity
                val dialog = ImageDialogFragment.newInstance(plant.PhotoLink1)
                dialog.show(activity.supportFragmentManager, "imageDialog")
            }

            binding.root.setOnClickListener {
                val intent = Intent(binding.root.context, PlantDetailsActivity::class.java).apply {
                    putExtra("NameConcat", plant.NameConcat)
                    putExtra("StockID", plant.StockID)
                    putExtra("StockQty", plant.StockQty)
                    putExtra("PhotoLink1", plant.PhotoLink1)
                }
                binding.root.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(plants[position])
    }

    override fun getItemCount(): Int {
        return plants.size
    }
}
