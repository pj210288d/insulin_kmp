package com.dj.insulink.shared.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Signal na nivou procesa: "ko je trenutni lokalni korisnik". Feature-i u :shared/commonMain
// (npr. GlucoseViewModel iz feature/glucose/ui) ne zavise direktno ni od jednog auth SDK-a -
// Firebase Auth i dalje postoji samo na Android strani (AuthRepository u :app), a puni pravi
// Firebase uid ovde (vidi SharedViewModel.getCurrentUser() u :app). iOS strana (bez Firebase
// Auth-a za sada - faza 4 MVP, vidi CLAUDE.md) upisuje fiksni lokalni demo id pri pokretanju
// (MainViewController.ios.kt).
object UserSession {
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
    }
}
