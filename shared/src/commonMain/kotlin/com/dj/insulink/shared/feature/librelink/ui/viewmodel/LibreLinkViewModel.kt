package com.dj.insulink.shared.feature.librelink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

    // Automatska periodična sinhronizacija - vidi startPeriodicSync() niže za obim/ograničenja.
    private var periodicSyncJob: Job? = null

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

    // Ručna sinhronizacija (dugme "Sinhronizuj sada") - isti obrazac kao Android-ov syncNow(),
    // namerno bez ikakvog rezultata prikazanog na ekranu (korisnik 2026-09-07 tražio da se
    // "izbace sve LibreLinkUp vrednosti" - ni broj novih očitavanja ni poruka o grešci se ne
    // prikazuju, isto kao Android-ov real ekran koji takođe ne prikazuje broj sinhronizovanih
    // očitavanja - vidi feature/librelink/ui/viewmodel/LibreLinkViewModel.kt tamo).
    fun syncNow() {
        val userId = UserSession.currentUserId.value ?: return
        _isSyncing.value = true
        viewModelScope.launch {
            runCatching { libreLinkRepository.syncLatestReadings(userId) }
            _isSyncing.value = false
        }
    }

    // Automatska periodična sinhronizacija sa LibreLinkUp nalogom - pokreće se čim je nalog
    // povezan (i pri restauraciji postojeće sesije u init-u, i odmah posle uspešnog connect()-a),
    // zaustavlja se pri disconnect()-u. Poziva ISTU syncLatestReadings() logiku koju Android
    // pokreće preko WorkManager-a (vidi core/sync/LibreLinkSyncScheduler.kt/
    // LibreLinkSyncWorker.kt - tamo 15-minutni interval, OS-nametnut pod za PeriodicWorkRequest,
    // ne WorkManager ograničenje), samo kao foreground coroutine petlja umesto pravog OS
    // pozadinskog zadatka.
    //
    // NAPOMENA - iskreno navedeno ograničenje (korisnik tražio 10 minuta "ako je moguće"): iOS
    // suspenduje (zamrzava) aplikacije čim odu u pozadinu, osim ako app eksplicitno ne deklariše
    // background mode (audio/location/BGTaskScheduler background fetch). Prava OS-nivo pozadinska
    // sinhronizacija (radi i kad je app potpuno ugašen/suspendovan) bi zahtevala BGTaskScheduler
    // registraciju - to je Swift-side posao (registruje se PRE nego što app završi lansiranje, u
    // iOSApp.swift) plus nova "Background Modes" capability u Xcode projektu - i čak i tada iOS
    // sam bira KADA će stvarno pokrenuti zadatak (opportunistic scheduling, nema garantovanog
    // intervala - isti tip ograničenja kao Android-ov WorkManager pod lošom baterijom/Doze).
    // Ova coroutine petlja garantovano radi na TAČNO 10 minuta DOK JE APP U FOREGROUND-U
    // (aktivan na ekranu) - dovoljno da se u snimku pokaže automatska sinhronizacija bez ijedne
    // ručne akcije, ali ne radi dok je app zatvoren/u pozadini. Prava pozadinska sinhronizacija
    // (BGTaskScheduler + Xcode capability + Swift kod) bi bila poseban, veći zahvat za posle roka.
    private fun startPeriodicSync(userId: String) {
        periodicSyncJob?.cancel()
        periodicSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(PERIODIC_SYNC_INTERVAL_MS)
                runCatching { libreLinkRepository.syncLatestReadings(userId) }
            }
        }
    }

    private fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }
}

private const val PERIODIC_SYNC_INTERVAL_MS = 10L * 60L * 1000L
