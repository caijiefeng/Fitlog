package com.example.fitlog.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.fitlog.R

enum class BottomNavItem(
    val route: String,
    @StringRes val labelResId: Int,
    val icon: ImageVector,
) {
    Today(
        route = "today",
        labelResId = R.string.nav_today,
        icon = Icons.Filled.CalendarMonth,
    ),
    Plan(
        route = "plan",
        labelResId = R.string.nav_plan,
        icon = Icons.Filled.DateRange,
    ),
    Record(
        route = "record",
        labelResId = R.string.nav_record,
        icon = Icons.Filled.EditNote,
    ),
    Progress(
        route = "progress",
        labelResId = R.string.nav_progress,
        icon = Icons.AutoMirrored.Filled.TrendingUp,
    ),
    Profile(
        route = "profile",
        labelResId = R.string.nav_profile,
        icon = Icons.Filled.Person,
    );

    companion object {
        val items: List<BottomNavItem> = entries.toList()
    }
}
