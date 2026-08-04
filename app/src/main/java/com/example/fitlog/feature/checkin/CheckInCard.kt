package com.example.fitlog.feature.checkin

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.component.StarCardEmphasis
import com.example.fitlog.core.designsystem.component.StarThemedCard
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccentVariant
import com.example.fitlog.core.designsystem.theme.FitLogAccentVariantContainer
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogOnAccent
import com.example.fitlog.core.designsystem.theme.StarAccentRole
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary

/**
 * One mood/energy rating choice: a value 1-5, its emoji and a Chinese label.
 */
internal data class CheckInRatingOption(
    val value: Int,
    val emoji: String,
    @StringRes val labelRes: Int,
)

private val moodOptions = listOf(
    CheckInRatingOption(1, "😞", R.string.checkin_mood_1), // 很差
    CheckInRatingOption(2, "🙁", R.string.checkin_mood_2), // 较差
    CheckInRatingOption(3, "😐", R.string.checkin_mood_3), // 一般
    CheckInRatingOption(4, "🙂", R.string.checkin_mood_4), // 不错
    CheckInRatingOption(5, "😄", R.string.checkin_mood_5), // 很好
)

private val energyOptions = listOf(
    CheckInRatingOption(1, "🪫", R.string.checkin_energy_1), // 很低
    CheckInRatingOption(2, "😴", R.string.checkin_energy_2), // 偏低
    CheckInRatingOption(3, "😌", R.string.checkin_energy_3), // 一般
    CheckInRatingOption(4, "⚡", R.string.checkin_energy_4), // 充沛
    CheckInRatingOption(5, "🔥", R.string.checkin_energy_5), // 极佳
)

@Composable
fun CheckInCard(
    viewModel: CheckInViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.section_daily_checkin))

        StarThemedCard(emphasis = StarCardEmphasis.PROMINENT) {
            if (uiState.existingCheckIn != null && uiState.isSaved && !uiState.isEditing) {
                // Already checked in — show summary with edit button
                ExistingCheckInContent(
                    uiState = uiState,
                    onEdit = viewModel::startEditing,
                )
            } else {
                // Show the form (for new check-in or editing existing)
                CheckInFormContent(
                    uiState = uiState,
                    onMoodChange = viewModel::onMoodChange,
                    onEnergyLevelChange = viewModel::onEnergyLevelChange,
                    onNotesChange = viewModel::onNotesChange,
                    onSave = viewModel::saveCheckIn,
                    onCancel = if (uiState.existingCheckIn != null) viewModel::cancelEditing else null,
                )
            }
        }
    }
}

@Composable
private fun ExistingCheckInContent(
    uiState: CheckInUiState,
    onEdit: () -> Unit,
) {
    val checkIn = uiState.existingCheckIn ?: return

    Text(
        text = stringResource(R.string.checkin_already_logged),
        style = MaterialTheme.typography.bodyMedium,
        color = FitLogSuccess,
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Show mood (emoji + label)
    if (checkIn.mood != null) {
        MoodEnergyRow(
            label = stringResource(R.string.checkin_mood_label),
            option = moodOptions.firstOrNull { it.value == checkIn.mood },
        )
    }

    // Show energy (emoji + label)
    if (checkIn.energyLevel != null) {
        MoodEnergyRow(
            label = stringResource(R.string.checkin_energy_label),
            option = energyOptions.firstOrNull { it.value == checkIn.energyLevel },
        )
    }

    // Show notes if present
    if (!checkIn.notes.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = checkIn.notes,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
        )
    }

    // Edit button
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, FitLogAccent),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = stringResource(R.string.checkin_edit),
            color = FitLogAccent,
        )
    }
}

@Composable
private fun MoodEnergyRow(
    label: String,
    option: CheckInRatingOption?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextTertiary,
        )
        if (option != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option.emoji,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(option.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitLogTextPrimary,
                )
            }
        }
    }
}

