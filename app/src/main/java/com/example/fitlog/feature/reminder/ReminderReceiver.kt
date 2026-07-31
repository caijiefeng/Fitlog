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
 * Uses goAsync() for async work; the actual handling lives in
 * [ReminderAlarmHandler] (a plain class, extracted for testability).
 *
 * - REMINDER_ALARM: Shows a notification and schedules the next occurrence.
 * - REMIND_LATER: Re-reminds after a short delay ("稍后10分钟" action).
 * - SKIP_TODAY: Skips the rest of today, keeps the weekly cadence ("跳过今天" action).
 * - BOOT_COMPLETED / TIME_SET / TIMEZONE_CHANGED / MY_PACKAGE_REPLACED /
 *   SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED: Reschedules all enabled reminders.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"

        /** Notification action "稍后10分钟" — re-schedules a one-shot alarm. */
        const val ACTION_REMIND_LATER = "com.example.fitlog.action.REMIND_LATER"

        /** Notification action "跳过今天" — skips the rest of today. */
        const val ACTION_SKIP_TODAY = "com.example.fitlog.action.SKIP_TODAY"

        /** PendingIntent request-code offsets for the later/skip actions. */
        const val RC_REMIND_LATER = 3100
        const val RC_SKIP_TODAY = 3200
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
                ReminderAlarmHandler(repository, scheduler, notificationHelper).handle(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm intent", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
