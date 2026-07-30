package com.example.fitlog.feature.template

import androidx.compose.foundation.background
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
import java.time.Instant
import java.time.LocalDate
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

    // Schedule date picker dialog
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker && uiState.showScheduleDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.setScheduleDate(date)
                        }
                        showDatePicker = false
                    },
                ) { Text("确认", color = FitLogAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消", color = FitLogTextSecondary)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Schedule dialog
    if (uiState.showScheduleDialog && !showDatePicker) {
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
                        // Date
                        Text(
                            text = "选择日期",
                            style = MaterialTheme.typography.titleSmall,
                            color = FitLogTextPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val date = uiState.scheduleDate ?: LocalDate.now()
                        val dateStr = "${date.year}年${date.monthValue}月${date.dayOfMonth}日" +
                            " (${chineseDayOfWeek(date.dayOfWeek.value)})"

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = FitLogAccent,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = dateStr, color = FitLogTextPrimary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                            Column {
                                Text("每周重复", color = FitLogTextPrimary)
                                if (!uiState.isOneTime) {
                                    Spacer(modifier = Modifier.height(4.dp))
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
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSchedule() },
                    enabled = !uiState.isScheduling,
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
