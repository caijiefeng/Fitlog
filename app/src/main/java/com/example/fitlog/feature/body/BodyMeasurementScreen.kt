package com.example.fitlog.feature.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.domain.body.BodyMeasurement
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BodyMeasurementScreen(
    viewModel: BodyMeasurementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.body_measurement_title))
        },
        containerColor = FitLogBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddForm() },
                containerColor = FitLogAccent,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_add),
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = FitLogAccent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                )
            }
            uiState.measurements.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.Biotech,
                    title = stringResource(R.string.body_measurement_empty_title),
                    subtitle = stringResource(R.string.body_measurement_empty_subtitle),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(title = stringResource(R.string.body_measurement_history))
                    }

                    items(uiState.measurements, key = { it.id }) { measurement ->
                        MeasurementCard(
                            measurement = measurement,
                            onEdit = { viewModel.showEditForm(measurement) },
                            onDelete = { viewModel.deleteMeasurement(measurement) },
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Measurement form dialog
    if (formState.isVisible) {
        MeasurementFormDialog(
            formState = formState,
            onDateChange = { viewModel.updateFormDate(it) },
            onWeightChange = { viewModel.updateFormWeight(it) },
            onBodyFatChange = { viewModel.updateFormBodyFat(it) },
            onMuscleChange = { viewModel.updateFormMuscle(it) },
            onWaistChange = { viewModel.updateFormWaist(it) },
            onNoteChange = { viewModel.updateFormNote(it) },
            onSave = { viewModel.saveMeasurement() },
            onDismiss = { viewModel.hideForm() },
        )
    }
}

@Composable
private fun MeasurementCard(
    measurement: BodyMeasurement,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    FitLogCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = measurement.date.format(formatter),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        measurement.weightKg?.let {
                            Text("${stringResource(R.string.body_measurement_weight)}: ${it} kg",
                                style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
                        }
                        measurement.bodyFatPercent?.let {
                            Text("${stringResource(R.string.body_measurement_bf)}: ${it}%",
                                style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        measurement.muscleKg?.let {
                            Text("${stringResource(R.string.body_measurement_muscle)}: ${it} kg",
                                style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
                        }
                        measurement.waistCm?.let {
                            Text("${stringResource(R.string.body_measurement_waist)}: ${it} cm",
                                style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
                        }
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = FitLogTextSecondary,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = FitLogError,
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementFormDialog(
    formState: BodyMeasurementFormState,
    onDateChange: (LocalDate) -> Unit,
    onWeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onMuscleChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val isEdit = formState.editId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) stringResource(R.string.body_measurement_edit)
                else stringResource(R.string.body_measurement_add)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = formState.date.format(formatter),
                    onValueChange = { input ->
                        try {
                            LocalDate.parse(input, formatter).let { onDateChange(it) }
                        } catch (_: Exception) { }
                    },
                    label = { Text(stringResource(R.string.body_measurement_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.weightKg,
                    onValueChange = onWeightChange,
                    label = { Text(stringResource(R.string.body_measurement_weight)) },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.bodyFatPercent,
                    onValueChange = onBodyFatChange,
                    label = { Text(stringResource(R.string.body_measurement_bf)) },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.muscleKg,
                    onValueChange = onMuscleChange,
                    label = { Text(stringResource(R.string.body_measurement_muscle)) },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.waistCm,
                    onValueChange = onWaistChange,
                    label = { Text(stringResource(R.string.body_measurement_waist)) },
                    suffix = { Text("cm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.body_measurement_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
            ) {
                Text(stringResource(R.string.action_save), color = FitLogTextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
