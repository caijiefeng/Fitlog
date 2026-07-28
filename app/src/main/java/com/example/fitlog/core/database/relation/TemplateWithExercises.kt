package com.example.fitlog.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity

data class TemplateWithExercises(
    @Embedded
    val template: WorkoutTemplateEntity,

    @Relation(parentColumn = "id", entityColumn = "template_id")
    val exercises: List<WorkoutTemplateExerciseEntity>,
)
