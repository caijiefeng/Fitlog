package com.example.fitlog.feature.reminder

import android.app.AlarmManager
import android.content.Intent
import android.util.Log
import com.example.fitlog.data.repository.ReminderRepository

/**
 * Core reminder-intent logic, extracted from [ReminderReceiver] into a plain
 * class so it can be unit-tested without Hilt or the Android framework.
 *
 * - REMINDER_ALARM: fetch the reminder, post a notification, schedule the next occurrence.
 * - REMIND_LATER: re-remind after a short delay ("稍后10分钟" action).
 * - SKIP_TODAY: skip the rest of today, keep the weekly cadence ("跳过今天" action).
 * - BOOT_COMPLETED / TIME_SET / TIMEZONE_CHANGED / MY_PACKAGE_REPLACED /
 *   SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED: reschedule all enabled reminders.
 */
class ReminderAlarmHandler(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val notificationHelper: NotificationHelper,
) {
    private val tag = "ReminderReceiver"

    suspend fun handle(intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_REMINDER_ALARM -> handleAlarm(intent)

            ReminderReceiver.ACTION_REMIND_LATER -> handleRemindLater(intent)

            ReminderReceiver.ACTION_SKIP_TODAY -> handleSkipToday(intent)

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> scheduler.rescheduleAllEnabled()

            else -> {
                // Also try to parse as alarm intent from data URI
                // (some legacy/manufacturer-triggered delivery)
                handleAlarm(intent)
            }
        }
    }

    /** Alarm fired: fetch the reminder, post the notification, chain the next occurrence. */
    private suspend fun handleAlarm(intent: Intent) {
        val reminderId = parseReminderId(intent) ?: return
        runCatching {
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
        }.onFailure { e ->
            Log.e(tag, "Failed to handle reminder $reminderId", e)
        }
    }

    /** "稍后10分钟" action: fetch the reminder and schedule a one-shot follow-up. */
    private suspend fun handleRemindLater(intent: Intent) {
        val reminderId = parseReminderId(intent) ?: return
        runCatching {
            val reminder = repository.getById(reminderId)
            if (reminder != null && reminder.isEnabled) {
                scheduler.scheduleReminderLater(reminder)
            }
        }.onFailure { e ->
            Log.e(tag, "Failed to remind later for reminder $reminderId", e)
        }
    }

    /** "跳过今天" action: cancel any pending follow-up alarm and re-chain next week. */
    private suspend fun handleSkipToday(intent: Intent) {
        val reminderId = parseReminderId(intent) ?: return
        runCatching {
            val reminder = repository.getById(reminderId)
            if (reminder != null && reminder.isEnabled) {
                // Cancel any pending alarm (e.g. a '稍后10分钟' follow-up), then
                // schedule the next weekly occurrence, skipping the rest of today.
                scheduler.cancelReminder(reminder.id)
                scheduler.scheduleReminder(reminder)
            }
        }.onFailure { e ->
            Log.e(tag, "Failed to skip reminder $reminderId", e)
        }
    }

    private fun parseReminderId(intent: Intent): Long? {
        val uri = intent.data ?: return null
        return if (uri.scheme == "fitlog" && uri.host == "reminder") {
            uri.lastPathSegment?.toLongOrNull()
        } else {
            null
        }
    }
}
