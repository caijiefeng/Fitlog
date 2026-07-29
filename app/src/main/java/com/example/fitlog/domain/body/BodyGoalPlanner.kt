package com.example.fitlog.domain.body

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyGoalPlanner @Inject constructor() {

    /**
     * Plans a body composition goal based on the formula:
     * fatMass = weight * bodyFat
     * leanMass = weight - fatMass
     * targetWeight = leanMass / (1 - targetFat)
     *
     * @param currentWeightKg Current body weight in kg.
     * @param currentBodyFatPercent Current body fat percentage (e.g., 25.0 for 25%).
     * @param targetBodyFatPercent Target body fat percentage (e.g., 15.0 for 15%).
     * @param tdee Total Daily Energy Expenditure used for calorie recommendation.
     * @return A [BodyGoalPlan] with estimated timeline and calorie target.
     */
    fun plan(
        currentWeightKg: Double,
        currentBodyFatPercent: Double,
        targetBodyFatPercent: Double,
        tdee: Int = 2500,
    ): BodyGoalPlan {
        val currentFatFraction = currentBodyFatPercent / 100.0
        val targetFatFraction = targetBodyFatPercent / 100.0

        val fatMass = currentWeightKg * currentFatFraction
        val leanMass = currentWeightKg - fatMass
        val targetWeightKg = leanMass / (1.0 - targetFatFraction)

        val weightDifferenceKg = targetWeightKg - currentWeightKg
        val fatToLoseKg = fatMass - (targetWeightKg * targetFatFraction)

        // Estimate weeks at a rate of 0.5 kg fat loss per week
        // For fat loss, use 0.5 kg/week. For gain, use a proportionate estimate.
        val weeklyRate = 0.5
        val estimatedWeeks = if (fatToLoseKg > 0) {
            (fatToLoseKg / weeklyRate).toInt().coerceAtLeast(1)
        } else {
            // Gain phase: lean gain ~0.25 kg/week
            ((-weightDifferenceKg) / 0.25).toInt().coerceAtLeast(1)
        }

        val recommendedDailyCalories = tdee - 400

        return BodyGoalPlan(
            currentWeightKg = currentWeightKg,
            targetWeightKg = targetWeightKg,
            weightDifferenceKg = weightDifferenceKg,
            fatToLoseKg = fatToLoseKg.coerceAtLeast(0.0),
            estimatedWeeks = estimatedWeeks,
            recommendedDailyCalories = recommendedDailyCalories,
        )
    }
}
