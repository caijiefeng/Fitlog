package com.example.fitlog.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitlog.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * BroadcastReceiver that handles reminder alarm intents and system broadcasts.
 * Uses goAsync() for async work.
 *
 * - REMINDER_ALARM: Shows a notification and schedules the next occurrence.
 * - BOOT_COMPLETED / TIME_SET / TIMEZONE_CHANGED: Reschedules all enabled reminders.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderRepository

    @Inject
    lateinit var scheduler: ReminderScheduler

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        try {
            when (intent.action) {
                ReminderScheduler.ACTION_REMINDER_ALARM -> {
                    handleAlarm(intent)
                }

                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.TIME_SET",
                Intent.ACTION_TIMEZONE_CHANGED,
                -> {
                    kotlinx.coroutines.runBlocking { scheduler.rescheduleAllEnabled() }
                }

                else -> {
                    // Also try to parse as alarm intent from data URI
                    // (some legacy/manufacturer-triggered delivery)
                    handleAlarm(intent)
                }
            }
        } catch (_: Exception) {
            // Swallow to avoid crashing the receiver
        } finally {
            pendingResult.finish()
        }
    }

    private fun handleAlarm(intent: Intent) {
        val reminderId = intent.data?.let { uri ->
            if (uri.scheme == "fitlog" && uri.host == "reminder") {
                uri.lastPathSegment?.toLongOrNull()
            } else {
                null
            }
        } ?: return

        runBlocking {
            val reminder = repository.getById(reminderId)
            if (reminder != null && reminder.isEnabled) {
                val dateText = notificationHelper.formatDateText()
                notificationHelper.showNotification(
                    reminderId = reminder.id,
                    label = reminder.label,
                    dateText = dateText,
                )

                // Schedule the next occurrence (repeating weekly)
                scheduler.scheduleReminder(reminder)
            }
        }
    }
}
