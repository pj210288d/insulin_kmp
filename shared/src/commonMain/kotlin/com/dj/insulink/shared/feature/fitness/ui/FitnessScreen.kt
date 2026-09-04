package com.dj.insulink.shared.feature.fitness.ui

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
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import com.dj.insulink.shared.feature.fitness.ui.viewmodel.FitnessViewModel

// Šesti deljeni Compose Multiplatform MVP ekran - vidi FitnessViewModel za obim/odluke.
@Composable
fun FitnessScreen(viewModel: FitnessViewModel) {
    val exercises by viewModel.exercises.collectAsState()
    val sportName by viewModel.sportName.collectAsState()
    val durationMinutes by viewModel.durationMinutes.collectAsState()
    val glucoseBefore by viewModel.glucoseBefore.collectAsState()
    val glucoseAfter by viewModel.glucoseAfter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = sportName,
            onValueChange = viewModel::setSportName,
            label = { Text("Sport/aktivnost") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(
                value = durationMinutes,
                onValueChange = viewModel::setDurationMinutes,
                label = { Text("Trajanje (min)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = glucoseBefore,
                onValueChange = viewModel::setGlucoseBefore,
                label = { Text("Šećer pre") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = glucoseAfter,
                onValueChange = viewModel::setGlucoseAfter,
                label = { Text("Šećer posle") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = viewModel::addExercise,
            enabled = sportName.isNotBlank() && durationMinutes.isNotBlank() &&
                glucoseBefore.isNotBlank() && glucoseAfter.isNotBlank()
        ) {
            Text("Dodaj aktivnost")
        }
        Spacer(Modifier.height(8.dp))

        if (exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = "Nema dodatih aktivnosti", color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            LazyColumn {
                items(items = exercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise)
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = exercise.sportName, fontWeight = FontWeight.Bold)
                Text(
                    text = "${exercise.durationHours}h ${exercise.durationMinutes}min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${exercise.glucoseBefore} → ${exercise.glucoseAfter} mg/dL",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
