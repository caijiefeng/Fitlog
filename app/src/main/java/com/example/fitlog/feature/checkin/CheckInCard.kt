package com.example.fitlog.feature.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogAccentVariant
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary

private val moodEmojis = listOf(
    "😞", // 1 - very bad
    "😕", // 2 - bad
    "😐", // 3 - neutral
    "🙂", // 4 - good
    "😄", // 5 - great
)

@Composable
fun CheckInCard(
    viewModel: CheckInViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.section_daily_checkin))

        FitLogCard {
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

    // Show mood
    if (checkIn.mood != null) {
        MoodEnergyRow(
            label = stringResource(R.string.checkin_mood_label),
            value = moodEmojis.getOrNull(checkIn.mood - 1)?.toString() ?: checkIn.mood.toString(),
        )
    }

    // Show energy
    if (checkIn.energyLevel != null) {
        MoodEnergyRow(
            label = stringResource(R.string.checkin_energy_label),
            value = checkIn.energyLevel.toString(),
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
private fun MoodEnergyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextTertiary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = FitLogTextPrimary,
        )
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
    // Mood row: 5 emoji buttons
    Text(
        text = stringResource(R.string.checkin_mood_label),
        style = MaterialTheme.typography.bodyMedium,
        color = FitLogTextPrimary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (i in 1..5) {
            val isSelected = uiState.mood == i
            val emoji = moodEmojis.getOrNull(i - 1)?.toString() ?: i.toString()
            val emojiButtonModifier = Modifier.size(48.dp)
            if (isSelected) {
                Button(
                    onClick = { onMoodChange(i) },
                    modifier = emojiButtonModifier,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FitLogAccent,
                    ),
                ) {
                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                OutlinedButton(
                    onClick = { onMoodChange(i) },
                    modifier = emojiButtonModifier,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, FitLogDivider),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FitLogTextSecondary,
                    ),
                ) {
                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Energy row: 5 numbered buttons
    Text(
        text = stringResource(R.string.checkin_energy_label),
        style = MaterialTheme.typography.bodyMedium,
        color = FitLogTextPrimary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (i in 1..5) {
            val isSelected = uiState.energyLevel == i
            if (isSelected) {
                Button(
                    onClick = { onEnergyLevelChange(i) },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FitLogAccent,
                    ),
                ) {
                    Text(
                        text = i.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = FitLogTextPrimary,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onEnergyLevelChange(i) },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, FitLogDivider),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FitLogTextSecondary,
                    ),
                ) {
                    Text(
                        text = i.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = FitLogTextSecondary,
                    )
                }
            }
        }
    }

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
        colors = ButtonDefaults.buttonColors(
            containerColor = FitLogAccent,
            disabledContainerColor = FitLogAccentVariant.copy(alpha = 0.4f),
        ),
    ) {
        Text(
            text = if (uiState.isSaving) {
                stringResource(R.string.checkin_saving)
            } else {
                stringResource(R.string.checkin_save)
            },
            color = FitLogTextPrimary,
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
