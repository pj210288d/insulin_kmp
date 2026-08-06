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

    fun getSession(userId: String): LibreLinkSession? = sessionStorage.getSession(userId)

    fun getLastSyncedTimestamp(userId: String): Long? = sessionStorage.getLastSyncedTimestamp(userId)

    fun getLastSyncError(userId: String): String? = sessionStorage.getLastSyncError(userId)

    suspend fun connect(userId: String, email: String, password: String): Result<LibreLinkSession> {
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
                sessionStorage.saveSession(userId, session)
                session
            }
        }
    }

    fun disconnect(userId: String) {
        sessionStorage.clearSession(userId)
    }

    suspend fun syncLatestReadings(userId: String): Result<Int> {
        return withContext(Dispatchers.IO) {
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
