package com.example.fitlog.feature.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.core.model.WorkoutStatus
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun RecordScreen(
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { FitLogTopAppBar(title = stringResource(R.string.nav_record)) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(modifier = Modifier.padding(innerPadding)) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = stringResource(R.string.section_recent_training))

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = FitLogAccent, modifier = Modifier.padding(32.dp))
                }
                uiState.isEmpty -> {
                    EmptyState(
                        icon = Icons.Filled.EditNote,
                        title = stringResource(R.string.empty_record_title),
                        subtitle = stringResource(R.string.empty_record_subtitle),
                    )
                }
                else -> {
                    LazyColumn {
                        items(uiState.sessions, key = { it.id }) { session ->
                            HistoryCard(session)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(title = stringResource(R.string.section_body_data))
            Text(stringResource(R.string.record_placeholder), style = MaterialTheme.typography.bodyMedium, color = FitLogTextSecondary)
        }
    }
}

@Composable
private fun HistoryCard(session: WorkoutSession) {
    FitLogCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.templateNameSnapshot ?: "训练", style = MaterialTheme.typography.bodyLarge, color = FitLogTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(formatDate(session), style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(statusLabel(session.status), style = MaterialTheme.typography.bodySmall, color = FitLogAccent)
                }
            }
        }
    }
}

private fun formatDate(session: WorkoutSession): String {
    val zdt = session.startTime.atZone(ZoneId.systemDefault())
    return zdt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
}

private fun statusLabel(status: WorkoutStatus): String = when (status) {
    WorkoutStatus.COMPLETED -> "已完成"
    WorkoutStatus.PARTIALLY_COMPLETED -> "部分完成"
    WorkoutStatus.CANCELLED -> "已取消"
    else -> ""
}
