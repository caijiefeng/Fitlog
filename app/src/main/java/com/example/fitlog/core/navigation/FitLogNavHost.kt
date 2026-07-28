package com.example.fitlog.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fitlog.feature.plan.PlanScreen
import com.example.fitlog.feature.profile.ProfileScreen
import com.example.fitlog.feature.progress.ProgressScreen
import com.example.fitlog.feature.record.RecordScreen
import com.example.fitlog.feature.today.TodayScreen

@Composable
fun FitLogNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Today.route,
        modifier = modifier,
    ) {
        composable(BottomNavItem.Today.route) {
            TodayScreen()
        }
        composable(BottomNavItem.Plan.route) {
            PlanScreen()
        }
        composable(BottomNavItem.Record.route) {
            RecordScreen()
        }
        composable(BottomNavItem.Progress.route) {
            ProgressScreen()
        }
        composable(BottomNavItem.Profile.route) {
            ProfileScreen()
        }
    }
}
