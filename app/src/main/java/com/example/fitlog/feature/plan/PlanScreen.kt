package com.example.fitlog.feature.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.data.repository.DaySchedule

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
    onNavigateToExercises: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { FitLogTopAppBar(title = "计划") },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(modifier = Modifier.padding(innerPadding)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Quick links
            Row {
                TextButton(onClick = onNavigateToExercises) {
                    Text("动作库 →", color = FitLogAccent)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Weekly schedule
            SectionHeader(title = "每周计划")

            if (uiState.weekSchedule.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = "还没有训练计划",
                    subtitle = "创建训练模板后，安排到每周的训练日",
                )
            } else {
                uiState.weekSchedule.forEach { day ->
                    DayScheduleRow(day)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayScheduleRow(day: DaySchedule) {
    FitLogCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                day.dayName,
                style = MaterialTheme.typography.titleSmall,
                color = FitLogAccent,
                modifier = Modifier.width(40.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (day.templateName != null) {
                    Text(day.templateName, style = MaterialTheme.typography.bodyMedium, color = FitLogTextPrimary)
                    Text("${day.exerciseCount} 个动作", style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
                } else {
                    Text("休息日", style = MaterialTheme.typography.bodyMedium, color = FitLogTextSecondary)
                }
            }
        }
    }
}
