package com.dj.insulink.shared.feature.librelink.data.remote

import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection

data class LibreLinkGlucoseReading(
    val timestamp: Long,
    val valueMgDl: Int
)

interface LibreLinkApiClient {
    suspend fun login(email: String, password: String): Result<LibreLinkAuth>
    suspend fun fetchConnections(auth: LibreLinkAuth): Result<List<LibreLinkConnection>>
    suspend fun fetchGlucoseReadings(auth: LibreLinkAuth, patientId: String): Result<List<LibreLinkGlucoseReading>>
}
