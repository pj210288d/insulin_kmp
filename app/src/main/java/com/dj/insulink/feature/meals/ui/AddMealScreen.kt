package com.dj.insulink.feature.meals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.MealIngredient

data class AddMealScreenParams(
    val mealName: State<String>,
    val onMealNameChange: (String) -> Unit,
    val mealComment: State<String>,
    val onMealCommentChange: (String) -> Unit,
    val mealTimestamp: State<Long>,
    val onMealTimestampChange: (Long) -> Unit,
    val searchQuery: State<String>,
    val onSearchQueryChange: (String) -> Unit,
    val searchResults: State<List<Ingredient>>,
    val selectedIngredients: State<List<MealIngredient>>,
    val onAddIngredient: (Ingredient, Double) -> Unit,
    val onRemoveIngredient: (MealIngredient) -> Unit,
    val onUpdateIngredientQuantity: (MealIngredient, Double) -> Unit,
    val isLoading: State<Boolean>,
    val showCreateIngredientDialog: State<Boolean>,
    val setShowCreateIngredientDialog: (Boolean) -> Unit,
    val showMyIngredientsDialog: State<Boolean>,
    val setShowMyIngredientsDialog: (Boolean) -> Unit,
    val userIngredients: State<List<Ingredient>>,
    val onSave: () -> Unit,
    val onNavigateBack: () -> Unit,
    val createCustomIngredient: (Ingredient) -> Unit,
    val deleteCustomIngredient: (Ingredient) -> Unit
)

