package com.example.fitlog.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccentVariant
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogOnAccent
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import com.example.fitlog.core.designsystem.theme.FitLogWarning
import com.example.fitlog.domain.calendar.CalendarDay
import com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence
import com.example.fitlog.domain.calendar.CalendarWorkoutStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onNavigateToExercises: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToSession: (Long) -> Unit = {},
    onNavigateToDayDetail: (Long) -> Unit = {},
    topBarExtra: @Composable () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CalendarEvent.ShowSnackbar -> { /* snackbar handler */ }
            }
        }
    }

    // ── Schedule Dialog ──────────────────────────────────────────────────
    if (uiState.showScheduleDialog) {
        ScheduleTrainingDialog(
            date = uiState.scheduleDialogDate ?: LocalDate.now(),
            templates = uiState.templates,
            selectedTemplateId = uiState.selectedTemplateId,
            isOneTime = uiState.isOneTime,
            repeatIntervalWeeks = uiState.repeatIntervalWeeks,
            isScheduling = uiState.isScheduling,
            onDismiss = { viewModel.dismissScheduleDialog() },
            onTemplateSelected = { viewModel.selectTemplateForSchedule(it) },
            onOneTimeChanged = { viewModel.setOneTime(it) },
            onRepeatIntervalChanged = { viewModel.setRepeatIntervalWeeks(it) },
            onConfirm = { viewModel.confirmSchedule() },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Quick links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onNavigateToTemplates) {
                Icon(
                    Icons.Filled.ViewList,
                    contentDescription = stringResource(R.string.calendar_templates),
                    tint = FitLogAccent,
                )
            }
            IconButton(onClick = onNavigateToExercises) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = stringResource(R.string.calendar_exercises),
                    tint = FitLogAccent,
                )
            }
            topBarExtra()
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Month header ─────────────────────────────────────────────────
        MonthHeader(
            yearMonth = uiState.yearMonth,
            onPrevMonth = { viewModel.prevMonth() },
            onNextMonth = { viewModel.nextMonth() },
            onGoToToday = { viewModel.goToToday() },
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Day-of-week headers ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(
                R.string.calendar_mon,
                R.string.calendar_tue,
                R.string.calendar_wed,
                R.string.calendar_thu,
                R.string.calendar_fri,
                R.string.calendar_sat,
                R.string.calendar_sun,
            ).forEach { labelRes ->
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = FitLogTextSecondary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Calendar grid ────────────────────────────────────────────────
        CalendarGrid(
            yearMonth = uiState.yearMonth,
            days = uiState.days,
            selectedDay = uiState.selectedDay,
            onDayClick = { epochDay ->
                viewModel.selectDay(epochDay)
                onNavigateToDayDetail(epochDay)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Selected day detail ──────────────────────────────────────────
        if (uiState.selectedDay != null) {
            val selected = uiState.days.find { it.epochDay == uiState.selectedDay }
            if (selected != null) {
                SelectedDayDetail(
                    day = selected,
                    onSessionClick = onNavigateToSession,
                    onSchedule = { viewModel.showScheduleDialog(selected.epochDay) },
                )
            }
        }
    }
}

// ── Month Header ───────────────────────────────────────────────────────────

/** 日格状态点颜色映射（与 DayCell 一致）。 */
@Composable
private fun statusColor(status: CalendarWorkoutStatus): Color = when (status) {
    CalendarWorkoutStatus.SCHEDULED -> FitLogAccent
    CalendarWorkoutStatus.COMPLETED -> FitLogSuccess
    CalendarWorkoutStatus.PARTIALLY_COMPLETED -> FitLogSuccess.copy(alpha = 0.5f)
    CalendarWorkoutStatus.IN_PROGRESS -> FitLogWarning
    CalendarWorkoutStatus.SKIPPED,
    CalendarWorkoutStatus.CANCELLED,
    CalendarWorkoutStatus.RESCHEDULED,
    -> FitLogTextTertiary
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.calendar_prev_month),
                tint = FitLogTextPrimary,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = yearMonth.year.toString() + "年",
                style = MaterialTheme.typography.labelMedium,
                color = FitLogTextSecondary,
            )
            Text(
                text = yearMonth.monthValue.toString() + "月",
                style = MaterialTheme.typography.headlineSmall,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = stringResource(R.string.calendar_next_month),
                tint = FitLogTextPrimary,
            )
        }

        TextButton(onClick = onGoToToday, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(FitLogAccentContainer)) {
            Text(
                stringResource(R.string.calendar_today),
                color = FitLogAccent,
            )
        }
    }
}

