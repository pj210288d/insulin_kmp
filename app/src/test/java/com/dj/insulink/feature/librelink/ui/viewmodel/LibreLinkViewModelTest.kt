package com.dj.insulink.feature.librelink.ui.viewmodel

import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.sync.LibreLinkSyncScheduler
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkLoginResult
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import com.dj.insulink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibreLinkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val libreLinkRepository: LibreLinkRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val syncScheduler: LibreLinkSyncScheduler = mockk(relaxed = true)

    private val auth = LibreLinkAuth("tok", "https://api.libreview.io", "hash")

    @Before
    fun setUp() {
        every { authRepository.getCurrentUserFlow() } returns flowOf("u1")
        every { libreLinkRepository.getSession(any()) } returns null
        every { libreLinkRepository.getLastSyncedTimestamp(any()) } returns null
        every { libreLinkRepository.getLastSyncError(any()) } returns null
    }

    private fun buildViewModel(): LibreLinkViewModel =
        LibreLinkViewModel(libreLinkRepository, authRepository, syncScheduler)

    @Test
    fun `setEmail and setPassword update state`() {
        val vm = buildViewModel()
        vm.setEmail("a@b.com")
        vm.setPassword("secret")

        assertEquals("a@b.com", vm.email.value)
        assertEquals("secret", vm.password.value)
    }

    @Test
    fun `connect with blank credentials does not call repository`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setPassword("secret") // email left blank

        vm.connect()
        advanceUntilIdle()

        coVerify(exactly = 0) { libreLinkRepository.login(any(), any()) }
    }

    @Test
    fun `connect with a single connection skips the picker and finalizes immediately`() =
        runTest(mainDispatcherRule.dispatcher) {
            val connection = LibreLinkConnection("p1", "Jane Doe")
            val session = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p1")
            coEvery { libreLinkRepository.login("a@b.com", "secret") } returns
                Result.success(LibreLinkLoginResult("a@b.com", auth, listOf(connection)))
            coEvery { libreLinkRepository.connect("u1", "a@b.com", auth, connection) } returns Result.success(session)
            coEvery { libreLinkRepository.syncLatestReadings("u1") } returns Result.success(2)
            every { libreLinkRepository.getSession("u1") } returns session

            val vm = buildViewModel()
            vm.setEmail("a@b.com")
            vm.setPassword("secret")

            vm.connect()
            advanceUntilIdle()

            assertEquals(session, vm.session.value)
            assertEquals("", vm.password.value)
            assertEquals(LibreLinkConnectState.Idle, vm.connectState.value)
            coVerify { libreLinkRepository.syncLatestReadings("u1") }
            verify { syncScheduler.enqueue() }
        }

    @Test
    fun `connect with multiple connections surfaces a picker instead of guessing`() =
        runTest(mainDispatcherRule.dispatcher) {
            val connections = listOf(LibreLinkConnection("p1", "Jane Doe"), LibreLinkConnection("p2", "Other"))
            coEvery { libreLinkRepository.login("a@b.com", "secret") } returns
                Result.success(LibreLinkLoginResult("a@b.com", auth, connections))

            val vm = buildViewModel()
            vm.setEmail("a@b.com")
            vm.setPassword("secret")

            vm.connect()
            advanceUntilIdle()

            val state = vm.connectState.value
            assertTrue(state is LibreLinkConnectState.ChoosingConnection)
            assertEquals(connections, (state as LibreLinkConnectState.ChoosingConnection).connections)
            coVerify(exactly = 0) { libreLinkRepository.connect(any(), any(), any(), any()) }
        }

    @Test
    fun `selectConnection finalizes the connect flow with the chosen connection`() =
        runTest(mainDispatcherRule.dispatcher) {
            val connections = listOf(LibreLinkConnection("p1", "Jane Doe"), LibreLinkConnection("p2", "Other"))
            val session = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p2")
            coEvery { libreLinkRepository.login("a@b.com", "secret") } returns
                Result.success(LibreLinkLoginResult("a@b.com", auth, connections))
            coEvery { libreLinkRepository.connect("u1", "a@b.com", auth, connections[1]) } returns Result.success(session)
            coEvery { libreLinkRepository.syncLatestReadings("u1") } returns Result.success(0)
            every { libreLinkRepository.getSession("u1") } returns session

            val vm = buildViewModel()
            vm.setEmail("a@b.com")
            vm.setPassword("secret")
            vm.connect()
            advanceUntilIdle()

            vm.selectConnection(connections[1])
            advanceUntilIdle()

            assertEquals(session, vm.session.value)
            assertEquals(LibreLinkConnectState.Idle, vm.connectState.value)
        }

    @Test
    fun `cancelSelectingConnection returns to idle without connecting`() =
        runTest(mainDispatcherRule.dispatcher) {
            val connections = listOf(LibreLinkConnection("p1", "Jane Doe"), LibreLinkConnection("p2", "Other"))
            coEvery { libreLinkRepository.login("a@b.com", "secret") } returns
                Result.success(LibreLinkLoginResult("a@b.com", auth, connections))

            val vm = buildViewModel()
            vm.setEmail("a@b.com")
            vm.setPassword("secret")
            vm.connect()
            advanceUntilIdle()

            vm.cancelSelectingConnection()

            assertEquals(LibreLinkConnectState.Idle, vm.connectState.value)
            coVerify(exactly = 0) { libreLinkRepository.connect(any(), any(), any(), any()) }
        }

    @Test
    fun `connect failure surfaces an error state`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { libreLinkRepository.login("a@b.com", "wrong") } returns Result.failure(RuntimeException("bad creds"))

        val vm = buildViewModel()
        vm.setEmail("a@b.com")
        vm.setPassword("wrong")

        vm.connect()
        advanceUntilIdle()

        val state = vm.connectState.value
        assertTrue(state is LibreLinkConnectState.Error)
        assertEquals("bad creds", (state as LibreLinkConnectState.Error).message)
    }

    @Test
    fun `disconnect clears the session and cancels scheduled work`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.disconnect()
        advanceUntilIdle()

        verify { libreLinkRepository.disconnect("u1") }
        verify { syncScheduler.cancel() }
        assertEquals(LibreLinkConnectState.Idle, vm.connectState.value)
    }

    @Test
    fun `connect scopes the session to the currently signed-in user, not a previous one`() =
        runTest(mainDispatcherRule.dispatcher) {
            val connection = LibreLinkConnection("p2", "Jane Doe")
            val sessionForUser2 = LibreLinkSession("second@b.com", "tok2", "https://api.libreview.io", "hash2", "p2")
            every { authRepository.getCurrentUserFlow() } returns flowOf("u2")
            coEvery { libreLinkRepository.login("second@b.com", "secret") } returns
                Result.success(LibreLinkLoginResult("second@b.com", auth, listOf(connection)))
            coEvery { libreLinkRepository.connect("u2", "second@b.com", auth, connection) } returns Result.success(sessionForUser2)
            coEvery { libreLinkRepository.syncLatestReadings("u2") } returns Result.success(0)
            every { libreLinkRepository.getSession("u2") } returns sessionForUser2
            // A different (first) user's session must never leak into this one's state.
            every { libreLinkRepository.getSession("u1") } returns
                LibreLinkSession("first@b.com", "tok1", "https://api.libreview.io", "hash1", "p1")

            val vm = buildViewModel()
            vm.setEmail("second@b.com")
            vm.setPassword("secret")

            vm.connect()
            advanceUntilIdle()

            assertEquals(sessionForUser2, vm.session.value)
            coVerify(exactly = 0) { libreLinkRepository.connect("u1", any(), any(), any()) }
        }
}
