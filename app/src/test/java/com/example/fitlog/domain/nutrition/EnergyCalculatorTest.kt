package com.example.fitlog.domain.nutrition

import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.BodyMeasurement
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.UserProfile
import org.junit.Assert.assertEquals
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
        assertEquals(1540, bmr)
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

        // BMR = 1825
        assertEquals(1825, summary.bmr)
        // TDEE = 1825 * 1.55 = 2828.75 -> trunc to int = 2828 -> /5*5 = 2825
        assertEquals(2825, summary.tdee)
        // FAT_LOSS target = 2825 - 400 = 2425
        assertEquals(2425, summary.targetCalories)
        // Protein = 1.8 * 85 = 153
        assertEquals(153, summary.proteinG)
        // Fat = 0.9 * 85 = 76.5 -> 76
        assertEquals(76, summary.fatG)
        // Remaining: 2425 - (153*4) - (76*9) = 2425 - 612 - 684 = 1129
        // Carbs = 1129 / 4 = 282.25 -> 282
        assertEquals(282, summary.carbsG)
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

        // BMR = 1668.75 -> trunc int = 1668 -> 1668/5*5 = 1665
        assertEquals(1665, summary.bmr)
        // TDEE = 1665 * 1.725 = 2872.125 -> trunc int = 2872 -> /5*5 = 2870
        assertEquals(2870, summary.tdee)
        // MUSCLE_GAIN target = 2870 + 300 = 3170
        assertEquals(3170, summary.targetCalories)
        // Protein = 1.8 * 75 = 135
        assertEquals(135, summary.proteinG)
        // Fat = 0.9 * 75 = 67.5 -> 67
        assertEquals(67, summary.fatG)
        // Remaining: 3170 - (135*4) - (67*9) = 3170 - 540 - 603 = 2027
        // Carbs = 2027 / 4 = 506.75 -> 506
        assertEquals(506, summary.carbsG)
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

        // LEAN_GAIN target = tdee + 250
        assertEquals(summary.tdee + 250, summary.targetCalories)
    }

    @Test
    fun `energy summary handles null weight with default fallback`() {
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

        // BMR with 75kg default: 1693.75 -> 1690
        assertEquals(1690, summary.bmr)
    }
}
