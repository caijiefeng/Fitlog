package com.example.fitlog.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Today(
        route = "today",
        label = "今日",
        icon = Icons.Filled.CalendarMonth,
    ),
    Plan(
        route = "plan",
        label = "计划",
        icon = Icons.Filled.DateRange,
    ),
    Record(
        route = "record",
        label = "记录",
        icon = Icons.Filled.EditNote,
    ),
    Progress(
        route = "progress",
        label = "进度",
        icon = Icons.AutoMirrored.Filled.TrendingUp,
    ),
    Profile(
        route = "profile",
        label = "我的",
        icon = Icons.Filled.Person,
    );

    companion object {
        val items: List<BottomNavItem> = entries.toList()
    }
}
