package com.example.fitlog.domain.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodPortionCalculatorTest {

    private val rice = FoodSearchResult(
        id = "staple_001",
        name = "白米饭",
        category = "主食",
        caloriesPer100g = 116.0,
        proteinPer100g = 2.6,
        carbsPer100g = 25.9,
        fatPer100g = 0.3,
        servingSizeG = 200.0,
        servingDesc = "1碗",
    )

    @Test
    fun `100g equals the per-100g values (factor 1)`() {
        val portion = FoodPortionCalculator.calculate(rice, 100.0)
        assertEquals(116.0, portion.calories, 1e-9)
        assertEquals(2.6, portion.protein, 1e-9)
        assertEquals(25.9, portion.carbs, 1e-9)
        assertEquals(0.3, portion.fat, 1e-9)
    }

    @Test
    fun `200g doubles the per-100g values (factor 2)`() {
        val portion = FoodPortionCalculator.calculate(rice, 200.0)
        assertEquals(232.0, portion.calories, 1e-9)
        assertEquals(5.2, portion.protein, 1e-9)
        assertEquals(51.8, portion.carbs, 1e-9)
        assertEquals(0.6, portion.fat, 1e-9)
    }

    @Test
    fun `50g halves the per-100g values (factor half)`() {
        val portion = FoodPortionCalculator.calculate(rice, 50.0)
        assertEquals(58.0, portion.calories, 1e-9)
        assertEquals(1.3, portion.protein, 1e-9)
        assertEquals(12.95, portion.carbs, 1e-9)
        assertEquals(0.15, portion.fat, 1e-9)
    }

    @Test
    fun `zero grams yields zero macros`() {
        val portion = FoodPortionCalculator.calculate(rice, 0.0)
        assertEquals(0.0, portion.calories, 1e-9)
        assertEquals(0.0, portion.protein, 1e-9)
        assertEquals(0.0, portion.carbs, 1e-9)
        assertEquals(0.0, portion.fat, 1e-9)
    }

    @Test
    fun `servings to grams scales by serving size`() {
        assertEquals(400.0, FoodPortionCalculator.servingToGrams(2.0, 200.0), 1e-9)
        assertEquals(100.0, FoodPortionCalculator.servingToGrams(0.5, 200.0), 1e-9)
    }

    @Test
    fun `grams to servings divides by serving size`() {
        assertEquals(1.5, FoodPortionCalculator.gramsToServings(300.0, 200.0), 1e-9)
        assertEquals(0.0, FoodPortionCalculator.gramsToServings(100.0, 0.0), 1e-9)
    }

    @Test
    fun `round-trip servings and grams agree`() {
        val grams = FoodPortionCalculator.servingToGrams(3.0, 200.0)
        assertEquals(3.0, FoodPortionCalculator.gramsToServings(grams, 200.0), 1e-9)
    }
}
