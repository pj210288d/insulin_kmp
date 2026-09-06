package com.dj.insulink.shared.feature.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.localization.tr
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.core.time.combineTimeWithDate
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.core.time.timeOfDayLabel
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import com.dj.insulink.shared.feature.reminders.ui.viewmodel.RemindersViewModel

// Peti deljeni Compose Multiplatform MVP ekran - vidi RemindersViewModel za obim/odluke. Faza 4:
// dodato vreme (time picker dugme) - potrebno da bi ReminderNotificationScheduler znao kada
// stvarno da zvoni, ne samo da čuva vreme kao podatak.
@Composable
fun RemindersScreen(viewModel: RemindersViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    val newTitle by viewModel.newTitle.collectAsState()
    val newType by viewModel.newType.collectAsState()
    val newTime by viewModel.newTime.collectAsState()
    val language by LocalizationSession.currentLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = viewModel::setNewTitle,
                label = { Text(tr(language, "Naslov podsetnika", "Reminder title")) },
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = viewModel::addReminder, enabled = newTitle.isNotBlank()) {
                Text(tr(language, "Dodaj", "Add"))
            }
        }
        Spacer(Modifier.height(8.dp))
        TimePickerButton(time = newTime, language = language, onTimeChange = viewModel::setNewTime)
        Spacer(Modifier.height(8.dp))
        TypeSelector(selected = newType, language = language, onSelect = viewModel::setNewType)
        Spacer(Modifier.height(16.dp))

        if (reminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = tr(language, "Nema dodatih podsetnika", "No reminders added"), color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            LazyColumn {
                items(items = reminders, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        language = language,
                        onToggleDone = { viewModel.toggleDoneForToday(reminder) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerButton(time: Long, language: AppLanguage, onTimeChange: (Long) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
        Text("${tr(language, "Vreme", "Time")}: ${timeOfDayLabel(time)}")
    }

    if (showTimePicker) {
        val timeOfDay = localTimeOfDay(time)
        val timePickerState = rememberTimePickerState(
            initialHour = timeOfDay.hour,
            initialMinute = timeOfDay.minute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(tr(language, "Izaberi vreme", "Choose time"), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text(tr(language, "Otkaži", "Cancel")) }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            onTimeChange(combineTimeWithDate(timePickerState.hour, timePickerState.minute, time))
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeSelector(selected: ReminderType, language: AppLanguage, onSelect: (ReminderType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReminderType.entries.forEach { type ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) InsulinkBlue else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(type) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = typeLabel(type, language),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun typeLabel(type: ReminderType, language: AppLanguage): String = when (type) {
    ReminderType.MEAL_REMINDER -> tr(language, "Obrok", "Meal")
    ReminderType.INSULIN_REMINDER -> "Insulin"
    ReminderType.BLOOD_SUGAR_CHECK_REMINDER -> tr(language, "Merenje šećera", "Glucose check")
}

@Composable
private fun ReminderRow(reminder: Reminder, language: AppLanguage, onToggleDone: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).clickable(onClick = onToggleDone),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DoneCheckbox(checked = reminder.isDoneForToday)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = reminder.title,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (reminder.isDoneForToday) TextDecoration.LineThrough else null
                    )
                    Text(
                        text = "${typeLabel(reminder.reminderType, language)} · ${timeOfDayLabel(reminder.time)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Text(text = "✕", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DoneCheckbox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(
                color = if (checked) InsulinkBlue else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text(text = "✓", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val InsulinkBlue = Color(0xFF4A7BF6)
