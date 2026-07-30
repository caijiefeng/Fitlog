package com.example.fitlog.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fitlog.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    companion object {
        private const val TAG = "ReminderReceiver"
    }

    @Inject
    lateinit var repository: ReminderRepository

    @Inject
    lateinit var scheduler: ReminderScheduler

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderScheduler.ACTION_REMINDER_ALARM -> {
                        handleAlarm(intent)
                    }

                    Intent.ACTION_BOOT_COMPLETED,
                    "android.intent.action.TIME_SET",
                    Intent.ACTION_TIMEZONE_CHANGED,
                    -> {
                        scheduler.rescheduleAllEnabled()
                    }

                    else -> {
                        // Also try to parse as alarm intent from data URI
                        // (some legacy/manufacturer-triggered delivery)
                        handleAlarm(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm intent", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarm(intent: Intent) {
        val reminderId = intent.data?.let { uri ->
            if (uri.scheme == "fitlog" && uri.host == "reminder") {
                uri.lastPathSegment?.toLongOrNull()
            } else {
                null
            }
        } ?: return

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
            Log.e(TAG, "Failed to handle reminder $reminderId", e)
        }
    }
}
