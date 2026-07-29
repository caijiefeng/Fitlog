package com.example.fitlog.feature.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.GoalType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyProfileScreen(
    viewModel: BodyProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.body_profile_title))
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(modifier = Modifier.padding(innerPadding)) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FitLogAccent)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Gender dropdown
                FitLogCard {
                    Text(
                        text = stringResource(R.string.body_profile_gender),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GenderDropdown(
                        selected = uiState.gender,
                        onSelected = { viewModel.updateGender(it) },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Birthday
                FitLogCard {
                    Text(
                        text = stringResource(R.string.body_profile_birthday),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BirthdayPicker(
                        selectedDate = uiState.birthday,
                        onDateSelected = { viewModel.updateBirthday(it) },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Height
                FitLogCard {
                    OutlinedTextField(
                        value = uiState.heightCm,
                        onValueChange = { viewModel.updateHeightCm(it) },
                        label = { Text(stringResource(R.string.body_profile_height)) },
                        suffix = { Text("cm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Weight
                FitLogCard {
                    OutlinedTextField(
                        value = uiState.weightKg,
                        onValueChange = { viewModel.updateWeightKg(it) },
                        label = { Text(stringResource(R.string.body_profile_weight)) },
                        suffix = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Body Fat
                FitLogCard {
                    OutlinedTextField(
                        value = uiState.bodyFatPercent,
                        onValueChange = { viewModel.updateBodyFatPercent(it) },
                        label = { Text(stringResource(R.string.body_profile_body_fat)) },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Activity Level
                FitLogCard {
                    Text(
                        text = stringResource(R.string.body_profile_activity_level),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ActivityLevelDropdown(
                        selected = uiState.activityLevel,
                        onSelected = { viewModel.updateActivityLevel(it) },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Goal Type
                FitLogCard {
                    Text(
                        text = stringResource(R.string.body_profile_goal_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GoalTypeDropdown(
                        selected = uiState.goalType,
                        onSelected = { viewModel.updateGoalType(it) },
                    )
                }

                if (uiState.goalType == GoalType.FAT_LOSS) {
                    Spacer(modifier = Modifier.height(8.dp))

                    FitLogCard {
                        OutlinedTextField(
                            value = uiState.targetBodyFat,
                            onValueChange = { viewModel.updateTargetBodyFat(it) },
                            label = { Text(stringResource(R.string.body_profile_target_bf)) },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error message
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                // Save button
                Button(
                    onClick = { viewModel.save() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = FitLogTextPrimary,
                            modifier = Modifier.height(20.dp).width(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.action_save),
                            color = FitLogTextPrimary,
                        )
                    }
                }

                if (uiState.saved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.body_profile_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogAccent,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(
    selected: String,
    onSelected: (String) -> Unit,
) {
    val options = listOf("MALE", "FEMALE", "OTHER")
    val labels = listOf(
        stringResource(R.string.gender_male),
        stringResource(R.string.gender_female),
        stringResource(R.string.gender_other),
    )
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = labels.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(labels[index]) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityLevelDropdown(
    selected: ActivityLevel,
    onSelected: (ActivityLevel) -> Unit,
) {
    val labels = mapOf(
        ActivityLevel.SEDENTARY to stringResource(R.string.activity_sedentary),
        ActivityLevel.LIGHT to stringResource(R.string.activity_light),
        ActivityLevel.MODERATE to stringResource(R.string.activity_moderate),
        ActivityLevel.ACTIVE to stringResource(R.string.activity_active),
        ActivityLevel.VERY_ACTIVE to stringResource(R.string.activity_very_active),
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = labels[selected] ?: "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ActivityLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(labels[level] ?: level.name) },
                    onClick = {
                        onSelected(level)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalTypeDropdown(
    selected: GoalType,
    onSelected: (GoalType) -> Unit,
) {
    val labels = mapOf(
        GoalType.FAT_LOSS to stringResource(R.string.goal_fat_loss),
        GoalType.MAINTAIN to stringResource(R.string.goal_maintain),
        GoalType.LEAN_GAIN to stringResource(R.string.goal_lean_gain),
        GoalType.MUSCLE_GAIN to stringResource(R.string.goal_muscle_gain),
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = labels[selected] ?: "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            GoalType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(labels[type] ?: type.name) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BirthdayPicker(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    val defaultDate = selectedDate ?: LocalDate.of(2000, 1, 1)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var dateText by remember(selectedDate) {
        mutableStateOf(selectedDate?.format(formatter) ?: "2000-01-01")
    }

    Column {
        OutlinedTextField(
            value = dateText,
            onValueChange = { input ->
                dateText = input
                try {
                    val parsed = LocalDate.parse(input, formatter)
                    onDateSelected(parsed)
                } catch (_: Exception) { }
            },
            label = { Text(stringResource(R.string.body_profile_birthday_format)) },
            placeholder = { Text("2000-01-01") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.body_profile_birthday_hint),
            style = MaterialTheme.typography.labelSmall,
            color = FitLogTextSecondary,
        )
    }
}
