package com.example.fitlog.feature.record

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
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
fun RecordScreen(
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = "记录")
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "最近训练")

            EmptyState(
                icon = Icons.Filled.EditNote,
                title = "还没有训练记录",
                subtitle = "完成训练后，记录将在这里显示",
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = "身体数据")

            Text(
                text = "体重、体脂、围度等记录将在这里显示",
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextSecondary,
            )
        }
    }
}
