package com.dj.insulink.shared.feature.librelink.data.repository

import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.librelink.data.local.LibreLinkSessionStorage
import com.dj.insulink.shared.feature.librelink.data.mapper.toGlucoseReading
import com.dj.insulink.shared.feature.librelink.data.remote.LibreLinkApiClient
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkLoginResult
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import com.dj.insulink.shared.core.dispatcher.ioDispatcher
import kotlinx.coroutines.withContext

class LibreLinkRepository(
    private val apiClient: LibreLinkApiClient,
    private val sessionStorage: LibreLinkSessionStorage,
    private val glucoseReadingRepository: GlucoseReadingRepository
) {

    fun getSession(userId: String): LibreLinkSession? = sessionStorage.getSession(userId)

    fun getLastSyncedTimestamp(userId: String): Long? = sessionStorage.getLastSyncedTimestamp(userId)

    fun getLastSyncError(userId: String): String? = sessionStorage.getLastSyncError(userId)

    // Step 1 of connecting: authenticates and returns every connection (patient) this
    // LibreLinkUp account can see. Does NOT persist a session yet - a LibreLinkUp account isn't
    // guaranteed to follow only the signed-in user's own sensor (it may also/instead follow a
    // family member's, for example), so the caller must let the user pick before we commit to a
    // patientId. See connect() below for step 2.
    suspend fun login(email: String, password: String): Result<LibreLinkLoginResult> {
        return withContext(ioDispatcher) {
            runCatching {
                val auth = apiClient.login(email, password).getOrThrow()
                val connections = apiClient.fetchConnections(auth).getOrThrow()
                if (connections.isEmpty()) {
                    error("No LibreLinkUp connections found for this account")
                }
                LibreLinkLoginResult(email = email, auth = auth, connections = connections)
            }
        }
    }

    // Step 2: persists a session for the chosen connection, using the auth obtained in login().
    suspend fun connect(userId: String, email: String, auth: LibreLinkAuth, connection: LibreLinkConnection): Result<LibreLinkSession> {
        return withContext(ioDispatcher) {
            runCatching {
                val session = LibreLinkSession(
                    email = email,
                    token = auth.token,
                    regionHost = auth.regionHost,
                    accountIdHash = auth.accountIdHash,
                    patientId = connection.patientId
                )
                sessionStorage.saveSession(userId, session)
                session
            }
        }
    }

    fun disconnect(userId: String) {
        sessionStorage.clearSession(userId)
    }

    suspend fun syncLatestReadings(userId: String): Result<Int> {
        return withContext(ioDispatcher) {
            runCatching {
                val session = sessionStorage.getSession(userId) ?: error("Not connected to LibreLinkUp")
                val readings = apiClient.fetchGlucoseReadings(session.auth, session.patientId).getOrThrow()

                val lastSyncedTimestamp = sessionStorage.getLastSyncedTimestamp(userId)
                val newReadings = readings.filter { lastSyncedTimestamp == null || it.timestamp > lastSyncedTimestamp }

                newReadings.forEach { reading ->
                    glucoseReadingRepository.insert(userId, reading.toGlucoseReading(userId))
                }

                newReadings.maxOfOrNull { it.timestamp }?.let { sessionStorage.setLastSyncedTimestamp(userId, it) }
                sessionStorage.setLastSyncError(userId, null)
                newReadings.size
            }.onFailure { throwable ->
                sessionStorage.setLastSyncError(userId, throwable.message ?: "Unknown error")
            }
        }
    }
}
