package com.dj.insulink.shared.feature.librelink.data.remote

import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class KtorLibreLinkApiClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun login_returnsAuthDirectlyWhenNoRegionRedirect() = runTest {
        val engine = MockEngine { request ->
            assertEquals("api.libreview.io", request.url.host)
            respond(
                """{"status":0,"data":{"authTicket":{"token":"tok-1"},"user":{"id":"user-123"}}}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        val client = KtorLibreLinkApiClient(createHttpClient(engine))

        val auth = client.login("a@b.com", "pw").getOrThrow()

        assertEquals("tok-1", auth.token)
        assertEquals("https://api.libreview.io", auth.regionHost)
        assertEquals(sha256Hex("user-123"), auth.accountIdHash)
    }

    @Test
    fun login_retriesAgainstRegionHostWhenRedirected() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            if (request.url.host == "api.libreview.io") {
                respond(
                    """{"status":0,"data":{"redirect":true,"region":"eu"}}""",
                    HttpStatusCode.OK,
                    jsonHeaders
                )
            } else {
                assertEquals("api-eu.libreview.io", request.url.host)
                respond(
                    """{"status":0,"data":{"authTicket":{"token":"tok-eu"},"user":{"id":"user-456"}}}""",
                    HttpStatusCode.OK,
                    jsonHeaders
                )
            }
        }
        val client = KtorLibreLinkApiClient(createHttpClient(engine))

        val auth = client.login("a@b.com", "pw").getOrThrow()

        assertEquals(2, callCount)
        assertEquals("tok-eu", auth.token)
        assertEquals("https://api-eu.libreview.io", auth.regionHost)
    }

    @Test
    fun login_returnsFailureOnHttpError() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.Unauthorized) }
        val client = KtorLibreLinkApiClient(createHttpClient(engine))

        val result = client.login("a@b.com", "wrong")

        assertTrue(result.isFailure)
    }

    @Test
    fun fetchConnections_mapsPatientIdAndFullName() = runTest {
        val engine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/llu/connections"))
            respond(
                """{"data":[{"patientId":"p1","firstName":"Jane","lastName":"Doe"}]}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        val client = KtorLibreLinkApiClient(createHttpClient(engine))
        val auth = LibreLinkAuth(token = "tok", regionHost = "https://api.libreview.io", accountIdHash = "hash")

        val connections = client.fetchConnections(auth).getOrThrow()

        assertEquals(1, connections.size)
        assertEquals("p1", connections.first().patientId)
        assertEquals("Jane Doe", connections.first().displayName)
    }

    @Test
    fun fetchConnections_returnsFailureOnHttpError() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.Forbidden) }
        val client = KtorLibreLinkApiClient(createHttpClient(engine))
        val auth = LibreLinkAuth(token = "tok", regionHost = "https://api.libreview.io", accountIdHash = "hash")

        assertTrue(client.fetchConnections(auth).isFailure)
    }

    @Test
    fun fetchGlucoseReadings_mapsGraphDataAndLatestMeasurement_withoutDuplicatingTheLatestEntry() = runTest {
        val engine = MockEngine { request ->
            assertTrue(request.url.encodedPath.contains("/llu/connections/p1/graph"))
            respond(
                """
                {"data":{
                    "connection":{"glucoseMeasurement":{"FactoryTimestamp":"8/2/2025 3:45:00 PM","ValueInMgPerDl":110}},
                    "graphData":[
                        {"FactoryTimestamp":"8/2/2025 3:30:00 PM","ValueInMgPerDl":105},
                        {"FactoryTimestamp":"8/2/2025 3:45:00 PM","ValueInMgPerDl":110}
                    ]
                }}
                """.trimIndent(),
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        val client = KtorLibreLinkApiClient(createHttpClient(engine))
        val auth = LibreLinkAuth(token = "tok", regionHost = "https://api.libreview.io", accountIdHash = "hash")

        val readings = client.fetchGlucoseReadings(auth, "p1").getOrThrow()

        assertEquals(2, readings.size) // the latest measurement duplicates a graphData entry and isn't added twice
        assertEquals(listOf(105, 110), readings.map { it.valueMgDl })
    }

    @Test
    fun fetchGlucoseReadings_skipsMeasurementsWithUnparsableTimestamps() = runTest {
        val engine = MockEngine {
            respond(
                """{"data":{"graphData":[{"FactoryTimestamp":"not-a-date","ValueInMgPerDl":99}]}}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        val client = KtorLibreLinkApiClient(createHttpClient(engine))
        val auth = LibreLinkAuth(token = "tok", regionHost = "https://api.libreview.io", accountIdHash = "hash")

        val readings = client.fetchGlucoseReadings(auth, "p1").getOrThrow()

        assertTrue(readings.isEmpty())
    }
}
