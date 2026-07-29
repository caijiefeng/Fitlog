package com.example.fitlog.feature.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.fitlog.data.repository.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.fitlog.MainActivity
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalTime
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
        private const val REQUEST_CODE_BASE = 3000

        /** Custom action used for alarm PendingIntents. */
        const val ACTION_REMINDER_ALARM = "com.example.fitlog.action.REMINDER_ALARM"
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules a one-shot alarm for the next occurrence of this reminder.
     * Replaces any previously scheduled alarm for the same [reminder.id].
     */
    fun scheduleReminder(reminder: com.example.fitlog.domain.reminder.Reminder) {
        if (!reminder.isEnabled) return
        if (reminder.daysOfWeekMask == 0) return

        val zoneId = try {
            ZoneId.of(reminder.zoneId)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val nextTrigger = computeNextTrigger(
            daysOfWeekMask = reminder.daysOfWeekMask,
            timeOfDayMinutes = reminder.timeOfDayMinutes,
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

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("fitlog://reminder/${reminder.id}")
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_BASE + reminder.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(
                nextTrigger.toEpochSecond() * 1000,
                showPendingIntent,
            ),
            pendingIntent,
        )
    }

    /**
     * Cancels a previously scheduled alarm for [reminderId].
     */
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
     * Reschedules all enabled reminders.
     * Call this on app startup and after BOOT_COMPLETED.
     */
    fun rescheduleAllEnabled() {
        runBlocking {
            val reminders = repository.getEnabledReminders()
            reminders.forEach { scheduleReminder(it) }
        }
    }

    /**
     * Computes the next [ZonedDateTime] when this reminder should fire.
     * Returns null if no day in the mask matches (mask == 0).
     */
    internal fun computeNextTrigger(
        daysOfWeekMask: Int,
        timeOfDayMinutes: Int,
        zoneId: ZoneId,
    ): ZonedDateTime? {
        if (daysOfWeekMask == 0) return null

        val now = ZonedDateTime.now(zoneId)
        val today = now.toLocalDate()
        val targetTime = LocalTime.of(timeOfDayMinutes / 60, timeOfDayMinutes % 60)

        // Check today first
        if (isDayInMask(today.dayOfWeek, daysOfWeekMask)) {
            val todayTrigger = ZonedDateTime.of(today, targetTime, zoneId)
            if (todayTrigger.isAfter(now)) {
                return todayTrigger
            }
        }

        // Find the next day
        var checkDate = today.plusDays(1)
        for (_i in 0 until 7) {
            if (isDayInMask(checkDate.dayOfWeek, daysOfWeekMask)) {
                return ZonedDateTime.of(checkDate, targetTime, zoneId)
            }
            checkDate = checkDate.plusDays(1)
        }

        return null // should not reach here if mask is valid
    }

    /**
     * Returns true if [dayOfWeek]'s bit is set in the 7-bit mask.
     * Bit 0 = Monday, Bit 6 = Sunday.
     */
    internal fun isDayInMask(dayOfWeek: DayOfWeek, mask: Int): Boolean {
        val bit = when (dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
        return (mask shr bit) and 1 == 1
    }
}
