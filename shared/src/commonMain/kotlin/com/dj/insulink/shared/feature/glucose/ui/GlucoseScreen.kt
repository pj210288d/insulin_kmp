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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.localization.tr
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.core.time.combineDateAndTime
import com.dj.insulink.shared.core.time.combineTimeWithDate
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.dateOnlyLabel
import com.dj.insulink.shared.core.time.dateTimeLabel
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.core.time.shiftedDayStartMillis
import com.dj.insulink.shared.core.time.shortWeekdayDateLabel
import com.dj.insulink.shared.core.time.startOfDayMillis
import com.dj.insulink.shared.core.time.timeOfDayLabel
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.glucose.ui.viewmodel.GlucoseViewModel
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

// Glucose ekran deljen preko Compose Multiplatform-a - vidi GlucoseViewModel u istom paketu za
// obim/odluke. Dijalog za dodavanje/izmenu je sada u punom paritetu sa Android-ovim
// AddGlucoseReadingDialog.kt (datum/vreme picker, insulin tip, insulinske jedinice, povezan
// obrok) - namerno bez ikonica (Icons.Filled.*) jer material-icons-core nije pouzdano dostupan
// za iOS target u pinovanoj Compose Multiplatform verziji (vidi dnevnik.md, ista odluka kao
// ostatak deljenog UI-ja) - dropdown strelice su tekstualni glifovi (▾/▴).
@Composable
fun GlucoseScreen(viewModel: GlucoseViewModel) {
    val readings by viewModel.glucoseReadingsForSelectedDay.collectAsState()
    val latest by viewModel.latestGlucoseReading.collectAsState()
    val selectedDay by viewModel.selectedDayStartMillis.collectAsState()
    val canGoNext by viewModel.canGoToNextDay.collectAsState()
    val unit by viewModel.glucoseUnit.collectAsState()
    val showDialog by viewModel.showAddDialog.collectAsState()
    val newTimestamp by viewModel.newTimestamp.collectAsState()
    val newValue by viewModel.newValue.collectAsState()
    val newComment by viewModel.newComment.collectAsState()
    val newInsulinTypeId by viewModel.newInsulinTypeId.collectAsState()
    val newInsulinUnits by viewModel.newInsulinUnits.collectAsState()
    val newLinkedMealId by viewModel.newLinkedMealId.collectAsState()
    val editing by viewModel.editingReading.collectAsState()
    val insulinTypes by viewModel.allInsulinTypesForUser.collectAsState()
    val sameDayMeals by viewModel.sameDayMealsForNewReading.collectAsState()
    val language by LocalizationSession.currentLanguage.collectAsState()

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
                StatusCard(latest, unit, language)
                Spacer(Modifier.height(12.dp))
                DayHeader(
                    selectedDayStartMillis = selectedDay,
                    canGoToNextDay = canGoNext,
                    language = language,
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
                        text = tr(language, "Nema očitavanja za ovaj dan", "No readings for this day"),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(items = readings, key = { it.id }) { reading ->
                        ReadingRow(
                            reading = reading,
                            unit = unit,
                            insulinTypes = insulinTypes,
                            language = language,
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
            timestamp = newTimestamp,
            onTimestampChange = viewModel::setNewTimestamp,
            value = newValue,
            onValueChange = viewModel::setNewValue,
            comment = newComment,
            onCommentChange = viewModel::setNewComment,
            insulinTypes = insulinTypes,
            selectedInsulinTypeId = newInsulinTypeId,
            onInsulinTypeSelected = viewModel::setNewInsulinTypeId,
            insulinUnits = newInsulinUnits,
            onInsulinUnitsChange = viewModel::setNewInsulinUnits,
            sameDayMeals = sameDayMeals,
            selectedMealId = newLinkedMealId,
            onMealSelected = viewModel::setNewLinkedMealId,
            unit = unit,
            isEditMode = editing != null,
            language = language,
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::submitReading
        )
    }
}

@Composable
private fun StatusCard(latest: GlucoseReading?, unit: GlucoseUnit, language: AppLanguage) {
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
            Text(text = tr(language, "Poslednje očitavanje", "Latest reading"), color = Color.White)
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
            GlucoseLevelRow(latest?.value, language)
        }
    }
}

