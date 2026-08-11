package com.example.fitlog.feature.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.StarSceneBanner
import com.example.fitlog.core.designsystem.theme.FitLogOnAccent
import com.example.fitlog.core.designsystem.theme.FitLogType
import com.example.fitlog.core.designsystem.theme.StarScenePlacement
import com.example.fitlog.feature.calendar.CalendarScreen
import java.time.YearMonth

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
    onNavigateToExercises: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToSession: (Long) -> Unit = {},
    onNavigateToDayDetail: (Long) -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            StarSceneBanner(placement = StarScenePlacement.PLAN, height = 104.dp)
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text("训练计划", style = FitLogType.cardTitle, color = FitLogOnAccent)
                Text(
                    "${YearMonth.now().year} · ${YearMonth.now().month.name}",
                    style = FitLogType.caption,
                    color = FitLogOnAccent.copy(alpha = 0.78f),
                )
            }
        }
        PageContainer(modifier = Modifier.weight(1f)) {
            CalendarScreen(
                onNavigateToExercises = onNavigateToExercises,
                onNavigateToTemplates = onNavigateToTemplates,
                onNavigateToSession = onNavigateToSession,
                onNavigateToDayDetail = onNavigateToDayDetail,
                topBarExtra = {
                    IconButton(onClick = onNavigateToReminders) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.reminder_manage),
                        )
                    }
                },
            )
        }
    }
}
