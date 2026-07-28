package com.example.fitlog.feature.progress

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = "进度")
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "训练统计")

            EmptyState(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "还没有统计数据",
                subtitle = "坚持训练，这里将展示你的训练和身体变化趋势",
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = "身体变化")

            Text(
                text = "体重趋势、围度变化等图表将在这里显示",
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextSecondary,
            )
        }
    }
}
