package com.dj.insulink.shared.feature.librelink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sedmi deljeni Compose Multiplatform MVP ekran - vidi ostale ViewModel-e u shared/commonMain
// za obrazac. Za razliku od Reminders/Meals, ovde nema svesnog umanjenja obima - LibreLinkRepository
// je već potpuno platform-agnostičan (Ktor za mrežu, LibreLinkSessionStorage kao interface -
// oba dokazano rade na iOS-u), pa je ovaj ekran realna 1:1 paritetna funkcionalnost sa Android-ovim
// (feature/librelink/ui/viewmodel/LibreLinkViewModel u :app), samo bez Wear OS push-a
// (WearSyncManager je Android-only i van dosega ovog MVP-a).
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
    private val libreLinkRepository: LibreLinkRepository
) : ViewModel() {

    private val _connectState = MutableStateFlow<LibreLinkConnectState>(LibreLinkConnectState.Disconnected)
    val connectState: StateFlow<LibreLinkConnectState> = _connectState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncMessage = MutableStateFlow<String?>(null)
    val lastSyncMessage: StateFlow<String?> = _lastSyncMessage.asStateFlow()

    init {
        val userId = UserSession.currentUserId.value
        val existingSession = userId?.let { libreLinkRepository.getSession(it) }
        if (existingSession != null) {
            _connectState.value = LibreLinkConnectState.Connected(existingSession)
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
        _lastSyncMessage.value = null
    }

    fun syncNow() {
        val userId = UserSession.currentUserId.value ?: return
        _isSyncing.value = true
        viewModelScope.launch {
            libreLinkRepository.syncLatestReadings(userId)
                .onSuccess { count -> _lastSyncMessage.value = "Sinhronizovano: $count novih očitavanja" }
                .onFailure { throwable -> _lastSyncMessage.value = "Greška: ${throwable.message}" }
            _isSyncing.value = false
        }
    }
}
