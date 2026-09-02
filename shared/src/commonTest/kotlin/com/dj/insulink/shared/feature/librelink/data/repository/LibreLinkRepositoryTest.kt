package com.dj.insulink.shared.feature.librelink.data.repository

import com.dj.insulink.shared.feature.glucose.data.repository.FakeGlucoseReadingDao
import com.dj.insulink.shared.feature.glucose.data.repository.FakeGlucoseRemoteDataSource
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.librelink.data.mapper.LIBRELINK_READING_COMMENT
import com.dj.insulink.shared.feature.librelink.data.remote.LibreLinkGlucoseReading
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LibreLinkRepositoryTest {

    private val apiClient = FakeLibreLinkApiClient()
    private val sessionStorage = FakeLibreLinkSessionStorage()
    private val glucoseDao = FakeGlucoseReadingDao()
    private val glucoseRemote = FakeGlucoseRemoteDataSource()
    private val glucoseReadingRepository = GlucoseReadingRepository(glucoseDao, glucoseRemote)
    private val repository = LibreLinkRepository(apiClient, sessionStorage, glucoseReadingRepository)

    @Test
    fun login_returnsEveryConnectionSoTheCallerCanChoose() = runTest {
        apiClient.loginResult = Result.success(LibreLinkAuth("tok", "https://api.libreview.io", "hash"))
        apiClient.connectionsResult = Result.success(
            listOf(LibreLinkConnection("p1", "Jane Doe"), LibreLinkConnection("p2", "Other"))
        )

        val result = repository.login("a@b.com", "pw")

        assertTrue(result.isSuccess)
        val loginResult = result.getOrThrow()
        assertEquals("a@b.com", loginResult.email)
        assertEquals("tok", loginResult.auth.token)
        assertEquals(listOf("p1", "p2"), loginResult.connections.map { it.patientId })
        assertNull(sessionStorage.storedSession) // login() alone must not persist anything yet
    }

    @Test
    fun login_failsWhenLoginFails() = runTest {
        apiClient.loginResult = Result.failure(RuntimeException("bad credentials"))

        val result = repository.login("a@b.com", "wrong")

        assertTrue(result.isFailure)
        assertNull(sessionStorage.storedSession)
    }

    @Test
    fun login_failsWhenThereAreNoConnections() = runTest {
        apiClient.loginResult = Result.success(LibreLinkAuth("tok", "https://api.libreview.io", "hash"))
        apiClient.connectionsResult = Result.success(emptyList())

        val result = repository.login("a@b.com", "pw")

        assertTrue(result.isFailure)
        assertNull(sessionStorage.storedSession)
    }

    @Test
    fun connect_savesSessionForTheChosenConnection() = runTest {
        val auth = LibreLinkAuth("tok", "https://api.libreview.io", "hash")

        val result = repository.connect("u1", "a@b.com", auth, LibreLinkConnection("p2", "Other"))

        assertTrue(result.isSuccess)
        val session = sessionStorage.storedSession
        assertEquals("a@b.com", session?.email)
        assertEquals("p2", session?.patientId)
        assertEquals("tok", session?.token)
    }

    @Test
    fun disconnect_clearsTheStoredSession() {
        sessionStorage.storedSession = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p1")

        repository.disconnect("u1")

        assertNull(sessionStorage.storedSession)
    }

    @Test
    fun connect_doesNotAffectAnotherUsersStoredSession() = runTest {
        sessionStorage.storedSessions["other-user"] =
            LibreLinkSession("other@b.com", "other-tok", "https://api.libreview.io", "other-hash", "p9")
        val auth = LibreLinkAuth("tok", "https://api.libreview.io", "hash")

        repository.connect("u1", "a@b.com", auth, LibreLinkConnection("p1", "Jane Doe"))

        assertEquals("other@b.com", sessionStorage.storedSessions["other-user"]?.email)
        assertEquals("a@b.com", sessionStorage.storedSessions["u1"]?.email)
    }

    @Test
    fun disconnect_onlyClearsTheGivenUsersSession() {
        sessionStorage.storedSessions["u1"] = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p1")
        sessionStorage.storedSessions["other-user"] =
            LibreLinkSession("other@b.com", "other-tok", "https://api.libreview.io", "other-hash", "p9")

        repository.disconnect("u1")

        assertNull(sessionStorage.storedSessions["u1"])
        assertEquals("other@b.com", sessionStorage.storedSessions["other-user"]?.email)
    }

    @Test
    fun syncLatestReadings_failsWhenNotConnected() = runTest {
        val result = repository.syncLatestReadings("u1")

        assertTrue(result.isFailure)
    }

    @Test
    fun syncLatestReadings_firstSyncInsertsAllReadingsAndTracksTheMaxTimestamp() = runTest {
        sessionStorage.storedSession = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p1")
        apiClient.readingsResult = Result.success(
            listOf(
                LibreLinkGlucoseReading(timestamp = 100L, valueMgDl = 90),
                LibreLinkGlucoseReading(timestamp = 200L, valueMgDl = 110)
            )
        )

        val result = repository.syncLatestReadings("u1")

        assertEquals(2, result.getOrThrow())
        assertEquals(2, glucoseDao.insertedEntities.size)
        assertTrue(glucoseDao.insertedEntities.all { it.comment == LIBRELINK_READING_COMMENT })
        assertEquals(200L, sessionStorage.storedLastSyncedTimestamp)
        assertNull(sessionStorage.storedLastSyncError)
    }

    @Test
    fun syncLatestReadings_onlyInsertsReadingsNewerThanTheLastSyncedTimestamp() = runTest {
        sessionStorage.storedSession = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p1")
        sessionStorage.storedLastSyncedTimestamp = 150L
        apiClient.readingsResult = Result.success(
            listOf(
                LibreLinkGlucoseReading(timestamp = 100L, valueMgDl = 90), // already synced, skipped
                LibreLinkGlucoseReading(timestamp = 200L, valueMgDl = 110) // new
            )
        )

        val result = repository.syncLatestReadings("u1")

        assertEquals(1, result.getOrThrow())
        assertEquals(1, glucoseDao.insertedEntities.size)
        assertEquals(110, glucoseDao.insertedEntities.single().value)
        assertEquals(200L, sessionStorage.storedLastSyncedTimestamp)
    }

    @Test
    fun syncLatestReadings_recordsTheErrorOnFailureAndReturnsFailure() = runTest {
        sessionStorage.storedSession = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p1")
        apiClient.readingsResult = Result.failure(RuntimeException("network down"))

        val result = repository.syncLatestReadings("u1")

        assertTrue(result.isFailure)
        assertEquals("network down", sessionStorage.storedLastSyncError)
    }
}