@Composable
private fun CheckInFormContent(
    uiState: CheckInUiState,
    onMoodChange: (Int) -> Unit,
    onEnergyLevelChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    // Mood row: 5 emoji + label buttons（心情使用主色容器）
    RatingSelectorRow(
        title = stringResource(R.string.checkin_mood_label),
        options = moodOptions,
        selectedValue = uiState.mood,
        contentDescriptionFormat = R.string.checkin_mood_option_cd,
        accentRole = StarAccentRole.PRIMARY,
        onValueChange = onMoodChange,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Energy row: 5 emoji + label buttons（精力使用辅助色容器）
    RatingSelectorRow(
        title = stringResource(R.string.checkin_energy_label),
        options = energyOptions,
        selectedValue = uiState.energyLevel,
        contentDescriptionFormat = R.string.checkin_energy_option_cd,
        accentRole = StarAccentRole.SECONDARY,
        onValueChange = onEnergyLevelChange,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Notes field
    OutlinedTextField(
        value = uiState.notes,
        onValueChange = onNotesChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                stringResource(R.string.checkin_notes_placeholder),
                color = FitLogTextTertiary,
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FitLogTextPrimary,
            unfocusedTextColor = FitLogTextPrimary,
            cursorColor = FitLogAccent,
            focusedBorderColor = FitLogAccent,
            unfocusedBorderColor = FitLogDivider,
        ),
        shape = RoundedCornerShape(8.dp),
        minLines = 2,
        maxLines = 4,
    )

    // Error
    uiState.error?.let { error ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogError,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Save button
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = !uiState.isSaving && (uiState.mood != null || uiState.energyLevel != null || uiState.notes.isNotBlank()),
        shape = RoundedCornerShape(8.dp),
        colors = checkInSaveButtonColors(),
    ) {
        Text(
            text = if (uiState.isSaving) {
                stringResource(R.string.checkin_saving)
            } else {
                stringResource(R.string.checkin_save)
            },
        )
    }

    // Cancel button (only shown when editing an existing check-in)
    if (onCancel != null) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, FitLogDivider),
            enabled = !uiState.isSaving,
        ) {
            Text(
                text = stringResource(R.string.action_cancel),
                color = FitLogTextSecondary,
            )
        }
    }
}

/**
 * A row of 5 rating options. Each option is a 48dp-tall button showing the
 * emoji above its label; the selected one gets an accent border and tint.
 */
@Composable
private fun RatingSelectorRow(
    title: String,
    options: List<CheckInRatingOption>,
    selectedValue: Int?,
    @StringRes contentDescriptionFormat: Int,
    accentRole: StarAccentRole,
    onValueChange: (Int) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = FitLogTextPrimary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            RatingOptionButton(
                option = option,
                isSelected = selectedValue == option.value,
                contentDescriptionFormat = contentDescriptionFormat,
                accentRole = accentRole,
                onClick = { onValueChange(option.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RatingOptionButton(
    option: CheckInRatingOption,
    isSelected: Boolean,
    @StringRes contentDescriptionFormat: Int,
    accentRole: StarAccentRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(option.labelRes)
    val description = buildString {
        append(stringResource(contentDescriptionFormat, label))
        if (isSelected) {
            append(stringResource(R.string.checkin_option_selected_suffix))
        }
    }

    val selectedContainer = when (accentRole) {
        StarAccentRole.PRIMARY -> FitLogAccentContainer
        StarAccentRole.SECONDARY -> FitLogAccentVariantContainer
    }
    val selectedText = when (accentRole) {
        StarAccentRole.PRIMARY -> FitLogAccent
        StarAccentRole.SECONDARY -> FitLogAccentVariant
    }
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) selectedText else FitLogDivider,
                shape = RoundedCornerShape(10.dp),
            )
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedContainer else FitLogSurfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = option.emoji,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) selectedText else FitLogTextSecondary,
            )
        }
    }
}

/**
 * 打卡保存按钮配色：主色背景 + onAccent 文字。
 * 独立函数便于测试断言（containerColor/contentColor 跟随主题）。
 */
@Composable
internal fun checkInSaveButtonColors(): androidx.compose.material3.ButtonColors =
    ButtonDefaults.buttonColors(
        containerColor = FitLogAccent,
        contentColor = FitLogOnAccent,
        disabledContainerColor = FitLogAccentContainer,
        disabledContentColor = FitLogTextTertiary,
    )
