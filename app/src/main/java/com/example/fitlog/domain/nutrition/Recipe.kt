package com.example.fitlog.domain.nutrition

/**
 * A single ingredient in a recipe, referencing a food by its provider ID
 * with the weight used in the recipe.
 */
data class RecipeIngredient(
    val foodId: String,
    val foodName: String,
    val weightGrams: Double,
)

/**
 * A recipe composed of multiple ingredients.
 *
 * @property id Unique identifier (empty for new recipes).
 * @property name Display name of the recipe.
 * @property ingredients List of ingredients with weights.
 * @property servings Number of servings the recipe yields.
 */
data class Recipe(
    val id: String = "",
    val name: String,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val servings: Int = 1,
)

/**
 * Per-serving and total nutrition for a recipe.
 */
data class RecipeNutrition(
    val perServing: FoodNutrition,
    val total: FoodNutrition,
)

/**
 * Calculates recipe nutrition from ingredients using a [FoodDataProvider].
 */
object RecipeCalculator {

    /**
     * Computes total and per-serving nutrition for the given recipe.
     *
     * @param recipe The recipe to calculate nutrition for.
     * @param provider Food data provider to look up nutrition per ingredient.
     * @return [RecipeNutrition] with per-serving and total values, or null
     *         if any ingredient cannot be found in the provider.
     */
    suspend fun calculateNutrition(
        recipe: Recipe,
        provider: FoodDataProvider,
    ): RecipeNutrition? {
        if (recipe.ingredients.isEmpty() || recipe.servings <= 0) return null

        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var totalWeightG = 0.0

        for (ingredient in recipe.ingredients) {
            val food = provider.getFood(ingredient.foodId) ?: return null
            val factor = ingredient.weightGrams / 100.0
            totalCalories += food.caloriesPer100g * factor
            totalProtein += food.proteinPer100g * factor
            totalCarbs += food.carbsPer100g * factor
            totalFat += food.fatPer100g * factor
            totalWeightG += ingredient.weightGrams
        }

        val perServingWeightG = totalWeightG / recipe.servings

        // Per-100g nutrition values (same for total and per-serving)
        val calsPer100g = if (totalWeightG > 0) totalCalories / (totalWeightG / 100.0) else 0.0
        val proteinPer100g = if (totalWeightG > 0) totalProtein / (totalWeightG / 100.0) else 0.0
        val carbsPer100g = if (totalWeightG > 0) totalCarbs / (totalWeightG / 100.0) else 0.0
        val fatPer100g = if (totalWeightG > 0) totalFat / (totalWeightG / 100.0) else 0.0

        val total = FoodNutrition(
            id = "recipe_${recipe.id}_total",
            name = "${recipe.name} (总量)",
            category = "食谱",
            caloriesPer100g = calsPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            servingSizeG = totalWeightG,
            servingDesc = "总量",
        )

        val perServing = FoodNutrition(
            id = "recipe_${recipe.id}_serving",
            name = "${recipe.name} (每份)",
            category = "食谱",
            caloriesPer100g = calsPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            servingSizeG = perServingWeightG,
            servingDesc = "1份 (${recipe.servings}份中的1份)",
        )

        return RecipeNutrition(
            perServing = perServing,
            total = total,
        )
    }
}
