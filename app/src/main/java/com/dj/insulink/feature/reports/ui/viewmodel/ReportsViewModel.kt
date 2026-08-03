package com.dj.insulink.feature.reports.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.feature.dataREMOVE.pdf.GlucoseReportPdfGenerator
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import com.dj.insulink.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val glucoseRepository: GlucoseReadingRepository,
    private val settingsPreferences: SettingsPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val pdfGenerator = GlucoseReportPdfGenerator(context)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _minDate = MutableStateFlow<Long?>(null)
    val minDate: StateFlow<Long?> = _minDate.asStateFlow()

    private val _maxDate = MutableStateFlow<Long?>(null)
    val maxDate: StateFlow<Long?> = _maxDate.asStateFlow()

    private val _selectedMinDate = MutableStateFlow<Long?>(null)
    val selectedMinDate: StateFlow<Long?> = _selectedMinDate.asStateFlow()

    private val _selectedMaxDate = MutableStateFlow<Long?>(null)
    val selectedMaxDate: StateFlow<Long?> = _selectedMaxDate.asStateFlow()

    private val _filteredReadings = MutableStateFlow<List<GlucoseReading>>(emptyList())
    val filteredReadings: StateFlow<List<GlucoseReading>> = _filteredReadings.asStateFlow()

    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()

    private val _pdfGenerationState = MutableStateFlow<PdfGenerationState>(PdfGenerationState.Idle)
    val pdfGenerationState: StateFlow<PdfGenerationState> = _pdfGenerationState.asStateFlow()

    fun initializeDateRange(userId: String) {
        viewModelScope.launch {
            try {
                val (earliestDate, latestDate) = glucoseRepository.getDateRange(userId)
                _minDate.value = earliestDate
                _maxDate.value = latestDate
                _selectedMinDate.value = earliestDate
                _selectedMaxDate.value = latestDate

                if (earliestDate != null && latestDate != null) {
                    filterReadingsByDateRange(userId, earliestDate, latestDate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun filterReadingsByCurrentDateRange(userId: String) {
        val currentMinDate = _minDate.value
        val currentMaxDate = _maxDate.value

        if (currentMinDate != null && currentMaxDate != null) {
            filterReadingsByDateRange(userId, currentMinDate, currentMaxDate)
        }
    }

    fun filterReadingsByDateRange(userId: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            try {
                glucoseRepository.getGlucoseReadingsByDateRange(userId, startDate, endDate)
                    .collect { readings ->
                        _filteredReadings.value = readings.sortedBy { it.timestamp }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateDateRange(startDate: Long, endDate: Long) {
        _selectedMinDate.value = startDate
        _selectedMaxDate.value = endDate
    }

    fun resetDateRange(userId: String) {
        viewModelScope.launch {
            val (earliestDate, latestDate) = glucoseRepository.getDateRange(userId)
            _minDate.value = earliestDate
            _maxDate.value = latestDate

            if (earliestDate != null && latestDate != null) {
                filterReadingsByDateRange(userId, earliestDate, latestDate)
            }
        }
    }

    fun generatePdfReport(userId: String) {
        val currentMinDate = _selectedMinDate.value
        val currentMaxDate = _selectedMaxDate.value
        val readings = _filteredReadings.value

        if (currentMinDate == null || currentMaxDate == null || readings.isEmpty()) {
            _pdfGenerationState.value = PdfGenerationState.Error(context.getString(R.string.report_no_data))
            return
        }

        viewModelScope.launch {
            _pdfGenerationState.value = PdfGenerationState.Generating

            try {
                val fileName = "glucose_report_${dateFormatter.format(Date(currentMinDate))}_to_${dateFormatter.format(Date(currentMaxDate))}.pdf"
                val outputFile = File(context.cacheDir, fileName)

                val glucoseUnit = settingsPreferences.getGlucoseUnit()
                val result = pdfGenerator.generatePdf(
                    readings = readings,
                    startDate = currentMinDate,
                    endDate = currentMaxDate,
                    outputFile = outputFile,
                    glucoseUnit = glucoseUnit
                )

                result.fold(
                    onSuccess = { file ->
                        _generatedPdfFile.value = file
                        _pdfGenerationState.value = PdfGenerationState.Success(file)
                    },
                    onFailure = { error ->
                        _pdfGenerationState.value = PdfGenerationState.Error(error.message ?: context.getString(R.string.report_pdf_failed))
                    }
                )
            } catch (e: Exception) {
                _pdfGenerationState.value = PdfGenerationState.Error(e.message ?: context.getString(R.string.report_unknown_error))
            }
        }
    }

    fun resetPdfGenerationState() {
        _pdfGenerationState.value = PdfGenerationState.Idle
        _generatedPdfFile.value = null
    }

    fun getCurrentPdfFile(): File? = _generatedPdfFile.value

}

sealed class PdfGenerationState {
    object Idle : PdfGenerationState()
    object Generating : PdfGenerationState()
    data class Success(val file: File) : PdfGenerationState()
    data class Error(val message: String) : PdfGenerationState()
}