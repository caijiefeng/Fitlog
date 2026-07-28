package com.example.fitlog.core.di

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room callback. Actual seed data insertion happens in
 * [SeedInitializer], which is invoked after Hilt provides the database.
 */
class DatabaseCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Schema creation only — seed data is handled by SeedInitializer
    }
}
