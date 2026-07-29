package com.example.fitlog.feature.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
        const val CHANNEL_ID = "WORKOUT_REMINDERS"
        const val CHANNEL_NAME = "训练提醒"
        const val CHANNEL_DESC = "健身训练提醒通知"

        private const val NOTIFICATION_ID_BASE = 2000
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Shows a workout reminder notification.
     *
     * @param reminderId  database ID of the reminder (used for notification tag/id).
     * @param label       user-visible reminder label.
     * @param dateText    formatted date string, e.g. "7月28日 周三"
     */
    fun showNotification(
        reminderId: Long,
        label: String,
        dateText: String,
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("fitlog://reminder/$reminderId")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label)
            .setContentText(dateText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + reminderId.toInt(), notification)
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
