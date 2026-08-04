package com.example.fitlog.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.StarCardEmphasis
import com.example.fitlog.core.designsystem.component.StarThemedCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.ScrollablePageContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.ThemeMode
import com.example.fitlog.feature.avatar.AvatarImage

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToBodyProfile: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToMedia: () -> Unit = {},
    onNavigateToAvatarPicker: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.nav_profile))
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        ScrollablePageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── 个人 Hero（球星主题卡片）──────────────────────────────────
            StarThemedCard(
                emphasis = StarCardEmphasis.PROMINENT,
            ) {
                Text(
                    text = if (uiState.userName.isNotEmpty()) {
                        uiState.userName
                    } else {
                        stringResource(R.string.profile_no_name)
                    },
                    style = com.example.fitlog.core.designsystem.theme.FitLogType.cardTitle,
                    color = FitLogAccent,
                )
                Text(
                    text = uiState.goalLabel
                        ?.let { stringResource(R.string.profile_hero_goal, it) }
                        ?: stringResource(R.string.profile_setup_hint),
                    style = com.example.fitlog.core.designsystem.theme.FitLogType.caption,
                    color = FitLogTextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarImage(
                        avatarType = uiState.avatarType,
                        avatarKey = uiState.avatarKey,
                        customAvatarPath = uiState.customAvatarPath,
                        contentDescription = stringResource(R.string.profile_avatar_cd),
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(2.dp, FitLogAccent.copy(alpha = 0.4f), CircleShape)
                            .clickable(onClick = onNavigateToAvatarPicker),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        com.example.fitlog.core.designsystem.component.MetricRow(
                            label = stringResource(R.string.profile_hero_height),
                            value = uiState.heightCm?.let { "%.0f cm".format(it) } ?: "—",
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        com.example.fitlog.core.designsystem.component.MetricRow(
                            label = stringResource(R.string.profile_hero_weight),
                            value = uiState.latestWeightKg?.let { "%.1f kg".format(it) } ?: "—",
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onNavigateToAvatarPicker,
                            modifier = Modifier.heightIn(min = 36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, FitLogAccent),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.profile_avatar_edit),
                                style = MaterialTheme.typography.labelMedium,
                                color = FitLogAccent,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 分组设置标题 ─────────────────────────────────────────────
            Text(
                text = stringResource(R.string.section_settings),
                style = com.example.fitlog.core.designsystem.theme.FitLogType.cardTitle,
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
                    // TODO: Implement training preferences screen
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

            // Appearance (theme selector)
            FitLogCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = null,
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
                Spacer(modifier = Modifier.height(12.dp))
                ThemeModeSelector(
                    currentMode = uiState.themeMode,
                    onModeSelected = { viewModel.setThemeMode(it) },
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

            // Media Library
            FitLogCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = onNavigateToMedia,
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = FitLogAccent,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.record_entry_media),
                            style = MaterialTheme.typography.bodyLarge,
                            color = FitLogTextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.media_library_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogTextSecondary,
                        )
                    }
                }
            }

            // About
            FitLogCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = {
                    // TODO: Show about dialog
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

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            val label = when (mode) {
                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clickable { onModeSelected(mode) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) FitLogAccent else FitLogSurfaceVariant,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) FitLogBackground else FitLogTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
