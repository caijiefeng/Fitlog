package com.example.fitlog.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.fitlog.core.designsystem.component.StarHero
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogOnAccent
import com.example.fitlog.core.designsystem.theme.LocalStarVisualProfile
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
    val profile = LocalStarVisualProfile.current
    var showNameDialog by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        topBar = { FitLogTopAppBar(title = stringResource(R.string.nav_profile)) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        ScrollablePageContainer(modifier = Modifier.padding(innerPadding), horizontalPadding = 0.dp) {
            StarHero(
                backgroundRes = profile.profileBackgroundRes,
                minHeight = 540.dp,
                backgroundContentScale = ContentScale.Fit,
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AvatarImage(
                        avatarType = uiState.avatarType,
                        avatarKey = uiState.avatarKey,
                        customAvatarPath = uiState.customAvatarPath,
                        contentDescription = stringResource(R.string.profile_avatar_cd),
                        modifier = Modifier.size(104.dp).clip(CircleShape)
                            .border(3.dp, FitLogAccent, CircleShape).clickable(onClick = onNavigateToAvatarPicker),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(if (uiState.userName.isNotEmpty()) uiState.userName else stringResource(R.string.profile_no_name), style = com.example.fitlog.core.designsystem.theme.FitLogType.cardTitle, color = FitLogOnAccent)
                    Text(uiState.goalLabel ?: stringResource(R.string.profile_setup_hint), style = com.example.fitlog.core.designsystem.theme.FitLogType.caption, color = FitLogOnAccent.copy(alpha = 0.8f))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onNavigateToAvatarPicker, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.profile_avatar_edit), color = FitLogAccent) }
                Spacer(Modifier.height(24.dp))
                SettingsGroup(title = "个人") {
                    SettingsRow(
                        icon = Icons.Filled.Badge,
                        title = stringResource(R.string.profile_display_name),
                        subtitle = uiState.userName.ifBlank { stringResource(R.string.profile_no_name) },
                        onClick = { showNameDialog = true },
                    )
                    SettingsRow(Icons.Filled.AccountCircle, stringResource(R.string.profile_personal_info), stringResource(R.string.profile_personal_info_desc), onNavigateToBodyProfile)
                    SettingsRow(Icons.Filled.FitnessCenter, stringResource(R.string.profile_training_prefs), stringResource(R.string.profile_training_prefs_desc), null)
                }
                Spacer(Modifier.height(24.dp))
                SettingsGroup(title = "应用") {
                    SettingsRow(Icons.Filled.Palette, stringResource(R.string.profile_appearance), stringResource(R.string.profile_appearance_desc), null)
                    ThemeModeSelector(currentMode = uiState.themeMode, onModeSelected = viewModel::setThemeMode)
                    SettingsRow(Icons.Filled.PhotoLibrary, stringResource(R.string.record_entry_media), stringResource(R.string.media_library_title), onNavigateToMedia)
                    SettingsRow(Icons.Filled.Storage, stringResource(R.string.profile_data_management), stringResource(R.string.profile_data_management_desc), onNavigateToDataManagement)
                    SettingsRow(Icons.Filled.Info, stringResource(R.string.profile_about), stringResource(R.string.profile_about_desc), onNavigateToAbout)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showNameDialog) {
        DisplayNameDialog(
            initialValue = uiState.userName,
            onDismiss = { showNameDialog = false },
            onSave = {
                viewModel.updateDisplayName(it)
                showNameDialog = false
            },
        )
    }
}

@Composable
private fun DisplayNameDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by androidx.compose.runtime.saveable.rememberSaveable(initialValue) {
        androidx.compose.runtime.mutableStateOf(initialValue)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_display_name)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(24) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.profile_display_name)) },
                placeholder = { Text(stringResource(R.string.profile_display_name_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) {
                Text(stringResource(R.string.action_save), color = FitLogAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = FitLogTextSecondary)
            }
        },
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(title, style = com.example.fitlog.core.designsystem.theme.FitLogType.cardTitle, color = FitLogTextPrimary)
    Spacer(Modifier.height(8.dp))
    Surface(shape = RoundedCornerShape(16.dp), color = FitLogSurfaceVariant) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = FitLogAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = FitLogTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
        }
        if (onClick != null) Text("›", style = MaterialTheme.typography.titleLarge, color = FitLogTextSecondary)
    }
    HorizontalDivider(color = FitLogDivider.copy(alpha = 0.5f))
}

@Composable
private fun LegacyProfileScreen(
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
