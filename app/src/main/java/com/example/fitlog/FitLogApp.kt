package com.example.fitlog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitlog.core.datastore.UserPreferencesRepository
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogAppTheme
import com.example.fitlog.core.navigation.BottomNavItem
import com.example.fitlog.core.navigation.FitLogBottomBar
import com.example.fitlog.core.navigation.FitLogNavHost
import androidx.compose.foundation.layout.padding
import com.example.fitlog.core.navigation.Routes
import com.example.fitlog.data.repository.UserProfileRepository

private val topLevelRoutes = BottomNavItem.items.map { it.route }.toSet()

@Composable
fun FitLogApp(
    preferencesRepository: UserPreferencesRepository,
    userProfileRepository: UserProfileRepository,
    openTodayCounter: Int = 0,
) {
    FitLogAppTheme(
        preferencesRepository = preferencesRepository,
        userProfileRepository = userProfileRepository,
    ) {
        val navController = rememberNavController()

        // Reminder notification deep link: navigate to the Today tab whenever
        // MainActivity receives a fitlog://reminder intent (click / actions).
        LaunchedEffect(openTodayCounter) {
            if (openTodayCounter > 0) {
                navController.navigate(BottomNavItem.Today.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

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
            floatingActionButton = {
                if (showBottomBar) {
                    FloatingActionButton(
                        onClick = { navController.navigate(Routes.camera(category = "GENERAL")) },
                        containerColor = FitLogAccent,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.fab_camera),
                        )
                    }
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
