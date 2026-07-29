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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.fitlog.core.designsystem.theme.FitLogAccentVariant
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
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
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Quick links (keep from original PlanScreen)
        Row {
            TextButton(onClick = onNavigateToTemplates) {
                Text(stringResource(R.string.calendar_templates), color = FitLogAccent)
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onNavigateToExercises) {
                Text(stringResource(R.string.calendar_exercises), color = FitLogAccent)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Month header ──────────────────────────────────────────────────────
        MonthHeader(
            yearMonth = uiState.yearMonth,
            onPrevMonth = { viewModel.prevMonth() },
            onNextMonth = { viewModel.nextMonth() },
            onGoToToday = { viewModel.goToToday() },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Day-of-week headers ───────────────────────────────────────────────
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

        // ── Calendar grid ─────────────────────────────────────────────────────
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

        // ── Selected day detail ───────────────────────────────────────────────
        if (uiState.selectedDay != null) {
            val selected = uiState.days.find { it.epochDay == uiState.selectedDay }
            if (selected != null) {
                SelectedDayDetail(
                    day = selected,
                    onSessionClick = onNavigateToSession,
                )
            }
        }
    }
}

// ── Month Header ───────────────────────────────────────────────────────────

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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = yearMonth.month.name + " " + yearMonth.year.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = stringResource(R.string.calendar_next_month),
                tint = FitLogTextPrimary,
            )
        }

        TextButton(onClick = onGoToToday) {
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
            .clip(RoundedCornerShape(12.dp))
            .background(FitLogCard)
            .padding(4.dp),
    ) {
        var cellIndex = 0

        // Leading empty cells
        val leadingEmpty = dayOfWeekOffset

        // Total cells needed
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
                        // Empty cell
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
        isSelected -> FitLogAccent.copy(alpha = 0.2f)
        isToday -> FitLogAccentVariant.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> FitLogAccent
        isToday -> FitLogAccentVariant
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
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
            color = if (isToday || isSelected) FitLogAccent else FitLogTextPrimary,
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
    val statuses = occurrences.map { it.status }.distinct()

    // Use colored dots
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
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

// ── Selected Day Detail ────────────────────────────────────────────────────

@Composable
private fun SelectedDayDetail(
    day: CalendarDay,
    onSessionClick: (Long) -> Unit,
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