@Composable
fun AddMealScreen(params: AddMealScreenParams) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = InsulinkTheme.dimens.commonPadding4,
                    vertical = InsulinkTheme.dimens.commonPadding12
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = params.onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
            }
            Text(
                text = stringResource(R.string.meals_screen_add_meal_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = InsulinkTheme.dimens.commonPadding16)
        ) {
            OutlinedTextField(
                value = params.mealName.value,
                onValueChange = params.onMealNameChange,
                label = { Text(stringResource(R.string.meals_screen_meal_name_label)) },
                placeholder = { Text(stringResource(R.string.meals_screen_meal_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding12))

            DateTimeInput(
                selectedTimestamp = params.mealTimestamp.value,
                onTimestampSelected = params.onMealTimestampChange
            )

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding12))

            OutlinedTextField(
                value = params.searchQuery.value,
                onValueChange = params.onSearchQueryChange,
                label = { Text(stringResource(R.string.meals_screen_add_ingredients_label)) },
                placeholder = { Text(stringResource(R.string.meals_screen_search_ingredients_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "") },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { params.setShowCreateIngredientDialog(true) }) {
                            Icon(Icons.Default.Add, contentDescription = "")
                        }
                        IconButton(onClick = { params.setShowMyIngredientsDialog(true) }) {
                            Icon(Icons.Default.Person, contentDescription = "")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding8))

            if (params.searchResults.value.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(InsulinkTheme.dimens.searchResultsListHeight)
                        .border(
                            InsulinkTheme.dimens.commonButtonBorder1,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius8)
                        ),
                    contentPadding = PaddingValues(InsulinkTheme.dimens.commonPadding8)
                ) {
                    items(params.searchResults.value) { ingredient ->
                        IngredientSearchItem(
                            ingredient = ingredient,
                            onAdd = { params.onAddIngredient(ingredient, 100.0) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))

            Text(
                text = stringResource(R.string.meals_screen_added_ingredients_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding8))

            if (params.selectedIngredients.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(InsulinkTheme.dimens.searchResultsListHeight)
                        .border(
                            InsulinkTheme.dimens.commonButtonBorder1,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius8)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "",
                            modifier = Modifier.size(InsulinkTheme.dimens.sideDrawerIconSize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.meals_screen_no_ingredients_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.meals_screen_no_ingredients_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(InsulinkTheme.dimens.ingredientsListHeight)
                        .border(
                            InsulinkTheme.dimens.commonButtonBorder1,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius8)
                        ),
                    contentPadding = PaddingValues(InsulinkTheme.dimens.commonPadding8)
                ) {
                    items(params.selectedIngredients.value) { mealIngredient ->
                        AddedIngredientItem(
                            mealIngredient = mealIngredient,
                            onRemove = { params.onRemoveIngredient(mealIngredient) },
                            onQuantityChange = { quantity ->
                                params.onUpdateIngredientQuantity(mealIngredient, quantity)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))

            Text(
                text = stringResource(R.string.meals_screen_nutrition_facts_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding8))

            val totalCalories =
                params.selectedIngredients.value.sumOf { (it.ingredient.caloriesPer100g * it.quantity / 100).toInt() }
            val totalProtein =
                params.selectedIngredients.value.sumOf { it.ingredient.proteinPer100g * it.quantity / 100 }
            val totalFat =
                params.selectedIngredients.value.sumOf { it.ingredient.fatPer100g * it.quantity / 100 }
            val totalCarbs =
                params.selectedIngredients.value.sumOf { it.ingredient.carbsPer100g * it.quantity / 100 }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
            ) {
                NutritionCard(stringResource(R.string.meals_screen_calories_label), totalCalories.toString(), InsulinkTheme.colors.insulinkBlue, Modifier.weight(1f))
                NutritionCard(stringResource(R.string.meals_screen_protein_label), stringResource(R.string.meals_screen_grams_value, String.format("%.1f", totalProtein)), InsulinkTheme.colors.glucoseNormal, Modifier.weight(1f))
                NutritionCard(stringResource(R.string.meals_screen_fats_label), stringResource(R.string.meals_screen_grams_value, String.format("%.1f", totalFat)), InsulinkTheme.colors.lastDropLabel, Modifier.weight(1f))
                NutritionCard(stringResource(R.string.meals_screen_carb_label), stringResource(R.string.meals_screen_grams_value, String.format("%.1f", totalCarbs)), InsulinkTheme.colors.glucoseLow, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))

            OutlinedTextField(
                value = params.mealComment.value,
                onValueChange = params.onMealCommentChange,
                label = { Text(stringResource(R.string.meals_screen_comment_label)) },
                placeholder = { Text(stringResource(R.string.meals_screen_comment_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))
        }

        Button(
            onClick = params.onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = InsulinkTheme.dimens.commonPadding16)
                .padding(bottom = InsulinkTheme.dimens.commonPadding16)
                .height(InsulinkTheme.dimens.commonButtonHeight50),
            enabled = !params.isLoading.value && params.mealName.value.isNotEmpty() && params.selectedIngredients.value.isNotEmpty(),
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
        ) {
            if (params.isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(InsulinkTheme.dimens.commonProgressIndicatorSize20),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    stringResource(R.string.meals_screen_save_meal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (params.showCreateIngredientDialog.value) {
        CreateIngredientDialog(
            onDismiss = { params.setShowCreateIngredientDialog(false) },
            onSave = params.createCustomIngredient,
            isLoading = params.isLoading.value
        )
    }

    if (params.showMyIngredientsDialog.value) {
        MyIngredientsDialog(
            userIngredients = params.userIngredients,
            onDismiss = { params.setShowMyIngredientsDialog(false) },
            onCreateIngredient = { params.setShowCreateIngredientDialog(true) },
            onDeleteIngredient = params.deleteCustomIngredient,
            isLoading = params.isLoading.value
        )
    }
}

@Composable
private fun IngredientSearchItem(
    ingredient: Ingredient,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() }
            .padding(vertical = InsulinkTheme.dimens.commonPadding4),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InsulinkTheme.dimens.commonPadding12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.meals_screen_cal_per_100g, ingredient.caloriesPer100g.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.Add,
                contentDescription = "",
                modifier = Modifier.size(InsulinkTheme.dimens.textFieldIconSize),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddedIngredientItem(
    mealIngredient: MealIngredient,
    onRemove: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    var quantityText by remember { mutableStateOf(mealIngredient.quantity.toInt().toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = InsulinkTheme.dimens.commonPadding4),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = InsulinkTheme.dimens.commonPadding12,
                    top = InsulinkTheme.dimens.commonPadding8,
                    bottom = InsulinkTheme.dimens.commonPadding8,
                    end = InsulinkTheme.dimens.commonPadding4
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mealIngredient.ingredient.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.meals_screen_cal_per_grams,
                        (mealIngredient.ingredient.caloriesPer100g * mealIngredient.quantity / 100).toInt(),
                        mealIngredient.quantity.toInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            BasicTextField(
                value = quantityText,
                onValueChange = { newValue ->
                    quantityText = newValue
                    newValue.toDoubleOrNull()?.let { onQuantityChange(it) }
                },
                modifier = Modifier
                    .width(InsulinkTheme.dimens.quantityFieldWidth)
                    .border(
                        InsulinkTheme.dimens.commonButtonBorder1,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius8)
                    )
                    .padding(
                        horizontal = InsulinkTheme.dimens.commonPadding8,
                        vertical = InsulinkTheme.dimens.commonPadding8
                    ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(InsulinkTheme.dimens.commonIconSize40)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "",
                    modifier = Modifier.size(InsulinkTheme.dimens.commonIconSize16),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NutritionCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(InsulinkTheme.dimens.nutritionCardHeight),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius8)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
