package com.example.fitlog.domain.nutrition

import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.BodyMeasurement
import com.example.fitlog.domain.body.UserProfile
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for all nutrition target calculations.
 *
 * Uses Mifflin-St Jeor BMR, activity factors, and goal-based adjustments
 * to compute daily calorie and macro targets.
 */
@Singleton
class NutritionTargetCalculator @Inject constructor() {

    /**
     * Calculates Basal Metabolic Rate using the Mifflin-St Jeor equation.
     *
     * For males:   BMR = 10 * weightKg + 6.25 * heightCm - 5 * age + 5
     * For females: BMR = 10 * weightKg + 6.25 * heightCm - 5 * age - 161
     * For other:   uses the average of male and female formulas
     */
    fun calculateBMR(gender: String, weightKg: Double, heightCm: Double, age: Int): Int {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        val raw = when (gender.uppercase()) {
            "MALE" -> base + 5
            "FEMALE" -> base - 161
            else -> ((base + 5) + (base - 161)) / 2.0
        }
        return (Math.round(raw / 5.0) * 5).toInt()
    }

    /**
     * Calculates Total Daily Energy Expenditure by applying an activity factor.
     * Rounds result to nearest 5.
     */
    fun calculateTDEE(bmr: Int, activityLevel: ActivityLevel): Int {
        return (bmr * activityLevel.factor).toInt()
            .let { (it / 5) * 5 }
    }

    /**
     * Computes complete daily nutrition targets from user profile, measurement, and goal.
     *
     * @param gender User's gender ("MALE", "FEMALE", or other).
     * @param weightKg Current body weight in kilograms.
     * @param heightCm Height in centimeters.
     * @param age Age in years.
     * @param activityLevel Activity level for TDEE multiplier.
     * @param goalType Fitness goal determining calorie surplus/deficit.
     * @param bodyFatPercent Optional body fat percentage (currently not used in calculation).
     * @return [NutritionTargets] with BMR, TDEE, target calories, and macro split.
     */
    fun calculateTargets(
        gender: String,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        bodyFatPercent: Double? = null,
    ): NutritionTargets {
        val bmr = calculateBMR(gender, weightKg, heightCm, age)
        val tdee = calculateTDEE(bmr, activityLevel)

        // Target calories based on goal type
        val targetCalories = when (goalType) {
            GoalType.FAT_LOSS -> {
                val deficit = minOf((tdee * 0.15).toInt(), 500)
                (tdee - deficit).let { (it / 5) * 5 }
            }
            GoalType.MAINTAIN -> tdee
            GoalType.LEAN_GAIN -> {
                val surplus = ((tdee * 0.07).toInt()).coerceIn(150, 300)
                (tdee + surplus).let { (it / 5) * 5 }
            }
            GoalType.MUSCLE_GAIN -> {
                val surplus = ((tdee * 0.10).toInt()).coerceIn(200, 350)
                (tdee + surplus).let { (it / 5) * 5 }
            }
        }

        // Protein based on goal type and body weight
        val proteinPerKg = when (goalType) {
            GoalType.MAINTAIN -> 1.6
            GoalType.LEAN_GAIN -> 1.8
            GoalType.MUSCLE_GAIN -> 1.8
            GoalType.FAT_LOSS -> 2.0
        }
        val proteinG = Math.round(proteinPerKg * weightKg).toInt()

        // Fat: 0.8g/kg, capped so that fat is 20-35% of total calories
        val fatGFromWeight = 0.8 * weightKg
        val fatGMax = (targetCalories * 0.35 / 9.0)
        val fatGMin = (targetCalories * 0.20 / 9.0)
        val fatG = fatGFromWeight.coerceIn(fatGMin, fatGMax).toInt()

        // Carbs: remaining calories / 4
        val proteinCalories = proteinG * 4
        val fatCalories = fatG * 9
        val remainingCalories = targetCalories - proteinCalories - fatCalories
        val carbsG = (remainingCalories / 4).coerceAtLeast(0)

        return NutritionTargets(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG,
        )
    }

    /**
     * Convenience method that extracts parameters from domain models.
     */
    fun calculateTargets(
        profile: UserProfile,
        measurement: BodyMeasurement,
        referenceDate: LocalDate = LocalDate.now(),
    ): NutritionTargets? {
        val weightKg = measurement.weightKg ?: return null
        val heightCm = profile.heightCm ?: return null
        val age = ChronoUnit.YEARS.between(profile.birthday, referenceDate).toInt()
        return calculateTargets(
            gender = profile.gender,
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            activityLevel = profile.activityLevel,
            goalType = profile.goalType,
            bodyFatPercent = measurement.bodyFatPercent,
        )
    }
}
