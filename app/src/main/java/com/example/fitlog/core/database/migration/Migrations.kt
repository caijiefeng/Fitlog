package com.example.fitlog.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    schedule_id INTEGER,
                    template_id INTEGER,
                    template_name_snapshot TEXT,
                    date INTEGER NOT NULL,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER,
                    status TEXT NOT NULL,
                    notes TEXT,
                    active_rest_started_at INTEGER,
                    active_rest_duration_seconds INTEGER,
                    active_rest_set_record_id INTEGER,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
            """)

            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_date ON workout_sessions (date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_status ON workout_sessions (status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_template_id ON workout_sessions (template_id)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS exercise_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    session_id INTEGER NOT NULL,
                    exercise_id INTEGER,
                    exercise_name_snapshot TEXT NOT NULL,
                    primary_muscle_group_snapshot TEXT NOT NULL,
                    target_sets INTEGER NOT NULL DEFAULT 3,
                    target_reps_min INTEGER,
                    target_reps_max INTEGER,
                    target_weight_kg REAL,
                    target_rpe REAL,
                    target_rir INTEGER,
                    planned_rest_seconds INTEGER NOT NULL DEFAULT 90,
                    notes TEXT,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    is_skipped INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE
                )
            """)

            db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_sessions_session_sort ON exercise_sessions (session_id, sort_order)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS set_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    exercise_session_id INTEGER NOT NULL,
                    set_number INTEGER NOT NULL,
                    set_type TEXT NOT NULL DEFAULT 'WORKING',
                    reps INTEGER,
                    weight_kg REAL,
                    rpe REAL,
                    rir INTEGER,
                    rest_seconds INTEGER,
                    completed INTEGER NOT NULL DEFAULT 0,
                    notes TEXT,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (exercise_session_id) REFERENCES exercise_sessions(id) ON DELETE CASCADE
                )
            """)

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_set_records_session_set ON set_records (exercise_session_id, set_number)")
        }
    }
}
