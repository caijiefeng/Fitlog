package com.example.fitlog.feature.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.fitlog.data.repository.ReminderRepository
import com.example.fitlog.domain.reminder.Reminder
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
    private var alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Test-only constructor that allows injecting a fake [AlarmManager]. */
    internal constructor(
        context: Context,
        repository: ReminderRepository,
        alarmManager: AlarmManager,
    ) : this(context, repository) {
        this.alarmManager = alarmManager
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val REQUEST_CODE_BASE = 3000
        const val ACTION_REMINDER_ALARM = "com.example.fitlog.action.REMINDER_ALARM"

        /** Default delay for the notification action "稍后10分钟". */
        const val DEFAULT_LATER_DELAY_MILLIS = 10 * 60 * 1000L
    }

    /**
     * Whether exact alarms are allowed:
     * - Android 12+ (S): requires the SCHEDULE_EXACT_ALARM permission.
     * - Below S: the permission was auto-granted when declared, so exact alarms work.
     */
    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Schedules the next weekly occurrence of [reminder].
     * Uses a tiered strategy:
     * - exact alarm (setExactAndAllowWhileIdle) when SCHEDULE_EXACT_ALARM is granted;
     * - otherwise an inexact alarm (setAndAllowWhileIdle) that may be delayed a few minutes.
     */
    fun scheduleReminder(reminder: Reminder) {
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

        scheduleAt(reminder, nextTrigger.toEpochSecond() * 1000)
    }

    /**
     * Schedules a one-shot reminder [delayMillis] from now (used by the
     * "稍后10分钟" notification action).
     */
    fun scheduleReminderLater(
        reminder: Reminder,
        delayMillis: Long = DEFAULT_LATER_DELAY_MILLIS,
    ) {
        if (!reminder.isEnabled) return
        scheduleAt(reminder, System.currentTimeMillis() + delayMillis.coerceAtLeast(0))
    }

    /**
     * Schedules a single alarm for [reminder] at [triggerMillis], chaining the
     * same PendingIntent used for weekly occurrences so that only one alarm
     * per reminder is ever pending.
     */
    private fun scheduleAt(reminder: Reminder, triggerMillis: Long) {
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

        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
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
