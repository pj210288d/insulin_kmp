package com.dj.insulink.shared.core.localization

import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Globalno posmatran trenutni jezik - isti obrazac kao AuthSession/UserSession (core/auth,
// core/session): jedan izvor istine koji App() root (navigacija) i SettingsScreen oboje čitaju,
// bez potrebe da App.kt zavisi direktno od SettingsViewModel-a.
//
// Namerno OGRANIČEN obim (odluka korisnika 2026-09-07, vidi dnevnik.md): stvarna promena jezika
// (ne samo perzistencija, koja je oduvek radila preko SettingsPreferences) pokriva SAMO
// navigaciju (bottom bar/sidebar nazivi, App.kt) i sam Settings ekran. Ostatak deljenog UI-ja
// (Glucose, Meals, Fitness, Reminders, Friends, Reports, Insulin, Statistics, LibreLink, Auth
// ekrani) ostaje hardkodovan na srpskom - pun i18n bi zahtevao prevod SVAKOG stringa na SVAKOM
// od tih ekrana, van obima večeri pred rok. Jedinica za glukozu (mmol/L ↔ mg/dL) je već potpuno
// funkcionalna u celom deljenom UI-ju preko SettingsPreferences.getGlucoseUnit() - nije ovde.
object LocalizationSession {
    private val _currentLanguage = MutableStateFlow(AppLanguage.SERBIAN)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage

    fun setCurrentLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    // Poziva se jednom iz App()-a pri prvoj kompoziciji da nav labele odmah odražavaju već
    // perzistiran izbor (ako korisnik nije još otvorio Settings tab ove sesije, SettingsViewModel
    // - koji inače ažurira ovaj isti state - nije još ni instanciran preko Koin-a).
    fun restoreFrom(settingsPreferences: SettingsPreferences) {
        _currentLanguage.value = settingsPreferences.getLanguage()
    }
}
