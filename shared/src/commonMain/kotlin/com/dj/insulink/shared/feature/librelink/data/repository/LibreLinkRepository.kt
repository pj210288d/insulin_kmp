package com.dj.insulink.shared.feature.librelink.data.repository

import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.librelink.data.local.LibreLinkSessionStorage
import com.dj.insulink.shared.feature.librelink.data.mapper.toGlucoseReading
import com.dj.insulink.shared.feature.librelink.data.remote.LibreLinkApiClient
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibreLinkRepository(
    private val apiClient: LibreLinkApiClient,
    private val sessionStorage: LibreLinkSessionStorage,
    private val glucoseReadingRepository: GlucoseReadingRepository
) {

    fun getSession(): LibreLinkSession? = sessionStorage.getSession()

    fun getLastSyncedTimestamp(): Long? = sessionStorage.getLastSyncedTimestamp()

    fun getLastSyncError(): String? = sessionStorage.getLastSyncError()

    suspend fun connect(email: String, password: String): Result<LibreLinkSession> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val auth = apiClient.login(email, password).getOrThrow()
                val connections = apiClient.fetchConnections(auth).getOrThrow()
                val connection = connections.firstOrNull()
                    ?: error("No LibreLinkUp connections found for this account")

                val session = LibreLinkSession(
                    email = email,
                    token = auth.token,
                    regionHost = auth.regionHost,
                    accountIdHash = auth.accountIdHash,
                    patientId = connection.patientId
                )
                sessionStorage.saveSession(session)
                session
            }
        }
    }

    fun disconnect() {
        sessionStorage.clearSession()
    }

    suspend fun syncLatestReadings(userId: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val session = sessionStorage.getSession() ?: error("Not connected to LibreLinkUp")
                val readings = apiClient.fetchGlucoseReadings(session.auth, session.patientId).getOrThrow()

                val lastSyncedTimestamp = sessionStorage.getLastSyncedTimestamp()
                val newReadings = readings.filter { lastSyncedTimestamp == null || it.timestamp > lastSyncedTimestamp }

                newReadings.forEach { reading ->
                    glucoseReadingRepository.insert(userId, reading.toGlucoseReading(userId))
                }

                newReadings.maxOfOrNull { it.timestamp }?.let { sessionStorage.setLastSyncedTimestamp(it) }
                sessionStorage.setLastSyncError(null)
                newReadings.size
            }.onFailure { throwable ->
                sessionStorage.setLastSyncError(throwable.message ?: "Unknown error")
            }
        }
    }
}
