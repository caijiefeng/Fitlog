package com.example.fitlog.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogCard as FitLogCardColor
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.data.repository.FoodRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    val mealTypes = listOf(
        "BREAKFAST" to stringResource(R.string.meal_breakfast),
        "LUNCH" to stringResource(R.string.meal_lunch),
        "DINNER" to stringResource(R.string.meal_dinner),
        "SNACK" to stringResource(R.string.meal_snack),
    )

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.nutrition_title))
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
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = FitLogAccent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                // Daily Summary Card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    DailySummaryCard(summary = uiState.summary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Advice card
                uiState.advice?.let { advice ->
                    item {
                        if (advice.dailyTargetText.isNotBlank()) {
                            FitLogCard {
                                Text(
                                    text = advice.dailyTargetText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FitLogTextSecondary,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Meal type tabs
                item {
                    MealTypeTabs(
                        selectedType = uiState.mealTypeFilter,
                        mealTypes = mealTypes,
                        onTabSelected = { type ->
                            viewModel.setMealTypeFilter(type)
                        },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Food records
                if (uiState.foodRecords.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Restaurant,
                            title = stringResource(R.string.nutrition_empty_title),
                            subtitle = stringResource(R.string.nutrition_empty_subtitle),
                        )
                    }
                } else {
                    items(uiState.foodRecords, key = { it.id }) { record ->
                        FoodRecordCard(
                            record = record,
                            onEdit = { viewModel.showEditForm(record) },
                            onDelete = { viewModel.deleteFoodRecord(record) },
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Food form dialog
    if (formState.isVisible) {
        FoodFormDialog(
            formState = formState,
            mealTypes = mealTypes,
            onFoodNameChange = { viewModel.updateFormFoodName(it) },
            onMealTypeChange = { viewModel.updateFormMealType(it) },
            onCaloriesChange = { viewModel.updateFormCalories(it) },
            onProteinChange = { viewModel.updateFormProtein(it) },
            onCarbsChange = { viewModel.updateFormCarbs(it) },
            onFatChange = { viewModel.updateFormFat(it) },
            onAmountChange = { viewModel.updateFormAmount(it) },
            onNoteChange = { viewModel.updateFormNote(it) },
            onSave = { viewModel.saveFoodRecord() },
            onDismiss = { viewModel.hideForm() },
        )
    }
}

@Composable
private fun DailySummaryCard(
    summary: com.example.fitlog.data.repository.DailyNutritionSummary,
) {
    FitLogCard {
        Text(
            text = stringResource(R.string.nutrition_daily_summary),
            style = MaterialTheme.typography.titleSmall,
            color = FitLogTextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Calories bar
        NutrientBar(
            label = stringResource(R.string.nutrition_calories),
            current = summary.calories,
            target = summary.targetCalories.toDouble(),
            unit = "kcal",
            color = FitLogAccent,
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Protein bar
        NutrientBar(
            label = stringResource(R.string.nutrition_protein),
            current = summary.protein,
            target = (summary.calories * 0.3 / 4).coerceAtLeast(50.0),
            unit = "g",
            color = FitLogSuccess,
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Carbs bar
        NutrientBar(
            label = stringResource(R.string.nutrition_carbs),
            current = summary.carbs,
            target = (summary.calories * 0.4 / 4).coerceAtLeast(100.0),
            unit = "g",
            color = FitLogAccent,
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Fat bar
        NutrientBar(
            label = stringResource(R.string.nutrition_fat),
            current = summary.fat,
            target = (summary.calories * 0.25 / 9).coerceAtLeast(30.0),
            unit = "g",
            color = FitLogError,
        )
    }
}

@Composable
private fun NutrientBar(
    label: String,
    current: Double,
    target: Double,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
) {
    val progress = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
            )
            Text(
                text = "%.0f / %.0f $unit".format(current, target),
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(FitLogCardColor.copy(alpha = 0.5f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealTypeTabs(
    selectedType: String?,
    mealTypes: List<Pair<String, String>>,
    onTabSelected: (String?) -> Unit,
) {
    val allLabel = stringResource(R.string.meal_all)
    val tabs = listOf(null to allLabel) + mealTypes

    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedType }.coerceAtLeast(0),
        containerColor = FitLogBackground,
        contentColor = FitLogAccent,
    ) {
        tabs.forEach { (type, label) ->
            Tab(
                selected = selectedType == type,
                onClick = { onTabSelected(type) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
        }
    }
}

@Composable
private fun FoodRecordCard(
    record: FoodRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    FitLogCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.foodName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (record.amount != null) {
                    Text(
                        text = record.amount,
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogTextSecondary,
                    )
                }
                Row {
                    record.calories?.let {
                        Text(
                            text = "%.0f kcal".format(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogAccent,
                        )
                    }
                    record.proteinGrams?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "P: %.0fg".format(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogTextSecondary,
                        )
                    }
                    record.carbsGrams?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "C: %.0fg".format(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogTextSecondary,
                        )
                    }
                    record.fatGrams?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "F: %.0fg".format(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = FitLogTextSecondary,
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodFormDialog(
    formState: FoodFormState,
    mealTypes: List<Pair<String, String>>,
    onFoodNameChange: (String) -> Unit,
    onMealTypeChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isEdit = formState.editId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) stringResource(R.string.nutrition_edit_food)
                else stringResource(R.string.nutrition_add_food)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = formState.foodName,
                    onValueChange = onFoodNameChange,
                    label = { Text(stringResource(R.string.nutrition_food_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.nutrition_amount)) },
                    placeholder = { Text(stringResource(R.string.nutrition_amount_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    OutlinedTextField(
                        value = formState.calories,
                        onValueChange = onCaloriesChange,
                        label = { Text(stringResource(R.string.nutrition_calories)) },
                        suffix = { Text("kcal") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = formState.proteinGrams,
                        onValueChange = onProteinChange,
                        label = { Text(stringResource(R.string.nutrition_protein)) },
                        suffix = { Text("g") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    OutlinedTextField(
                        value = formState.carbsGrams,
                        onValueChange = onCarbsChange,
                        label = { Text(stringResource(R.string.nutrition_carbs)) },
                        suffix = { Text("g") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = formState.fatGrams,
                        onValueChange = onFatChange,
                        label = { Text(stringResource(R.string.nutrition_fat)) },
                        suffix = { Text("g") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.nutrition_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                enabled = formState.foodName.isNotBlank(),
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
