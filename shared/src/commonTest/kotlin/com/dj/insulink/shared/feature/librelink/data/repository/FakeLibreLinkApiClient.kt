package com.dj.insulink.shared.feature.librelink.data.repository

import com.dj.insulink.shared.feature.librelink.data.remote.LibreLinkApiClient
import com.dj.insulink.shared.feature.librelink.data.remote.LibreLinkGlucoseReading
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection

class FakeLibreLinkApiClient : LibreLinkApiClient {
    var loginResult: Result<LibreLinkAuth> = Result.failure(IllegalStateException("not stubbed"))
    var connectionsResult: Result<List<LibreLinkConnection>> = Result.success(emptyList())
    var readingsResult: Result<List<LibreLinkGlucoseReading>> = Result.success(emptyList())

    override suspend fun login(email: String, password: String): Result<LibreLinkAuth> = loginResult

    override suspend fun fetchConnections(auth: LibreLinkAuth): Result<List<LibreLinkConnection>> = connectionsResult

    override suspend fun fetchGlucoseReadings(auth: LibreLinkAuth, patientId: String): Result<List<LibreLinkGlucoseReading>> =
        readingsResult
}
