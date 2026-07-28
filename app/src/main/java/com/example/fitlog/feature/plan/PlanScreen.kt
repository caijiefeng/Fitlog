package com.example.fitlog.feature.plan

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = "计划")
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "每周计划")

            EmptyState(
                icon = Icons.Filled.DateRange,
                title = "还没有训练计划",
                subtitle = "创建你的每周训练计划，让训练更有条理",
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = "训练日")

            Text(
                text = "周一到周日 训练安排将在这里显示",
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextSecondary,
            )
        }
    }
}
