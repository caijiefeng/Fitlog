package com.example.fitlog.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogCard as FitLogCardColor
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogWarning
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.data.repository.FoodRecord
import com.example.fitlog.data.repository.MealSubtotal
import com.example.fitlog.domain.nutrition.FoodSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entryState by viewModel.entryState.collectAsStateWithLifecycle()

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
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddForm() },
                containerColor = FitLogAccent,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("添加食物") },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = FitLogAccent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            )
        } else if (uiState.error != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitLogError,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.refresh() },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        } else {
            PageContainer(modifier = Modifier.padding(innerPadding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                ) {
                // Missing data message
                uiState.missingDataMessage?.let { message ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        FitLogCard {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FitLogTextSecondary,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Daily Summary Card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    DailySummaryCard(summary = uiState.summary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Meal subtotals
                item {
                    MealSubtotalsSection(subtotals = uiState.summary.mealSubtotals)
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
                    items(uiState.foodRecords.groupBy { it.mealType }.toList(), key = { it.first }) { (mealType, records) ->
                        MealRecordGroup(
                            mealType = mealType,
                            records = records,
                            onEdit = viewModel::showEditForm,
                            onDelete = viewModel::deleteFoodRecord,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
    }

    // Food entry dialog
    if (entryState.isVisible) {
        FoodEntryDialog(
            viewModel = viewModel,
            entryState = entryState,
        )
    }
}

@Composable
private fun DailySummaryCard(
    summary: com.example.fitlog.data.repository.DailyNutritionSummary,
) {
    val target = summary.targetCalories.takeIf { it > 0 }?.toDouble()
    val progress = target?.let { (summary.calories / it).toFloat().coerceIn(0f, 1f) } ?: 0f
    FitLogCard(style = com.example.fitlog.core.designsystem.component.FitLogCardStyle.HERO) {
        Text("今日摄入", style = MaterialTheme.typography.titleSmall, color = FitLogTextSecondary)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(154.dp)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 11.dp, color = FitLogAccent, trackColor = FitLogCardColor)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.0f".format(summary.calories), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = FitLogTextPrimary)
                    Text("kcal", style = MaterialTheme.typography.labelMedium, color = FitLogTextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (target != null) "目标 %.0f kcal".format(target) else "今日已摄入", style = MaterialTheme.typography.bodyMedium, color = FitLogTextSecondary)
                if (target != null) Text("还可以摄入 %.0f kcal".format((target - summary.calories).coerceAtLeast(0.0)), style = MaterialTheme.typography.titleSmall, color = FitLogAccent)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniMacro("蛋白质", summary.protein, summary.targetProtein.toDouble(), FitLogSuccess)
            MiniMacro("碳水", summary.carbs, summary.targetCarbs.toDouble(), FitLogAccent)
            MiniMacro("脂肪", summary.fat, summary.targetFat.toDouble(), FitLogWarning)
        }
    }
}

@Composable
private fun MiniMacro(label: String, value: Double, target: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitLogTextSecondary)
        Text("%.0fg".format(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(if (target > 0) "${(value / target * 100).toInt().coerceAtMost(999)}%" else "—", style = MaterialTheme.typography.labelSmall, color = FitLogTextSecondary)
    }
}

@Composable
private fun MealSubtotalsSection(
    subtotals: List<MealSubtotal>,
) {
    val visible = subtotals.filter { it.count > 0 }
    if (visible.isEmpty()) return

    FitLogCard {
        Text(
            text = stringResource(R.string.meal_subtotal_title),
            style = MaterialTheme.typography.titleSmall,
            color = FitLogTextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        visible.forEach { subtotal ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mealTypeLabel(subtotal.mealType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "%.0f kcal".format(subtotal.calories),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitLogAccent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "蛋白质 %.0f g · 碳水 %.0f g · 脂肪 %.0f g".format(
                    subtotal.protein, subtotal.carbs, subtotal.fat,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun mealTypeLabel(mealType: String): String = when (mealType) {
    "BREAKFAST" -> stringResource(R.string.meal_breakfast)
    "LUNCH" -> stringResource(R.string.meal_lunch)
    "DINNER" -> stringResource(R.string.meal_dinner)
    "SNACK" -> stringResource(R.string.meal_snack)
    else -> mealType
}

@Composable
private fun NutrientBar(
    label: String,
    current: Double,
    target: Double?,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
) {
    val progress = if (target != null && target > 0) {
        (current / target).toFloat().coerceIn(0f, 1f)
    } else 0f

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
                text = if (target != null && target > 0) {
                    "%.0f / %.0f $unit".format(current, target)
                } else {
                    "%.0f $unit".format(current)
                },
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
private fun MealRecordGroup(
    mealType: String,
    records: List<FoodRecord>,
    onEdit: (FoodRecord) -> Unit,
    onDelete: (FoodRecord) -> Unit,
) {
    FitLogCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(mealTypeLabel(mealType), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = FitLogTextPrimary)
            Text("%.0f kcal".format(records.sumOf { it.calories ?: 0.0 }), style = MaterialTheme.typography.bodyMedium, color = FitLogAccent)
        }
        Spacer(Modifier.height(8.dp))
        records.forEachIndexed { index, record ->
            FoodRecordCard(record, onEdit = { onEdit(record) }, onDelete = { onDelete(record) })
            if (index != records.lastIndex) androidx.compose.material3.HorizontalDivider()
        }
    }
}

@Composable
private fun FoodRecordCard(
    record: FoodRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodEntryDialog(
    viewModel: NutritionViewModel,
    entryState: FoodEntryState,
) {
    val isEdit = entryState.editId != null
    val canSave = entryState.selectedFood != null || entryState.manualName.isNotBlank()

    AlertDialog(
        onDismissRequest = { viewModel.hideForm() },
        title = {
            Text(
                if (isEdit) stringResource(R.string.nutrition_edit_food)
                else stringResource(R.string.nutrition_add_food)
            )
        },
        text = {
            Column {
                if (entryState.selectedFood == null) {
                    // Mode toggle (search food vs manual macros)
                    Row {
                        FilterChip(
                            selected = !entryState.useManual,
                            onClick = {
                                if (entryState.useManual) viewModel.toggleEntryMode()
                            },
                            label = {
                                Text(
                                    stringResource(R.string.food_search_mode),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = entryState.useManual,
                            onClick = {
                                if (!entryState.useManual) viewModel.toggleEntryMode()
                            },
                            label = {
                                Text(
                                    stringResource(R.string.food_manual_mode),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (entryState.useManual) {
                        ManualEntryFields(viewModel = viewModel, entryState = entryState)
                    } else {
                        FoodSearchFields(viewModel = viewModel, entryState = entryState)
                    }
                } else {
                    SelectedFoodFields(viewModel = viewModel, entryState = entryState)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveFoodRecord() },
                colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                enabled = canSave,
            ) {
                Text(stringResource(R.string.action_save), color = FitLogTextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideForm() }) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun FoodSearchFields(
    viewModel: NutritionViewModel,
    entryState: FoodEntryState,
) {
    OutlinedTextField(
        value = entryState.query,
        onValueChange = { viewModel.updateEntryQuery(it) },
        label = { Text(stringResource(R.string.nutrition_food_name)) },
        placeholder = { Text(stringResource(R.string.food_search_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))

    when {
        entryState.isSearching -> {
            Text(
                text = stringResource(R.string.food_searching),
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
            )
        }
        entryState.query.isBlank() -> {
            Text(
                text = stringResource(R.string.food_search_hint),
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
            )
        }
        entryState.searchResults.isEmpty() -> {
            Text(
                text = stringResource(R.string.food_search_empty),
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
            ) {
                items(entryState.searchResults, key = { it.id }) { result ->
                    FoodSearchResultRow(
                        result = result,
                        onClick = { viewModel.selectFood(result) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodSearchResultRow(
    result: FoodSearchResult,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextPrimary,
            )
            if (result.category.isNotBlank()) {
                Text(
                    text = result.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }
        }
        Text(
            text = stringResource(R.string.food_per_100g, result.caloriesPer100g.toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = FitLogAccent,
        )
    }
}

@Composable
private fun SelectedFoodFields(
    viewModel: NutritionViewModel,
    entryState: FoodEntryState,
) {
    val food = entryState.selectedFood ?: return
    val decimalOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)

    Text(
        text = food.name,
        style = MaterialTheme.typography.titleMedium,
        color = FitLogTextPrimary,
        fontWeight = FontWeight.SemiBold,
    )
    if (food.category.isNotBlank()) {
        Text(
            text = food.category,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    Row {
        OutlinedTextField(
            value = entryState.quantity,
            onValueChange = { viewModel.updateEntryQuantity(it) },
            label = { Text(stringResource(R.string.food_servings)) },
            keyboardOptions = decimalOptions,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = entryState.unit,
            onValueChange = { viewModel.updateEntryUnit(it) },
            label = { Text(stringResource(R.string.food_unit)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = entryState.grams,
            onValueChange = { viewModel.updateEntryGrams(it) },
            label = { Text(stringResource(R.string.food_grams)) },
            keyboardOptions = decimalOptions,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Real-time nutrition preview (grams / 100 * per-100g macros)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(FitLogAccent.copy(alpha = 0.1f))
            .padding(12.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.food_preview),
                style = MaterialTheme.typography.labelMedium,
                color = FitLogTextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.food_preview_format,
                    entryState.calories,
                    entryState.protein,
                    entryState.carbs,
                    entryState.fat,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogAccent,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = entryState.note,
        onValueChange = { viewModel.updateEntryNote(it) },
        label = { Text(stringResource(R.string.nutrition_note)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ManualEntryFields(
    viewModel: NutritionViewModel,
    entryState: FoodEntryState,
) {
    val decimalOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)

    OutlinedTextField(
        value = entryState.manualName,
        onValueChange = { viewModel.updateEntryManualName(it) },
        label = { Text(stringResource(R.string.nutrition_food_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row {
        OutlinedTextField(
            value = entryState.manualCalories,
            onValueChange = { viewModel.updateEntryManualCalories(it) },
            label = { Text(stringResource(R.string.nutrition_calories)) },
            suffix = { Text("kcal") },
            keyboardOptions = decimalOptions,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = entryState.manualProtein,
            onValueChange = { viewModel.updateEntryManualProtein(it) },
            label = { Text(stringResource(R.string.nutrition_protein)) },
            suffix = { Text("g") },
            keyboardOptions = decimalOptions,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    Row {
        OutlinedTextField(
            value = entryState.manualCarbs,
            onValueChange = { viewModel.updateEntryManualCarbs(it) },
            label = { Text(stringResource(R.string.nutrition_carbs)) },
            suffix = { Text("g") },
            keyboardOptions = decimalOptions,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = entryState.manualFat,
            onValueChange = { viewModel.updateEntryManualFat(it) },
            label = { Text(stringResource(R.string.nutrition_fat)) },
            suffix = { Text("g") },
            keyboardOptions = decimalOptions,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = entryState.manualAmount,
        onValueChange = { viewModel.updateEntryManualAmount(it) },
        label = { Text(stringResource(R.string.nutrition_amount)) },
        placeholder = { Text(stringResource(R.string.nutrition_amount_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = entryState.note,
        onValueChange = { viewModel.updateEntryNote(it) },
        label = { Text(stringResource(R.string.nutrition_note)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
