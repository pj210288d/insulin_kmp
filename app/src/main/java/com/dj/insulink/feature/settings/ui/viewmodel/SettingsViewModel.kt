package com.dj.insulink.feature.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(settingsPreferences.getLanguage())
    val selectedLanguage = _selectedLanguage.asStateFlow()

    private val _selectedGlucoseUnit = MutableStateFlow(settingsPreferences.getGlucoseUnit())
    val selectedGlucoseUnit = _selectedGlucoseUnit.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        settingsPreferences.setLanguage(language)
        _selectedLanguage.value = language
    }

    fun setGlucoseUnit(unit: GlucoseUnit) {
        settingsPreferences.setGlucoseUnit(unit)
        _selectedGlucoseUnit.value = unit
    }
}
