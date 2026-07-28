package com.example.fitlog.feature.exercise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.model.MuscleGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFormScreen(
    viewModel: ExerciseEditViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
    onCancelled: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseFormEvent.Saved -> onSaved()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isCreateMode) "新增动作" else if (state.isBuiltIn) "查看动作" else "编辑动作",
                        color = FitLogTextPrimary,
                    )
                },
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
                // Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("动作名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBuiltIn,
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    singleLine = true,
                    colors = fieldColors(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Primary muscle group
                MuscleGroupDropdown(
                    label = "主要肌群 *",
                    selected = state.primaryMuscleGroup,
                    enabled = !state.isBuiltIn,
                    onSelected = { viewModel.onPrimaryMuscleGroupChanged(it) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary muscle group
                MuscleGroupDropdown(
                    label = "次要肌群 (可选)",
                    selected = state.secondaryMuscleGroup,
                    enabled = !state.isBuiltIn,
                    onSelected = { viewModel.onSecondaryMuscleGroupChanged(it) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBuiltIn,
                    minLines = 2,
                    maxLines = 4,
                    colors = fieldColors(),
                )

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(state.error!!, color = FitLogTextSecondary)
                }

                if (!state.isBuiltIn) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.onSave() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(color = FitLogBackground, modifier = Modifier.height(20.dp))
                        } else {
                            Text(if (viewModel.isCreateMode) "保存" else "更新")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleGroupDropdown(
    label: String,
    selected: MuscleGroup?,
    enabled: Boolean,
    onSelected: (MuscleGroup?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selected?.let { muscleGroupLabel(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = fieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("—") },
                onClick = { onSelected(null); expanded = false },
            )
            MuscleGroup.entries.forEach { group ->
                DropdownMenuItem(
                    text = { Text(muscleGroupLabel(group)) },
                    onClick = { onSelected(group); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = FitLogTextPrimary,
    unfocusedTextColor = FitLogTextPrimary,
    focusedBorderColor = FitLogAccent,
    unfocusedBorderColor = FitLogTextSecondary.copy(alpha = 0.3f),
    focusedLabelColor = FitLogAccent,
    unfocusedLabelColor = FitLogTextSecondary,
    disabledTextColor = FitLogTextSecondary,
    disabledBorderColor = FitLogTextSecondary.copy(alpha = 0.15f),
)

private fun muscleGroupLabel(group: MuscleGroup): String = when (group) {
    MuscleGroup.CHEST -> "胸"
    MuscleGroup.BACK -> "背"
    MuscleGroup.SHOULDERS -> "肩"
    MuscleGroup.BICEPS -> "肱二头肌"
    MuscleGroup.TRICEPS -> "肱三头肌"
    MuscleGroup.FOREARMS -> "前臂"
    MuscleGroup.QUADRICEPS -> "股四头肌"
    MuscleGroup.HAMSTRINGS -> "腘绳肌"
    MuscleGroup.GLUTES -> "臀"
    MuscleGroup.CALVES -> "小腿"
    MuscleGroup.CORE -> "核心"
    MuscleGroup.CARDIO -> "有氧"
    MuscleGroup.FULL_BODY -> "全身"
}
