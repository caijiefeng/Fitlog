package com.example.fitlog.domain.nutrition

/**
 * Macros for a consumed portion of a food.
 */
data class FoodPortion(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/**
 * Pure per-portion macro math. Nutrition data is stored per 100 g, so a
 * portion of [grams] g scales every value by `factor = grams / 100.0`.
 */
object FoodPortionCalculator {

    fun calculate(food: FoodSearchResult, grams: Double): FoodPortion {
        val factor = grams / 100.0
        return FoodPortion(
            calories = food.caloriesPer100g * factor,
            protein = food.proteinPer100g * factor,
            carbs = food.carbsPer100g * factor,
            fat = food.fatPer100g * factor,
        )
    }

    /** Grams consumed for [quantity] servings of a food with the given serving size. */
    fun servingToGrams(quantity: Double, servingSizeG: Double): Double {
        return quantity * servingSizeG
    }

    /** Servings equivalent to [grams] with the given serving size. */
    fun gramsToServings(grams: Double, servingSizeG: Double): Double {
        if (servingSizeG <= 0.0) return 0.0
        return grams / servingSizeG
    }
}
