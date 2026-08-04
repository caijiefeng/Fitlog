package com.example.fitlog.core.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

@Composable
fun FitLogBottomBar(
    currentRoute: String?,
    onNavigate: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = FitLogSurface,
        contentColor = FitLogTextPrimary,
    ) {
        BottomNavItem.items.forEach { item ->
            val selected = currentRoute == item.route
            val label = stringResource(item.labelResId)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = label,
                    )
                },
                label = {
                    Text(text = label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FitLogAccent,
                    selectedTextColor = FitLogAccent,
                    unselectedIconColor = FitLogTextSecondary,
                    unselectedTextColor = FitLogTextSecondary,
                    indicatorColor = FitLogAccentContainer,
                ),
            )
        }
    }
}
