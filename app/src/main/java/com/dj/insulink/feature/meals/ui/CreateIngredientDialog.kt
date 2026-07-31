package com.dj.insulink.feature.meals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient

@Composable
fun CreateIngredientDialog(
    onDismiss: () -> Unit,
    onSave: (Ingredient) -> Unit,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var salt by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
        errorTextColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = InsulinkTheme.colors.insulinkBlue,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
        errorLabelColor = MaterialTheme.colorScheme.error,
        focusedBorderColor = InsulinkTheme.colors.insulinkBlue,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = DISABLED_ALPHA),
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = InsulinkTheme.colors.insulinkBlue,
        errorCursorColor = MaterialTheme.colorScheme.error,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

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
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.meals_screen_create_ingredient_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.meals_screen_ingredient_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding16))

                Text(
                    text = stringResource(R.string.meals_screen_nutrition_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding8))

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text(stringResource(R.string.meals_screen_calories_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding12))

                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text(stringResource(R.string.meals_screen_protein_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding12))

                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text(stringResource(R.string.meals_screen_carbs_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding12))

                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text(stringResource(R.string.meals_screen_fat_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding12))

                OutlinedTextField(
                    value = sugar,
                    onValueChange = { sugar = it },
                    label = { Text(stringResource(R.string.meals_screen_sugar_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(InsulinkTheme.dimens.commonPadding12))

                OutlinedTextField(
                    value = salt,
                    onValueChange = { salt = it },
                    label = { Text(stringResource(R.string.meals_screen_salt_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.size(InsulinkTheme.dimens.commonSpacing16))

                Button(
                    onClick = {
                        val ingredient = Ingredient(
                            name = name,
                            caloriesPer100g = calories.toDoubleOrNull() ?: 0.0,
                            proteinPer100g = protein.toDoubleOrNull() ?: 0.0,
                            carbsPer100g = carbs.toDoubleOrNull() ?: 0.0,
                            fatPer100g = fat.toDoubleOrNull() ?: 0.0,
                            sugarPer100g = sugar.toDoubleOrNull() ?: 0.0,
                            saltPer100g = salt.toDoubleOrNull() ?: 0.0
                        )
                        onSave(ingredient)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(InsulinkTheme.dimens.commonButtonHeight50)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    InsulinkTheme.colors.insulinkBlue,
                                    InsulinkTheme.colors.insulinkPurple
                                )
                            ),
                            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                        ),
                    enabled = !isLoading && name.isNotEmpty() && calories.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(InsulinkTheme.dimens.commonProgressIndicatorSize20),
                            color = Color.White
                        )
                    } else {
                        Text(stringResource(R.string.meals_screen_create_ingredient_button), color = Color.White)
                    }
                }
            }
        }
    }
}

private const val DIALOG_HEIGHT_FRACTION = 0.9f
private const val DISABLED_ALPHA = 0.6f
