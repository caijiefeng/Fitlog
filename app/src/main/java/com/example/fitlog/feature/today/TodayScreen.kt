package com.example.fitlog.feature.today

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = "今日")
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Greeting
            Text(
                text = uiState.greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "准备开始今天的训练了吗？",
                style = MaterialTheme.typography.bodyLarge,
                color = FitLogTextSecondary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Today's Workout
            SectionHeader(title = "今日训练")

            if (uiState.hasWorkoutToday) {
                FitLogCard {
                    Text(
                        text = uiState.todayWorkoutName ?: "今日训练",
                        style = MaterialTheme.typography.titleMedium,
                        color = FitLogTextPrimary,
                    )
                }
            } else {
                EmptyState(
                    icon = Icons.Filled.FitnessCenter,
                    title = "今日暂无训练计划",
                    subtitle = "前往「计划」页面创建你的每周训练计划",
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            SectionHeader(title = "快速操作")

            FitLogCard(
                onClick = { viewModel.onQuickStart() },
            ) {
                Text(
                    text = "快速开始训练",
                    style = MaterialTheme.typography.titleMedium,
                    color = FitLogAccent,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "不基于计划，直接开始记录训练",
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }
        }
    }
}
