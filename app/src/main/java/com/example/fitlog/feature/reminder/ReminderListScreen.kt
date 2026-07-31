package com.example.fitlog.feature.reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import com.example.fitlog.domain.reminder.Reminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderListViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingToggleReminder by remember { mutableStateOf<Reminder?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionDenied by remember { mutableStateOf(false) }

    val postNotificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val reminder = pendingToggleReminder
        if (granted && reminder != null) {
            pendingToggleReminder = null
            viewModel.onToggleEnabled(reminder)
        } else if (!granted) {
            // Permission denied: show the "通知权限未开启" dialog with a
            // "打开通知设置" action. The toggle stays pending until granted.
            showPermissionDenied = true
        }
    }

    // Permission rationale dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text(stringResource(R.string.reminder_notification_permission_title)) },
            text = { Text(stringResource(R.string.reminder_notification_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    Text(stringResource(R.string.reminder_notification_permission_grant))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text(stringResource(R.string.reminder_notification_permission_deny))
                }
            },
        )
    }

    // Permission denied dialog: '通知权限未开启' + '打开通知设置'
    if (showPermissionDenied) {
        NotificationPermissionDeniedDialog(
            onDismiss = { showPermissionDenied = false },
            onOpenSettings = viewModel::openNotificationSettings,
        )
    }

    fun handleToggle(reminder: Reminder) {
        if (!reminder.isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pendingToggleReminder = reminder
                val activity = context as? android.app.Activity
                val needsRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ?: true
                if (needsRationale) {
                    showPermissionRationale = true
                } else {
                    postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                return
            }
        }
        // Permission already granted or Android < 13
        viewModel.onToggleEnabled(reminder)
    }

    // Refresh diagnostics and retry a pending toggle when returning to the screen
    // (e.g. after granting permission in system settings).
    LifecycleResumeEffect(Unit) {
        viewModel.refreshDiagnostics()
        val reminder = pendingToggleReminder
        if (reminder != null &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        ) {
            pendingToggleReminder = null
            viewModel.onToggleEnabled(reminder)
        }
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReminderListEvent.ShowSnackbar -> { /* snackbar */ }
                is ReminderListEvent.NavigateToCreate -> onNavigateToCreate()
                is ReminderListEvent.NavigateToEdit -> onNavigateToEdit(event.reminderId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reminder_list_title),
                        color = FitLogTextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.calendar_nav_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FitLogSurface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = FitLogAccent,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.reminder_add))
            }
        },
        containerColor = FitLogSurface,
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FitLogAccent)
            }
        } else {
            PageContainer(modifier = Modifier.padding(padding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    // Diagnostic card: permission / channel / exact alarm / battery
                    item {
                        ReminderDiagnosticsCard(
                            diagnostics = uiState.diagnostics,
                            onSendTest = viewModel::sendTestNotification,
                            onOpenNotificationSettings = viewModel::openNotificationSettings,
                            onOpenExactAlarmSettings = viewModel::openExactAlarmSettings,
                            onOpenBatterySettings = viewModel::openBatteryOptimizationSettings,
                        )
                    }

                    if (uiState.reminders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                EmptyState(
                                    icon = Icons.Filled.NotificationsOff,
                                    title = stringResource(R.string.reminder_empty),
                                    subtitle = stringResource(R.string.reminder_empty_subtitle),
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.reminders,
                            key = { it.id },
                        ) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onToggleEnabled = { handleToggle(reminder) },
                                onDelete = { viewModel.onDelete(reminder) },
                                onClick = { viewModel.onEdit(reminder) },
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ReminderDiagnosticsCard(
    diagnostics: ReminderDiagnosticsState,
    onSendTest: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FitLogCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.reminder_diag_title),
                style = MaterialTheme.typography.titleSmall,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Notification permission ────────────────────────────────────
            DiagnosticsRow(
                label = stringResource(R.string.reminder_diag_permission),
                status = if (diagnostics.notificationPermissionGranted) {
                    stringResource(R.string.reminder_diag_permission_ok)
                } else {
                    stringResource(R.string.reminder_diag_permission_missing)
                },
                statusError = !diagnostics.notificationPermissionGranted,
            ) {
                if (!diagnostics.notificationPermissionGranted) {
                    DiagnosticsAction(
                        text = stringResource(R.string.reminder_open_notification_settings),
                        onClick = onOpenNotificationSettings,
                    )
                }
            }

            // ── Notification channel ───────────────────────────────────────
            DiagnosticsRow(
                label = stringResource(R.string.reminder_diag_channel),
                status = when {
                    !diagnostics.channelExists -> stringResource(R.string.reminder_diag_channel_missing)
                    diagnostics.channelBlocked -> stringResource(R.string.reminder_diag_channel_blocked)
                    else -> stringResource(R.string.reminder_diag_channel_ok)
                },
                statusError = !diagnostics.channelExists || diagnostics.channelBlocked,
            ) {
                if (diagnostics.channelBlocked) {
                    DiagnosticsAction(
                        text = stringResource(R.string.reminder_open_notification_settings),
                        onClick = onOpenNotificationSettings,
                    )
                }
            }

            // ── Exact alarm status ─────────────────────────────────────────
            DiagnosticsRow(
                label = stringResource(R.string.reminder_diag_exact),
                status = if (diagnostics.exactAlarmAllowed) {
                    stringResource(R.string.reminder_diag_exact_allowed)
                } else {
                    stringResource(R.string.reminder_diag_exact_delayed)
                },
                statusError = !diagnostics.exactAlarmAllowed,
            ) {
                if (!diagnostics.exactAlarmAllowed) {
                    DiagnosticsAction(
                        text = stringResource(R.string.reminder_diag_open_settings),
                        onClick = onOpenExactAlarmSettings,
                    )
                }
            }

            // ── Battery optimization ───────────────────────────────────────
            DiagnosticsRow(
                label = stringResource(R.string.reminder_diag_battery),
                status = if (diagnostics.batteryOptimizationIgnored) {
                    stringResource(R.string.reminder_diag_battery_ok)
                } else {
                    stringResource(R.string.reminder_diag_battery_affected)
                },
                statusError = !diagnostics.batteryOptimizationIgnored,
            ) {
                if (!diagnostics.batteryOptimizationIgnored) {
                    DiagnosticsAction(
                        text = stringResource(R.string.reminder_diag_open_settings),
                        onClick = onOpenBatterySettings,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = FitLogTextTertiary.copy(alpha = 0.15f),
            )

            // ── Test notification button ───────────────────────────────────
            TextButton(
                onClick = onSendTest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.reminder_diag_test),
                    color = FitLogAccent,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsRow(
    label: String,
    status: String,
    statusError: Boolean,
    action: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = FitLogTextSecondary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (statusError) FitLogError else FitLogTextPrimary,
        )
        Spacer(modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
private fun DiagnosticsAction(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
        Text(text = text, color = FitLogAccent, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.reminder_delete)) },
            text = { Text(stringResource(R.string.reminder_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.reminder_delete), color = FitLogError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.calendar_action_skip))
                }
            },
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FitLogCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Notification icon
            Icon(
                imageVector = if (reminder.isEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                contentDescription = null,
                tint = if (reminder.isEnabled) FitLogAccent else FitLogTextTertiary,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTime(reminder.timeOfDayMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDaysOfWeek(reminder.daysOfWeekMask),
                    style = MaterialTheme.typography.labelSmall,
                    color = FitLogTextTertiary,
                )
            }

            // Toggle
            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = { onToggleEnabled() },
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Delete
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.reminder_delete),
                    tint = FitLogTextTertiary,
                )
            }
        }
    }
}

/**
 * Formats time-of-day minutes to "HH:MM" display string.
 */
internal fun formatTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format("%02d:%02d", h, m)
}

/**
 * Converts a 7-bit days-of-week mask to a Chinese short-form label,
 * e.g. "一三五" or "每天" or "一~五".
 */
internal fun formatDaysOfWeek(mask: Int): String {
    if (mask == 0) return "从不"

    val allDays = (1 shl 7) - 1 // 127 = 0b1111111
    if (mask == allDays) return "每天"

    val weekdaysMask = 0b00011111 // Mon-Fri = bits 0-4
    val weekendsMask = 0b01100000 // Sat-Sun = bits 5-6

    if (mask == weekdaysMask) return "工作日"
    if (mask == weekendsMask) return "周末"

    val names = arrayOf("一", "二", "三", "四", "五", "六", "日")
    return (0..6)
        .filter { (mask shr it) and 1 == 1 }
        .joinToString("") { names[it] }
}
