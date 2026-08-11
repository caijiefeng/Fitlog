package com.example.fitlog.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.fitlog.core.designsystem.component.ScrollablePageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.component.StarSceneBanner
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogOnAccent
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import com.example.fitlog.core.designsystem.theme.StarScenePlacement
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { FitLogTopAppBar(title = stringResource(R.string.nav_progress)) },
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
            SectionHeader(title = stringResource(R.string.section_training_stats))
            if (uiState.isLoaded) {
                StreakHighlight(
                    currentStreak = uiState.currentStreak,
                    bestStreak = uiState.bestStreak,
                    adherenceRate = uiState.adherenceRate,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(title = stringResource(R.string.section_body_changes))
            Spacer(modifier = Modifier.height(8.dp))
            TrendChartsContent(
                points = uiState.trendPoints,
                selectedRange = uiState.selectedRange,
                isLoading = uiState.isTrendLoading,
                onRangeChange = viewModel::setTrendRange,
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
    Box(modifier = Modifier.fillMaxWidth()) {
        StarSceneBanner(placement = StarScenePlacement.PROGRESS, height = 224.dp)
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = "本月训练",
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogOnAccent.copy(alpha = 0.84f),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$workouts",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = FitLogOnAccent,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "次",
                    style = MaterialTheme.typography.titleMedium,
                    color = FitLogOnAccent.copy(alpha = 0.86f),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "当前体重",
                style = MaterialTheme.typography.labelMedium,
                color = FitLogOnAccent.copy(alpha = 0.78f),
            )
            Text(
                text = weight?.let { "%.1f kg".format(it) } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FitLogOnAccent,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "本月容量",
                style = MaterialTheme.typography.labelMedium,
                color = FitLogOnAccent.copy(alpha = 0.78f),
            )
            Text(
                text = if (volume >= 1000) "%.1f t".format(volume / 1000) else "%.0f kg".format(volume),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FitLogOnAccent,
            )
        }
    }
}

@Composable
private fun TrainingHeatmap(trainingDates: Set<LocalDate>) {
    val currentWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY)
    val start = currentWeekMonday.minusWeeks(12)
    FitLogCard(style = FitLogCardStyle.TONAL) {
        Text("最近 13 周", style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                (0..6).forEach { day ->
                    Text(
                        text = when (day) {
                            0 -> "M"
                            2 -> "W"
                            4 -> "F"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = FitLogTextTertiary,
                        modifier = Modifier.height(13.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                (0..6).forEach { day ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        (0..12).forEach { week ->
                            val date = start.plusWeeks(week.toLong()).plusDays(day.toLong())
                            val completed = date in trainingDates
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .background(
                                        color = when {
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
