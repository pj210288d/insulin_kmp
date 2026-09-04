package com.dj.insulink.shared.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.settings.ui.viewmodel.SettingsViewModel

// Četvrti deljeni Compose Multiplatform MVP ekran - vidi SettingsViewModel za obim. Namerno
// druga vrsta UI-ja od prethodna tri (biranje iz par opcija, ne lista+CRUD), da se u snimku
// vidi da se i lokalno perzistiran izbor (NSUserDefaults na iOS-u) menja identično na oba OS-a.
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val language by viewModel.language.collectAsState()
    val glucoseUnit by viewModel.glucoseUnit.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        SectionTitle("Jezik")
        Spacer(Modifier.height(8.dp))
        AppLanguage.entries.forEach { option ->
            SelectableRow(
                label = option.displayName,
                selected = option == language,
                onClick = { viewModel.setLanguage(option) }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Jedinica za glukozu")
        Spacer(Modifier.height(8.dp))
        GlucoseUnit.entries.forEach { option ->
            SelectableRow(
                label = option.label,
                selected = option == glucoseUnit,
                onClick = { viewModel.setGlucoseUnit(option) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) InsulinkBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionDot(selected = selected)
            Spacer(Modifier.width(12.dp))
            Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color = if (selected) InsulinkBlue else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            )
    )
}

private val InsulinkBlue = Color(0xFF4A7BF6)
