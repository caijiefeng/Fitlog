package com.example.fitlog

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.navigation.BottomNavItem
import com.example.fitlog.core.navigation.FitLogBottomBar
import com.example.fitlog.core.navigation.FitLogNavHost

private val topLevelRoutes = BottomNavItem.items.map { it.route }.toSet()

@Composable
fun FitLogApp() {
    FitLogTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute in topLevelRoutes

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    FitLogBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { item ->
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
            containerColor = FitLogBackground,
        ) { innerPadding ->
            FitLogNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
