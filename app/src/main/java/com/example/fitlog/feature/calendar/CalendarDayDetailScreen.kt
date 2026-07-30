package com.example.fitlog.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence
import com.example.fitlog.domain.calendar.CalendarWorkoutStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDayDetailScreen(
    viewModel: CalendarDayDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToExecution: (Long) -> Unit = {},
    onNavigateToWorkoutDetail: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDatePicker by remember { mutableStateOf(false) }
    var rescheduleTarget by remember { mutableStateOf<CalendarWorkoutOccurrence?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CalendarDayDetailEvent.NavigateToExecution -> onNavigateToExecution(event.sessionId)
                is CalendarDayDetailEvent.NavigateToWorkout -> onNavigateToWorkoutDetail(event.sessionId)
                is CalendarDayDetailEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is CalendarDayDetailEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    val dateStr = buildString {
        append(uiState.date.year)
        append(stringResource(R.string.calendar_date_year))
        append(uiState.date.monthValue)
        append(stringResource(R.string.calendar_date_month))
        append(uiState.date.dayOfMonth)
        append(stringResource(R.string.calendar_date_day))
        append(" ")
        append(uiState.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE))
    }

    // Reschedule DatePicker Dialog
    if (showDatePicker && rescheduleTarget != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                rescheduleTarget = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val targetDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.onReschedule(rescheduleTarget!!, targetDate)
                        }
                        showDatePicker = false
                        rescheduleTarget = null
                    },
                ) {
                    Text(stringResource(R.string.calendar_reschedule_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        rescheduleTarget = null
                    },
                ) {
                    Text(stringResource(R.string.calendar_reschedule_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.titleMedium,
                        color = FitLogTextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.calendar_nav_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FitLogBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = FitLogAccent)
                    }
                } else if (uiState.error != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitLogError,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.refresh() },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                } else if (uiState.occurrences.isEmpty()) {
                    Text(
                        text = stringResource(R.string.calendar_no_workouts_detail),
                        style = MaterialTheme.typography.bodyLarge,
                        color = FitLogTextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        items(uiState.occurrences, key = { it.key }) { occurrence ->
                            OccurrenceDetailCard(
                                occurrence = occurrence,
                                onStart = { viewModel.onStartWorkout(occurrence) },
                                onSkip = { viewModel.onSkip(occurrence) },
                                onRestore = { viewModel.onRestore(occurrence) },
                                onContinue = { occurrence.sessionId?.let { viewModel.onContinueWorkout(it) } },
                                onViewDetail = { occurrence.sessionId?.let { viewModel.onViewDetail(it) } },
                                onReschedule = {
                                    rescheduleTarget = occurrence
                                    showDatePicker = true
                                },
                                onPostpone = { viewModel.onPostpone(occurrence) },
                            )
                        }
                    }
                }
            }

            // Loading overlay during reschedule
            if (uiState.isRescheduling) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FitLogAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.calendar_reschedule_loading),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OccurrenceDetailCard(
    occurrence: CalendarWorkoutOccurrence,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    onRestore: () -> Unit,
    onContinue: () -> Unit,
    onViewDetail: () -> Unit,
    onReschedule: () -> Unit,
    onPostpone: () -> Unit,
) {
    val (statusColor, statusLabelRes) = when (occurrence.status) {
        CalendarWorkoutStatus.COMPLETED -> FitLogSuccess to R.string.calendar_status_completed
        CalendarWorkoutStatus.IN_PROGRESS -> FitLogAccent to R.string.calendar_status_in_progress
        CalendarWorkoutStatus.SKIPPED -> FitLogTextTertiary to R.string.calendar_status_skipped
        CalendarWorkoutStatus.RESCHEDULED -> Color(0xFFFFA726) to R.string.calendar_status_rescheduled
        CalendarWorkoutStatus.CANCELLED -> FitLogTextTertiary to R.string.calendar_status_cancelled
        CalendarWorkoutStatus.PARTIALLY_COMPLETED -> FitLogAccent.copy(alpha = 0.7f) to R.string.calendar_status_partial
        CalendarWorkoutStatus.SCHEDULED -> FitLogTextSecondary to R.string.calendar_status_scheduled
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .then(
            if (occurrence.isOriginalDateMarker)
                Modifier.border(1.dp, Color(0xFFFFA726).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            else Modifier
        )
        .background(
            if (occurrence.isOriginalDateMarker) FitLogSurfaceVariant.copy(alpha = 0.5f)
            else FitLogCard
        )
        .padding(16.dp)

    Column(modifier = cardModifier) {
        // Header row: template name + status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = occurrence.templateName,
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(statusLabelRes),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }

        // Original date marker note
        if (occurrence.isOriginalDateMarker) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "原定日期: ${occurrence.occurrenceDate?.monthValue}/${occurrence.occurrenceDate?.dayOfMonth}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFA726),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons based on status
        when (occurrence.status) {
            CalendarWorkoutStatus.SCHEDULED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_action_start))
                    }
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_action_skip))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onReschedule,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_action_reschedule))
                    }
                    OutlinedButton(
                        onClick = onPostpone,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_action_postpone))
                    }
                }
            }

            CalendarWorkoutStatus.IN_PROGRESS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_action_continue))
                    }
                }
            }

            CalendarWorkoutStatus.COMPLETED, CalendarWorkoutStatus.PARTIALLY_COMPLETED -> {
                Button(
                    onClick = onViewDetail,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogSuccess),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.calendar_action_view_detail))
                }
            }

            CalendarWorkoutStatus.SKIPPED, CalendarWorkoutStatus.RESCHEDULED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (occurrence.scheduleId != null) {
                        Button(
                            onClick = onRestore,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.calendar_action_restore))
                        }
                    }
                    if (occurrence.sessionId != null) {
                        OutlinedButton(
                            onClick = onViewDetail,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.calendar_action_view_detail))
                        }
                    }
                }
            }

            CalendarWorkoutStatus.CANCELLED -> {
                if (occurrence.sessionId != null) {
                    Button(
                        onClick = onViewDetail,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_action_view_detail))
                    }
                }
            }
        }
    }
}
