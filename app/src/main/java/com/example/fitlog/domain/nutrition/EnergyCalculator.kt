package com.example.fitlog.domain.nutrition

import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.BodyMeasurement
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.UserProfile
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnergyCalculator @Inject constructor() {

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
     */
    fun calculateTDEE(bmr: Int, activityLevel: ActivityLevel): Int {
        return (bmr * activityLevel.factor).toInt()
            .let { (it / 5) * 5 } // round to nearest 5
    }

    /**
     * Calculates a full energy summary including macros.
     *
     * @param profile The user's profile (gender, birthday, activity level, goal type, height).
     * @param measurement The most recent body measurement (used for weight and body fat).
     * @return [EnergySummary] containing BMR, TDEE, target calories, and macro split.
     */
    fun calculateEnergySummary(profile: UserProfile, measurement: BodyMeasurement): EnergySummary {
        val age = ChronoUnit.YEARS.between(profile.birthday, LocalDate.now()).toInt()
        val weightKg = measurement.weightKg ?: 75.0 // fallback
        val heightCm = profile.heightCm ?: 175.0 // fallback

        val bmr = calculateBMR(profile.gender, weightKg, heightCm, age)
        val tdee = calculateTDEE(bmr, profile.activityLevel)

        val targetCalories = when (profile.goalType) {
            GoalType.FAT_LOSS -> tdee - 400
            GoalType.MAINTAIN -> tdee
            GoalType.LEAN_GAIN -> tdee + 250
            GoalType.MUSCLE_GAIN -> tdee + 300
        }

        val proteinG = (1.8 * weightKg).toInt()
        val fatG = (0.9 * weightKg).toInt()
        val proteinCalories = proteinG * 4
        val fatCalories = fatG * 9
        val remainingCalories = targetCalories - proteinCalories - fatCalories
        val carbsG = (remainingCalories / 4).coerceAtLeast(0)

        return EnergySummary(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
        )
    }
}
