package com.example.fitlog.feature.today

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.feature.checkin.CheckInCard
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.StarCardEmphasis
import com.example.fitlog.core.designsystem.component.StarThemedCard
import com.example.fitlog.core.designsystem.component.MetricCard
import com.example.fitlog.core.designsystem.component.QuickAction
import com.example.fitlog.core.designsystem.component.QuickActionGrid
import com.example.fitlog.core.designsystem.component.SectionTitle
import com.example.fitlog.core.designsystem.component.ScrollablePageContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogType
import com.example.fitlog.domain.calendar.CalendarWorkoutStatus
import java.time.LocalDate

private val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    checkInViewModel: com.example.fitlog.feature.checkin.CheckInViewModel = hiltViewModel(),
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

    if (uiState.showStartWorkoutDialog) {
        StartWorkoutDialog(
            onDismiss = { viewModel.onDismissStartDialog() },
            onFromTemplate = { viewModel.onStartFromTemplate() },
            onFreeWorkout = { viewModel.onStartFreeWorkout() },
        )
    }

    Scaffold(
        topBar = { FitLogTopAppBar(title = stringResource(R.string.nav_today)) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        ScrollablePageContainer(modifier = Modifier.padding(innerPadding)) {
            val today = LocalDate.now()
            val greeting = when (java.time.LocalTime.now().hour) {
                in 5..11 -> R.string.today_greeting_morning
                in 12..17 -> R.string.today_greeting_afternoon
                else -> R.string.today_greeting_evening
            }

            // ── 问候与日期 ────────────────────────────────────────────────
            Text(
                text = stringResource(greeting),
                style = FitLogType.pageTitle,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.today_date_format,
                    today.monthValue,
                    today.dayOfMonth,
                    weekDays[today.dayOfWeek.value - 1],
                ),
                style = FitLogType.caption,
                color = FitLogTextSecondary,
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ── 今日训练 Hero ─────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    BoxLoading()
                }
                uiState.hasInProgressWorkout -> {
                    StarThemedCard(
                        emphasis = StarCardEmphasis.PROMINENT,
                    ) {
                        Text(
                            text = stringResource(R.string.today_dashboard_in_progress_title),
                            style = FitLogType.cardTitle,
                            color = FitLogAccent,
                        )
                        Text(
                            text = stringResource(
                                R.string.today_dashboard_sets_progress,
                                uiState.inProgressCompletedSets,
                                uiState.inProgressTotalSets.coerceAtLeast(uiState.inProgressCompletedSets),
                            ),
                            style = FitLogType.caption,
                            color = FitLogTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.onResumeInProgressWorkout() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.today_dashboard_continue))
                        }
                    }
                }
                uiState.occurrences.isEmpty() -> {
                    StarThemedCard(
                        emphasis = StarCardEmphasis.PROMINENT,
                    ) {
                        Text(
                            text = stringResource(R.string.today_dashboard_no_plan_title),
                            style = FitLogType.cardTitle,
                            color = FitLogAccent,
                        )
                        Text(
                            text = stringResource(R.string.empty_today_subtitle),
                            style = FitLogType.caption,
                            color = FitLogTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = { viewModel.onQuickStart() },
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) {
                                Text(stringResource(R.string.today_dashboard_schedule))
                            }
                            OutlinedButton(
                                onClick = { viewModel.onStartFreeWorkout() },
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) {
                                Text(
                                    stringResource(R.string.today_dashboard_free),
                                    color = FitLogAccent,
                                )
                            }
                        }
                    }
                }
                else -> {
                    val primary = uiState.occurrences.first()
                    StarThemedCard(
                        emphasis = StarCardEmphasis.PROMINENT,
                    ) {
                        Text(
                            text = stringResource(R.string.section_todays_workout),
                            style = FitLogType.cardTitle,
                            color = FitLogAccent,
                        )
                        Text(
                            text = primary.templateName,
                            style = FitLogType.caption,
                            color = FitLogTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.onStartWorkout(primary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Text(stringResource(R.string.today_dashboard_start))
                        }
                    }
                    if (uiState.occurrences.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.occurrences.drop(1).forEach { occurrence ->
                            FitLogCard(onClick = {
                                if (occurrence.status == CalendarWorkoutStatus.SCHEDULED ||
                                    occurrence.status == CalendarWorkoutStatus.COMPLETED ||
                                    occurrence.status == CalendarWorkoutStatus.PARTIALLY_COMPLETED
                                ) {
                                    viewModel.onStartWorkout(occurrence)
                                }
                            }) {
                                Text(
                                    text = occurrence.templateName,
                                    style = FitLogType.body,
                                    color = FitLogTextPrimary,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    style = FitLogType.caption,
                    color = FitLogError,
                )
            }

            // ── 本周统计 ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(title = stringResource(R.string.today_section_week_stats))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    label = stringResource(R.string.today_week_workout_days),
                    value = "${uiState.weekWorkoutCount}",
                    icon = Icons.Filled.FitnessCenter,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.today_week_volume),
                    value = formatVolume(uiState.weekVolumeKg),
                    icon = Icons.Filled.CalendarMonth,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.today_current_weight),
                    value = uiState.currentWeightKg?.let { formatWeight(it) } ?: "—",
                    icon = Icons.Filled.Straighten,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── 快速操作 2×2 ──────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(title = stringResource(R.string.section_quick_actions))
            Spacer(modifier = Modifier.height(8.dp))
            QuickActionGrid(
                actions = listOf(
                    QuickAction(
                        icon = Icons.Filled.FitnessCenter,
                        label = stringResource(
                            if (uiState.hasInProgressWorkout) {
                                R.string.quick_action_resume_workout
                            } else {
                                R.string.quick_action_start_workout
                            },
                        ),
                        onClick = { viewModel.onQuickStart() },
                    ),
                    QuickAction(
                        icon = Icons.Filled.Restaurant,
                        label = stringResource(R.string.quick_action_nutrition),
                        onClick = onNavigateToNutrition,
                    ),
                    QuickAction(
                        icon = Icons.Filled.Straighten,
                        label = stringResource(R.string.quick_action_body_measurement),
                        onClick = onNavigateToBodyMeasurement,
                    ),
                    QuickAction(
                        icon = Icons.Filled.PhotoCamera,
                        label = stringResource(R.string.quick_action_camera),
                        onClick = onNavigateToCamera,
                    ),
                ),
            )

            // ── 今日营养摘要 ──────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(title = stringResource(R.string.today_section_nutrition))
            Spacer(modifier = Modifier.height(8.dp))
            NutritionSummaryCard(
                summary = uiState.nutritionSummary,
                onOpenNutrition = onNavigateToNutrition,
            )

            // ── 每日打卡 ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            CheckInCard(viewModel = checkInViewModel)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BoxLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = FitLogAccent)
    }
}

