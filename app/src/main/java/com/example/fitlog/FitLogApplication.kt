package com.example.fitlog

import android.app.Application
import com.example.fitlog.core.database.seed.SeedInitializer
import com.example.fitlog.feature.reminder.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FitLogApplication : Application() {

    @Inject
    lateinit var seedInitializer: SeedInitializer

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedInitializer.initialize()
            reminderScheduler.rescheduleAllEnabled()
        }
    }
}
