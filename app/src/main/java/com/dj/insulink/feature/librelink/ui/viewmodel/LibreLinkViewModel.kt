package com.dj.insulink.feature.librelink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.sync.LibreLinkSyncScheduler
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LibreLinkConnectState {
    data object Idle : LibreLinkConnectState()
    data object Connecting : LibreLinkConnectState()
    data class Error(val message: String) : LibreLinkConnectState()
}

@HiltViewModel
class LibreLinkViewModel @Inject constructor(
    private val libreLinkRepository: LibreLinkRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: LibreLinkSyncScheduler
) : ViewModel() {

    private val _session = MutableStateFlow(libreLinkRepository.getSession())
    val session: StateFlow<LibreLinkSession?> = _session.asStateFlow()

    private val _lastSyncedTimestamp = MutableStateFlow(libreLinkRepository.getLastSyncedTimestamp())
    val lastSyncedTimestamp: StateFlow<Long?> = _lastSyncedTimestamp.asStateFlow()

    private val _lastSyncError = MutableStateFlow(libreLinkRepository.getLastSyncError())
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private val _connectState = MutableStateFlow<LibreLinkConnectState>(LibreLinkConnectState.Idle)
    val connectState: StateFlow<LibreLinkConnectState> = _connectState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun setEmail(value: String) {
        _email.value = value
    }

    fun setPassword(value: String) {
        _password.value = value
    }

    fun refreshStatus() {
        _session.value = libreLinkRepository.getSession()
        _lastSyncedTimestamp.value = libreLinkRepository.getLastSyncedTimestamp()
        _lastSyncError.value = libreLinkRepository.getLastSyncError()
    }

    fun connect() {
        val emailValue = _email.value
        val passwordValue = _password.value
        if (emailValue.isBlank() || passwordValue.isBlank()) return

        viewModelScope.launch {
            _connectState.value = LibreLinkConnectState.Connecting

            libreLinkRepository.connect(emailValue, passwordValue).fold(
                onSuccess = { session ->
                    _session.value = session
                    _connectState.value = LibreLinkConnectState.Idle
                    _password.value = ""
                    syncScheduler.enqueue()

                    val userId = authRepository.getCurrentUserFlow().first()
                    if (userId != null) {
                        libreLinkRepository.syncLatestReadings(userId)
                    }
                    refreshStatus()
                },
                onFailure = { error ->
                    _connectState.value = LibreLinkConnectState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun disconnect() {
        libreLinkRepository.disconnect()
        syncScheduler.cancel()
        _connectState.value = LibreLinkConnectState.Idle
        refreshStatus()
    }
}