@Composable
private fun NutritionSummaryCard(
    summary: com.example.fitlog.data.repository.DailyNutritionSummary?,
    onOpenNutrition: () -> Unit,
) {
    FitLogCard(
        style = com.example.fitlog.core.designsystem.component.FitLogCardStyle.TONAL,
        onClick = onOpenNutrition,
    ) {
        if (summary == null || (summary.calories <= 0 && summary.protein <= 0)) {
            Text(
                text = stringResource(R.string.today_nutrition_empty),
                style = FitLogType.body,
                color = FitLogTextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.quick_action_nutrition) + " ›",
                style = FitLogType.caption,
                color = FitLogAccent,
            )
        } else {
            Text(
                text = stringResource(
                    R.string.today_nutrition_kcal,
                    summary.calories.toInt(),
                    summary.targetCalories,
                ),
                style = FitLogType.statistic,
                color = FitLogAccent,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.today_nutrition_protein,
                    summary.protein.toInt(),
                    summary.targetProtein,
                ),
                style = FitLogType.caption,
                color = FitLogTextSecondary,
            )
        }
    }
}

private fun formatVolume(kg: Double): String =
    if (kg >= 1000) "%.1f t".format(kg / 1000) else "%.0f".format(kg)

private fun formatWeight(kg: Double): String = "%.1f".format(kg)

@Composable
private fun StartWorkoutDialog(
    onDismiss: () -> Unit,
    onFromTemplate: () -> Unit,
    onFreeWorkout: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.today_quick_start)) },
        text = { Text(stringResource(R.string.today_quick_start_desc)) },
        confirmButton = {
            TextButton(onClick = onFromTemplate) {
                Text(stringResource(R.string.today_dashboard_schedule), color = FitLogAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onFreeWorkout) {
                Text(stringResource(R.string.today_dashboard_free), color = FitLogTextSecondary)
            }
        },
    )
}
