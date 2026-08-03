package com.example.fitlog.feature.template

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
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
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    viewModel: TemplateListViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    // Picker mode (used from TodayScreen "从训练模板开始")
    isPickerMode: Boolean = false,
    onTemplateSelected: (templateId: Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplateListEvent.NavigateToCreate -> onNavigateToCreate()
                is TemplateListEvent.NavigateToEdit -> onNavigateToEdit(event.templateId)
                is TemplateListEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Date picker for recurring start/end dates
    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }

    if (datePickerTarget != null && uiState.showScheduleDialog) {
        val initialMillis = when (datePickerTarget) {
            DatePickerTarget.START_DATE -> uiState.startDate?.toEpochMillis()
            DatePickerTarget.END_DATE -> uiState.endDate?.toEpochMillis()
                ?: uiState.startDate?.toEpochMillis()
            null -> null
        } ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = remember { pastDisabledSelectableDates() },
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            when (datePickerTarget) {
                                DatePickerTarget.START_DATE -> viewModel.setStartDate(date)
                                DatePickerTarget.END_DATE -> viewModel.setEndDate(date)
                                null -> Unit
                            }
                        }
                        datePickerTarget = null
                    },
                ) { Text("确认", color = FitLogAccent) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) {
                    Text("取消", color = FitLogTextSecondary)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Schedule dialog
    if (uiState.showScheduleDialog && datePickerTarget == null) {
        var calendarMonth by remember { mutableStateOf(YearMonth.now()) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissScheduleDialog() },
            title = {
                Text(
                    text = "安排训练: ${uiState.scheduleTemplateName}",
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                if (uiState.isScheduling) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = FitLogAccent)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    ) {
                        // Scheduling type
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
                                .clickable { viewModel.setOneTime(true) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = uiState.isOneTime,
                                onClick = { viewModel.setOneTime(true) },
                                colors = RadioButtonDefaults.colors(selectedColor = FitLogAccent),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("仅这一次", color = FitLogTextPrimary)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setOneTime(false) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = !uiState.isOneTime,
                                onClick = { viewModel.setOneTime(false) },
                                colors = RadioButtonDefaults.colors(selectedColor = FitLogAccent),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("每周重复", color = FitLogTextPrimary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.isOneTime) {
                            // ── Multi-select calendar ──────────────────────────
                            Text(
                                text = "选择日期 (可多选)",
                                style = MaterialTheme.typography.titleSmall,
                                color = FitLogTextPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            MultiDateCalendar(
                                month = calendarMonth,
                                selectedDates = uiState.selectedDates,
                                today = LocalDate.now(),
                                onToggleDate = { viewModel.toggleScheduleDate(it) },
                                onPrevMonth = { calendarMonth = calendarMonth.minusMonths(1) },
                                onNextMonth = { calendarMonth = calendarMonth.plusMonths(1) },
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.clearSelectedDates() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("清空选择", color = FitLogTextSecondary,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.selectThisWeek() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("选择本周", color = FitLogAccent,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.selectNextWeek() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("选择下周", color = FitLogAccent,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "已选 ${uiState.selectedDates.size} 天",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.selectedDates.isEmpty()) FitLogTextTertiary else FitLogAccent,
                                fontWeight = if (uiState.selectedDates.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        } else {
                            // ── Multi-weekday recurring ────────────────────────
                            Text(
                                text = "选择星期 (可多选)",
                                style = MaterialTheme.typography.titleSmall,
                                color = FitLogTextPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                (1..7).forEach { day ->
                                    val selected = day in uiState.selectedWeekdays
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selected) FitLogAccent.copy(alpha = 0.15f)
                                                else FitLogCard.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                width = if (selected) 1.dp else 0.dp,
                                                color = FitLogAccent,
                                                shape = RoundedCornerShape(8.dp),
                                            )
                                            .clickable { viewModel.toggleWeekday(day) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = weekdayChar(day),
                                            color = if (selected) FitLogAccent else FitLogTextSecondary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Start / end date
                            Text(
                                text = "开始日期",
                                style = MaterialTheme.typography.titleSmall,
                                color = FitLogTextPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val startDate = uiState.startDate
                            OutlinedButton(
                                onClick = { datePickerTarget = DatePickerTarget.START_DATE },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = FitLogAccent,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = startDate?.let { formatChineseDate(it) } ?: "未设置",
                                    color = FitLogTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "结束日期 (可选)",
                                style = MaterialTheme.typography.titleSmall,
                                color = FitLogTextPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val endDate = uiState.endDate
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { datePickerTarget = DatePickerTarget.END_DATE },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = FitLogAccent,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = endDate?.let { formatChineseDate(it) } ?: "长期有效",
                                        color = FitLogTextPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                if (endDate != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(onClick = { viewModel.clearEndDate() }) {
                                        Text("清除", color = FitLogTextSecondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "重复周期",
                                style = MaterialTheme.typography.titleSmall,
                                color = FitLogTextPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("每", color = FitLogTextSecondary,
                                    style = MaterialTheme.typography.bodySmall)
                                repeatIntervalSelector(
                                    current = uiState.repeatIntervalWeeks,
                                    onChanged = { viewModel.setRepeatIntervalWeeks(it) },
                                )
                                Text("周", color = FitLogTextSecondary,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSchedule() },
                    enabled = !uiState.isScheduling && uiState.canConfirmSchedule,
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("确认安排")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissScheduleDialog() }) {
                    Text("取消", color = FitLogTextSecondary)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isPickerMode) "选择模板" else "训练模板",
                        color = FitLogTextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                actions = {
                    if (!isPickerMode) {
                        IconButton(onClick = { viewModel.onCreateNew() }) {
                            Icon(Icons.Filled.Add, "新建模板", tint = FitLogAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FitLogSurface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FitLogBackground,
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FitLogAccent)
                }
            }
            uiState.templates.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.FitnessCenter,
                    title = if (isPickerMode) "还没有训练模板" else "还没有训练模板",
                    subtitle = if (isPickerMode) "请先在「计划」页面创建训练模板" else "创建训练模板来组织你的训练动作",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                ) {
                    items(uiState.templates, key = { it.id }) { template ->
                        FitLogCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                if (isPickerMode) {
                                    onTemplateSelected(template.id)
                                } else {
                                    viewModel.onTemplateClicked(template.id)
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                com.example.fitlog.feature.exercise.ExerciseThumbnailByKey(
                                    builtInKey = uiState.firstBuiltInKeys[template.id],
                                    contentDescription = template.name,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        template.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = FitLogTextPrimary,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        template.notes ?: "无备注",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FitLogTextSecondary,
                                    )
                                }
                                if (!isPickerMode) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Schedule button
                                    Button(
                                        onClick = {
                                            viewModel.onScheduleClick(template.id, template.name)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FitLogAccent.copy(alpha = 0.15f),
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                                    ) {
                                        Icon(
                                            Icons.Filled.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = FitLogAccent,
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "安排",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = FitLogAccent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun repeatIntervalSelector(
    current: Int,
    onChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(FitLogCard.copy(alpha = 0.5f))
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

private fun chineseDayOfWeek(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    7 -> "周日"
    else -> ""
}

private fun weekdayChar(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> ""
}

private fun formatChineseDate(date: LocalDate): String =
    "${date.year}年${date.monthValue}月${date.dayOfMonth}日 (${chineseDayOfWeek(date.dayOfWeek.value)})"

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** SelectableDates that reject any date before today (past scheduling is not allowed). */
@OptIn(ExperimentalMaterial3Api::class)
private fun pastDisabledSelectableDates(): SelectableDates = object : SelectableDates {
    private val todayUtcMillis: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis >= todayUtcMillis

    override fun isSelectableYear(year: Int): Boolean = true
}

private enum class DatePickerTarget { START_DATE, END_DATE }

// ── Multi-select calendar ───────────────────────────────────────────────────

@Composable
private fun MultiDateCalendar(
    month: YearMonth,
    selectedDates: Set<LocalDate>,
    today: LocalDate,
    onToggleDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Month header with navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPrevMonth) { Text("‹", color = FitLogAccent) }
            Text(
                text = "${month.year}年${month.monthValue}月",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onNextMonth) { Text("›", color = FitLogAccent) }
        }

        // Day-of-week header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = FitLogTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        // Day grid (weeks start on Monday, matching the weekday numbering)
        val firstOfMonth = month.atDay(1)
        val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
        val daysInMonth = month.lengthOfMonth()
        val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7

        for (cellIndex in 0 until totalCells) {
            if (cellIndex % 7 == 0) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val index = cellIndex + col
                        val date = firstOfMonth.minusDays((leadingBlanks - index).toLong())
                        if (index < leadingBlanks || index >= leadingBlanks + daysInMonth) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            )
                        } else {
                            DateCell(
                                date = date,
                                isSelected = date in selectedDates,
                                isToday = date == today,
                                isEnabled = !date.isBefore(today),
                                onClick = { onToggleDate(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .padding(2.dp)
            .clip(shape)
            .then(if (isEnabled) Modifier.clickable(onClick = onClick) else Modifier)
            .background(
                when {
                    isSelected -> FitLogAccent
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday && !isSelected) 1.dp else 0.dp,
                color = FitLogAccent,
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            textAlign = TextAlign.Center,
            color = when {
                !isEnabled -> FitLogTextTertiary.copy(alpha = 0.4f)
                isSelected -> Color.White
                isToday -> FitLogAccent
                else -> FitLogTextPrimary
            },
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
