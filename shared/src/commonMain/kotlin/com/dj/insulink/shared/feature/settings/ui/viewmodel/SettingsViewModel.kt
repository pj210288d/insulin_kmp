package com.dj.insulink.shared.feature.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.session.SettingsSession
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _language = MutableStateFlow(settingsPreferences.getLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    val glucoseUnit: StateFlow<GlucoseUnit> = SettingsSession.currentGlucoseUnit

    fun setLanguage(language: AppLanguage) {
        settingsPreferences.setLanguage(language)
        _language.value = language
        LocalizationSession.setCurrentLanguage(language)
    }

    fun setGlucoseUnit(unit: GlucoseUnit) {
        settingsPreferences.setGlucoseUnit(unit)
        SettingsSession.setCurrentGlucoseUnit(unit)
    }
}
