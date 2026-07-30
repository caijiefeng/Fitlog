package com.example.fitlog.data.nutrition

import android.content.Context
import com.example.fitlog.domain.nutrition.FoodDataProvider
import com.example.fitlog.domain.nutrition.FoodNutrition
import com.example.fitlog.domain.nutrition.FoodSearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFoodDataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : FoodDataProvider {

    private var foods: List<FoodNutrition>? = null

    private suspend fun loadFoods(): List<FoodNutrition> {
        return withContext(Dispatchers.IO) {
            foods ?: run {
                val json = context.assets.open("common_foods_zh.json")
                    .bufferedReader().use { it.readText() }
                val array = JSONArray(json)
                val list = mutableListOf<FoodNutrition>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseFood(obj))
                }
                foods = list
                list
            }
        }
    }

    override suspend fun search(query: String): List<FoodSearchResult> {
        if (query.isBlank()) return emptyList()
        val queryLower = query.lowercase()
        val all = loadFoods()
        return all.filter { it.name.lowercase().contains(queryLower) }
            .map { it.toSearchResult() }
    }

    override suspend fun getFood(id: String): FoodNutrition? {
        val all = loadFoods()
        return all.find { it.id == id }
    }

    private fun parseFood(obj: JSONObject): FoodNutrition {
        return FoodNutrition(
            id = obj.getString("id"),
            name = obj.getString("name"),
            category = obj.optString("category", ""),
            caloriesPer100g = obj.getDouble("calories"),
            proteinPer100g = obj.getDouble("protein"),
            carbsPer100g = obj.getDouble("carbs"),
            fatPer100g = obj.getDouble("fat"),
            servingSizeG = if (obj.has("serving_size_g")) obj.getDouble("serving_size_g") else null,
            servingDesc = if (obj.has("serving_desc")) obj.getString("serving_desc") else null,
        )
    }

    private fun FoodNutrition.toSearchResult(): FoodSearchResult {
        return FoodSearchResult(
            id = id,
            name = name,
            category = category,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
        )
    }
}
