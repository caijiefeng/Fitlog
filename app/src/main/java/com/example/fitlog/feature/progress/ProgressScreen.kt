package com.example.fitlog.feature.progress

import androidx.compose.foundation.background
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
import com.example.fitlog.core.designsystem.component.FitLogCardStyle
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.ScrollablePageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import java.time.DayOfWeek
import java.time.LocalDate

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
        ScrollablePageContainer(modifier = Modifier.padding(innerPadding)) {
                Spacer(modifier = Modifier.height(8.dp))

                MonthHighlight(
                    workouts = uiState.monthWorkoutCount,
                    weight = uiState.currentWeightKg,
                    volume = uiState.monthVolumeKg,
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader(title = "训练轨迹")
                TrainingHeatmap(trainingDates = uiState.trainingDates)
                Spacer(modifier = Modifier.height(24.dp))

                // ── Training statistics ──────────────────────────────────
                SectionHeader(title = stringResource(R.string.section_training_stats))

                if (uiState.isLoaded) {
                    StreakHighlight(
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
private fun MonthHighlight(
    workouts: Int,
    weight: Double?,
    volume: Double,
) {
    FitLogCard(style = FitLogCardStyle.HERO) {
        Text("本月训练", style = MaterialTheme.typography.bodyMedium, color = FitLogTextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$workouts", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = FitLogAccent)
            Spacer(Modifier.size(6.dp))
            Text("次", style = MaterialTheme.typography.titleMedium, color = FitLogTextSecondary)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniKpi("当前体重", weight?.let { "%.1f kg".format(it) } ?: "—", Modifier.weight(1f))
            MiniKpi("本月容量", if (volume >= 1000) "%.1f t".format(volume / 1000) else "%.0f kg".format(volume), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniKpi(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = FitLogTextSecondary)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FitLogTextPrimary)
    }
}

@Composable
private fun TrainingHeatmap(trainingDates: Set<LocalDate>) {
    val today = LocalDate.now()
    val start = today.minusDays(89)
    FitLogCard(style = FitLogCardStyle.TONAL) {
        Text("最近 90 天", style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..12).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..6).forEach { day ->
                        val date = start.plusDays((week * 7L) + day)
                        val completed = date in trainingDates
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    when {
                                        completed && date.dayOfWeek == DayOfWeek.SUNDAY -> FitLogSuccess
                                        completed -> FitLogAccent
                                        else -> FitLogSurfaceVariant
                                    },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("训练日以球星主题色标记", style = MaterialTheme.typography.labelSmall, color = FitLogTextTertiary)
    }
}

@Composable
private fun StreakHighlight(
    currentStreak: Int,
    bestStreak: Int,
    adherenceRate: Double,
) {
    FitLogCard(style = FitLogCardStyle.TONAL) {
        Text("🔥 ${stringResource(R.string.stats_current_streak)}", style = MaterialTheme.typography.bodyMedium, color = FitLogTextSecondary)
        Text(stringResource(R.string.stats_streak_days, currentStreak), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = FitLogAccent)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(stringResource(R.string.stats_best_streak), stringResource(R.string.stats_streak_days, bestStreak), Modifier.weight(1f))
            StatItem(stringResource(R.string.stats_adherence), stringResource(R.string.stats_adherence_percent, (adherenceRate * 100).toInt()), Modifier.weight(1f))
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
