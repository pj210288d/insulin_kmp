package com.dj.insulink.feature.statistics.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.statistics.domain.StatisticsCalculator
import com.dj.insulink.shared.feature.statistics.domain.model.GlucoseStatistics
import com.dj.insulink.shared.feature.statistics.domain.model.StatisticsRange
import com.dj.insulink.shared.feature.statistics.domain.model.startMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val glucoseReadingRepository: GlucoseReadingRepository,
    private val authRepository: AuthRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _glucoseUnit = MutableStateFlow(settingsPreferences.getGlucoseUnit())
    val glucoseUnit: StateFlow<GlucoseUnit> = _glucoseUnit.asStateFlow()

    fun refreshGlucoseUnit() {
        _glucoseUnit.value = settingsPreferences.getGlucoseUnit()
    }

    private val _selectedRange = MutableStateFlow(StatisticsRange.LAST_7_DAYS)
    val selectedRange: StateFlow<StatisticsRange> = _selectedRange.asStateFlow()

    fun setRange(range: StatisticsRange) {
        _selectedRange.value = range
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val statistics: StateFlow<GlucoseStatistics?> =
        combine(authRepository.getCurrentUserFlow(), _selectedRange) { userId, range -> userId to range }
            .flatMapLatest { (userId, range) ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    glucoseReadingRepository.getGlucoseReadingsByDateRange(userId, range.startMillis(), currentTimeMillis())
                }
            }
            .map { StatisticsCalculator.calculateGlucoseStatistics(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
