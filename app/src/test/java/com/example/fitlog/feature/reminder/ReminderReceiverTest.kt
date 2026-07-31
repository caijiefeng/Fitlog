package com.example.fitlog.feature.reminder

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import com.example.fitlog.data.repository.ReminderRepository
import com.example.fitlog.domain.reminder.Reminder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the reminder receiver's core logic ([ReminderAlarmHandler], which
 * [ReminderReceiver] delegates to): alarm delivery, the "稍后10分钟" and
 * "跳过今天" actions, and rescheduling on system broadcasts.
 */
@RunWith(RobolectricTestRunner::class)
class ReminderReceiverTest {

    private lateinit var repository: ReminderRepository
    private lateinit var scheduler: ReminderScheduler
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var handler: ReminderAlarmHandler

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
        every { notificationHelper.formatDateText() } returns "7月28日 周三"
        handler = ReminderAlarmHandler(repository, scheduler, notificationHelper)
    }

    private fun alarmIntent(reminderId: Long): Intent =
        Intent(ReminderScheduler.ACTION_REMINDER_ALARM).apply {
            data = Uri.parse("fitlog://reminder/$reminderId")
        }

    private fun actionIntent(action: String, reminderId: Long): Intent =
        Intent(action).apply {
            data = Uri.parse("fitlog://reminder/$reminderId")
        }

    private fun reminder(id: Long) = Reminder(
        id = id,
        scheduleId = null,
        label = "练腿日",
        timeOfDayMinutes = 8 * 60,
        daysOfWeekMask = 1 shl 1,
        zoneId = "Asia/Shanghai",
        isEnabled = true,
    )

    @Test
    fun `alarm intent shows notification and schedules the next occurrence`() = runTest {
        val reminder = reminder(id = 5)
        coEvery { repository.getById(5) } returns reminder

        handler.handle(alarmIntent(5))

        verify { notificationHelper.showNotification(5, "练腿日", "7月28日 周三") }
        verify { scheduler.scheduleReminder(reminder) }
    }

    @Test
    fun `alarm for a missing reminder does nothing`() = runTest {
        coEvery { repository.getById(9) } returns null

        handler.handle(alarmIntent(9))

        verify(exactly = 0) { notificationHelper.showNotification(any(), any(), any()) }
        verify(exactly = 0) { scheduler.scheduleReminder(any()) }
    }

    @Test
    fun `alarm for a disabled reminder does nothing`() = runTest {
        coEvery { repository.getById(6) } returns reminder(id = 6).copy(isEnabled = false)

        handler.handle(alarmIntent(6))

        verify(exactly = 0) { notificationHelper.showNotification(any(), any(), any()) }
        verify(exactly = 0) { scheduler.scheduleReminder(any()) }
    }

    @Test
    fun `alarm without a reminder data uri does nothing`() = runTest {
        handler.handle(Intent(ReminderScheduler.ACTION_REMINDER_ALARM))

        verify(exactly = 0) { notificationHelper.showNotification(any(), any(), any()) }
        verify(exactly = 0) { scheduler.scheduleReminder(any()) }
    }

    @Test
    fun `remind later action schedules a one-shot follow-up alarm`() = runTest {
        coEvery { repository.getById(3) } returns reminder(id = 3)

        handler.handle(actionIntent(ReminderReceiver.ACTION_REMIND_LATER, 3))

        verify { scheduler.scheduleReminderLater(reminder(id = 3)) }
        verify(exactly = 0) { notificationHelper.showNotification(any(), any(), any()) }
    }

    @Test
    fun `skip today cancels pending alarm and schedules the next occurrence`() = runTest {
        coEvery { repository.getById(4) } returns reminder(id = 4)

        handler.handle(actionIntent(ReminderReceiver.ACTION_SKIP_TODAY, 4))

        verify { scheduler.cancelReminder(4) }
        verify { scheduler.scheduleReminder(reminder(id = 4)) }
        verify(exactly = 0) { notificationHelper.showNotification(any(), any(), any()) }
    }

    @Test
    fun `boot completed reschedules all enabled reminders`() = runTest {
        handler.handle(Intent(Intent.ACTION_BOOT_COMPLETED))

        coVerify(exactly = 1) { scheduler.rescheduleAllEnabled() }
    }

    @Test
    fun `time set reschedules all enabled reminders`() = runTest {
        handler.handle(Intent(Intent.ACTION_TIME_CHANGED))

        coVerify(exactly = 1) { scheduler.rescheduleAllEnabled() }
    }

    @Test
    fun `timezone changed reschedules all enabled reminders`() = runTest {
        handler.handle(Intent(Intent.ACTION_TIMEZONE_CHANGED))

        coVerify(exactly = 1) { scheduler.rescheduleAllEnabled() }
    }

    @Test
    fun `package replaced reschedules all enabled reminders`() = runTest {
        handler.handle(Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        coVerify(exactly = 1) { scheduler.rescheduleAllEnabled() }
    }

    @Test
    fun `exact alarm permission state change reschedules all enabled reminders`() = runTest {
        handler.handle(Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))

        coVerify(exactly = 1) { scheduler.rescheduleAllEnabled() }
    }
}
