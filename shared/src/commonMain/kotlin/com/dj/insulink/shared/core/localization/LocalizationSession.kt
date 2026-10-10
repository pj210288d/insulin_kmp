package com.dj.insulink.shared.core.localization

import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LocalizationSession {
    private val _currentLanguage = MutableStateFlow(AppLanguage.SERBIAN)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage

    fun setCurrentLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun restoreFrom(settingsPreferences: SettingsPreferences) {
        _currentLanguage.value = settingsPreferences.getLanguage()
    }
}
