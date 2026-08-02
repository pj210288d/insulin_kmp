package com.dj.insulink.feature.reminders.ui

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.core.utils.combineTimeWithDate
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import com.dj.insulink.feature.glucose.ui.GlucoseDropdownMenu
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RemindersScreen(
    params: RemindersScreenParams
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
                    items = params.reminders,
                    key = { item -> item.id }
                ) {
                    ReminderListItem(
                        reminder = it,
                        onSwipeFromStartToEnd = params.onSwipeFromStartToEnd
                    )
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                }
            }

            FloatingActionButton(
                onClick = {
                    params.setReminderTime(System.currentTimeMillis())
                    params.setShowAddReminderDialog(true)
                },
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
    if (params.showAddReminderDialog) {
        AddReminderDialog(
            setShowAddReminderDialog = params.setShowAddReminderDialog,
            reminderTitle = params.reminderTitle,
            setReminderTitle = params.setReminderTitle,
            reminderType = params.reminderType,
            setReminderType = params.setReminderType,
            reminderTime = params.reminderTime,
            setReminderTime = params.setReminderTime,
            onAddReminderClick = params.onAddReminderClick
        )
    }
}

@Composable
private fun ReminderListItem(
    reminder: Reminder,
    onSwipeFromStartToEnd: (Reminder) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var hasBeenSwiped by remember { mutableStateOf(false) }
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd && !hasBeenSwiped) {
                hasBeenSwiped = true
                onSwipeFromStartToEnd(reminder)
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
            Icon(
                painter = painterResource(reminder.reminderType.icon),
                tint = Color.Unspecified,
                contentDescription = "",
                modifier = Modifier.padding(InsulinkTheme.dimens.commonPadding8)
            )
            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
            Column {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeFormatter.format(Date(reminder.time)),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(WEIGHT_VALUE))
            Icon(
                painter = if (reminder.isDoneForToday) {
                    painterResource(R.drawable.ic_done)
                } else {
                    painterResource(R.drawable.ic_upcoming)
                },
                tint = Color.Unspecified,
                contentDescription = "",
                modifier = Modifier
                    .size(InsulinkTheme.dimens.reminderIconSize)
                    .padding(InsulinkTheme.dimens.commonPadding16)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    setShowAddReminderDialog: (Boolean) -> Unit,
    reminderTitle: String,
    setReminderTitle: (String) -> Unit,
    reminderType: ReminderType,
    setReminderType: (ReminderType) -> Unit,
    reminderTime: Long,
    setReminderTime: (Long) -> Unit,
    onAddReminderClick: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(reminderTime) {
        timeFormatter.format(
            Date(reminderTime)
        )
    }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { setShowAddReminderDialog(false) }) {
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
                        text = stringResource(R.string.reminders_screen_add_new_reminder_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing24))
                OutlinedTextField(
                    value = reminderTitle,
                    onValueChange = { newValue ->
                        setReminderTitle(newValue)
                    },
                    label = { Text(stringResource(R.string.reminders_screen_add_new_reminder_title_label)) },
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
                Text(
                    text = stringResource(R.string.reminders_screen_add_new_reminder_select_type_label)
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing4))
                val reminderTypeLabels = ReminderType.entries.map { stringResource(it.displayNameRes) }
                GlucoseDropdownMenu(
                    items = reminderTypeLabels,
                    selectedItem = stringResource(reminderType.displayNameRes),
                    onItemSelected = { selected ->
                        val index = reminderTypeLabels.indexOf(selected)
                        if (index >= 0) {
                            setReminderType(ReminderType.entries[index])
                        }
                    }
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                Text(
                    text = stringResource(R.string.reminders_screen_add_new_reminder_select_time_label)
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing4))
                TextButton(
                    onClick = {
                        showTimePicker = true
                    },
                    border = BorderStroke(
                        InsulinkTheme.dimens.commonButtonBorder1,
                        MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$timeString",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { setShowAddReminderDialog(false) }) {
                        Text(
                            text = stringResource(R.string.new_reading_cancel),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                    Button(
                        onClick = {
                            onAddReminderClick()
                            setShowAddReminderDialog(false)
                        },
                        enabled = reminderTitle.isNotEmpty(),
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
    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = reminderTime }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(InsulinkTheme.dimens.commonPadding24),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.reminders_screen_add_new_reminder_select_time_label),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(
                                text = stringResource(R.string.new_reading_cancel),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                        TextButton(
                            onClick = {
                                setReminderTime(
                                    combineTimeWithDate(
                                        hour = timePickerState.hour,
                                        minute = timePickerState.minute,
                                        existingTimestamp = reminderTime
                                    )
                                )
                                showTimePicker = false
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.new_reading_ok),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class RemindersScreenParams(
    val reminders: List<Reminder>,
    val showAddReminderDialog: Boolean,
    val reminderTitle: String,
    val reminderType: ReminderType,
    val reminderTime: Long,
    val setShowAddReminderDialog: (Boolean) -> Unit,
    val setReminderTitle: (String) -> Unit,
    val setReminderType: (ReminderType) -> Unit,
    val setReminderTime: (Long) -> Unit,
    val onSwipeFromStartToEnd: (Reminder) -> Unit,
    val onAddReminderClick: () -> Unit
)

private const val WEIGHT_VALUE = 1f
private const val OPACITY_VALUE = 0.6f
private const val POSITIONAL_MODIFIER = 0.25f
