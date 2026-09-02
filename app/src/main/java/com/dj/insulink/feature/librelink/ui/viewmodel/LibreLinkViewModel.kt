package com.dj.insulink.feature.librelink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.sync.LibreLinkSyncScheduler
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkLoginResult
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class LibreLinkConnectState {
    data object Idle : LibreLinkConnectState()
    data object Connecting : LibreLinkConnectState()
    // A LibreLinkUp account can follow more than one patient (e.g. also a family member's
    // sensor) - the user picks which one Insulink should sync before we persist a session.
    data class ChoosingConnection(val connections: List<LibreLinkConnection>) : LibreLinkConnectState()
    data class Error(val message: String) : LibreLinkConnectState()
}

@HiltViewModel
class LibreLinkViewModel @Inject constructor(
    private val libreLinkRepository: LibreLinkRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: LibreLinkSyncScheduler
) : ViewModel() {

    // LibreLinkUp session/status are keyed by the currently signed-in Insulink user, not a
    // single device-wide slot - so refreshStatus() must be re-invoked by the caller (see
    // SettingsWrapper's LaunchedEffect(currentUser)) whenever the signed-in user changes,
    // instead of ever showing one Insulink user's LibreLinkUp connection to another.
    // Exposed so callers (see SettingsWrapper's LaunchedEffect) can key a refreshStatus()
    // trigger off the signed-in user actually changing, not just the first composition.
    val currentUserId: StateFlow<String?> = authRepository.getCurrentUserFlow()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    private val _session = MutableStateFlow<LibreLinkSession?>(null)
    val session: StateFlow<LibreLinkSession?> = _session.asStateFlow()

    private val _lastSyncedTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncedTimestamp: StateFlow<Long?> = _lastSyncedTimestamp.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private val _connectState = MutableStateFlow<LibreLinkConnectState>(LibreLinkConnectState.Idle)
    val connectState: StateFlow<LibreLinkConnectState> = _connectState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    // Held in memory only (never persisted) between login() returning multiple connections and
    // the user picking one via selectConnection().
    private var pendingLogin: LibreLinkLoginResult? = null

    fun setEmail(value: String) {
        _email.value = value
    }

    fun setPassword(value: String) {
        _password.value = value
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserFlow().first()
            if (userId != null) {
                _session.value = libreLinkRepository.getSession(userId)
                _lastSyncedTimestamp.value = libreLinkRepository.getLastSyncedTimestamp(userId)
                _lastSyncError.value = libreLinkRepository.getLastSyncError(userId)
            } else {
                _session.value = null
                _lastSyncedTimestamp.value = null
                _lastSyncError.value = null
            }
        }
    }

    fun connect() {
        val emailValue = _email.value
        val passwordValue = _password.value
        if (emailValue.isBlank() || passwordValue.isBlank()) return

        viewModelScope.launch {
            _connectState.value = LibreLinkConnectState.Connecting

            libreLinkRepository.login(emailValue, passwordValue).fold(
                onSuccess = { loginResult ->
                    if (loginResult.connections.size == 1) {
                        finalizeConnect(loginResult, loginResult.connections.single())
                    } else {
                        pendingLogin = loginResult
                        _connectState.value = LibreLinkConnectState.ChoosingConnection(loginResult.connections)
                    }
                },
                onFailure = { error ->
                    _connectState.value = LibreLinkConnectState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun selectConnection(connection: LibreLinkConnection) {
        val loginResult = pendingLogin ?: return
        viewModelScope.launch {
            _connectState.value = LibreLinkConnectState.Connecting
            finalizeConnect(loginResult, connection)
        }
    }

    fun cancelSelectingConnection() {
        pendingLogin = null
        _connectState.value = LibreLinkConnectState.Idle
    }

    private suspend fun finalizeConnect(loginResult: LibreLinkLoginResult, connection: LibreLinkConnection) {
        val userId = authRepository.getCurrentUserFlow().first() ?: return
        libreLinkRepository.connect(userId, loginResult.email, loginResult.auth, connection).fold(
            onSuccess = {
                pendingLogin = null
                _connectState.value = LibreLinkConnectState.Idle
                _password.value = ""
                syncScheduler.enqueue()
                libreLinkRepository.syncLatestReadings(userId)
                refreshStatus()
            },
            onFailure = { error ->
                _connectState.value = LibreLinkConnectState.Error(error.message ?: "Unknown error")
            }
        )
    }

    fun syncNow() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserFlow().first() ?: return@launch
            _isSyncing.value = true
            libreLinkRepository.syncLatestReadings(userId)
            refreshStatus()
            _isSyncing.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserFlow().first() ?: return@launch
            libreLinkRepository.disconnect(userId)
            syncScheduler.cancel()
            _connectState.value = LibreLinkConnectState.Idle
            refreshStatus()
        }
    }
}
