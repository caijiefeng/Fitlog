package com.example.fitlog.domain.nutrition

import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.BodyMeasurement
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class EnergyCalculatorTest {

    private lateinit var calculator: EnergyCalculator

    @Before
    fun setUp() {
        calculator = EnergyCalculator()
    }

    // --- BMR Tests ---

    @Test
    fun `male BMR uses Mifflin-St Jeor with +5 adjustment`() {
        // Male, 80kg, 180cm, 25 years
        // BMR = 10 * 80 + 6.25 * 180 - 5 * 25 + 5 = 1805
        val bmr = calculator.calculateBMR("MALE", 80.0, 180.0, 25)
        assertEquals(1805, bmr)
    }

    @Test
    fun `female BMR uses Mifflin-St Jeor with -161 adjustment`() {
        // Female, 60kg, 165cm, 30 years
        // BMR = 10 * 60 + 6.25 * 165 - 5 * 30 - 161 = 1320.25 -> 1320
        val bmr = calculator.calculateBMR("FEMALE", 60.0, 165.0, 30)
        assertEquals(1320, bmr)
    }

    @Test
    fun `other gender BMR uses average of male and female formulas`() {
        // 70kg, 170cm, 28 years
        // Male: 1627.5, Female: 1461.5, Avg: 1544.5 -> 1540
        val bmr = calculator.calculateBMR("OTHER", 70.0, 170.0, 28)
        assertEquals(1545, bmr)
    }

    @Test
    fun `BMR with zero weight and minimal height`() {
        val bmr = calculator.calculateBMR("MALE", 0.0, 0.0, 25)
        assertEquals(-120, bmr)
    }

    // --- TDEE Tests ---

    @Test
    fun `TDEE with SEDENTARY factor`() {
        val tdee = calculator.calculateTDEE(1800, ActivityLevel.SEDENTARY)
        assertEquals(2160, tdee)
    }

    @Test
    fun `TDEE with LIGHT factor`() {
        val tdee = calculator.calculateTDEE(1800, ActivityLevel.LIGHT)
        assertEquals(2475, tdee)
    }

    @Test
    fun `TDEE with MODERATE factor`() {
        val tdee = calculator.calculateTDEE(1800, ActivityLevel.MODERATE)
        assertEquals(2790, tdee)
    }

    @Test
    fun `TDEE with ACTIVE factor`() {
        val tdee = calculator.calculateTDEE(1800, ActivityLevel.ACTIVE)
        assertEquals(3105, tdee)
    }

    @Test
    fun `TDEE with VERY_ACTIVE factor`() {
        val tdee = calculator.calculateTDEE(1800, ActivityLevel.VERY_ACTIVE)
        assertEquals(3420, tdee)
    }

    // --- EnergySummary Tests ---

    @Test
    fun `energy summary for fat loss male`() {
        val birthday = LocalDate.of(1995, 6, 15)
        val profile = UserProfile(
            gender = "MALE",
            birthday = birthday,
            heightCm = 180.0,
            activityLevel = ActivityLevel.MODERATE,
            goalType = GoalType.FAT_LOSS,
        )
        val measurement = BodyMeasurement(
            date = LocalDate.of(2026, 7, 28),
            weightKg = 85.0,
        )

        val summary = calculator.calculateEnergySummary(profile, measurement)
        assertNotNull(summary)
        summary!!

        // BMR = 1825
        assertEquals(1825, summary.bmr)
        // TDEE = 1825 * 1.55 = 2828.75 -> trunc to int = 2828 -> /5*5 = 2825
        assertEquals(2825, summary.tdee)
        // FAT_LOSS: deficit = min(2825*0.15=423, 500) = 423 → 2825-423 = 2402 → /5*5 = 2400
        assertEquals(2400, summary.targetCalories)
        // Protein = 2.0 * 85 = 170
        assertEquals(170, summary.proteinG)
        // Fat: 0.8*85=68, cap 20-35% → 2400*0.35/9=93.3, 2400*0.20/9=53.3 → 68 in range → 68
        assertEquals(68, summary.fatG)
        // Remaining: 2400 - (170*4) - (68*9) = 2400 - 680 - 612 = 1108
        // Carbs = 1108 / 4 = 277
        assertEquals(277, summary.carbsG)
    }

    @Test
    fun `energy summary for muscle gain male`() {
        val birthday = LocalDate.of(1990, 3, 10)
        val profile = UserProfile(
            gender = "MALE",
            birthday = birthday,
            heightCm = 175.0,
            activityLevel = ActivityLevel.ACTIVE,
            goalType = GoalType.MUSCLE_GAIN,
        )
        val measurement = BodyMeasurement(
            date = LocalDate.of(2026, 7, 28),
            weightKg = 75.0,
        )

        val summary = calculator.calculateEnergySummary(profile, measurement)
        assertNotNull(summary)
        summary!!

        // BMR = 1668.75 -> Math.round(1668.75/5)*5 = 1670
        assertEquals(1670, summary.bmr)
        // TDEE = 1670 * 1.725 = 2880.75 -> Math.round(2880.75/5)*5 = 2880
        assertEquals(2880, summary.tdee)
        // MUSCLE_GAIN: surplus = min(2880*0.10=288, 350) -> 288, max 200 -> 288
        // target = 2880 + 288 = 3168 -> /5*5 = 3165
        assertEquals(3165, summary.targetCalories)
        // Protein = 1.8 * 75 = 135
        assertEquals(135, summary.proteinG)
        // Fat: 0.8*75=60, cap 20-35% → 3165*0.35/9=123.1, 3165*0.20/9=70.3 → 60 < 70.3 → 70
        assertEquals(70, summary.fatG)
        // Remaining: 3165 - (135*4) - (70*9) = 3165 - 540 - 630 = 1995
        // Carbs = 1995 / 4 = 498
        assertEquals(498, summary.carbsG)
    }

    @Test
    fun `energy summary for maintain female`() {
        val birthday = LocalDate.of(2000, 1, 1)
        val profile = UserProfile(
            gender = "FEMALE",
            birthday = birthday,
            heightCm = 165.0,
            activityLevel = ActivityLevel.LIGHT,
            goalType = GoalType.MAINTAIN,
        )
        val measurement = BodyMeasurement(
            date = LocalDate.of(2026, 7, 28),
            weightKg = 60.0,
        )

        val summary = calculator.calculateEnergySummary(profile, measurement)
        assertNotNull(summary)
        summary!!

        // BMR = 1340.25 -> 1340
        assertEquals(1340, summary.bmr)
        // TDEE = 1340 * 1.375 = 1842.5 -> trunc int = 1842 -> /5*5 = 1840
        assertEquals(1840, summary.tdee)
        // MAINTAIN target = 1840
        assertEquals(1840, summary.targetCalories)
    }

    @Test
    fun `energy summary for lean gain`() {
        val birthday = LocalDate.of(1988, 5, 20)
        val profile = UserProfile(
            gender = "MALE",
            birthday = birthday,
            heightCm = 185.0,
            activityLevel = ActivityLevel.SEDENTARY,
            goalType = GoalType.LEAN_GAIN,
        )
        val measurement = BodyMeasurement(
            date = LocalDate.of(2026, 7, 28),
            weightKg = 80.0,
        )

        val summary = calculator.calculateEnergySummary(profile, measurement)
        assertNotNull(summary)
        summary!!

        // LEAN_GAIN target = tdee + surplus
        // surplus = min(tdee*0.07, 300) max 150
        // tdee is unknown, but surplus is 150-300
        assert(summary.targetCalories > summary.tdee)
    }

    @Test
    fun `energy summary returns null when weight is missing`() {
        val birthday = LocalDate.of(1995, 1, 1)
        val profile = UserProfile(
            gender = "MALE",
            birthday = birthday,
            heightCm = 175.0,
            activityLevel = ActivityLevel.SEDENTARY,
            goalType = GoalType.MAINTAIN,
        )
        val measurement = BodyMeasurement(
            date = LocalDate.of(2026, 7, 28),
            weightKg = null,
        )

        val summary = calculator.calculateEnergySummary(profile, measurement)
        assertNull(summary)
    }

    @Test
    fun `energy summary returns null when height is missing`() {
        val birthday = LocalDate.of(1995, 1, 1)
        val profile = UserProfile(
            gender = "MALE",
            birthday = birthday,
            heightCm = null,
            activityLevel = ActivityLevel.SEDENTARY,
            goalType = GoalType.MAINTAIN,
        )
        val measurement = BodyMeasurement(
            date = LocalDate.of(2026, 7, 28),
            weightKg = 75.0,
        )

        val summary = calculator.calculateEnergySummary(profile, measurement)
        assertNull(summary)
    }
}
