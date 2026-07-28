package com.example.fitlog.core.di

/*
 * Database Hilt module — deferred to V1.
 *
 * Room is not instantiated in V0. The DatabaseModule will provide
 * FitLogDatabase and DAOs once real entities are introduced in V1.
 *
 * When restoring:
 *   1. Import FitLogDatabase and Room.
 *   2. Uncomment the @Provides method below.
 */

/*
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFitLogDatabase(
        @ApplicationContext context: Context,
    ): FitLogDatabase {
        return Room.databaseBuilder(
            context,
            FitLogDatabase::class.java,
            "fitlog.db",
        ).build()
    }
}
*/
