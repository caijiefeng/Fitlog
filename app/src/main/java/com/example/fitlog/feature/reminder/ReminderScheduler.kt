package com.example.fitlog.feature.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.fitlog.data.repository.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ReminderRepository,
) {

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val REQUEST_CODE_BASE = 3000
        const val ACTION_REMINDER_ALARM = "com.example.fitlog.action.REMINDER_ALARM"
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: com.example.fitlog.domain.reminder.Reminder) {
        if (!reminder.isEnabled) return
        if (reminder.daysOfWeekMask == 0) return
        val timeMinutes = reminder.timeOfDayMinutes.coerceIn(0, 1439)
        if (timeMinutes != reminder.timeOfDayMinutes) {
            Log.w(TAG, "Invalid timeOfDayMinutes ${reminder.timeOfDayMinutes} for reminder ${reminder.id}, clamped to $timeMinutes")
        }

        val zoneId = try {
            ZoneId.of(reminder.zoneId)
        } catch (_: Exception) {
            Log.w(TAG, "Invalid zoneId '${reminder.zoneId}' for reminder ${reminder.id}, using system default")
            ZoneId.systemDefault()
        }

        val nextTrigger = computeNextTrigger(
            daysOfWeekMask = reminder.daysOfWeekMask,
            timeOfDayMinutes = timeMinutes,
            zoneId = zoneId,
        ) ?: return

        val intent = Intent(ACTION_REMINDER_ALARM).apply {
            data = Uri.parse("fitlog://reminder/${reminder.id}")
            `package` = context.packageName
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Use non-exact alarm (setWindow) — no SCHEDULE_EXACT_ALARM permission needed
        val triggerMillis = nextTrigger.toEpochSecond() * 1000
        val windowMillis = 15 * 60 * 1000L
        try {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                windowMillis,
                pendingIntent,
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling reminder ${reminder.id}", e)
        }
    }

    fun cancelReminder(reminderId: Long) {
        val intent = Intent(ACTION_REMINDER_ALARM).apply {
            data = Uri.parse("fitlog://reminder/$reminderId")
            `package` = context.packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Reschedules all enabled reminders. Each failure is logged individually
     * so one bad reminder cannot prevent others from being scheduled.
     */
    suspend fun rescheduleAllEnabled() {
        val reminders = repository.getEnabledReminders()
        reminders.forEach { reminder ->
            runCatching {
                scheduleReminder(reminder)
            }.onFailure {
                Log.e(TAG, "Failed to schedule reminder ${reminder.id}", it)
            }
        }
    }

    private fun computeNextTrigger(
        daysOfWeekMask: Int,
        timeOfDayMinutes: Int,
        zoneId: ZoneId,
    ): ZonedDateTime? {
        if (daysOfWeekMask == 0 || daysOfWeekMask !in 1..127) return null

        val now = ZonedDateTime.now(zoneId)
        val hour = timeOfDayMinutes / 60
        val minute = timeOfDayMinutes % 60

        // Check up to 14 days ahead
        for (offset in 0..13) {
            val candidate = now.plusDays(offset.toLong())
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            val dayBit = 1 shl ((candidate.dayOfWeek.value - 1) % 7)
            if ((daysOfWeekMask and dayBit) != 0 && candidate.isAfter(now)) {
                return candidate
            }
        }
        return null
    }
}
