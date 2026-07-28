package com.example.fitlog.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ── Timestamp Strategy ──────────────────────────────────────────────────────
//
// LocalDate       — calendar day (training date, measurement date, schedule day)
// Instant         — precise point in time (record creation, updates, session
//                   start/end), stored as epoch milliseconds in Room
// ZoneId (String)  — user's timezone for reminders and schedule interpretation
//
// Do NOT convert all LocalDateTime to epoch seconds indiscriminately.

// ── Muscle Group ────────────────────────────────────────────────────────────

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    QUADRICEPS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    CORE,
    CARDIO,
    FULL_BODY,
}

// ── Exercise ────────────────────────────────────────────────────────────────

data class Exercise(
    val id: Long = 0,
    val name: String = "",
    val primaryMuscleGroup: MuscleGroup = MuscleGroup.FULL_BODY,
    val secondaryMuscleGroup: MuscleGroup? = null,
    val categoryId: Long? = null,          // FK → ExerciseCategory (user-defined)
    val notes: String? = null,
    val isCustom: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class ExerciseCategory(
    val id: Long = 0,
    val name: String = "",                 // e.g. "康复训练", "CrossFit"
    val description: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
)

// ── Training Template ───────────────────────────────────────────────────────

data class WorkoutTemplate(
    val id: Long = 0,
    val name: String = "",                 // e.g. "Push Day A"
    val notes: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class WorkoutTemplateExercise(
    val id: Long = 0,
    val templateId: Long,                  // FK → WorkoutTemplate
    val exerciseId: Long,                  // FK → Exercise
    val exerciseName: String = "",          // denormalized for display
    val primaryMuscleGroup: MuscleGroup = MuscleGroup.FULL_BODY,
    val targetSets: Int = 3,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetWeightKg: Double? = null,
    val targetRpe: Double? = null,         // 1–10
    val targetRir: Int? = null,            // 0–5
    val restSeconds: Int = 90,
    val notes: String? = null,
    val sortOrder: Int = 0,
)

data class WorkoutTemplateDetail(
    val template: WorkoutTemplate,
    val exercises: List<WorkoutTemplateExerciseDetail>,
)

data class WorkoutTemplateExerciseDetail(
    val templateExercise: WorkoutTemplateExercise,
    val exercise: Exercise?,
)

data class WorkoutSchedule(
    val id: Long = 0,
    val templateId: Long,                  // FK → WorkoutTemplate
    val dayOfWeek: Int,                    // 1 = Mon … 7 = Sun
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
)

// ── Workout Execution ───────────────────────────────────────────────────────

enum class WorkoutStatus { PLANNED, IN_PROGRESS, PARTIALLY_COMPLETED, COMPLETED, CANCELLED, SKIPPED }

data class WorkoutSession(
    val id: Long = 0,
    val scheduleId: Long? = null,          // FK → WorkoutSchedule (null = ad-hoc)
    val templateId: Long? = null,          // FK → WorkoutTemplate (denorm for quick lookup)
    val date: LocalDate = LocalDate.now(),
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val durationSeconds: Int? = null,
    val status: WorkoutStatus = WorkoutStatus.PLANNED,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class ExerciseSession(
    val id: Long = 0,
    val sessionId: Long,                   // FK → WorkoutSession
    val exerciseId: Long,                  // FK → Exercise
    val notes: String? = null,
    val sortOrder: Int = 0,
)

enum class SetType { WARMUP, WORKING, DROP, FAILURE }

data class SetRecord(
    val id: Long = 0,
    val exerciseSessionId: Long,           // FK → ExerciseSession
    val setNumber: Int,                    // 1-indexed
    val setType: SetType = SetType.WORKING,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val rpe: Double? = null,               // 1–10
    val rir: Int? = null,                  // 0–5
    val restSeconds: Int? = null,
    val completed: Boolean = false,
    val notes: String? = null,
)

// ── User Profile ────────────────────────────────────────────────────────────

enum class Gender { MALE, FEMALE, OTHER, UNSPECIFIED }

data class UserProfile(
    val id: Long = 0,
    val name: String = "",
    val birthDate: LocalDate? = null,
    val heightCm: Double? = null,
    val gender: Gender = Gender.UNSPECIFIED,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

// NOTE: Current weight and body fat are derived from the latest
// BodyMeasurement entry, not stored as independent fields on UserProfile.

// ── Body Measurements ───────────────────────────────────────────────────────

data class BodyMeasurement(
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val leftArmCm: Double? = null,
    val rightArmCm: Double? = null,
    val leftThighCm: Double? = null,
    val rightThighCm: Double? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)

// ── Nutrition ───────────────────────────────────────────────────────────────

data class Food(
    val id: Long = 0,
    val name: String = "",
    val brand: String? = null,
    val servingSize: String? = null,
    val caloriesPerServing: Double? = null,
    val proteinGramsPerServing: Double? = null,
    val carbsGramsPerServing: Double? = null,
    val fatGramsPerServing: Double? = null,
    val isCustom: Boolean = false,
    val createdAt: Instant = Instant.now(),
)

data class Meal(
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val mealType: MealType = MealType.OTHER,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK, OTHER }

/**
 * Links a food item to a meal. Nutrient fields are historical snapshots
 * captured at logging time — editing a Food's nutritional values does not
 * retroactively change past MealEntry records.
 */
data class MealEntry(
    val id: Long = 0,
    val mealId: Long,                      // FK → Meal
    val foodId: Long,                      // FK → Food
    val servings: Double = 1.0,
    val calories: Double? = null,          // historical snapshot
    val proteinGrams: Double? = null,      // historical snapshot
    val carbsGrams: Double? = null,        // historical snapshot
    val fatGrams: Double? = null,          // historical snapshot
)

data class DailyNutritionTarget(
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val tdee: Double? = null,
    val targetCalories: Double? = null,
    val targetProteinG: Double? = null,
    val targetCarbsG: Double? = null,
    val targetFatG: Double? = null,
    val targetBodyFatPercent: Double? = null,
)

data class MealNutritionTarget(
    val id: Long = 0,
    val dailyTargetId: Long,               // FK → DailyNutritionTarget
    val mealType: MealType = MealType.OTHER,
    val budgetCalories: Double? = null,
    val budgetProteinG: Double? = null,
    val budgetCarbsG: Double? = null,
    val budgetFatG: Double? = null,
)

// ── Reminder ────────────────────────────────────────────────────────────────

data class Reminder(
    val id: Long = 0,
    val scheduleId: Long? = null,          // FK → WorkoutSchedule
    val label: String = "",
    val timeOfDay: String = "08:00",       // HH:mm in user's local zone
    val daysOfWeek: Int? = null,           // bitmask, null = all
    val zoneId: String = ZoneId.systemDefault().id,
    val isEnabled: Boolean = true,
    val createdAt: Instant = Instant.now(),
)

// ── Check-In ────────────────────────────────────────────────────────────────

data class CheckIn(
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val sessionId: Long? = null,           // FK → WorkoutSession (if linked)
    val measurementId: Long? = null,       // FK → BodyMeasurement
    val mood: Int? = null,                 // 1–5
    val energyLevel: Int? = null,          // 1–5
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)

// ── Media ───────────────────────────────────────────────────────────────────

data class MediaRecord(
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val sessionId: Long? = null,           // FK → WorkoutSession
    val measurementId: Long? = null,       // FK → BodyMeasurement
    val filePath: String = "",             // app-internal path
    val mediaType: MediaType = MediaType.PHOTO,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)

enum class MediaType { PHOTO, VIDEO }

// ── Personal Record ─────────────────────────────────────────────────────────

data class PersonalRecord(
    val id: Long = 0,
    val exerciseId: Long,                  // FK → Exercise
    val recordType: PersonalRecordType = PersonalRecordType.ONE_REP_MAX,
    val value: Double = 0.0,
    val achievedDate: LocalDate = LocalDate.now(),
    val sessionId: Long? = null,           // FK → WorkoutSession
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)

enum class PersonalRecordType { ONE_REP_MAX, MAX_VOLUME, MAX_REPS, MAX_WEIGHT }
