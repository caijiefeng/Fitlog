package com.example.fitlog.domain.workout

import com.example.fitlog.core.model.ExerciseSession
import com.example.fitlog.core.model.SetRecord
import com.example.fitlog.core.model.SetType
import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.core.model.WorkoutSessionDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCompletionEvaluatorTest {

    private val evaluator = WorkoutCompletionEvaluator()

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun exercise(
        id: Long,
        targetSets: Int = 3,
        isSkipped: Boolean = false,
    ) = ExerciseSession(
        id = id,
        sessionId = 1,
        exerciseNameSnapshot = "Bench Press",
        targetSets = targetSets,
        isSkipped = isSkipped,
    )

    private fun set(
        number: Int,
        completed: Boolean = true,
        type: SetType = SetType.WORKING,
    ) = SetRecord(
        id = number.toLong(),
        exerciseSessionId = 1,
        setNumber = number,
        setType = type,
        completed = completed,
    )

    private fun detail(vararg pairs: Pair<ExerciseSession, List<SetRecord>>) =
        WorkoutSessionDetail(
            session = WorkoutSession(),
            exercises = pairs.toList(),
        )

    private fun plannedSets(count: Int, completed: Boolean = true): List<SetRecord> =
        (1..count).map { set(it, completed) }

    // ── evaluate() ───────────────────────────────────────────────────────────

    @Test
    fun `no completed sets returns NOTHING_COMPLETED`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1) to plannedSets(3, completed = false),
                exercise(2) to plannedSets(3, completed = false),
            )
        )
        assertEquals(WorkoutCompletion.NOTHING_COMPLETED, result)
    }

    @Test
    fun `all planned sets completed returns COMPLETED`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1) to plannedSets(3),
                exercise(2) to plannedSets(4),
            )
        )
        assertEquals(WorkoutCompletion.COMPLETED, result)
    }

    @Test
    fun `some planned sets completed returns PARTIALLY_COMPLETED`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1) to plannedSets(3),
                exercise(2) to listOf(set(1), set(2), set(3, completed = false)),
            )
        )
        assertEquals(WorkoutCompletion.PARTIALLY_COMPLETED, result)
    }

    @Test
    fun `skipped exercises do not block completion`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1, targetSets = 3, isSkipped = true) to plannedSets(3, completed = false),
                exercise(2) to plannedSets(3),
            )
        )
        assertEquals(WorkoutCompletion.COMPLETED, result)
    }

    @Test
    fun `all skipped exercises returns NOTHING_COMPLETED`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1, isSkipped = true) to plannedSets(3, completed = false),
            )
        )
        assertEquals(WorkoutCompletion.NOTHING_COMPLETED, result)
    }

    @Test
    fun `all set types count toward completion`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1) to listOf(
                    set(1, type = SetType.WARMUP),
                    set(2, type = SetType.WORKING),
                    set(3, type = SetType.DROP),
                )
            )
        )
        assertEquals(WorkoutCompletion.COMPLETED, result)
    }

    @Test
    fun `extra incomplete sets do not block completion`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1) to listOf(
                    set(1), set(2), set(3),
                    set(4, completed = false), // extra set beyond the plan
                )
            )
        )
        assertEquals(WorkoutCompletion.COMPLETED, result)
    }

    @Test
    fun `completed extra sets do not count toward the plan`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1) to listOf(
                    set(1, completed = false), set(2, completed = false), set(3, completed = false),
                    set(4), // extra set completed but not part of the plan
                )
            )
        )
        assertEquals(WorkoutCompletion.NOTHING_COMPLETED, result)
    }

    @Test
    fun `mixed skipped and partial returns PARTIALLY_COMPLETED`() {
        val result = evaluator.evaluate(
            detail(
                exercise(1, isSkipped = true) to emptyList(),
                exercise(2) to listOf(set(1), set(2), set(3, completed = false)),
            )
        )
        assertEquals(WorkoutCompletion.PARTIALLY_COMPLETED, result)
    }

    // ── isExerciseComplete() ─────────────────────────────────────────────────

    @Test
    fun `skipped exercise is considered complete`() {
        assertTrue(evaluator.isExerciseComplete(exercise(1, isSkipped = true), plannedSets(3, completed = false)))
    }

    @Test
    fun `exercise with all planned sets done is complete`() {
        assertTrue(evaluator.isExerciseComplete(exercise(1), plannedSets(3)))
    }

    @Test
    fun `exercise with outstanding planned set is not complete`() {
        assertFalse(
            evaluator.isExerciseComplete(
                exercise(1),
                listOf(set(1), set(2), set(3, completed = false)),
            )
        )
    }

    @Test
    fun `exercise with only extra sets done is not complete`() {
        assertFalse(
            evaluator.isExerciseComplete(
                exercise(1),
                listOf(set(1, completed = false), set(2, completed = false), set(3, completed = false), set(4)),
            )
        )
    }

    // ── countCompletedPlannedSets() ──────────────────────────────────────────

    @Test
    fun `countCompletedPlannedSets only counts completed planned sets`() {
        val ex = exercise(1, targetSets = 3)
        val sets = listOf(
            set(1, completed = false),
            set(2),
            set(3, type = SetType.WARMUP),
            set(4, completed = false), // extra, incomplete
            set(5),                    // extra, completed — should not count
        )
        assertEquals(2, evaluator.countCompletedPlannedSets(ex, sets))
    }
}
