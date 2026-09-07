package com.dj.insulink.shared.feature.reports.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.reports.data.PdfReportGenerator
import com.dj.insulink.shared.feature.reports.data.PdfShareCoordinator
import com.dj.insulink.shared.feature.reports.data.isPdfReportSupported
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Deseti (i poslednji planirani) deljeni Compose Multiplatform MVP ekran - Faza 6, najniži
// prioritet (Statistics tab već pokriva glavnu vrednost bez PDF izvoza). Isti obim kao
// Android-ov app/feature/reports (period + statistika + lista očitavanja), samo bez preview
// dugmeta (samo generiši + podeli - vidi PdfShareCoordinator). Reports tab se ne prikazuje
// uopšte na Android strani - vidi isPdfReportSupported.
class ReportsViewModel(
    private val glucoseRepository: GlucoseReadingRepository,
    private val settingsPreferences: SettingsPreferences,
    private val pdfReportGenerator: PdfReportGenerator,
    private val pdfShareCoordinator: PdfShareCoordinator
) : ViewModel() {

    val isSupported: Boolean = isPdfReportSupported

    init {
        viewModelScope.launch {
            UserSession.currentUserId.collect { userId ->
                if (userId != null) {
                    runCatching { initializeDateRange(userId) }
                }
            }
        }
    }

    private val _minDate = MutableStateFlow<Long?>(null)
    val minDate: StateFlow<Long?> = _minDate.asStateFlow()

    private val _maxDate = MutableStateFlow<Long?>(null)
    val maxDate: StateFlow<Long?> = _maxDate.asStateFlow()

    private val _selectedMinDate = MutableStateFlow<Long?>(null)
    val selectedMinDate: StateFlow<Long?> = _selectedMinDate.asStateFlow()

    private val _selectedMaxDate = MutableStateFlow<Long?>(null)
    val selectedMaxDate: StateFlow<Long?> = _selectedMaxDate.asStateFlow()

    private val _generationState = MutableStateFlow<PdfGenerationState>(PdfGenerationState.Idle)
    val generationState: StateFlow<PdfGenerationState> = _generationState.asStateFlow()

    private suspend fun initializeDateRange(userId: String) {
        val (earliest, latest) = glucoseRepository.getDateRange(userId)
        _minDate.value = earliest
        _maxDate.value = latest
        _selectedMinDate.value = earliest
        _selectedMaxDate.value = latest
    }

    fun updateSelectedRange(start: Long, end: Long) {
        _selectedMinDate.value = start
        _selectedMaxDate.value = end
    }

    fun generateReport() {
        val userId = UserSession.currentUserId.value ?: return
        val start = _selectedMinDate.value
        val end = _selectedMaxDate.value
        if (start == null || end == null) {
            _generationState.value = PdfGenerationState.Error("Nema očitavanja za izveštaj.")
            return
        }
        viewModelScope.launch {
            _generationState.value = PdfGenerationState.Generating
            runCatching {
                val readings = glucoseRepository.getGlucoseReadingsByDateRange(userId, start, end).first()
                val unit = settingsPreferences.getGlucoseUnit()
                pdfReportGenerator.generate(readings, start, end, unit)
            }.onSuccess { path ->
                _generationState.value = PdfGenerationState.Success(path)
            }.onFailure { error ->
                _generationState.value = PdfGenerationState.Error(
                    error.message ?: "Generisanje PDF-a nije uspelo."
                )
            }
        }
    }

    fun shareReport() {
        val path = (_generationState.value as? PdfGenerationState.Success)?.filePath ?: return
        pdfShareCoordinator.share(path)
    }
}

sealed class PdfGenerationState {
    object Idle : PdfGenerationState()
    object Generating : PdfGenerationState()
    data class Success(val filePath: String) : PdfGenerationState()
    data class Error(val message: String) : PdfGenerationState()
}
