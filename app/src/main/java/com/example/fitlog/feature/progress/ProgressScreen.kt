package com.example.fitlog.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.ScrollablePageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.StarScenePlacement

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.nav_progress))
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        ScrollablePageContainer(
            modifier = Modifier.padding(innerPadding),
            scenePlacement = StarScenePlacement.PROGRESS,
            sceneAlpha = 0.22f,
        ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── 顶部三个主要指标 ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    com.example.fitlog.core.designsystem.component.MetricCard(
                        label = stringResource(R.string.progress_metric_weight),
                        value = uiState.currentWeightKg?.let { "%.1f".format(it) } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    com.example.fitlog.core.designsystem.component.MetricCard(
                        label = stringResource(R.string.progress_metric_month_workouts),
                        value = "${uiState.monthWorkoutCount}",
                        modifier = Modifier.weight(1f),
                    )
                    com.example.fitlog.core.designsystem.component.MetricCard(
                        label = stringResource(R.string.progress_metric_month_volume),
                        value = if (uiState.monthVolumeKg >= 1000) {
                            "%.1f".format(uiState.monthVolumeKg / 1000)
                        } else {
                            "%.0f".format(uiState.monthVolumeKg)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Training statistics ──────────────────────────────────
                SectionHeader(title = stringResource(R.string.section_training_stats))

                if (uiState.isLoaded) {
                    StatsCard(
                        currentStreak = uiState.currentStreak,
                        bestStreak = uiState.bestStreak,
                        adherenceRate = uiState.adherenceRate,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Body trend charts ────────────────────────────────────
                SectionHeader(title = stringResource(R.string.section_body_changes))

                Spacer(modifier = Modifier.height(8.dp))

                TrendChartsContent(
                    points = uiState.trendPoints,
                    selectedRange = uiState.selectedRange,
                    isLoading = uiState.isTrendLoading,
                    onRangeChange = { viewModel.setTrendRange(it) },
                )

                Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatsCard(
    currentStreak: Int,
    bestStreak: Int,
    adherenceRate: Double,
) {
    FitLogCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                label = stringResource(R.string.stats_current_streak),
                value = stringResource(R.string.stats_streak_days, currentStreak),
                modifier = Modifier.weight(1f),
            )
            StatItem(
                label = stringResource(R.string.stats_best_streak),
                value = stringResource(R.string.stats_streak_days, bestStreak),
                modifier = Modifier.weight(1f),
            )
            StatItem(
                label = stringResource(R.string.stats_adherence),
                value = stringResource(R.string.stats_adherence_percent, (adherenceRate * 100).toInt()),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = FitLogAccent,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
        )
    }
}
