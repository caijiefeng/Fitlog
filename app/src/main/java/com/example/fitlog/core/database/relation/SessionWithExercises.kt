package com.example.fitlog.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.fitlog.core.database.entity.ExerciseSessionEntity
import com.example.fitlog.core.database.entity.SetRecordEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity

data class SessionWithExercises(
    @Embedded
    val session: WorkoutSessionEntity,

    @Relation(parentColumn = "id", entityColumn = "session_id")
    val exercises: List<ExerciseSessionEntity>,
)

data class ExerciseSessionWithSets(
    @Embedded
    val exerciseSession: ExerciseSessionEntity,

    @Relation(parentColumn = "id", entityColumn = "exercise_session_id")
    val sets: List<SetRecordEntity>,
)
