package com.dj.insulink.shared.feature.meals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dj.insulink.shared.core.time.combineDateAndTime
import com.dj.insulink.shared.core.time.combineTimeWithDate
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.dateOnlyLabel
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.core.time.startOfDayMillis
import com.dj.insulink.shared.core.time.timeOfDayLabel
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysis
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.domain.model.MealIngredient
import com.dj.insulink.shared.feature.meals.photo.rememberMealPhotoPickerLauncher
import com.dj.insulink.shared.feature.meals.ui.viewmodel.MealsViewModel

// Osmi deljeni Compose Multiplatform MVP ekran - sada u punom paritetu sa Android-ovim
// app/feature/meals (MealsScreen.kt + AddMealScreen.kt + CreateIngredientDialog.kt +
// MyIngredientsDialog.kt tamo), namerno bez ikonica (Icons.Filled.*, isti razlog kao ostatak
// deljenog UI-ja) i bez InsulinkTheme (app-module-specifično) - dugmad su tekstualna, dodavanje
// obroka je Dialog umesto poseban navigacioni ekran (App() tab-sistem nema push navigaciju,
// isti obrazac kao Glucose/Friends dijalozi).
@Composable
fun MealsScreen(viewModel: MealsViewModel) {
    val meals by viewModel.mealsForSelectedDate.collectAsState()
    val dailyNutrition by viewModel.dailyNutrition.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val showAddDialog by viewModel.showAddMealDialog.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DateSelector(
                selectedDate = selectedDate,
                onDateSelected = viewModel::setSelectedDate,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            DailyNutritionCard(
                dailyNutrition,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Obroci",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))

            if (meals.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "Nema dodatih obroka za ovaj dan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    items(items = meals, key = { it.id }) { meal ->
                        MealRow(meal = meal, onDelete = { viewModel.deleteMeal(meal) })
                        Spacer(Modifier.height(8.dp))
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.setShowAddMealDialog(true) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Text(text = "+", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }

    if (showAddDialog) {
        AddMealDialog(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(selectedDate: Long, onDateSelected: (Long) -> Unit, modifier: Modifier = Modifier) {
    var showDatePicker by remember { mutableStateOf(false) }
    val isToday = startOfDayMillis(selectedDate) == startOfDayMillis(currentTimeMillis())

    Card(modifier = modifier.clickable { showDatePicker = true }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isToday) "Danas" else dateOnlyLabel(selectedDate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Otkaži") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DailyNutritionCard(nutrition: DailyNutrition, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NutritionCard("Kalorije", nutrition.calories.toString(), InsulinkBlue, Modifier.weight(1f))
            NutritionCard("Proteini", "${nutrition.protein}g", GlucoseNormal, Modifier.weight(1f))
            NutritionCard("Masti", "${nutrition.fat}g", LastDropLabel, Modifier.weight(1f))
            NutritionCard("UH", "${nutrition.carbs}g", GlucoseLow, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MealRow(meal: Meal, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = meal.name, fontWeight = FontWeight.Bold)
                Text(
                    text = buildString {
                        append(timeOfDayLabel(meal.timestamp))
                        meal.calories?.let { append(" · $it kcal") }
                        meal.carbs?.let { append(" · ${it}g UH") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!meal.comment.isNullOrBlank()) {
                    Text(meal.comment, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Text(text = "✕", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Isti obrazac kao Glucose-ov AddEditReadingDialog (dva OutlinedButton-a koja otvaraju
// Material3 DatePicker/TimePicker) - vidi GlucoseScreen.kt. Za razliku od Glucose-a, obroci nemaju
// ograničenje na prošlost/sadašnjost (Android-ova DateTimeInput.kt takođe nema).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealDateTimeSelector(timestamp: Long, onTimestampChange: (Long) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
            Text(dateOnlyLabel(timestamp))
        }
        OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
            Text(timeOfDayLabel(timestamp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Otkaži") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val time = localTimeOfDay(timestamp)
        val timePickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Izaberi vreme", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Otkaži") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            onTimestampChange(combineTimeWithDate(timePickerState.hour, timePickerState.minute, timestamp))
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMealDialog(viewModel: MealsViewModel) {
    val name by viewModel.newMealName.collectAsState()
    val comment by viewModel.newMealComment.collectAsState()
    val timestamp by viewModel.newMealTimestamp.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedIngredients by viewModel.selectedIngredients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showCreateIngredientDialog by viewModel.showCreateIngredientDialog.collectAsState()
    val showMyIngredientsDialog by viewModel.showMyIngredientsDialog.collectAsState()
    val userIngredients by viewModel.userIngredients.collectAsState()
    val isAnalyzingMealPhoto by viewModel.isAnalyzingMealPhoto.collectAsState()
    val mealPhotoAnalysis by viewModel.mealPhotoAnalysis.collectAsState()
    val mealPhotoAnalysisError by viewModel.mealPhotoAnalysisError.collectAsState()

    val photoPicker = rememberMealPhotoPickerLauncher(
        onPhotoPicked = { bytes -> viewModel.analyzeMealPhoto(bytes) },
        onError = { message -> viewModel.reportMealPhotoError(message) }
    )

    Dialog(onDismissRequest = { viewModel.setShowAddMealDialog(false) }) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Novi obrok",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.setShowAddMealDialog(false) }) {
                        Text("✕")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = viewModel::setNewMealName,
                        label = { Text("Naziv obroka") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    MealDateTimeSelector(timestamp = timestamp, onTimestampChange = viewModel::setNewMealTimestamp)

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { photoPicker.pickFromGallery() }) {
                            Text(if (isAnalyzingMealPhoto) "..." else "🖼 Iz galerije")
                        }
                        if (photoPicker.isCameraAvailable) {
                            TextButton(onClick = { photoPicker.takePhoto() }) {
                                Text(if (isAnalyzingMealPhoto) "..." else "📷 Slikaj")
                            }
                        }
                        if (isAnalyzingMealPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }
                    if (mealPhotoAnalysisError != null) {
                        Text(
                            text = "Greška: $mealPhotoAnalysisError",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        label = { Text("Pretraži sastojke") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.setShowCreateIngredientDialog(true) }) {
                            Text("+ Novi sastojak")
                        }
                        TextButton(onClick = { viewModel.setShowMyIngredientsDialog(true) }) {
                            Text("Moji sastojci")
                        }
                    }

                    if (searchResults.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            LazyColumn(contentPadding = PaddingValues(8.dp)) {
                                items(searchResults) { ingredient ->
                                    IngredientSearchItem(
                                        ingredient = ingredient,
                                        onAdd = { viewModel.addIngredient(ingredient, 100.0) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Dodati sastojci",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    if (selectedIngredients.isEmpty()) {
                        Text(
                            text = "Još nema dodatih sastojaka",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column {
                            selectedIngredients.forEach { mealIngredient ->
                                AddedIngredientItem(
                                    mealIngredient = mealIngredient,
                                    onRemove = { viewModel.removeIngredient(mealIngredient) },
                                    onQuantityChange = { viewModel.updateIngredientQuantity(mealIngredient, it) }
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Nutritivne vrednosti",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    val totalCalories = selectedIngredients.sumOf { (it.ingredient.caloriesPer100g * it.quantity / 100).toInt() }
                    val totalProtein = selectedIngredients.sumOf { it.ingredient.proteinPer100g * it.quantity / 100 }
                    val totalFat = selectedIngredients.sumOf { it.ingredient.fatPer100g * it.quantity / 100 }
                    val totalCarbs = selectedIngredients.sumOf { it.ingredient.carbsPer100g * it.quantity / 100 }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NutritionCard("Kalorije", totalCalories.toString(), InsulinkBlue, Modifier.weight(1f))
                        NutritionCard("Proteini", formatGrams(totalProtein), GlucoseNormal, Modifier.weight(1f))
                        NutritionCard("Masti", formatGrams(totalFat), LastDropLabel, Modifier.weight(1f))
                        NutritionCard("UH", formatGrams(totalCarbs), GlucoseLow, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = viewModel::setNewMealComment,
                        label = { Text("Komentar") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Button(
                    onClick = viewModel::submitNewMeal,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = !isLoading && name.isNotBlank() && selectedIngredients.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Sačuvaj obrok")
                    }
                }
            }
        }
    }

    if (showCreateIngredientDialog) {
        CreateIngredientDialog(
            onDismiss = { viewModel.setShowCreateIngredientDialog(false) },
            onSave = viewModel::createCustomIngredient,
            isLoading = isLoading
        )
    }

    if (showMyIngredientsDialog) {
        MyIngredientsDialog(
            userIngredients = userIngredients,
            onDismiss = { viewModel.setShowMyIngredientsDialog(false) },
            onCreateIngredient = { viewModel.setShowCreateIngredientDialog(true) },
            onDeleteIngredient = viewModel::deleteCustomIngredient
        )
    }

    val analysis = mealPhotoAnalysis
    if (analysis != null) {
        MealPhotoAnalysisDialog(
            analysis = analysis,
            onAccept = viewModel::acceptMealPhotoAnalysis,
            onDismiss = viewModel::dismissMealPhotoAnalysis
        )
    }
}

@Composable
private fun IngredientSearchItem(ingredient: Ingredient, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onAdd() }.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredient.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${ingredient.caloriesPer100g.toInt()} kcal / 100g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = "+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mealIngredient.ingredient.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${(mealIngredient.ingredient.caloriesPer100g * mealIngredient.quantity / 100).toInt()} kcal / ${mealIngredient.quantity.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = quantityText,
                onValueChange = { newValue ->
                    quantityText = newValue
                    newValue.toDoubleOrNull()?.let(onQuantityChange)
                },
                modifier = Modifier
                    .width(56.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            IconButton(onClick = onRemove) {
                Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NutritionCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(64.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CreateIngredientDialog(
    onDismiss: () -> Unit,
    onSave: (Ingredient) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var salt by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(12.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Novi sastojak", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Text("✕") }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Naziv") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(12.dp))
                Text("Nutritivne vrednosti (na 100g)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                NumberField(calories, { calories = it }, "Kalorije")
                NumberField(protein, { protein = it }, "Proteini (g)")
                NumberField(carbs, { carbs = it }, "Ugljeni hidrati (g)")
                NumberField(fat, { fat = it }, "Masti (g)")
                NumberField(sugar, { sugar = it }, "Šećeri (g)")
                NumberField(salt, { salt = it }, "So (g)")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        onSave(
                            Ingredient(
                                name = name,
                                caloriesPer100g = calories.toDoubleOrNull() ?: 0.0,
                                proteinPer100g = protein.toDoubleOrNull() ?: 0.0,
                                carbsPer100g = carbs.toDoubleOrNull() ?: 0.0,
                                fatPer100g = fat.toDoubleOrNull() ?: 0.0,
                                sugarPer100g = sugar.toDoubleOrNull() ?: 0.0,
                                saltPer100g = salt.toDoubleOrNull() ?: 0.0
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && name.isNotBlank() && calories.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Sačuvaj sastojak")
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

@Composable
private fun MyIngredientsDialog(
    userIngredients: List<Ingredient>,
    onDismiss: () -> Unit,
    onCreateIngredient: () -> Unit,
    onDeleteIngredient: (Ingredient) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Moji sastojci", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row {
                        TextButton(onClick = onCreateIngredient) { Text("+ Novi") }
                        IconButton(onClick = onDismiss) { Text("✕") }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (userIngredients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Još nemaš sopstvenih sastojaka", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(userIngredients) { ingredient ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ingredient.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${ingredient.caloriesPer100g.toInt()} kcal / 100g",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteIngredient(ingredient) }) {
                                        Text("✕", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealPhotoAnalysisDialog(
    analysis: FoodImageAnalysis,
    onAccept: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val editedNames = remember(analysis) { mutableStateListOf(*analysis.recognizedFoodNames.toTypedArray()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Prepoznata hrana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Text("✕") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Izmeni imena ako je potrebno:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    editedNames.forEachIndexed { index, name ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { editedNames[index] = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = { editedNames.removeAt(index) }) { Text("✕") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val ingredient = analysis.estimatedIngredient
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutritionCard("Kalorije", ingredient.caloriesPer100g.toInt().toString(), InsulinkBlue, Modifier.weight(1f))
                    NutritionCard("UH", formatGrams(ingredient.carbsPer100g), GlucoseLow, Modifier.weight(1f))
                    NutritionCard("Proteini", formatGrams(ingredient.proteinPer100g), GlucoseNormal, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Procena na osnovu fotografije - ispravi ako nije tačno.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Odbaci") }
                    Button(
                        onClick = { onAccept(editedNames.toList()) },
                        modifier = Modifier.weight(1f),
                        enabled = editedNames.any { it.isNotBlank() }
                    ) { Text("Dodaj") }
                }
            }
        }
    }
}

private fun formatGrams(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return "${rounded}g"
}

private val InsulinkBlue = Color(0xFF4A7BF6)
private val GlucoseNormal = Color(0xFF66BB6A)
private val GlucoseLow = Color(0xFFEF5350)
private val LastDropLabel = Color(0xFFFFA726)
