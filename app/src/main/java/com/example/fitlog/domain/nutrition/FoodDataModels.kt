package com.example.fitlog.domain.nutrition

data class FoodSearchResult(
    val id: String,
    val name: String,
    val category: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val servingSizeG: Double? = null,
    val servingDesc: String? = null,
)

data class FoodNutrition(
    val id: String,
    val name: String,
    val category: String = "",
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val servingSizeG: Double? = null,
    val servingDesc: String? = null,
)
