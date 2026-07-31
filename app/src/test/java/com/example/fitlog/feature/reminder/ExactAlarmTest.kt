package com.example.fitlog.feature.reminder

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.fitlog.data.repository.ReminderRepository
import com.example.fitlog.domain.reminder.Reminder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ExactAlarmTest {

    private lateinit var context: Context
    private lateinit var repository: ReminderRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
    }

    private fun reminder(id: Long = 1) = Reminder(
        id = id,
        scheduleId = null,
        label = "练腿日",
        timeOfDayMinutes = 8 * 60,
        daysOfWeekMask = 1 shl 1, // Tuesday
        zoneId = "Asia/Shanghai",
        isEnabled = true,
    )

    @Test
    fun `uses an exact alarm when canScheduleExactAlarms returns true`() {
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        every { alarmManager.canScheduleExactAlarms() } returns true
        val scheduler = ReminderScheduler(context, repository, alarmManager)

        scheduler.scheduleReminder(reminder())

        verify { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
        verify(exactly = 0) { alarmManager.setAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun `falls back to an inexact alarm when exact alarms are not allowed`() {
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        every { alarmManager.canScheduleExactAlarms() } returns false
        val scheduler = ReminderScheduler(context, repository, alarmManager)

        scheduler.scheduleReminder(reminder())

        verify { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
        verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun `does not schedule disabled reminders`() {
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        every { alarmManager.canScheduleExactAlarms() } returns true
        val scheduler = ReminderScheduler(context, repository, alarmManager)

        scheduler.scheduleReminder(reminder().copy(isEnabled = false))

        verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
        verify(exactly = 0) { alarmManager.setAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun `scheduleReminderLater schedules a one-shot alarm roughly 10 minutes ahead`() {
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        every { alarmManager.canScheduleExactAlarms() } returns true
        val scheduler = ReminderScheduler(context, repository, alarmManager)

        val before = System.currentTimeMillis()
        scheduler.scheduleReminderLater(reminder(id = 3))
        val after = System.currentTimeMillis()

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                match { it in (before + 9 * 60 * 1000)..(after + 11 * 60 * 1000) },
                any(),
            )
        }
    }

    @Test
    fun `canScheduleExactAlarms reflects the alarm manager permission`() {
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        every { alarmManager.canScheduleExactAlarms() } returns true
        val scheduler = ReminderScheduler(context, repository, alarmManager)
        assertTrue(scheduler.canScheduleExactAlarms())

        every { alarmManager.canScheduleExactAlarms() } returns false
        assertFalse(scheduler.canScheduleExactAlarms())
    }

    @Test
    fun `schedules an alarm carrying the reminder data uri and cancels it`() {
        val scheduler = ReminderScheduler(context, repository)
        val realAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        scheduler.scheduleReminder(reminder(id = 42))

        val scheduled = shadowOf(realAlarmManager).getScheduledAlarms()
        assertEquals(1, scheduled.size)
        val alarm = scheduled.first()
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.getType())
        assertTrue("alarm should allow delivery while idle", alarm.isAllowWhileIdle())
        assertEquals("fitlog://reminder/42", shadowOf(alarm.operation).savedIntent.data.toString())
        assertEquals(3000 + 42, shadowOf(alarm.operation).requestCode)

        scheduler.cancelReminder(42)
        assertEquals(0, shadowOf(realAlarmManager).getScheduledAlarms().size)
    }
}
