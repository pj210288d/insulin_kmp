package com.dj.insulink.shared.core.session

import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsSession {
    private val _currentGlucoseUnit = MutableStateFlow(GlucoseUnit.MG_DL)
    val currentGlucoseUnit: StateFlow<GlucoseUnit> = _currentGlucoseUnit

    fun setCurrentGlucoseUnit(unit: GlucoseUnit) {
        _currentGlucoseUnit.value = unit
    }

    fun restoreFrom(settingsPreferences: SettingsPreferences) {
        _currentGlucoseUnit.value = settingsPreferences.getGlucoseUnit()
    }
}
