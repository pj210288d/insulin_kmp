package com.dj.insulink.shared.feature.reminders.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.core.time.timeOfDayLabel
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import com.dj.insulink.shared.feature.reminders.ui.viewmodel.RemindersViewModel

// Peti deljeni Compose Multiplatform MVP ekran - vidi RemindersViewModel za obim/odluke. Faza 4:
// dodato vreme (time picker dugme) - potrebno da bi ReminderNotificationScheduler znao kada
// stvarno da zvoni, ne samo da čuva vreme kao podatak.
// Dodavanje podsetnika je 2026-09-07 prebačeno sa uvek-vidljivog inline reda u FAB + dijalog,
// isto kao Android-ov app/feature/reminders/ui/RemindersScreen.kt (parity zahtev korisnika).
// Birač tipa podsetnika je istom prilikom prvo uklonjen pa vraćen (korisnik se predomislio) -
// sad je u AddReminderDialog-u, kao dropdown (SharedDropdownMenu, isti obrazac kao Android-ov
// GlucoseDropdownMenu korišćen za ReminderType u app/feature/reminders/ui/RemindersScreen.kt),
// ne kao stari uvek-vidljivi chip red (TypeSelector, uklonjen).
@Composable
fun RemindersScreen(viewModel: RemindersViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    val newTitle by viewModel.newTitle.collectAsState()
    val newType by viewModel.newType.collectAsState()
    val newTime by viewModel.newTime.collectAsState()
    val language by LocalizationSession.currentLanguage.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (reminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = tr(language, "Nema dodatih podsetnika", "No reminders added"), color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(items = reminders, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        language = language,
                        onToggleDone = { viewModel.toggleDoneForToday(reminder) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = {
                viewModel.setNewTime(currentTimeMillis())
                showAddDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = InsulinkBlue
        ) {
            Text(text = "+", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            title = newTitle,
            onTitleChange = viewModel::setNewTitle,
            type = newType,
            onTypeChange = viewModel::setNewType,
            time = newTime,
            onTimeChange = viewModel::setNewTime,
            language = language,
            onDismiss = { showAddDialog = false },
            onAdd = {
                viewModel.addReminder()
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    type: ReminderType,
    onTypeChange: (ReminderType) -> Unit,
    time: Long,
    onTimeChange: (Long) -> Unit,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onAdd: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = tr(language, "Novi podsetnik", "New reminder"),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(tr(language, "Naslov podsetnika", "Reminder title")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(text = tr(language, "Izaberi tip", "Select type"))
                Spacer(Modifier.height(4.dp))
                val typeLabels = ReminderType.entries.map { typeLabel(it, language) }
                SharedDropdownMenu(
                    items = typeLabels,
                    selectedItem = typeLabel(type, language),
                    onItemSelected = { selected ->
                        val index = typeLabels.indexOf(selected)
                        if (index >= 0) onTypeChange(ReminderType.entries[index])
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${tr(language, "Vreme", "Time")}: ${timeOfDayLabel(time)}")
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr(language, "Otkaži", "Cancel")) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onAdd, enabled = title.isNotBlank()) {
                        Text(tr(language, "Sačuvaj", "Save"))
                    }
                }
            }
        }
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

private fun typeLabel(type: ReminderType, language: AppLanguage): String = when (type) {
    ReminderType.MEAL_REMINDER -> tr(language, "Obrok", "Meal")
    ReminderType.INSULIN_REMINDER -> "Insulin"
    ReminderType.BLOOD_SUGAR_CHECK_REMINDER -> tr(language, "Merenje šećera", "Glucose check")
}

@Composable
private fun ReminderRow(reminder: Reminder, language: AppLanguage, onToggleDone: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
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

// Bez ikonica (Icons.Filled.ArrowDropDown/Up) - vidi napomenu o material-icons-core u
// GlucoseScreen.kt. Isti obrazac kao Android-ov GlucoseDropdownMenu.kt.
@Composable
private fun SharedDropdownMenu(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedItem, modifier = Modifier.weight(1f))
            Text(if (expanded) "▴" else "▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val InsulinkBlue = Color(0xFF4A7BF6)
