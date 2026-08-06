package com.dj.insulink.feature.insulin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType

@Composable
fun InsulinScreen(
    params: InsulinScreenParams
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))

        Box(
            modifier = Modifier.weight(WEIGHT_VALUE)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = InsulinkTheme.dimens.commonPadding80)
            ) {
                items(
                    items = params.insulinTypes,
                    key = { item -> item.id }
                ) {
                    InsulinTypeListItem(
                        insulinType = it,
                        onSwipeFromStartToEnd = params.onSwipeFromStartToEnd
                    )
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                }
            }

            FloatingActionButton(
                onClick = { params.setShowAddInsulinTypeDialog(true) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(InsulinkTheme.dimens.commonPadding16),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = ""
                )
            }
        }
    }
    if (params.showAddInsulinTypeDialog) {
        AddInsulinTypeDialog(
            setShowAddInsulinTypeDialog = params.setShowAddInsulinTypeDialog,
            insulinTypeName = params.insulinTypeName,
            setInsulinTypeName = params.setInsulinTypeName,
            onAddInsulinTypeClick = params.onAddInsulinTypeClick
        )
    }
}

@Composable
private fun InsulinTypeListItem(
    insulinType: InsulinType,
    onSwipeFromStartToEnd: (InsulinType) -> Unit
) {
    var hasBeenSwiped by remember { mutableStateOf(false) }
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd && !hasBeenSwiped) {
                hasBeenSwiped = true
                onSwipeFromStartToEnd(insulinType)
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * POSITIONAL_MODIFIER }
    )

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        enableDismissFromEndToStart = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InsulinkTheme.dimens.commonPadding12),
        backgroundContent = {}
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = InsulinkTheme.dimens.commonPadding12)
                .clip(RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12))
                .border(
                    BorderStroke(
                        InsulinkTheme.dimens.commonButtonBorder1,
                        MaterialTheme.colorScheme.outline
                    ),
                    RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                )
        ) {
            Text(
                text = insulinType.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(InsulinkTheme.dimens.commonPadding16)
            )
        }
    }
}

@Composable
private fun AddInsulinTypeDialog(
    setShowAddInsulinTypeDialog: (Boolean) -> Unit,
    insulinTypeName: String,
    setInsulinTypeName: (String) -> Unit,
    onAddInsulinTypeClick: () -> Unit
) {
    Dialog(onDismissRequest = { setShowAddInsulinTypeDialog(false) }) {
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
                        text = stringResource(R.string.insulin_screen_add_new_insulin_type_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing24))
                OutlinedTextField(
                    value = insulinTypeName,
                    onValueChange = { newValue ->
                        setInsulinTypeName(newValue)
                    },
                    label = { Text(stringResource(R.string.insulin_screen_add_new_insulin_type_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = OPACITY_VALUE),
                        errorTextColor = MaterialTheme.colorScheme.error,

                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = OPACITY_VALUE),
                        errorLabelColor = MaterialTheme.colorScheme.error,

                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = OPACITY_VALUE),
                        errorBorderColor = MaterialTheme.colorScheme.error,

                        cursorColor = MaterialTheme.colorScheme.primary,
                        errorCursorColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,

                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = OPACITY_VALUE),
                        errorPlaceholderColor = MaterialTheme.colorScheme.error
                    )
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { setShowAddInsulinTypeDialog(false) }) {
                        Text(
                            text = stringResource(R.string.new_reading_cancel),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                    Button(
                        onClick = {
                            onAddInsulinTypeClick()
                            setShowAddInsulinTypeDialog(false)
                        },
                        enabled = insulinTypeName.isNotEmpty(),
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

data class InsulinScreenParams(
    val insulinTypes: List<InsulinType>,
    val showAddInsulinTypeDialog: Boolean,
    val insulinTypeName: String,
    val setShowAddInsulinTypeDialog: (Boolean) -> Unit,
    val setInsulinTypeName: (String) -> Unit,
    val onSwipeFromStartToEnd: (InsulinType) -> Unit,
    val onAddInsulinTypeClick: () -> Unit
)

private const val WEIGHT_VALUE = 1f
private const val OPACITY_VALUE = 0.6f
private const val POSITIONAL_MODIFIER = 0.25f
