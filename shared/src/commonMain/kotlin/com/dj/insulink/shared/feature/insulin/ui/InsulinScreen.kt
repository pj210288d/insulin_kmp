package com.dj.insulink.shared.feature.insulin.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.shared.feature.insulin.ui.viewmodel.InsulinViewModel

// Treći deljeni Compose Multiplatform MVP ekran - vidi InsulinViewModel za obim/odluke. Isti
// stil kao Glucose/Statistics ekrani u istom modulu (bez ikonica, bez string resursa/teme).
@Composable
fun InsulinScreen(viewModel: InsulinViewModel) {
    val insulinTypes by viewModel.insulinTypes.collectAsState()
    val newTypeName by viewModel.newTypeName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTypeName,
                onValueChange = viewModel::setNewTypeName,
                label = { Text("Naziv insulina (npr. Humalog)") },
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = viewModel::addInsulinType, enabled = newTypeName.isNotBlank()) {
                Text("Dodaj")
            }
        }
        Spacer(Modifier.height(12.dp))

        if (insulinTypes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nema dodatih tipova insulina",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            LazyColumn {
                items(items = insulinTypes, key = { it.id }) { insulinType ->
                    InsulinTypeRow(insulinType = insulinType, onDelete = { viewModel.deleteInsulinType(insulinType) })
                }
            }
        }
    }
}

@Composable
private fun InsulinTypeRow(insulinType: InsulinType, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = insulinType.name)
            IconButton(onClick = onDelete) {
                Text(text = "✕", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
