package com.dj.insulink.shared.core.session

import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Globalno posmatrana trenutna jedinica za glukozu - isti obrazac kao UserSession/AuthSession/
// LocalizationSession (jedan izvor istine koji SVI ViewModel-i čitaju direktno, umesto da svaki
// drži sopstvenu kopiju pročitanu samo jednom pri konstrukciji).
//
// Popravka 2026-09-07 (korisnik prijavio bug uživo): svaki od GlucoseViewModel/
// StatisticsViewModel/FriendsViewModel je ranije imao SOPSTVENI
// `MutableStateFlow(settingsPreferences.getGlucoseUnit())` inicijalizovan JEDNOM pri Koin
// kreiranju (single, živi ceo život aplikacije) - promena jedinice u Settings ekranu nije imala
// efekta dok se aplikacija ne ugasi i ponovo pokrene (novo Koin kreiranje = nova inicijalna
// vrednost). Svaki ViewModel je imao i mrtvu `refreshGlucoseUnit()` funkciju koju niko nije
// pozivao (nema lifecycle-based re-entry eventa u App()-ovom `when`-blok navigacionom obrascu
// da bi je pozvao, za razliku od Android-ovih pravih ekrana). Rešenje: isti princip kao
// LocalizationSession - jedan globalni StateFlow, SettingsViewModel.setGlucoseUnit() ga
// ažurira, svi ostali ViewModel-i ga direktno izlažu (bez sopstvene kopije), pa promena stiže
// do svakog ekrana odmah preko Compose recompozicije.
object SettingsSession {
    private val _currentGlucoseUnit = MutableStateFlow(GlucoseUnit.MG_DL)
    val currentGlucoseUnit: StateFlow<GlucoseUnit> = _currentGlucoseUnit

    fun setCurrentGlucoseUnit(unit: GlucoseUnit) {
        _currentGlucoseUnit.value = unit
    }

    // Poziva se jednom iz App()-a pri prvoj kompoziciji (isti obrazac kao
    // LocalizationSession.restoreFrom) da svi ekrani odmah odražavaju već perzistiran izbor, i
    // pre nego što korisnik ikad otvori Settings tab ove sesije.
    fun restoreFrom(settingsPreferences: SettingsPreferences) {
        _currentGlucoseUnit.value = settingsPreferences.getGlucoseUnit()
    }
}
