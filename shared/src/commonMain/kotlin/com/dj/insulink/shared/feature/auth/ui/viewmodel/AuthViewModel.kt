package com.dj.insulink.shared.feature.auth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import com.dj.insulink.shared.feature.auth.domain.repository.isGoogleSignInSupported
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = authRepository.currentUserFlow
    val googleSignInAvailable: Boolean = isGoogleSignInSupported

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

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean
    ) {
        if (password.length < 8) {
            _errorMessage.value = "Lozinka mora imati najmanje 8 karaktera."
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Lozinke se ne poklapaju."
            return
        }
        if (!termsAccepted) {
            _errorMessage.value = "Morate prihvatiti Uslove korišćenja i Politiku privatnosti"
            return
        }
        runAuthAction {
            authRepository.register(firstName.trim(), lastName.trim(), email.trim(), password)
        }
    }

    fun signInWithGoogle() {
        runAuthAction { authRepository.signInWithGoogle() }
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
