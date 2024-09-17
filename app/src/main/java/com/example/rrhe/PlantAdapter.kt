package com.example.rrhe

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.rrhe.databinding.ItemPlantBinding

class PlantAdapter(
    private var plants: List<Plant>,
    private val inactivityDetector: InactivityDetector,
    private val textColor: Int // Pass the text color as a parameter
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    private var onItemClickListener: ((Plant) -> Unit)? = null

    fun setOnItemClickListener(listener: (Plant) -> Unit) {
        onItemClickListener = listener
    }

    inner class PlantViewHolder(private val binding: ItemPlantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plant: Plant) {
            binding.plantName.text = plant.NameConcat
            binding.plantName.setTextColor(textColor)
            binding.plantStock.text = binding.root.context.getString(R.string.stock_qty, plant.StockQty)
            binding.plantStock.setTextColor(textColor)
            binding.plantStockId.text = binding.root.context.getString(R.string.stock_id, plant.StockID)
            binding.plantStockId.setTextColor(textColor)
            binding.plantStockPrice.text = binding.root.context.getString(R.string.stock_price, plant.StockPrice)
            binding.plantStockPrice.setTextColor(textColor)

            val context = binding.plantImage.context
            val baseUrl = ApiConfig.getHttpServerBaseUrl()

            // Construct the full URL for Photo1
            val photoUrl = if (plant.Photo1?.startsWith("http://") == true || plant.Photo1?.startsWith("https://") == true) {
                plant.Photo1  // Already a full URL
            } else {
                baseUrl + plant.Photo1  // Prepend the base URL
            }

            val requestOptions = RequestOptions()
                .circleCrop()
                .placeholder(R.drawable.circle_shape) // Use a circular placeholder
                .error(R.drawable.circle_shape) // Use the same circular placeholder for errors

            Glide.with(context)
                .load(photoUrl)
                .apply(requestOptions)
                .into(binding.plantImage)

            // Click listener for the root view to navigate to plant details
            binding.root.setOnClickListener {
                inactivityDetector.reset()  // Reset inactivity timer on plant item click
                val ctx = it.context
                val intent = Intent(ctx, PlantDetailsActivity::class.java).apply {
                    putExtra("plant", plant)
                }
                ctx.startActivity(intent)
            }

            // Click listener for the plant image to open in fullscreen
            binding.plantImage.setOnClickListener {
                inactivityDetector.reset()  // Reset inactivity timer on image click
                val fragment = ImageDialogFragment.newInstance(photoUrl)
                (context as AppCompatActivity).supportFragmentManager
                    .beginTransaction()
                    .add(fragment, "imageDialog")
                    .commitAllowingStateLoss()
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

    fun updatePlants(newPlants: List<Plant>) {
        val diffCallback = PlantDiffCallback(plants, newPlants)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.plants = newPlants
        diffResult.dispatchUpdatesTo(this)
    }

    private class PlantDiffCallback(
        private val oldList: List<Plant>,
        private val newList: List<Plant>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].StockID == newList[newItemPosition].StockID
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldList[oldItem] == newList[newItem]
        }
    }
}
