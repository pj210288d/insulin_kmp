package com.dj.insulink.shared.feature.librelink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.librelink.data.sync.LibreLinkSyncCoordinator
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibreLinkConnectState {
    data object Disconnected : LibreLinkConnectState
    data object Connecting : LibreLinkConnectState
    data class ChoosingConnection(
        val email: String,
        val auth: LibreLinkAuth,
        val connections: List<LibreLinkConnection>
    ) : LibreLinkConnectState
    data class Connected(val session: LibreLinkSession) : LibreLinkConnectState
    data class Error(val message: String) : LibreLinkConnectState
}

class LibreLinkViewModel(
    private val libreLinkRepository: LibreLinkRepository,
    private val syncCoordinator: LibreLinkSyncCoordinator
) : ViewModel() {

    private val _connectState = MutableStateFlow<LibreLinkConnectState>(LibreLinkConnectState.Disconnected)
    val connectState: StateFlow<LibreLinkConnectState> = _connectState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        val userId = UserSession.currentUserId.value
        val existingSession = userId?.let { libreLinkRepository.getSession(it) }
        if (existingSession != null && userId != null) {
            _connectState.value = LibreLinkConnectState.Connected(existingSession)
            startPeriodicSync(userId)
        }
    }

    fun setEmail(value: String) {
        _email.value = value
    }

    fun setPassword(value: String) {
        _password.value = value
    }

    fun login() {
        val userId = UserSession.currentUserId.value ?: return
        val enteredEmail = _email.value.trim()
        val enteredPassword = _password.value
        if (enteredEmail.isEmpty() || enteredPassword.isEmpty()) return

        _connectState.value = LibreLinkConnectState.Connecting
        viewModelScope.launch {
            libreLinkRepository.login(enteredEmail, enteredPassword)
                .onSuccess { result ->
                    if (result.connections.size == 1) {
                        finishConnecting(userId, enteredEmail, result.auth, result.connections.first())
                    } else {
                        _connectState.value = LibreLinkConnectState.ChoosingConnection(
                            email = enteredEmail,
                            auth = result.auth,
                            connections = result.connections
                        )
                    }
                }
                .onFailure { throwable ->
                    _connectState.value = LibreLinkConnectState.Error(throwable.message ?: "Prijava nije uspela")
                }
        }
    }

    fun selectConnection(connection: LibreLinkConnection) {
        val userId = UserSession.currentUserId.value ?: return
        val state = _connectState.value as? LibreLinkConnectState.ChoosingConnection ?: return
        viewModelScope.launch {
            finishConnecting(userId, state.email, state.auth, connection)
        }
    }

    private suspend fun finishConnecting(
        userId: String,
        email: String,
        auth: LibreLinkAuth,
        connection: LibreLinkConnection
    ) {
        libreLinkRepository.connect(userId, email, auth, connection)
            .onSuccess { session ->
                _connectState.value = LibreLinkConnectState.Connected(session)
                _password.value = ""
                startPeriodicSync(userId)
            }
            .onFailure { throwable ->
                _connectState.value = LibreLinkConnectState.Error(throwable.message ?: "Povezivanje nije uspelo")
            }
    }

    fun cancelChoosingConnection() {
        _connectState.value = LibreLinkConnectState.Disconnected
    }

    fun disconnect() {
        val userId = UserSession.currentUserId.value ?: return
        libreLinkRepository.disconnect(userId)
        _connectState.value = LibreLinkConnectState.Disconnected
        _email.value = ""
        stopPeriodicSync()
    }

    fun syncNow() {
        val userId = UserSession.currentUserId.value ?: return
        _isSyncing.value = true
        viewModelScope.launch {
            runCatching { libreLinkRepository.syncLatestReadings(userId) }
            _isSyncing.value = false
        }
    }

    private fun startPeriodicSync(userId: String) {
        syncCoordinator.start(userId)
    }

    private fun stopPeriodicSync() {
        syncCoordinator.stop()
    }
}
