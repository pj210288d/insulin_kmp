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

// Četvrti deljeni Compose Multiplatform MVP ekran - vidi Glucose/Statistics/Insulin ViewModel-e
// za obrazac. SettingsPreferences (NSUserDefaults na iOS, SharedPreferences-ekvivalent na
// Android-u preko postojećeg actual-a) je već sinhrona, čisto lokalna, bez Flow-a - ovaj
// ViewModel je samo tanak StateFlow omotač da UI reaguje na promenu izbora.
class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _language = MutableStateFlow(settingsPreferences.getLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    // Direktno izloženo iz SettingsSession (ne sopstvena kopija) - vidi SettingsSession za bitan
    // kontekst zašto (bug: promena jedinice se ranije primenjivala tek posle restarta aplikacije).
    val glucoseUnit: StateFlow<GlucoseUnit> = SettingsSession.currentGlucoseUnit

    fun setLanguage(language: AppLanguage) {
        settingsPreferences.setLanguage(language)
        _language.value = language
        // Ažurira i nav labele u App.kt (bottom bar/sidebar) - vidi LocalizationSession za obim.
        LocalizationSession.setCurrentLanguage(language)
    }

    fun setGlucoseUnit(unit: GlucoseUnit) {
        settingsPreferences.setGlucoseUnit(unit)
        SettingsSession.setCurrentGlucoseUnit(unit)
    }
}
