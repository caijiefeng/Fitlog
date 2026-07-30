package com.example.fitlog.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.ScrollablePageContainer
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToBodyProfile: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.nav_profile))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        ScrollablePageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Profile card
            FitLogCard(onClick = onNavigateToBodyProfile) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (uiState.userName.isNotEmpty()) uiState.userName
                                   else stringResource(R.string.profile_no_name),
                            style = MaterialTheme.typography.titleMedium,
                            color = FitLogTextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.profile_setup_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogTextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.section_settings),
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Personal Info
            FitLogCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = onNavigateToBodyProfile,
            ) {
                Text(
                    text = stringResource(R.string.profile_personal_info),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                )
                Text(
                    text = stringResource(R.string.profile_personal_info_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }

            // Training Prefs
            FitLogCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("训练偏好功能即将推出")
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.profile_training_prefs),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                )
                Text(
                    text = stringResource(R.string.profile_training_prefs_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }

            // Appearance
            FitLogCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("外观设置功能即将推出")
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.profile_appearance),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                )
                Text(
                    text = stringResource(R.string.profile_appearance_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }

            // Data Management
            FitLogCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = onNavigateToDataManagement,
            ) {
                Text(
                    text = stringResource(R.string.profile_data_management),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                )
                Text(
                    text = stringResource(R.string.profile_data_management_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }

            // About
            FitLogCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("FitLog v0.1.0")
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.profile_about),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                )
                Text(
                    text = stringResource(R.string.profile_about_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }
        }
    }
}
