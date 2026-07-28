package com.example.fitlog.feature.template

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    viewModel: TemplateEditViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
    onCancelled: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplateEditEvent.Saved -> onSaved()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isCreateMode) "新建模板" else "编辑模板", color = FitLogTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onCancelled) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = FitLogTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FitLogSurface),
            )
        },
        containerColor = FitLogBackground,
    ) { padding ->
        if (!state.isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FitLogAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("模板名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    singleLine = true,
                    colors = fieldColors(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    colors = fieldColors(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Add exercise
                ExerciseAdder(
                    availableExercises = state.availableExercises,
                    existingIds = state.exercises.map { it.exerciseId }.toSet(),
                    onAdd = { viewModel.onAddExercise(it) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Exercise list
                state.exercises.forEachIndexed { index, item ->
                    ExerciseConfigCard(
                        index = index,
                        item = item,
                        onFieldChanged = { field, value -> viewModel.onExerciseFieldChanged(index, field, value) },
                        onRemove = { viewModel.onRemoveExercise(index) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (state.error != null) {
                    Text(state.error!!, color = FitLogTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { viewModel.onSave() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = FitLogBackground, modifier = Modifier.height(20.dp))
                    } else {
                        Text("保存模板")
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseAdder(
    availableExercises: List<com.example.fitlog.core.model.Exercise>,
    existingIds: Set<Long>,
    onAdd: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = availableExercises.filter { it.id !in existingIds }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            label = { Text("添加动作") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = fieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("所有动作已添加") }, onClick = { expanded = false })
            } else {
                filtered.forEach { ex ->
                    DropdownMenuItem(
                        text = { Text(ex.name) },
                        onClick = { onAdd(ex.id); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseConfigCard(
    index: Int,
    item: TemplateExerciseItem,
    onFieldChanged: (String, String) -> Unit,
    onRemove: () -> Unit,
) {
    FitLogCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${index + 1}. ${item.exerciseName}", style = MaterialTheme.typography.titleSmall, color = FitLogTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    NumberField("组", item.targetSets, 60.dp) { onFieldChanged("targetSets", it) }
                    Spacer(modifier = Modifier.width(4.dp))
                    NumberField("min次", item.targetRepsMin, 60.dp) { onFieldChanged("targetRepsMin", it) }
                    Spacer(modifier = Modifier.width(4.dp))
                    NumberField("max次", item.targetRepsMax, 60.dp) { onFieldChanged("targetRepsMax", it) }
                    Spacer(modifier = Modifier.width(4.dp))
                    NumberField("休息s", item.restSeconds, 60.dp) { onFieldChanged("restSeconds", it) }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, "删除", tint = FitLogTextSecondary)
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, width: androidx.compose.ui.unit.Dp, onChanged: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChanged,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.width(width),
        singleLine = true,
        colors = fieldColors(),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = FitLogTextPrimary,
    unfocusedTextColor = FitLogTextPrimary,
    focusedBorderColor = FitLogAccent,
    unfocusedBorderColor = FitLogTextSecondary.copy(alpha = 0.3f),
    focusedLabelColor = FitLogAccent,
    unfocusedLabelColor = FitLogTextSecondary,
)
