package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.nutrition.EnergyCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class DailyNutritionSummary(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val targetCalories: Int = 0,
    val completionRate: Double = 0.0, // 0.0 to 1.0
)

data class FoodRecord(
    val id: Long = 0,
    val date: LocalDate,
    val mealType: String,
    val foodName: String,
    val calories: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val amount: String? = null,
    val note: String? = null,
)

@Singleton
class FoodRecordRepository @Inject constructor(
    private val foodRecordDao: FoodRecordDao,
    private val userProfileRepository: UserProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val energyCalculator: EnergyCalculator,
) {

    suspend fun save(record: FoodRecord): FoodRecord {
        val entity = FoodRecordEntity(
            id = record.id,
            date = record.date.toEpochDay(),
            mealType = record.mealType,
            foodName = record.foodName,
            calories = record.calories,
            proteinGrams = record.proteinGrams,
            carbsGrams = record.carbsGrams,
            fatGrams = record.fatGrams,
            amount = record.amount,
            note = record.note,
        )
        val id = foodRecordDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun delete(id: Long) {
        foodRecordDao.delete(id)
    }

    suspend fun getByDate(date: LocalDate): List<FoodRecord> {
        return foodRecordDao.getByDate(date.toEpochDay()).map { it.toDomain() }
    }

    fun observeByDate(date: LocalDate): Flow<List<FoodRecord>> {
        return foodRecordDao.observeByDate(date.toEpochDay()).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getDailySummary(date: LocalDate): DailyNutritionSummary {
        val epochDay = date.toEpochDay()
        val totalCalories = foodRecordDao.getTotalCaloriesByDate(epochDay) ?: 0.0
        val totalProtein = foodRecordDao.getTotalProteinByDate(epochDay) ?: 0.0
        val totalCarbs = foodRecordDao.getTotalCarbsByDate(epochDay) ?: 0.0
        val totalFat = foodRecordDao.getTotalFatByDate(epochDay) ?: 0.0

        // Calculate target calories from TDEE using current profile + measurement
        val profile = userProfileRepository.get()
        val targetCalories = if (profile != null) {
            val measurements = bodyMeasurementRepository.getByDateRange(date, date)
            val measurement = measurements.lastOrNull()
            if (measurement != null) {
                val age = java.time.temporal.ChronoUnit.YEARS.between(profile.birthday, date).toInt()
                val weightKg = measurement.weightKg ?: 75.0
                val heightCm = profile.heightCm ?: 175.0
                val bmr = energyCalculator.calculateBMR(profile.gender, weightKg, heightCm, age)
                val tdee = energyCalculator.calculateTDEE(bmr, profile.activityLevel)
                when (profile.goalType) {
                    GoalType.FAT_LOSS -> tdee - 400
                    GoalType.MAINTAIN -> tdee
                    GoalType.LEAN_GAIN -> tdee + 250
                    GoalType.MUSCLE_GAIN -> tdee + 300
                }
            } else {
                2000
            }
        } else {
            2000
        }

        val completionRate = if (targetCalories > 0) {
            (totalCalories / targetCalories).coerceIn(0.0, 1.5)
        } else 0.0

        return DailyNutritionSummary(
            calories = totalCalories,
            protein = totalProtein,
            carbs = totalCarbs,
            fat = totalFat,
            targetCalories = targetCalories,
            completionRate = completionRate,
        )
    }

    private fun FoodRecordEntity.toDomain(): FoodRecord = FoodRecord(
        id = id,
        date = LocalDate.ofEpochDay(date),
        mealType = mealType,
        foodName = foodName,
        calories = calories,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        amount = amount,
        note = note,
    )
}
