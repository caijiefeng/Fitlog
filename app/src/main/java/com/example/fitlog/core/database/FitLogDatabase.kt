package com.example.fitlog.core.database

/*
 * FitLog Room database — deferred to V1.
 *
 * Room does not support empty entity lists. Rather than creating a
 * placeholder table that would pollute the schema and require a
 * destructive migration in V1, Room instantiation is deferred until
 * the first real entities are created.
 *
 * V1 will introduce:
 *   MuscleGroup, Exercise, WorkoutTemplate, WorkoutTemplateExercise,
 *   WorkoutSchedule
 *
 * See docs/DATA_MODEL.md for the complete future schema.
 *
 * When reintroducing Room:
 *   1. Uncomment the Room dependencies in app/build.gradle.kts.
 *   2. Create the first Entity classes with @Entity annotations.
 *   3. Restore @Database(entities = [...], version = 1) below.
 *   4. Restore the DatabaseModule Hilt provider.
 */

/*
@Database(
    entities = [],
    version = 1,
    exportSchema = false,
)
abstract class FitLogDatabase : RoomDatabase()
*/
