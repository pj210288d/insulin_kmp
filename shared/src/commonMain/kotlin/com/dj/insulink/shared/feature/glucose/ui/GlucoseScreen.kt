package com.dj.insulink.shared.feature.glucose.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.dateTimeLabel
import com.dj.insulink.shared.core.time.shiftedDayStartMillis
import com.dj.insulink.shared.core.time.shortWeekdayDateLabel
import com.dj.insulink.shared.core.time.startOfDayMillis
import com.dj.insulink.shared.core.time.timeOfDayLabel
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.glucose.ui.viewmodel.GlucoseViewModel
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

// MVP Glucose ekran deljen preko Compose Multiplatform-a - vidi GlucoseViewModel u istom
// paketu za obim/odluke. Ovaj fajl namerno ne zavisi ni od čega Android-specifičnog
// (string resursi, tema, ikonice iz extended seta) da bi bio siguran za prvi iOS build.
@Composable
fun GlucoseScreen(viewModel: GlucoseViewModel) {
    val readings by viewModel.glucoseReadingsForSelectedDay.collectAsState()
    val latest by viewModel.latestGlucoseReading.collectAsState()
    val selectedDay by viewModel.selectedDayStartMillis.collectAsState()
    val canGoNext by viewModel.canGoToNextDay.collectAsState()
    val unit by viewModel.glucoseUnit.collectAsState()
    val showDialog by viewModel.showAddDialog.collectAsState()
    val newValue by viewModel.newValue.collectAsState()
    val newComment by viewModel.newComment.collectAsState()
    val editing by viewModel.editingReading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Swipe-za-promenu-dana ograničen na status karticu + zaglavlje + grafik, ne na
            // celu listu ispod (isti razlog kao u Android ekranu: lista ima svoje dugme za
            // brisanje po redu, dva horizontalna gesta na istoj listi bi se sudarala).
            Column(
                modifier = Modifier.pointerInput(Unit) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        },
                        onDragEnd = {
                            when {
                                dragTotal <= -SWIPE_THRESHOLD_PX -> viewModel.goToNextDay()
                                dragTotal >= SWIPE_THRESHOLD_PX -> viewModel.goToPreviousDay()
                            }
                        }
                    )
                }
            ) {
                StatusCard(latest, unit)
                Spacer(Modifier.height(12.dp))
                DayHeader(
                    selectedDayStartMillis = selectedDay,
                    canGoToNextDay = canGoNext,
                    onPreviousDay = viewModel::goToPreviousDay,
                    onNextDay = viewModel::goToNextDay
                )
                Spacer(Modifier.height(12.dp))
                if (readings.isNotEmpty()) {
                    SimpleLineChart(
                        readings = readings,
                        unit = unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (readings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nema očitavanja za ovaj dan",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(items = readings, key = { it.id }) { reading ->
                        ReadingRow(
                            reading = reading,
                            unit = unit,
                            onClick = { viewModel.startEditReading(reading) },
                            onDelete = { viewModel.deleteReading(reading) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.startAddReading() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = InsulinkBlue
        ) {
            Text(text = "+", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (showDialog) {
        AddEditReadingDialog(
            value = newValue,
            onValueChange = viewModel::setNewValue,
            comment = newComment,
            onCommentChange = viewModel::setNewComment,
            unit = unit,
            isEditMode = editing != null,
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::submitReading
        )
    }
}

@Composable
private fun StatusCard(latest: GlucoseReading?, unit: GlucoseUnit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(
                brush = Brush.verticalGradient(listOf(InsulinkBlue, InsulinkPurple)),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp).padding(start = 24.dp, end = 16.dp)) {
            Text(text = "Poslednje očitavanje", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (latest != null) {
                    "${unit.formatValue(latest.value)} ${unit.suffix}"
                } else {
                    "-- ${unit.suffix}"
                },
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(text = latest?.let { dateTimeLabel(it.timestamp) } ?: "", color = Color.White)
            Spacer(Modifier.height(8.dp))
            GlucoseLevelRow(latest?.value)
        }
    }
}

@Composable
private fun GlucoseLevelRow(value: Int?) {
    if (value == null) return
    val (label, color) = when {
        value < LOWER_GLUCOSE_THRESHOLD -> "Ispod cilja" to GlucoseLow
        value <= HIGH_GLUCOSE_THRESHOLD -> "U cilju" to GlucoseNormal
        else -> "Iznad cilja" to GlucoseHigh
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayHeader(
    selectedDayStartMillis: Long,
    canGoToNextDay: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavArrow(symbol = "‹", enabled = true, onClick = onPreviousDay)
        Text(
            text = dayLabel(selectedDayStartMillis),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        NavArrow(symbol = "›", enabled = canGoToNextDay, onClick = onNextDay)
    }
}

@Composable
private fun NavArrow(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.headlineSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
            }
        )
    }
}

private fun dayLabel(dayStartMillis: Long): String {
    val today = startOfDayMillis(currentTimeMillis())
    return when (dayStartMillis) {
        today -> "Danas"
        shiftedDayStartMillis(today, -1) -> "Juče"
        else -> shortWeekdayDateLabel(dayStartMillis)
    }
}

@Composable
private fun ReadingRow(
    reading: GlucoseReading,
    unit: GlucoseUnit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "${unit.formatValue(reading.value)} ${unit.suffix}", fontWeight = FontWeight.Bold)
                Text(text = timeOfDayLabel(reading.timestamp), style = MaterialTheme.typography.bodySmall)
                if (reading.comment.isNotBlank()) {
                    Text(text = reading.comment, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Text(text = "✕", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Prost Canvas-baziran linijski grafik (bez eksternih biblioteka - Vico, korišćen u Android
// ekranu, nije Compose Multiplatform kompatibilan) - dovoljan za MVP.
@Composable
private fun SimpleLineChart(readings: List<GlucoseReading>, unit: GlucoseUnit, modifier: Modifier = Modifier) {
    val ordered = remember(readings) { readings.sortedBy { it.timestamp } }
    val values = remember(ordered, unit) {
        ordered.map { reading ->
            if (unit == GlucoseUnit.MMOL_L) {
                GlucoseUnit.convertMgDlToMmolL(reading.value.toDouble()).toFloat()
            } else {
                reading.value.toFloat()
            }
        }
    }
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minValue = values.min()
        val maxValue = values.max()
        val range = (maxValue - minValue).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = size.height - ((value - minValue) / range) * size.height
            )
        }
        for (index in 0 until points.size - 1) {
            drawLine(color = InsulinkBlue, start = points[index], end = points[index + 1], strokeWidth = 6f)
        }
        points.forEach { point ->
            drawCircle(color = InsulinkBlue, radius = 8f, center = point)
        }
    }
}

@Composable
private fun AddEditReadingDialog(
    value: String,
    onValueChange: (String) -> Unit,
    comment: String,
    onCommentChange: (String) -> Unit,
    unit: GlucoseUnit,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isEditMode) "Izmeni očitavanje" else "Novo očitavanje") },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Vrednost (${unit.suffix})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    label = { Text("Komentar") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = value.toDoubleOrNull() != null) {
                Text("Sačuvaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Otkaži") }
        }
    )
}

private val InsulinkBlue = Color(0xFF4A7BF6)
private val InsulinkPurple = Color(0xFF8A5CF5)
private val GlucoseLow = Color(0xFFEF5350)
private val GlucoseNormal = Color(0xFF66BB6A)
private val GlucoseHigh = Color(0xFFFFEE58)
private const val LOWER_GLUCOSE_THRESHOLD = 70
private const val HIGH_GLUCOSE_THRESHOLD = 126
private const val SWIPE_THRESHOLD_PX = 120f
private const val DISABLED_ALPHA = 0.4f
