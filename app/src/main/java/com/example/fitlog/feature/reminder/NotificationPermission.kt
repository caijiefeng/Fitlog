package com.example.fitlog.feature.reminder

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.fitlog.R

/**
 * Dialog shown when the POST_NOTIFICATIONS permission was denied:
 * "通知权限未开启" + a button that opens the app notification settings.
 */
@Composable
fun NotificationPermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_permission_denied_title)) },
        text = { Text(stringResource(R.string.reminder_permission_denied_message)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onOpenSettings()
            }) {
                Text(stringResource(R.string.reminder_open_notification_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
