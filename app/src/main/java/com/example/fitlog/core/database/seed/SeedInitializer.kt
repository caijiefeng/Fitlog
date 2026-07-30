package com.example.fitlog.core.database.seed

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Invoked from FitLogApplication.onCreate() to ensure seed data
 * is synced on every app launch. Idempotent by design.
 */
@Singleton
class SeedInitializer @Inject constructor(
    private val seedDataProvider: SeedDataProvider,
) {

    suspend fun initialize() {
        seedDataProvider.syncBuiltInExercises()
    }
}
