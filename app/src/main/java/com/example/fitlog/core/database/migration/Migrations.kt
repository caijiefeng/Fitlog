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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN occurrence_date INTEGER")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_plan_overrides (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    schedule_id INTEGER NOT NULL,
                    template_id INTEGER NOT NULL,
                    occurrence_date INTEGER NOT NULL,
                    planned_date INTEGER,
                    action TEXT NOT NULL,
                    notes TEXT,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (schedule_id) REFERENCES workout_schedules(id) ON DELETE CASCADE,
                    FOREIGN KEY (template_id) REFERENCES workout_templates(id) ON DELETE RESTRICT
                )
            """)

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_plan_overrides_schedule_id_occurrence_date ON workout_plan_overrides (schedule_id, occurrence_date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_plan_overrides_planned_date ON workout_plan_overrides (planned_date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_plan_overrides_action ON workout_plan_overrides (action)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    schedule_id INTEGER,
                    label TEXT NOT NULL DEFAULT '',
                    time_of_day_minutes INTEGER NOT NULL DEFAULT 480,
                    days_of_week_mask INTEGER NOT NULL DEFAULT 0,
                    zone_id TEXT NOT NULL DEFAULT '',
                    is_enabled INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (schedule_id) REFERENCES workout_schedules(id) ON DELETE SET NULL
                )
            """)

            db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_is_enabled ON reminders (is_enabled)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS check_ins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date INTEGER NOT NULL,
                    session_id INTEGER,
                    mood INTEGER,
                    energy_level INTEGER,
                    notes TEXT,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
            """)

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_check_ins_date ON check_ins (date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_session_id ON check_ins (session_id)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_schedule_id_occurrence_date ON workout_sessions (schedule_id, occurrence_date)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS user_profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    gender TEXT NOT NULL,
                    birthday INTEGER NOT NULL,
                    height_cm REAL,
                    activity_level TEXT NOT NULL,
                    goal_type TEXT NOT NULL,
                    target_body_fat REAL,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
            """)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS body_measurements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date INTEGER NOT NULL,
                    weight_kg REAL,
                    body_fat_percent REAL,
                    muscle_kg REAL,
                    waist_cm REAL,
                    note TEXT,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
            """)

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_body_measurements_date ON body_measurements (date)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS food_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date INTEGER NOT NULL,
                    meal_type TEXT NOT NULL,
                    food_name TEXT NOT NULL,
                    calories REAL,
                    protein_grams REAL,
                    carbs_grams REAL,
                    fat_grams REAL,
                    amount TEXT,
                    note TEXT,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL DEFAULT 0
                )
            """)

            db.execSQL("CREATE INDEX IF NOT EXISTS index_food_records_date ON food_records (date)")
        }
    }
}
