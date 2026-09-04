package com.dj.insulink.shared.feature.meals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.time.timeOfDayLabel
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.ui.viewmodel.MealsViewModel

// Osmi deljeni Compose Multiplatform MVP ekran - vidi MealsViewModel za obim/odluke (namerno
// bez foto-prepoznavanja, samo ručan unos naziva/kalorija/ugljenih hidrata).
@Composable
fun MealsScreen(viewModel: MealsViewModel) {
    val meals by viewModel.meals.collectAsState()
    val newName by viewModel.newName.collectAsState()
    val newCalories by viewModel.newCalories.collectAsState()
    val newCarbs by viewModel.newCarbs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = newName,
            onValueChange = viewModel::setNewName,
            label = { Text("Naziv obroka") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(
                value = newCalories,
                onValueChange = viewModel::setNewCalories,
                label = { Text("Kalorije") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = newCarbs,
                onValueChange = viewModel::setNewCarbs,
                label = { Text("UH (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = viewModel::addMeal, enabled = newName.isNotBlank()) {
            Text("Dodaj obrok")
        }
        Spacer(Modifier.height(8.dp))

        if (meals.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = "Nema dodatih obroka", color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            LazyColumn {
                items(items = meals, key = { it.id }) { meal ->
                    MealRow(meal = meal, onDelete = { viewModel.deleteMeal(meal) })
                }
            }
        }
    }
}

@Composable
private fun MealRow(meal: Meal, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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
            }
            IconButton(onClick = onDelete) {
                Text(text = "✕", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
