package com.example.rrhe

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.rrhe.databinding.ActivityEditPlantBinding
import com.example.rrhe.databinding.ActivityNewPlantBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PlantValueCalculator {
    private const val TAG = "PlantValueCalculator"

    // Overloaded function to handle both EditPlant and NewPlant activities
    fun recalculateValues(
        binding: ActivityEditPlantBinding,
        lifecycleScope: LifecycleCoroutineScope,
        context: Context
    ) {
        recalculateValuesCommon(EditPlantBindingWrapper(binding), lifecycleScope, context)
    }

    fun recalculateValues(
        binding: ActivityNewPlantBinding,
        lifecycleScope: LifecycleCoroutineScope,
        context: Context
    ) {
        recalculateValuesCommon(NewPlantBindingWrapper(binding), lifecycleScope, context)
    }

    // Common function to handle the shared logic
    private fun recalculateValuesCommon(
        binding: PlantBindingWrapper,
        lifecycleScope: LifecycleCoroutineScope,
        context: Context
    ) {
        val stockPrice = binding.getStockPrice().toIntOrNull() ?: return
        val stockQty = binding.getStockQty().toIntOrNull() ?: return

        val totalValue = stockPrice * stockQty
        binding.setTotalValue(context.getString(R.string.total_value_edit_text, totalValue))

        Log.d(TAG, "Recalculating values: stockPrice=$stockPrice, stockQty=$stockQty, totalValue=$totalValue")

        lifecycleScope.launch {
            try {
                val (latestUSD, latestEUR) = withContext(Dispatchers.IO) {
                    PlantRepository.getLatestExchangeRatesFromLocal()
                }
                val usdValue = (stockPrice * latestUSD).toInt()
                val eurValue = (stockPrice * latestEUR).toInt()

                Log.d(TAG, "Fetched exchange rates: latestUSD=$latestUSD, latestEUR=$latestEUR")
                Log.d(TAG, "Calculated USD and EUR values: usdValue=$usdValue, eurValue=$eurValue")

                withContext(Dispatchers.Main) {
                    binding.setUSD(context.getString(R.string.usd_edit_text, usdValue))
                    binding.setEUR(context.getString(R.string.eur_edit_text, eurValue))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching exchange rates: ${e.message}", e)
            }
        }

        // Translate Family, Species, and Subspecies to Thai and update ThaiName
        lifecycleScope.launch {
            try {
                val familyThai = withContext(Dispatchers.IO) {
                    TranslationHelper.translateToThai(binding.getFamily())
                }
                val speciesThai = withContext(Dispatchers.IO) {
                    TranslationHelper.translateToThai(binding.getSpecies())
                }
                val subspeciesThai = if (binding.getSubspecies().isBlank()) {
                    ""
                } else {
                    withContext(Dispatchers.IO) {
                        TranslationHelper.translateToThai(binding.getSubspecies())
                    }
                }

                val thaiName = if (subspeciesThai.isEmpty()) {
                    context.getString(R.string.thai_name_format_no_subspecies, familyThai, speciesThai)
                } else {
                    context.getString(R.string.thai_name_format, familyThai, speciesThai, subspeciesThai)
                }

                Log.d(TAG, "Translated to Thai: familyThai=$familyThai, speciesThai=$speciesThai, subspeciesThai=$subspeciesThai")

                withContext(Dispatchers.Main) {
                    binding.setThaiName(thaiName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error translating to Thai: ${e.message}", e)
            }
        }

        // Concatenate Family, Species, and Subspecies in English and update NameConcat
        val family = binding.getFamily()
        val species = binding.getSpecies()
        val subspecies = binding.getSubspecies()

        val nameConcat = if (subspecies.isEmpty()) {
            "$family $species"
        } else {
            "$family $species $subspecies"
        }

        Log.d(TAG, "Concatenated names: nameConcat=$nameConcat")

        binding.setNameConcat(nameConcat)
    }

    // Wrapper interface to abstract common binding functions
    interface PlantBindingWrapper {
        fun getStockPrice(): String
        fun getStockQty(): String
        fun getUSD(): String
        fun getEUR(): String
        fun setTotalValue(value: String)
        fun setUSD(value: String)
        fun setEUR(value: String)
        fun getFamily(): String
        fun getSpecies(): String
        fun getSubspecies(): String
        fun setThaiName(value: String)
        fun setNameConcat(value: String)
    }

    // Wrapper for EditPlantBinding
    class EditPlantBindingWrapper(private val binding: ActivityEditPlantBinding) : PlantBindingWrapper {
        override fun getStockPrice() = binding.stockPriceEditText.text.toString()
        override fun getStockQty() = binding.stockQtyEditText.text.toString()

        override fun getUSD(): String {
            val usdValue = binding.usdEditText.text.toString().replace(Regex("\\D"), "")
            return usdValue
        }

        override fun getEUR(): String {
            val eurValue = binding.eurEditText.text.toString().replace(Regex("\\D"), "")
            return eurValue
        }

        override fun setTotalValue(value: String) {
            binding.totalValueEditText.text = value
        }

        override fun setUSD(value: String) {
            binding.usdEditText.text = value
        }

        override fun setEUR(value: String) {
            binding.eurEditText.text = value
        }

        override fun getFamily() = binding.familyAutoCompleteTextView.text.toString()
        override fun getSpecies() = binding.speciesAutoCompleteTextView.text.toString()
        override fun getSubspecies() = binding.subspeciesAutoCompleteTextView.text.toString()
        override fun setThaiName(value: String) {
            binding.thaiNameText.text = value
        }
        override fun setNameConcat(value: String) {
            binding.nameConcatText.text = value
        }
    }

    // Wrapper for NewPlantBinding
    class NewPlantBindingWrapper(private val binding: ActivityNewPlantBinding) : PlantBindingWrapper {
        override fun getStockPrice() = binding.stockPriceEditText.text.toString()
        override fun getStockQty() = binding.stockQtyEditText.text.toString()
        override fun getUSD() = binding.usdEditText.text.toString()
        override fun getEUR() = binding.eurEditText.text.toString()
        override fun setTotalValue(value: String) {
            binding.totalValueEditText.text = value
        }
        override fun setUSD(value: String) {
            binding.usdEditText.text = value  // Ensure we set the value to the EditText
        }
        override fun setEUR(value: String) {
            binding.eurEditText.text = value  // Ensure we set the value to the EditText
        }
        override fun getFamily() = binding.familyAutoCompleteTextView.text.toString()
        override fun getSpecies() = binding.speciesAutoCompleteTextView.text.toString()
        override fun getSubspecies() = binding.subspeciesAutoCompleteTextView.text.toString()
        override fun setThaiName(value: String) {
            binding.thaiNameText.text = value
        }
        override fun setNameConcat(value: String) {
            binding.nameConcatText.text = value
        }
    }
}
