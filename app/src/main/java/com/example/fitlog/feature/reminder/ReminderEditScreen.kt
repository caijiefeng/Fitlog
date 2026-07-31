package com.example.fitlog.feature.reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditScreen(
    viewModel: ReminderEditViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
    onCancelled: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionDenied by remember { mutableStateOf(false) }

    val postNotificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.save()
        } else {
            // Permission denied: show the "通知权限未开启" dialog with a
            // "打开通知设置" action.
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
            onOpenSettings = {
                context.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    ).putExtra(
                        android.provider.Settings.EXTRA_APP_PACKAGE,
                        context.packageName,
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        )
    }

    // Intercept save to request notification permission on Android 13+
    fun handleSave() {
        if (uiState.isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
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
        viewModel.save()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReminderEditEvent.Saved -> onSaved()
                is ReminderEditEvent.ShowError -> { /* could show snackbar */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode)
                            stringResource(R.string.reminder_edit_title)
                        else
                            stringResource(R.string.reminder_create_title),
                        color = FitLogTextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancelled) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.calendar_nav_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FitLogCard,
                ),
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = FitLogAccent)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.reminder_loading),
                    color = FitLogTextSecondary,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // ── Label ─────────────────────────────────────────────────────
                SectionHeader(stringResource(R.string.reminder_label))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.label,
                    onValueChange = viewModel::onLabelChanged,
                    placeholder = { Text(stringResource(R.string.reminder_label_placeholder)) },
                    isError = uiState.fieldErrors.containsKey("label"),
                    supportingText = uiState.fieldErrors["label"]?.let { { Text(it, color = FitLogError) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Time ──────────────────────────────────────────────────────
                SectionHeader(stringResource(R.string.reminder_time))
                Spacer(modifier = Modifier.height(8.dp))
                TimePickerField(
                    minutes = uiState.timeOfDayMinutes,
                    onMinutesChanged = viewModel::onTimeChanged,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Repeat days (weekday multi-select) ────────────────────────
                SectionHeader(stringResource(R.string.reminder_repeat_days))
                Spacer(modifier = Modifier.height(8.dp))
                val dayLabels = listOf(
                    R.string.reminder_monday,
                    R.string.reminder_tuesday,
                    R.string.reminder_wednesday,
                    R.string.reminder_thursday,
                    R.string.reminder_friday,
                    R.string.reminder_saturday,
                    R.string.reminder_sunday,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayLabels.forEachIndexed { index, labelRes ->
                        val isSelected = (uiState.daysOfWeekMask shr index) and 1 == 1
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onDayToggled(index) },
                            label = { Text(stringResource(labelRes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FitLogAccent.copy(alpha = 0.25f),
                                selectedLabelColor = FitLogAccent,
                            ),
                        )
                    }
                }
                if (uiState.fieldErrors.containsKey("daysOfWeekMask")) {
                    Text(
                        text = uiState.fieldErrors["daysOfWeekMask"] ?: "",
                        color = FitLogError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // Quick-select rows
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButtonCompact(
                        text = stringResource(R.string.reminder_everyday),
                        onClick = {
                            (0..6).forEach { viewModel.onDayToggled(it) }
                            // Ensure at least one is toggled on
                        },
                    )
                    TextButtonCompact(
                        text = stringResource(R.string.reminder_weekdays),
                        onClick = {
                            // Mon-Fri = bits 0-4
                            (0..4).forEach { viewModel.onDayToggled(it) }
                            // Ensure at least one is toggled on
                        },
                    )
                    TextButtonCompact(
                        text = stringResource(R.string.reminder_weekends),
                        onClick = {
                            // Sat-Sun = bits 5-6
                            (5..6).forEach { viewModel.onDayToggled(it) }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Link to plan ──────────────────────────────────────────────
                SectionHeader(stringResource(R.string.reminder_link_plan))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.reminder_link_plan_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val scheduleOptions = uiState.schedules
                if (scheduleOptions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.reminder_no_plan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    ScheduleDropdown(
                        options = scheduleOptions,
                        selectedId = uiState.scheduleId,
                        onSelected = viewModel::onScheduleSelected,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Enabled switch ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (uiState.isEnabled)
                            stringResource(R.string.reminder_enabled)
                        else
                            stringResource(R.string.reminder_disabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextPrimary,
                    )
                    Switch(
                        checked = uiState.isEnabled,
                        onCheckedChange = viewModel::onEnabledChanged,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Save button ───────────────────────────────────────────────
                Button(
                    onClick = ::handleSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = FitLogAccent,
                            modifier = Modifier
                                .width(20.dp)
                                .height(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.reminder_save),
                        color = FitLogTextPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = FitLogTextPrimary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun TextButtonCompact(
    text: String,
    onClick: () -> Unit,
) {
    val border = BorderStroke(1.dp, FitLogDivider)
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = border,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text = text, color = FitLogTextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDropdown(
    options: List<ScheduleOption>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.id == selectedId }?.label ?: stringResource(R.string.reminder_no_plan)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reminder_no_plan)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
) {
    val hour = minutes / 60
    val minute = minutes % 60
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = String.format("%02d:%02d", hour, minute),
        onValueChange = {},
        readOnly = true,
        trailingIcon = {
            Text(
                text = stringResource(R.string.reminder_select_time),
                color = FitLogAccent,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        enabled = true,
    )

    if (showDialog) {
        android.app.TimePickerDialog(
            androidx.compose.ui.platform.LocalContext.current,
            { _: android.widget.TimePicker, h: Int, m: Int ->
                onMinutesChanged(h * 60 + m)
                showDialog = false
            },
            hour,
            minute,
            true, // 24-hour format
        ).apply {
            setOnDismissListener { showDialog = false }
            show()
        }
    }
}