@Composable
private fun GlucoseLevelRow(value: Int?, language: AppLanguage) {
    if (value == null) return
    val (label, color) = when {
        value < LOWER_GLUCOSE_THRESHOLD -> tr(language, "Ispod cilja", "Below target") to GlucoseLow
        value <= HIGH_GLUCOSE_THRESHOLD -> tr(language, "U cilju", "In target") to GlucoseNormal
        else -> tr(language, "Iznad cilja", "Above target") to GlucoseHigh
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
    language: AppLanguage,
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
            text = dayLabel(selectedDayStartMillis, language),
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

private fun dayLabel(dayStartMillis: Long, language: AppLanguage): String {
    val today = startOfDayMillis(currentTimeMillis())
    return when (dayStartMillis) {
        today -> tr(language, "Danas", "Today")
        shiftedDayStartMillis(today, -1) -> tr(language, "Juče", "Yesterday")
        else -> shortWeekdayDateLabel(dayStartMillis)
    }
}

@Composable
private fun ReadingRow(
    reading: GlucoseReading,
    unit: GlucoseUnit,
    insulinTypes: List<InsulinType>,
    language: AppLanguage,
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
                val insulinLabel = reading.insulinTypeId
                    ?.let { id -> insulinTypes.find { it.id == id }?.name }
                if (insulinLabel != null) {
                    val units = reading.insulinUnits
                    Text(
                        text = if (units != null) "$insulinLabel · $units ${tr(language, "j.", "u.")}" else insulinLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
//
// 2026-09-07: korisnik prijavio da se na iOS-u ne vide koordinate (grafik je ranije bio "goli"
// Canvas bez ijedne ose) - dodate Y osa (fiksni opseg 2-25 mmol/L, konvertovan u mg/dL kad je
// ta jedinica izabrana - isti pravi klinički opseg hipo/hiperglikemije nezavisno od jedinice) i
// X osa (sati - vreme prve/srednje/poslednje tačke, ispod grafika). Tekst se crta preko
// `TextMeasurer`/`drawText(textLayoutResult, ...)` - Compose Multiplatform-bezbedan način da se
// tekst iscrta unutar `Canvas`-a (za razliku od `nativeCanvas`, koji je platform-specifičan tip
// i ne bi radio na iOS-u).
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
    val fixedMin = remember(unit) {
        if (unit == GlucoseUnit.MMOL_L) FIXED_MIN_MMOL else GlucoseUnit.convertMmolLToMgDl(FIXED_MIN_MMOL.toDouble()).toFloat()
    }
    val fixedMax = remember(unit) {
        if (unit == GlucoseUnit.MMOL_L) FIXED_MAX_MMOL else GlucoseUnit.convertMmolLToMgDl(FIXED_MAX_MMOL.toDouble()).toFloat()
    }
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisColor)

    Canvas(modifier = modifier) {
        val leftAxisWidth = 34.dp.toPx()
        val bottomAxisHeight = 18.dp.toPx()
        val plotLeft = leftAxisWidth
        val plotWidth = (size.width - leftAxisWidth).coerceAtLeast(0f)
        val plotHeight = (size.height - bottomAxisHeight).coerceAtLeast(0f)
        val range = (fixedMax - fixedMin).coerceAtLeast(1f)

        // Y osa - fiksne linije/labele na 5 podeoka (min, ..., max), ne zavisi od stvarnih
        // vrednosti očitavanja - uvek isti opseg da bi se grafici različitih dana mogli vizuelno
        // uporediti.
        val tickCount = 4
        for (i in 0..tickCount) {
            val value = fixedMin + range * i / tickCount
            val y = plotHeight - ((value - fixedMin) / range) * plotHeight
            drawLine(
                color = axisColor.copy(alpha = 0.15f),
                start = Offset(plotLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            val layout = textMeasurer.measure(axisValueLabel(value, unit), style = labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(0f, (y - layout.size.height / 2f).coerceIn(0f, plotHeight - layout.size.height))
            )
        }

        if (values.isNotEmpty()) {
            val stepX = if (values.size > 1) plotWidth / (values.size - 1) else 0f
            val points = values.mapIndexed { index, value ->
                val clamped = value.coerceIn(fixedMin, fixedMax)
                Offset(
                    x = plotLeft + index * stepX,
                    y = plotHeight - ((clamped - fixedMin) / range) * plotHeight
                )
            }
            for (index in 0 until points.size - 1) {
                drawLine(color = InsulinkBlue, start = points[index], end = points[index + 1], strokeWidth = 6f)
            }
            points.forEach { point ->
                drawCircle(color = InsulinkBlue, radius = 8f, center = point)
            }

            // X osa - sati, prikazani samo na prvoj/srednjoj/poslednjoj tački (izbegava
            // pretrpanost kad ima puno očitavanja u danu) - koristi STVARNO vreme te tačke.
            val labelIndices = listOf(0, points.size / 2, points.size - 1).distinct()
            labelIndices.forEach { index ->
                val layout = textMeasurer.measure(timeOfDayLabel(ordered[index].timestamp), style = labelStyle)
                val x = (points[index].x - layout.size.width / 2f)
                    .coerceIn(plotLeft, size.width - layout.size.width)
                drawText(textLayoutResult = layout, topLeft = Offset(x, plotHeight + 2.dp.toPx()))
            }
        }
    }
}

// NAPOMENA (bug uhvaćen uživo na screenshot-u pre commit-a): `GlucoseUnit.formatValue(Double)`
// UVEK očekuje ulaznu vrednost u mg/dL (sam radi konverziju u mmol/L kad treba) - ne sme se
// pozvati sa vrednošću koja je VEĆ konvertovana u prikazanu jedinicu (kao `value` ovde, koji
// dolazi iz `fixedMin`/`fixedMax`, već izračunatih po jedinici) jer bi se onda mmol/L vrednost
// podelila konverzionim faktorom DRUGI PUT (2 mmol/L bi se prikazalo kao "0.1"). Ista logika kao
// `oneDecimal()` u StatisticsScreen.kt - ručno zaokruživanje bez oslanjanja na Float.toString()
// (izbegava lokalizaciono/platformsko nekonzistentno formatiranje decimala).
private fun axisValueLabel(value: Float, unit: GlucoseUnit): String {
    return when (unit) {
        GlucoseUnit.MG_DL -> value.toInt().toString()
        GlucoseUnit.MMOL_L -> {
            val scaled = kotlin.math.round(value * 10).toLong()
            val whole = scaled / 10
            val fraction = kotlin.math.abs(scaled % 10)
            "$whole.$fraction"
        }
    }
}

private const val FIXED_MIN_MMOL = 2f
private const val FIXED_MAX_MMOL = 25f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditReadingDialog(
    timestamp: Long,
    onTimestampChange: (Long) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    comment: String,
    onCommentChange: (String) -> Unit,
    insulinTypes: List<InsulinType>,
    selectedInsulinTypeId: Long?,
    onInsulinTypeSelected: (Long?) -> Unit,
    insulinUnits: String,
    onInsulinUnitsChange: (String) -> Unit,
    sameDayMeals: List<Meal>,
    selectedMealId: Long?,
    onMealSelected: (Long?) -> Unit,
    unit: GlucoseUnit,
    isEditMode: Boolean,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = timestamp,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onTimestampChange(combineDateAndTime(millis, timestamp))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(tr(language, "Otkaži", "Cancel")) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val time = localTimeOfDay(timestamp)
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
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
                            onTimestampChange(
                                combineTimeWithDate(timePickerState.hour, timePickerState.minute, timestamp)
                            )
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEditMode) tr(language, "Izmeni očitavanje", "Edit reading") else tr(language, "Novo očitavanje", "New reading"),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dateOnlyLabel(timestamp))
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(timeOfDayLabel(timestamp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        onValueChange(
                            if (unit == GlucoseUnit.MMOL_L) {
                                newValue.filter { it.isDigit() || it == '.' }
                            } else {
                                newValue.filter { it.isDigit() }
                            }
                        )
                    },
                    label = { Text("${tr(language, "Vrednost", "Value")} (${unit.suffix})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    label = { Text(tr(language, "Komentar", "Comment")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(text = tr(language, "Tip insulina", "Insulin type"), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                val noneLabel = tr(language, "Bez", "None")
                val insulinLabels = listOf(noneLabel) + insulinTypes.map { it.name }
                val selectedInsulinLabel = insulinTypes.find { it.id == selectedInsulinTypeId }?.name
                    ?: noneLabel
                SharedDropdownMenu(
                    items = insulinLabels,
                    selectedItem = selectedInsulinLabel,
                    onItemSelected = { selected ->
                        if (selected == noneLabel) {
                            onInsulinTypeSelected(null)
                        } else {
                            insulinTypes.find { it.name == selected }?.let { onInsulinTypeSelected(it.id) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = insulinUnits,
                    onValueChange = onInsulinUnitsChange,
                    label = { Text(tr(language, "Insulinske jedinice", "Insulin units")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(text = tr(language, "Povezan obrok (isti dan)", "Linked meal (same day)"), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                val mealLabels = listOf(noneLabel) + sameDayMeals.map { it.name }
                val selectedMealLabel = sameDayMeals.find { it.id == selectedMealId }?.name ?: noneLabel
                SharedDropdownMenu(
                    items = mealLabels,
                    selectedItem = selectedMealLabel,
                    onItemSelected = { selected ->
                        if (selected == noneLabel) {
                            onMealSelected(null)
                        } else {
                            sameDayMeals.find { it.name == selected }?.let { onMealSelected(it.id) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr(language, "Otkaži", "Cancel")) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (value.toDoubleOrNull() != null) {
                                onSave()
                                onDismiss()
                            }
                        },
                        enabled = value.toDoubleOrNull() != null
                    ) {
                        Text(tr(language, "Sačuvaj", "Save"))
                    }
                }
            }
        }
    }
}

// Bez ikonica (Icons.Filled.ArrowDropDown/Up) - vidi napomenu na vrhu fajla. Isti obrazac kao
// Android-ov GlucoseDropdownMenu.kt, samo bez InsulinkTheme (app-module-specifično) i bez ikonica.
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
private val InsulinkPurple = Color(0xFF8A5CF5)
private val GlucoseLow = Color(0xFFEF5350)
private val GlucoseNormal = Color(0xFF66BB6A)
private val GlucoseHigh = Color(0xFFFFEE58)
private const val LOWER_GLUCOSE_THRESHOLD = 70
private const val HIGH_GLUCOSE_THRESHOLD = 126
private const val SWIPE_THRESHOLD_PX = 120f
private const val DISABLED_ALPHA = 0.4f
