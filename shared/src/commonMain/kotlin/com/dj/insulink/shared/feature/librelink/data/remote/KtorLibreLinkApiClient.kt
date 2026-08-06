package com.dj.insulink.shared.feature.librelink.data.remote

import com.dj.insulink.shared.feature.librelink.data.mapper.parseLibreLinkTimestamp
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

private const val DEFAULT_HOST = "https://api.libreview.io"
private const val CLIENT_VERSION = "4.16.0"
private const val PRODUCT = "llu.android"

class KtorLibreLinkApiClient(
    private val httpClient: HttpClient
) : LibreLinkApiClient {

    override suspend fun login(email: String, password: String): Result<LibreLinkAuth> {
        return runCatching {
            val response = requestLogin(DEFAULT_HOST, email, password)
            val data = response.data ?: error("Empty LibreLinkUp login response")

            if (data.redirect == true && data.region != null) {
                val redirectedHost = "https://api-${data.region}.libreview.io"
                val redirected = requestLogin(redirectedHost, email, password)
                buildAuth(redirected.data ?: error("Empty LibreLinkUp login response after region redirect"), redirectedHost)
            } else {
                buildAuth(data, DEFAULT_HOST)
            }
        }
    }

    private suspend fun requestLogin(host: String, email: String, password: String): LibreLinkLoginResponse {
        val response = httpClient.post("$host/llu/auth/login") {
            contentType(ContentType.Application.Json)
            baseHeaders()
            setBody(LibreLinkLoginRequest(email, password))
        }
        requireSuccess(response) { "LibreLinkUp login failed with status ${response.status}" }
        return response.body()
    }

    private fun buildAuth(data: LibreLinkLoginData, host: String): LibreLinkAuth {
        val token = data.authTicket?.token?.takeIf { it.isNotBlank() } ?: error("LibreLinkUp login response is missing an auth token")
        val userId = data.user?.id?.takeIf { it.isNotBlank() } ?: error("LibreLinkUp login response is missing a user id")
        return LibreLinkAuth(
            token = token,
            regionHost = host,
            accountIdHash = sha256Hex(userId)
        )
    }

    override suspend fun fetchConnections(auth: LibreLinkAuth): Result<List<LibreLinkConnection>> {
        return runCatching {
            val response = httpClient.get("${auth.regionHost}/llu/connections") {
                authHeaders(auth)
            }
            requireSuccess(response) { "Failed to fetch LibreLinkUp connections: ${response.status}" }
            val body: LibreLinkConnectionsResponse = response.body()
            body.data.map {
                LibreLinkConnection(
                    patientId = it.patientId,
                    displayName = "${it.firstName} ${it.lastName}".trim()
                )
            }
        }
    }

    override suspend fun fetchGlucoseReadings(auth: LibreLinkAuth, patientId: String): Result<List<LibreLinkGlucoseReading>> {
        return runCatching {
            val response = httpClient.get("${auth.regionHost}/llu/connections/$patientId/graph") {
                authHeaders(auth)
            }
            requireSuccess(response) { "Failed to fetch LibreLinkUp glucose graph: ${response.status}" }
            val body: LibreLinkGraphResponse = response.body()

            val measurements = body.data?.graphData.orEmpty().toMutableList()
            body.data?.connection?.glucoseMeasurement?.let { latest ->
                if (measurements.none { it.factoryTimestamp == latest.factoryTimestamp }) {
                    measurements += latest
                }
            }

            measurements.mapNotNull { measurement ->
                val timestamp = parseLibreLinkTimestamp(measurement.factoryTimestamp) ?: return@mapNotNull null
                LibreLinkGlucoseReading(timestamp = timestamp, valueMgDl = measurement.valueInMgPerDl)
            }
        }
    }

    // Sent on every request (login included). LibreLinkUp's backend appears to fingerprint
    // requests against the official app's header set and reject anything narrower with a 403
    // rather than a 401 — omitting these (while still sending valid credentials) reproduced
    // that exact symptom.
    private fun HttpRequestBuilder.baseHeaders() {
        header("product", PRODUCT)
        header("version", CLIENT_VERSION)
        header("accept-encoding", "gzip")
        header("cache-control", "no-cache")
        header("connection", "Keep-Alive")
    }

    private fun HttpRequestBuilder.authHeaders(auth: LibreLinkAuth) {
        baseHeaders()
        header("Authorization", "Bearer ${auth.token}")
        header("Account-Id", auth.accountIdHash)
    }

    private inline fun requireSuccess(response: HttpResponse, lazyMessage: () -> String) {
        if (!response.status.isSuccess()) {
            error(lazyMessage())
        }
    }
}
