package com.example.fitlog.feature.plan

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.StarScenePlacement
import com.example.fitlog.feature.calendar.CalendarScreen

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
    onNavigateToExercises: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToSession: (Long) -> Unit = {},
    onNavigateToDayDetail: (Long) -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
) {
    PageContainer(
        scenePlacement = StarScenePlacement.PLAN,
        sceneAlpha = 0.22f,
    ) {
        CalendarScreen(
            onNavigateToExercises = onNavigateToExercises,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToSession = onNavigateToSession,
            onNavigateToDayDetail = onNavigateToDayDetail,
            topBarExtra = {
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onNavigateToReminders) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        modifier = Modifier.width(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.reminder_manage),
                        color = FitLogAccent,
                    )
                }
            },
        )
    }
}
