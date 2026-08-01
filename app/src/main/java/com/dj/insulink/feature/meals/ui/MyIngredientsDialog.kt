package com.dj.insulink.feature.meals.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient

@Composable
fun MyIngredientsDialog(
    userIngredients: State<List<Ingredient>>,
    onDismiss: () -> Unit,
    onCreateIngredient: () -> Unit,
    onDeleteIngredient: (Ingredient) -> Unit,
    isLoading: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(DIALOG_HEIGHT_FRACTION),
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(InsulinkTheme.dimens.commonPadding16)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.meals_screen_my_ingredients_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row {
                        IconButton(onClick = onCreateIngredient) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "",
                                tint = InsulinkTheme.colors.insulinkBlue
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))

                if (userIngredients.value.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(WEIGHT_VALUE),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "",
                                modifier = Modifier.size(InsulinkTheme.dimens.commonIconSize64),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))
                            Text(
                                text = stringResource(R.string.meals_screen_no_custom_ingredients_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.meals_screen_no_custom_ingredients_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))
                            Button(
                                onClick = onCreateIngredient,
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                InsulinkTheme.colors.insulinkBlue,
                                                InsulinkTheme.colors.insulinkPurple
                                            )
                                        ),
                                        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                                    ),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                            ) {
                                Text(stringResource(R.string.meals_screen_create_ingredient_button), color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(WEIGHT_VALUE),
                        verticalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
                    ) {
                        items(userIngredients.value) { ingredient ->
                            IngredientItem(
                                ingredient = ingredient,
                                onDelete = { onDeleteIngredient(ingredient) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientItem(
    ingredient: Ingredient,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InsulinkTheme.dimens.commonPadding16),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(WEIGHT_VALUE)) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonSpacing4))
                Text(
                    text = stringResource(R.string.meals_screen_cal_per_100g, ingredient.caloriesPer100g.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(
                        R.string.meals_screen_nutrition_summary,
                        String.format("%.1f", ingredient.proteinPer100g),
                        String.format("%.1f", ingredient.carbsPer100g),
                        String.format("%.1f", ingredient.fatPer100g)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(InsulinkTheme.dimens.commonIconSize40)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private const val DIALOG_HEIGHT_FRACTION = 0.9f
private const val WEIGHT_VALUE = 1f
