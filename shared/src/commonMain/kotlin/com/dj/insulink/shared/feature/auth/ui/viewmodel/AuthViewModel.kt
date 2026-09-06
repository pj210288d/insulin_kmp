package com.dj.insulink.shared.feature.auth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Koin single, isti obrazac kao ostalih 8 deljenih ekrana. Pokriva Login/Registration/
// ForgotPassword ekrane (App.kt bira koji se prikazuje preko lokalnog nav state-a - vidi
// AuthScreen enum tamo). restoreSession() se zove jednom iz App()-a pri prvoj kompoziciji.
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = authRepository.currentUserFlow

    private val _isRestoringSession = MutableStateFlow(true)
    val isRestoringSession: StateFlow<Boolean> = _isRestoringSession.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    fun restoreSession() {
        viewModelScope.launch {
            runCatching { authRepository.restoreSession() }
            _isRestoringSession.value = false
        }
    }

    fun login(email: String, password: String) {
        runAuthAction { authRepository.login(email.trim(), password) }
    }

    fun register(firstName: String, lastName: String, email: String, password: String) {
        runAuthAction {
            authRepository.register(firstName.trim(), lastName.trim(), email.trim(), password)
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            runCatching { authRepository.sendPasswordResetEmail(email.trim()) }
                .onSuccess { _infoMessage.value = "Email za reset lozinke je poslat." }
                .onFailure { _errorMessage.value = it.message ?: "Slanje emaila nije uspelo." }
            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _infoMessage.value = null
    }

    private fun runAuthAction(action: suspend () -> AuthUser) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            runCatching { action() }
                .onFailure { _errorMessage.value = it.message ?: "Nešto nije u redu, pokušaj ponovo." }
            _isLoading.value = false
        }
    }
}
