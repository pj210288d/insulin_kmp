package com.dj.insulink.feature.fitness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

@Composable
fun AddSportsActivityDialog(
    setShowAddExerciseDialog: (Boolean) -> Unit,
    sportsList: List<String>,
    sportName: State<String>,
    setSportName: (String) -> Unit,
    durationHours: State<String>,
    setDurationHours: (String) -> Unit,
    durationMinutes: State<String>,
    setDurationMinutes: (String) -> Unit,
    glucoseBefore: State<String>,
    setGlucoseBefore: (String) -> Unit,
    glucoseAfter: State<String>,
    setGlucoseAfter: (String) -> Unit,
    onAddExerciseClick: () -> Unit,
    glucoseUnit: GlucoseUnit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { setShowAddExerciseDialog(false) }) {
        Card(
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(InsulinkTheme.dimens.commonPadding24),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.fitness_screen_add_new_exercise_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing24))
                Box (
                    modifier = Modifier.fillMaxWidth()
                ){
                    OutlinedTextField(
                        value = sportName.value,
                        onValueChange = { newValue ->
                            setSportName(newValue)
                        },
                        label = { Text(stringResource(R.string.fitness_screen_add_sport_name_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                        trailingIcon = {
                            IconButton(
                                onClick = { isDropdownExpanded = !isDropdownExpanded }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select sport from list",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorTextColor = MaterialTheme.colorScheme.error,

                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorLabelColor = MaterialTheme.colorScheme.error,

                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            errorBorderColor = MaterialTheme.colorScheme.error,

                            cursorColor = MaterialTheme.colorScheme.primary,
                            errorCursorColor = MaterialTheme.colorScheme.error,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,

                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorPlaceholderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth(0.65f)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = InsulinkTheme.dimens.commonSpacing4
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                sportsList.forEach { sport ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sport,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            setSportName(sport)
                                            isDropdownExpanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                Text(
                    text = stringResource(R.string.fitness_screen_add_duration_label),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing4))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
                ) {
                    OutlinedTextField(
                        value = durationHours.value,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                                setDurationHours(newValue)
                            }
                        },
                        label = { Text(stringResource(R.string.fitness_screen_hours_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorTextColor = MaterialTheme.colorScheme.error,

                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorLabelColor = MaterialTheme.colorScheme.error,

                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            errorBorderColor = MaterialTheme.colorScheme.error,

                            cursorColor = MaterialTheme.colorScheme.primary,
                            errorCursorColor = MaterialTheme.colorScheme.error,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,

                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorPlaceholderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    OutlinedTextField(
                        value = durationMinutes.value,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                                setDurationMinutes(newValue)
                            }
                        },
                        label = { Text(stringResource(R.string.fitness_screen_minutes_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorTextColor = MaterialTheme.colorScheme.error,

                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorLabelColor = MaterialTheme.colorScheme.error,

                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            errorBorderColor = MaterialTheme.colorScheme.error,

                            cursorColor = MaterialTheme.colorScheme.primary,
                            errorCursorColor = MaterialTheme.colorScheme.error,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,

                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            errorPlaceholderColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                OutlinedTextField(
                    value = glucoseBefore.value,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' } && newValue.length <= 5) {
                            setGlucoseBefore(newValue)
                        }
                    },
                    label = { Text(stringResource(R.string.fitness_screen_glucose_before_label, glucoseUnit.suffix)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        errorTextColor = MaterialTheme.colorScheme.error,

                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        errorLabelColor = MaterialTheme.colorScheme.error,

                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        errorBorderColor = MaterialTheme.colorScheme.error,

                        cursorColor = MaterialTheme.colorScheme.primary,
                        errorCursorColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,

                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        errorPlaceholderColor = MaterialTheme.colorScheme.error
                    )
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                OutlinedTextField(
                    value = glucoseAfter.value,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' } && newValue.length <= 5) {
                            setGlucoseAfter(newValue)
                        }
                    },
                    label = { Text(stringResource(R.string.fitness_screen_glucose_after_label, glucoseUnit.suffix)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        errorTextColor = MaterialTheme.colorScheme.error,

                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        errorLabelColor = MaterialTheme.colorScheme.error,

                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        errorBorderColor = MaterialTheme.colorScheme.error,

                        cursorColor = MaterialTheme.colorScheme.primary,
                        errorCursorColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,

                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        errorPlaceholderColor = MaterialTheme.colorScheme.error
                    )
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { setShowAddExerciseDialog(false) }) {
                        Text(
                            text = stringResource(R.string.new_reading_cancel),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                    Button(
                        onClick = {
                            onAddExerciseClick()
                            setShowAddExerciseDialog(false)
                        },
                        enabled = sportName.value.isNotEmpty() &&
                                glucoseBefore.value.isNotEmpty() &&
                                glucoseAfter.value.isNotEmpty() &&
                                (durationHours.value.isNotEmpty() || durationMinutes.value.isNotEmpty()),
                        modifier = Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    InsulinkTheme.colors.insulinkBlue,
                                    InsulinkTheme.colors.insulinkPurple
                                )
                            ),
                            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.new_reading_save),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}