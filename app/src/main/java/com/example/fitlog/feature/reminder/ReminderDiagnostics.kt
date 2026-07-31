package com.example.fitlog.feature.reminder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the runtime status of everything that affects reminder delivery:
 * notification permission, notification channel, exact alarm permission and
 * battery optimization. Also opens the corresponding system settings screens.
 */
@Singleton
class ReminderDiagnostics @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduler: ReminderScheduler,
) {

    /** POST_NOTIFICATIONS granted (on Android 13+; implicitly granted below). */
    fun notificationPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    /** Whether the TRAINING_REMINDERS_V2 channel was created. */
    fun channelExists(): Boolean =
        notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_V2) != null

    /** Whether the v2 channel is blocked by the user (importance NONE or missing). */
    fun channelBlocked(): Boolean {
        val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_V2)
        return channel == null || channel.importance == android.app.NotificationManager.IMPORTANCE_NONE
    }

    /** Whether SCHEDULE_EXACT_ALARM is allowed (tiered exact alarms). */
    fun exactAlarmAllowed(): Boolean = scheduler.canScheduleExactAlarms()

    /** True when the app is whitelisted from battery optimization (alarms not delayed). */
    fun batteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Opens the app notification settings screen. */
    fun openNotificationSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Opens the exact alarm settings screen (Android 12+). */
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Opens the battery optimization exemption request screen. */
    fun openBatteryOptimizationSettings() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
}
