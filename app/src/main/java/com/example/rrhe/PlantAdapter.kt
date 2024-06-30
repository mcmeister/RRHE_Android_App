package com.example.rrhe

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.rrhe.databinding.ItemPlantBinding

class PlantAdapter(private var plants: List<Plant>) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    private var onItemClickListener: ((Plant) -> Unit)? = null

    fun setOnItemClickListener(listener: (Plant) -> Unit) {
        onItemClickListener = listener
    }

    fun updatePlants(newPlants: List<Plant>) {
        val diffCallback = PlantDiffCallback(plants, newPlants)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.plants = newPlants
        diffResult.dispatchUpdatesTo(this)
        Log.d("PlantAdapter", "updatePlants called with ${newPlants.size} plants")
    }

    inner class PlantViewHolder(private val binding: ItemPlantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plant: Plant) {
            binding.plantName.text = plant.NameConcat
            binding.plantStock.text = binding.root.context.getString(R.string.stock_text, plant.StockQty)
            binding.plantStockId.text = binding.root.context.getString(R.string.stock_id_text, plant.StockID)

            val requestOptions = RequestOptions()
                .circleCrop()

            val photoLink = plant.PhotoLink1
            if (photoLink.isNullOrEmpty() || !photoLink.startsWith("http://") && !photoLink.startsWith("https://")) {
                // Clear the image if the photo link is invalid
                binding.plantImage.setImageDrawable(null)
            } else {
                // Load the actual image
                Glide.with(binding.plantImage.context)
                    .load(photoLink)
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
            }

            binding.plantImage.setOnClickListener {
                val context = it.context
                if (context is FragmentActivity) {
                    val dialog = ImageDialogFragment.newInstance(if (photoLink.isNullOrEmpty() || !photoLink.startsWith("http://") && !photoLink.startsWith("https://")) null else photoLink)
                    dialog.show(context.supportFragmentManager, "imageDialog")
                } else {
                    Log.e("PlantAdapter", "Context is not a FragmentActivity")
                }
            }

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(plant)
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

    override fun getItemCount(): Int = plants.size

    private class PlantDiffCallback(
        private val oldList: List<Plant>,
        private val newList: List<Plant>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].StockID == newList[newItemPosition].StockID
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldList[oldItem] == newList[newItem]
        }
    }
}
