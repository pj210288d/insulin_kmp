package com.dj.insulink.shared.feature.reports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.localization.tr
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.dateOnlyLabel
import com.dj.insulink.shared.feature.reports.ui.viewmodel.PdfGenerationState
import com.dj.insulink.shared.feature.reports.ui.viewmodel.ReportsViewModel

// Deseti (i poslednji planirani) deljeni Compose Multiplatform MVP ekran - vidi ReportsViewModel
// za obim/odluke. Ne prikazuje se uopšte kad viewModel.isSupported == false (Android demo).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel) {
    val language by LocalizationSession.currentLanguage.collectAsState()

    if (!viewModel.isSupported) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tr(
                    language,
                    "PDF izveštaj je dostupan samo na iOS-u u ovoj deljenoj demo verziji - " +
                        "Android ima svoj puni PDF izvoz u glavnom Reports ekranu.",
                    "The PDF report is only available on iOS in this shared demo version - " +
                        "Android has its own full PDF export in the main Reports screen."
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val minDate by viewModel.minDate.collectAsState()
    val maxDate by viewModel.maxDate.collectAsState()
    val selectedMinDate by viewModel.selectedMinDate.collectAsState()
    val selectedMaxDate by viewModel.selectedMaxDate.collectAsState()
    val generationState by viewModel.generationState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var isSelectingStart by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = tr(language, "Izveštaj o glukozi", "Glucose report"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tr(
                language,
                "Izaberi period i generiši PDF izveštaj sa statistikom i listom očitavanja.",
                "Choose a period and generate a PDF report with statistics and the list of readings."
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Text(tr(language, "Od:", "From:"))
        OutlinedButton(
            onClick = { isSelectingStart = true; showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedMinDate?.let { dateOnlyLabel(it) } ?: "-")
        }
        Spacer(Modifier.height(12.dp))
        Text(tr(language, "Do:", "To:"))
        OutlinedButton(
            onClick = { isSelectingStart = false; showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedMaxDate?.let { dateOnlyLabel(it) } ?: "-")
        }
        Spacer(Modifier.height(24.dp))

        val isGenerating = generationState is PdfGenerationState.Generating
        Button(
            onClick = viewModel::generateReport,
            enabled = !isGenerating && selectedMinDate != null && selectedMaxDate != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
            } else {
                Text(tr(language, "Generiši PDF", "Generate PDF"))
            }
        }

        when (val state = generationState) {
            is PdfGenerationState.Success -> {
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::shareReport, modifier = Modifier.fillMaxWidth()) {
                    Text(tr(language, "Podeli PDF", "Share PDF"))
                }
            }
            is PdfGenerationState.Error -> {
                Spacer(Modifier.height(12.dp))
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
            else -> Unit
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = maxDate ?: currentTimeMillis(),
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
                        if (isSelectingStart) {
                            viewModel.updateSelectedRange(millis, selectedMaxDate ?: maxDate ?: millis)
                        } else {
                            viewModel.updateSelectedRange(selectedMinDate ?: minDate ?: millis, millis)
                        }
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
}
