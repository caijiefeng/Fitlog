package com.example.fitlog.domain.workout

import com.example.fitlog.core.model.ExerciseSession
import com.example.fitlog.core.model.SetRecord
import com.example.fitlog.core.model.WorkoutSessionDetail

/**
 * Completion status of a workout evaluated from its planned workload.
 */
enum class WorkoutCompletion {
    /** No planned set has been completed yet (workout cannot be finished). */
    NOTHING_COMPLETED,

    /** Some, but not all, planned sets are complete. */
    PARTIALLY_COMPLETED,

    /** Every planned set across all non-skipped exercises is complete. */
    COMPLETED,
}

/**
 * Pure Kotlin evaluator for workout completion. No framework dependencies.
 *
 * Rules (single source of truth, used by both the execution flow and tests):
 * - Skipped exercises never block completion and never count toward the total.
 * - Only *planned* sets are checked: `setNumber <= targetSets`. Extra sets
 *   added beyond the plan do not count and incomplete extras do not block.
 * - All set types (WARMUP / WORKING / DROP / FAILURE) count toward completion
 *   as long as they are completed planned sets.
 */
class WorkoutCompletionEvaluator {

    fun evaluate(detail: WorkoutSessionDetail): WorkoutCompletion {
        var completedPlannedSets = 0
        var totalPlannedSets = 0
        detail.exercises.forEach { (exercise, sets) ->
            if (exercise.isSkipped) return@forEach
            totalPlannedSets += exercise.targetSets
            completedPlannedSets += countCompletedPlannedSets(exercise, sets)
        }

        return when {
            completedPlannedSets == 0 -> WorkoutCompletion.NOTHING_COMPLETED
            completedPlannedSets >= totalPlannedSets -> WorkoutCompletion.COMPLETED
            else -> WorkoutCompletion.PARTIALLY_COMPLETED
        }
    }

    /**
     * Whether a single exercise's planned workload is done. Skipped exercises
     * count as complete so they never block the workout.
     */
    fun isExerciseComplete(exercise: ExerciseSession, sets: List<SetRecord>): Boolean {
        if (exercise.isSkipped) return true
        return countCompletedPlannedSets(exercise, sets) >= exercise.targetSets
    }

    /**
     * Number of completed sets that belong to the plan of [exercise]
     * (setNumber <= targetSets), regardless of set type.
     */
    fun countCompletedPlannedSets(exercise: ExerciseSession, sets: List<SetRecord>): Int =
        sets.count { it.completed && it.setNumber <= exercise.targetSets }
}
