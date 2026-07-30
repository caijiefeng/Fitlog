package com.example.fitlog.data.nutrition

import com.example.fitlog.domain.nutrition.FoodDataProvider
import com.example.fitlog.domain.nutrition.FoodNutrition
import com.example.fitlog.domain.nutrition.FoodSearchResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder remote food data provider.
 * No API keys are embedded in the code. This class exists as a stub
 * for future integration with a food API service.
 */
@Singleton
class RemoteFoodDataProvider @Inject constructor() : FoodDataProvider {

    override suspend fun search(query: String): List<FoodSearchResult> {
        // Stub — returns empty until a remote API is integrated
        return emptyList()
    }

    override suspend fun getFood(id: String): FoodNutrition? {
        // Stub — returns null until a remote API is integrated
        return null
    }
}
