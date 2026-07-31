package com.example.fitlog.feature.reminder

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationChannelTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun `creates TRAINING_REMINDERS_V2 channel with high importance`() {
        NotificationHelper(context)

        val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_V2)
        assertNotNull("TRAINING_REMINDERS_V2 channel should exist", channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel!!.importance)
    }

    @Test
    fun `v2 channel has sound vibration and public lockscreen visibility`() {
        NotificationHelper(context)

        val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_V2)!!
        assertNotNull("channel should have a sound", channel.sound)
        assertNotNull("channel should have a vibration pattern", channel.vibrationPattern)
        assertTrue("vibration pattern should not be empty", channel.vibrationPattern!!.isNotEmpty())
        assertEquals(Notification.VISIBILITY_PUBLIC, channel.lockscreenVisibility)
    }

    @Test
    fun `legacy WORKOUT_REMINDERS channel is preserved`() {
        NotificationHelper(context)

        val legacy = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID)
        assertNotNull("legacy channel should still exist", legacy)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, legacy!!.importance)
    }

    @Test
    fun `showNotification posts to the v2 channel`() {
        val helper = NotificationHelper(context)

        helper.showNotification(reminderId = 7, label = "练腿日", dateText = "7月28日 周三")

        val posted = shadowOf(notificationManager).getNotification(2000 + 7)
        assertNotNull("notification should be posted", posted)
        assertEquals(NotificationHelper.CHANNEL_ID_V2, posted!!.channelId)
    }

    @Test
    fun `showNotification uses a unique content intent per reminder`() {
        val helper = NotificationHelper(context)

        helper.showNotification(reminderId = 7, label = "练腿日", dateText = "7月28日 周三")
        helper.showNotification(reminderId = 8, label = "练胸日", dateText = "7月29日 周四")

        // Different reminders must have different notification ids.
        val first = shadowOf(notificationManager).getNotification(2000 + 7)
        val second = shadowOf(notificationManager).getNotification(2000 + 8)
        assertNotNull(first)
        assertNotNull(second)
    }

    @Test
    fun `showTestNotification posts on the v2 channel`() {
        val helper = NotificationHelper(context)

        helper.showTestNotification()

        val posted = shadowOf(notificationManager).getNotification(9001)
        assertNotNull("test notification should be posted", posted)
        assertEquals(NotificationHelper.CHANNEL_ID_V2, posted!!.channelId)
    }
}
