package com.example.fitlog.feature.today

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.ScrollablePageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.domain.calendar.CalendarWorkoutStatus
import com.example.fitlog.feature.checkin.CheckInCard
import com.example.fitlog.feature.checkin.CheckInViewModel
import java.util.Calendar

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onStartWorkout: (Long) -> Unit = {},
    onResumeWorkout: (Long) -> Unit = {},
    onNavigateToWorkoutDetail: (Long) -> Unit = {},
    onNavigateToNutrition: () -> Unit = {},
    onNavigateToBodyMeasurement: () -> Unit = {},
    onNavigateToCamera: () -> Unit = {},
    onNavigateToTemplatePicker: () -> Unit = {},
    onNavigateToQuickSetup: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.StartWorkout -> onStartWorkout(event.sessionId)
                is TodayEvent.ResumeWorkout -> onResumeWorkout(event.sessionId)
                is TodayEvent.NavigateToWorkoutDetail -> onNavigateToWorkoutDetail(event.sessionId)
                is TodayEvent.NavigateToTemplatePicker -> onNavigateToTemplatePicker()
                is TodayEvent.NavigateToQuickSetup -> onNavigateToQuickSetup()
            }
        }
    }

    val greeting = greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    // Start workout dialog
    if (uiState.showStartWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissStartDialog() },
            title = { Text("开始训练", color = FitLogTextPrimary) },
            text = {
                Column {
                    Text(
                        "选择训练方式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextSecondary,
                    )
                }
            },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = { viewModel.onStartFromTemplate() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "从训练模板开始",
                            color = FitLogAccent,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    TextButton(
                        onClick = { viewModel.onStartFreeWorkout() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "自由训练",
                            color = FitLogAccent,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissStartDialog() }) {
                    Text("取消", color = FitLogTextSecondary)
                }
            },
        )
    }

    Scaffold(
        topBar = { FitLogTopAppBar(title = stringResource(R.string.nav_today)) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        ScrollablePageContainer(modifier = Modifier.padding(innerPadding)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(greeting),
                    style = MaterialTheme.typography.headlineSmall,
                    color = FitLogTextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.today_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextSecondary,
                )
                Spacer(modifier = Modifier.height(24.dp))

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogError,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SectionHeader(title = stringResource(R.string.section_todays_workout))

                if (uiState.hasInProgressWorkout) {
                    FitLogCard(onClick = { viewModel.onQuickStart() }) {
                        Text(
                            stringResource(R.string.today_workout_in_progress_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = FitLogAccent,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.today_workout_in_progress_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogTextSecondary,
                        )
                    }
                } else if (uiState.occurrences.isNotEmpty()) {
                    uiState.occurrences.forEach { occurrence ->
                        val isClickable = occurrence.status == CalendarWorkoutStatus.SCHEDULED
                        FitLogCard(
                            onClick = if (isClickable || occurrence.status == CalendarWorkoutStatus.COMPLETED
                                || occurrence.status == CalendarWorkoutStatus.PARTIALLY_COMPLETED
                            ) {
                                { viewModel.onStartWorkout(occurrence) }
                            } else null,
                        ) {
                            Text(
                                text = occurrence.templateName,
                                style = MaterialTheme.typography.titleMedium,
                                color = FitLogTextPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val statusLabel = when (occurrence.status) {
                                CalendarWorkoutStatus.SCHEDULED -> stringResource(R.string.today_occurrence_scheduled)
                                CalendarWorkoutStatus.RESCHEDULED -> stringResource(R.string.today_occurrence_rescheduled)
                                CalendarWorkoutStatus.SKIPPED -> stringResource(R.string.today_occurrence_skipped)
                                CalendarWorkoutStatus.COMPLETED -> stringResource(R.string.today_occurrence_completed)
                                CalendarWorkoutStatus.PARTIALLY_COMPLETED -> stringResource(R.string.today_occurrence_partial)
                                CalendarWorkoutStatus.CANCELLED -> stringResource(R.string.today_occurrence_cancelled)
                                CalendarWorkoutStatus.IN_PROGRESS -> stringResource(R.string.today_occurrence_in_progress)
                            }
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = FitLogTextSecondary,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    EmptyState(
                        icon = Icons.Filled.FitnessCenter,
                        title = stringResource(R.string.empty_today_title),
                        subtitle = stringResource(R.string.empty_today_subtitle),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                CheckInCard(viewModel = hiltViewModel())
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = stringResource(R.string.section_quick_actions))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.FitnessCenter,
                        label = stringResource(R.string.quick_action_start_workout),
                        onClick = { viewModel.onQuickStart() },
                    )
                    QuickActionCard(
                        icon = Icons.Filled.Restaurant,
                        label = stringResource(R.string.quick_action_nutrition),
                        onClick = onNavigateToNutrition,
                    )
                    QuickActionCard(
                        icon = Icons.Filled.MonitorWeight,
                        label = stringResource(R.string.quick_action_body_measurement),
                        onClick = onNavigateToBodyMeasurement,
                    )
                    QuickActionCard(
                        icon = Icons.Filled.CameraAlt,
                        label = stringResource(R.string.quick_action_camera),
                        onClick = onNavigateToCamera,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FitLogCard(
        onClick = onClick,
        modifier = Modifier.width(120.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = FitLogAccent,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = FitLogTextPrimary,
            )
        }
    }
}

private fun greetingForHour(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.today_greeting_morning
    in 12..17 -> R.string.today_greeting_afternoon
    else -> R.string.today_greeting_evening
}