// ── Calendar Grid ──────────────────────────────────────────────────────────

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    days: List<CalendarDay>,
    selectedDay: Long?,
    onDayClick: (Long) -> Unit,
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1 // 0=Mon … 6=Sun
    val totalDays = yearMonth.lengthOfMonth()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        var cellIndex = 0

        val leadingEmpty = dayOfWeekOffset
        val totalCells = leadingEmpty + totalDays
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (col in 0 until 7) {
                    val cellPos = row * 7 + col
                    if (cellPos < leadingEmpty || cellPos >= leadingEmpty + totalDays) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dayIndex = cellPos - leadingEmpty
                        val calendarDay = days.find { it.dayOfMonth == dayIndex + 1 }
                        val dayNum = dayIndex + 1
                        val epochDay = yearMonth.atDay(dayNum).toEpochDay()
                        val isSelected = selectedDay == epochDay
                        val isToday = calendarDay?.isToday == true

                        DayCell(
                            dayOfMonth = dayNum,
                            isToday = isToday,
                            isSelected = isSelected,
                            occurrences = calendarDay?.occurrences ?: emptyList(),
                            modifier = Modifier.weight(1f),
                            onClick = { onDayClick(epochDay) },
                        )
                    }
                }
            }
        }
    }
}

// ── Day Cell ───────────────────────────────────────────────────────────────

@Composable
private fun DayCell(
    dayOfMonth: Int,
    isToday: Boolean,
    isSelected: Boolean,
    occurrences: List<CalendarWorkoutOccurrence>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgColor = when {
        isSelected -> FitLogAccent
        isToday -> Color.Transparent
        else -> Color.Transparent
    }
    val borderColor = when {
        isToday -> FitLogAccent
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(if (isSelected) CircleShape else RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) FitLogOnAccent else if (isToday) FitLogAccent else FitLogTextPrimary,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )

        if (occurrences.isNotEmpty()) {
            StatusIndicators(occurrences)
        }
    }
}

// ── Status Indicators ──────────────────────────────────────────────────────

@Composable
private fun StatusIndicators(occurrences: List<CalendarWorkoutOccurrence>) {
    val statuses = occurrences.map { it.status }.distinct().take(3)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        statuses.forEach { status ->
            val color = when (status) {
                CalendarWorkoutStatus.IN_PROGRESS -> FitLogAccent
                CalendarWorkoutStatus.COMPLETED -> FitLogSuccess
                CalendarWorkoutStatus.PARTIALLY_COMPLETED -> FitLogAccent.copy(alpha = 0.7f)
                CalendarWorkoutStatus.SKIPPED -> FitLogTextTertiary
                CalendarWorkoutStatus.CANCELLED -> FitLogTextTertiary
                CalendarWorkoutStatus.RESCHEDULED -> FitLogAccentVariant
                CalendarWorkoutStatus.SCHEDULED -> FitLogTextSecondary
            }
            Box(
                modifier = Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color),
            )
        }
    }
}

// ── Selected Day Detail ────────────────────────────────────────────────────

@Composable
private fun SelectedDayDetail(
    day: CalendarDay,
    onSessionClick: (Long) -> Unit,
    onSchedule: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FitLogSurfaceVariant)
            .padding(16.dp),
    ) {
        val dateStr = buildString {
            append(day.date.year)
            append("年")
            append(day.date.monthValue)
            append("月")
            append(day.dayOfMonth)
            append("日 ")
            append(
                day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.titleSmall,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (day.isToday) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(FitLogAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.calendar_today_tag),
                        style = MaterialTheme.typography.labelSmall,
                        color = FitLogAccent,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (day.occurrences.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_no_workouts),
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextSecondary,
            )
        } else {
            day.occurrences.forEach { occurrence ->
                OccurrenceRow(occurrence, onSessionClick)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Schedule button
        Button(
            onClick = onSchedule,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.calendar_schedule_title))
        }
    }
}

