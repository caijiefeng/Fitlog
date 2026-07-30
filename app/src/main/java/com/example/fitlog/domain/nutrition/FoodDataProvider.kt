package com.example.fitlog.domain.nutrition

/**
 * Interface for providing food nutrition data from various sources.
 */
interface FoodDataProvider {

    /**
     * Search for foods matching the given query string.
     * Results should be sorted by relevance.
     */
    suspend fun search(query: String): List<FoodSearchResult>

    /**
     * Get detailed nutrition info for a specific food by its ID.
     */
    suspend fun getFood(id: String): FoodNutrition?
}
