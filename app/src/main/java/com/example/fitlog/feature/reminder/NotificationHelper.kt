package com.example.fitlog.feature.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.fitlog.MainActivity
import com.example.fitlog.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        // Legacy channel — preserved for notifications that were already posted
        // with it, but no longer used for new reminders.
        const val CHANNEL_ID = "WORKOUT_REMINDERS"
        const val CHANNEL_NAME = "训练提醒"
        const val CHANNEL_DESC = "健身训练提醒通知"

        // New channel used for all reminders going forward.
        const val CHANNEL_ID_V2 = "TRAINING_REMINDERS_V2"
        const val CHANNEL_NAME_V2 = "训练提醒"
        const val CHANNEL_DESC_V2 = "健身训练提醒通知（准时、响铃）"

        // Deep-link actions for the notification content/start intents.
        // Each action + data URI + request code yields a unique PendingIntent.
        const val ACTION_OPEN_TODAY = "com.example.fitlog.action.OPEN_TODAY"
        const val ACTION_START_WORKOUT = "com.example.fitlog.action.START_WORKOUT"

        private const val NOTIFICATION_ID_BASE = 2000
        private const val TEST_NOTIFICATION_ID = 9001

        // PendingIntent request-code offsets (must not collide with ReminderScheduler.REQUEST_CODE_BASE).
        private const val RC_OPEN = 4000
        private const val RC_START = 4100

        private const val SCHEME = "fitlog"
        private const val HOST_REMINDER = "reminder"

        /** Builds the deep-link URI for a reminder notification intent. */
        fun reminderUri(reminderId: Long, path: String? = null): Uri {
            val suffix = if (path != null) "/$path" else ""
            return Uri.parse("$SCHEME://$HOST_REMINDER/$reminderId$suffix")
        }
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Legacy channel preserved (not used for new reminders).
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
        )

        // New channel: high importance, sound, vibration, public on lockscreen.
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID_V2, CHANNEL_NAME_V2, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC_V2
                enableVibration(true)
                setVibrationPattern(longArrayOf(0, 250, 150, 250))
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    /**
     * Shows a workout reminder notification on the TRAINING_REMINDERS_V2 channel.
     *
     * @param reminderId  database ID of the reminder (used for notification tag/id).
     * @param label       user-visible reminder label.
     * @param dateText    formatted date string, e.g. "7月28日 周三".
     */
    fun showNotification(
        reminderId: Long,
        label: String,
        dateText: String,
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_V2)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label)
            .setContentText(dateText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openTodayPendingIntent(reminderId, ACTION_OPEN_TODAY, RC_OPEN, "open"))
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "开始训练",
                    openTodayPendingIntent(reminderId, ACTION_START_WORKOUT, RC_START, "start"),
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "稍后10分钟",
                    remindLaterPendingIntent(reminderId),
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "跳过今天",
                    skipTodayPendingIntent(reminderId),
                ).build()
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + reminderId.toInt(), notification)
    }

    /**
     * Posts a test notification on the v2 channel (used by the diagnostic card).
     */
    fun showTestNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_V2)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_test_title))
            .setContentText(context.getString(R.string.notification_test_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(TEST_NOTIFICATION_ID, notification)
    }

    /** Unique PendingIntent (action + data URI + request code) that opens the app on Today. */
    private fun openTodayPendingIntent(
        reminderId: Long,
        action: String,
        requestCode: Int,
        path: String,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            this.action = action
            data = reminderUri(reminderId, path)
        }
        return PendingIntent.getActivity(
            context,
            requestCode + reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Unique PendingIntent that asks the receiver to re-remind in 10 minutes. */
    private fun remindLaterPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND_LATER
            data = reminderUri(reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderReceiver.RC_REMIND_LATER + reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Unique PendingIntent that asks the receiver to skip today's reminder. */
    private fun skipTodayPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SKIP_TODAY
            data = reminderUri(reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderReceiver.RC_SKIP_TODAY + reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Builds a human-readable date text in Chinese, e.g. "7月28日 周三".
     */
    fun formatDateText(date: LocalDate = LocalDate.now()): String {
        val dayOfWeekChinese = date.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.SHORT,
            Locale.CHINESE,
        )
        return "${date.monthValue}月${date.dayOfMonth}日 $dayOfWeekChinese"
    }
}