@Composable
private fun OccurrenceRow(
    occurrence: CalendarWorkoutOccurrence,
    onSessionClick: (Long) -> Unit,
) {
    val statusColor = when (occurrence.status) {
        CalendarWorkoutStatus.COMPLETED -> FitLogSuccess
        CalendarWorkoutStatus.IN_PROGRESS -> FitLogAccent
        CalendarWorkoutStatus.SKIPPED -> FitLogTextTertiary
        CalendarWorkoutStatus.RESCHEDULED -> FitLogAccentVariant
        CalendarWorkoutStatus.CANCELLED -> FitLogTextTertiary
        else -> FitLogTextSecondary
    }
    val statusIcon: @Composable () -> Unit = {
        when {
            occurrence.status == CalendarWorkoutStatus.COMPLETED ->
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = FitLogSuccess,
                    modifier = Modifier.size(18.dp),
                )
            occurrence.status == CalendarWorkoutStatus.RESCHEDULED ->
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = FitLogAccentVariant,
                    modifier = Modifier.size(18.dp),
                )
            else ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
        }
    }

    val clickModifier = if (occurrence.sessionId != null) {
        Modifier.clickable { onSessionClick(occurrence.sessionId) }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(FitLogCard)
            .then(clickModifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        statusIcon()

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = occurrence.templateName,
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.Medium,
            )
            val statusLabel = when (occurrence.status) {
                CalendarWorkoutStatus.COMPLETED -> stringResource(R.string.calendar_status_completed)
                CalendarWorkoutStatus.IN_PROGRESS -> stringResource(R.string.calendar_status_in_progress)
                CalendarWorkoutStatus.SKIPPED -> stringResource(R.string.calendar_status_skipped)
                CalendarWorkoutStatus.RESCHEDULED -> stringResource(R.string.calendar_status_rescheduled)
                CalendarWorkoutStatus.CANCELLED -> stringResource(R.string.calendar_status_cancelled)
                else -> stringResource(R.string.calendar_status_scheduled)
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
            )
        }

        if (occurrence.status == CalendarWorkoutStatus.RESCHEDULED &&
            occurrence.occurrenceDate != occurrence.plannedDate
        ) {
            Text(
                text = "→ ${occurrence.plannedDate.monthValue}/${occurrence.plannedDate.dayOfMonth}",
                style = MaterialTheme.typography.labelSmall,
                color = FitLogAccentVariant,
            )
        }
    }
}

// ── Schedule Training Dialog ───────────────────────────────────────────────

@Composable
private fun ScheduleTrainingDialog(
    date: LocalDate,
    templates: List<com.example.fitlog.core.model.WorkoutTemplate>,
    selectedTemplateId: Long?,
    isOneTime: Boolean,
    repeatIntervalWeeks: Int,
    isScheduling: Boolean,
    onDismiss: () -> Unit,
    onTemplateSelected: (Long) -> Unit,
    onOneTimeChanged: (Boolean) -> Unit,
    onRepeatIntervalChanged: (Int) -> Unit,
    onConfirm: () -> Unit,
) {
    val dateStr = "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${stringResource(R.string.calendar_schedule_title)} - $dateStr",
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            if (isScheduling) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FitLogAccent)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                ) {
                    // Template selection
                    Text(
                        text = stringResource(R.string.calendar_schedule_select_template),
                        style = MaterialTheme.typography.titleSmall,
                        color = FitLogTextPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (templates.isEmpty()) {
                        Text(
                            text = stringResource(R.string.calendar_schedule_no_templates),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitLogTextSecondary,
                        )
                    } else {
                        templates.forEach { template ->
                            val isSelected = selectedTemplateId == template.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) FitLogAccent.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .clickable { onTemplateSelected(template.id) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onTemplateSelected(template.id) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = FitLogAccent,
                                    ),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = template.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FitLogTextPrimary,
                                    )
                                    if (template.notes != null) {
                                        Text(
                                            text = template.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FitLogTextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scheduling type
                    if (selectedTemplateId != null) {
                        Text(
                            text = "安排方式",
                            style = MaterialTheme.typography.titleSmall,
                            color = FitLogTextPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOneTimeChanged(true) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isOneTime,
                                onClick = { onOneTimeChanged(true) },
                                colors = RadioButtonDefaults.colors(selectedColor = FitLogAccent),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.calendar_schedule_one_time),
                                style = MaterialTheme.typography.bodyMedium,
                                color = FitLogTextPrimary,
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOneTimeChanged(false) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = !isOneTime,
                                onClick = { onOneTimeChanged(false) },
                                colors = RadioButtonDefaults.colors(selectedColor = FitLogAccent),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.calendar_schedule_recurring),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FitLogTextPrimary,
                                )
                                if (!isOneTime) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "每",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FitLogTextSecondary,
                                        )
                                        // Repeat interval selector
                                        repeatIntervalSelector(
                                            current = repeatIntervalWeeks,
                                            onChanged = onRepeatIntervalChanged,
                                        )
                                        Text(
                                            text = "周",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FitLogTextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedTemplateId != null && !isScheduling,
                colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.calendar_schedule_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.calendar_reschedule_cancel), color = FitLogTextSecondary)
            }
        },
    )
}

@Composable
private fun repeatIntervalSelector(
    current: Int,
    onChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(FitLogCard)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { if (current > 1) onChanged(current - 1) },
            enabled = current > 1,
        ) { Text("-", color = FitLogAccent) }

        Text(
            text = current.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = FitLogTextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        TextButton(
            onClick = { if (current < 4) onChanged(current + 1) },
            enabled = current < 4,
        ) { Text("+", color = FitLogAccent) }
    }
}
