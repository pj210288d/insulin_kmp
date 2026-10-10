package com.dj.insulink.feature.librelink.ui.viewmodel

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.librelink.data.sync.LibreLinkSyncCoordinator
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkAuth
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkLoginResult
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import com.dj.insulink.shared.feature.librelink.ui.viewmodel.LibreLinkConnectState
import com.dj.insulink.shared.feature.librelink.ui.viewmodel.LibreLinkViewModel
import com.dj.insulink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibreLinkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val libreLinkRepository: LibreLinkRepository = mockk(relaxed = true)
    private val syncCoordinator: LibreLinkSyncCoordinator = mockk(relaxed = true)

    private val auth = LibreLinkAuth("tok", "https://api.libreview.io", "hash")
    private val jane = LibreLinkConnection("p1", "Jane Doe")
    private val john = LibreLinkConnection("p2", "John Doe")
    private val session = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p1")

    @Before
    fun setUp() {
        UserSession.setCurrentUserId("u1")
        every { libreLinkRepository.getSession(any()) } returns null
    }

    @After
    fun tearDown() {
        UserSession.setCurrentUserId(null)
    }

    private fun buildViewModel() = LibreLinkViewModel(libreLinkRepository, syncCoordinator)

    private fun LibreLinkViewModel.enterCredentials() {
        setEmail(" a@b.com ")
        setPassword("secret")
    }

    private fun stubLogin(vararg connections: LibreLinkConnection) {
        coEvery { libreLinkRepository.login("a@b.com", "secret") } returns
            Result.success(LibreLinkLoginResult("a@b.com", auth, connections.toList()))
    }

    @Test
    fun `starts disconnected without a stored session`() {
        val vm = buildViewModel()

        assertEquals(LibreLinkConnectState.Disconnected, vm.connectState.value)
        verify(exactly = 0) { syncCoordinator.start(any()) }
    }

    @Test
    fun `starts connected and syncing when a session is stored`() {
        every { libreLinkRepository.getSession("u1") } returns session

        val vm = buildViewModel()

        assertEquals(LibreLinkConnectState.Connected(session), vm.connectState.value)
        verify { syncCoordinator.start("u1") }
    }

    @Test
    fun `login with blank credentials does not call the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setPassword("secret")

        vm.login()
        advanceUntilIdle()

        coVerify(exactly = 0) { libreLinkRepository.login(any(), any()) }
        assertEquals(LibreLinkConnectState.Disconnected, vm.connectState.value)
    }

    @Test
    fun `login with a single connection connects immediately`() = runTest(mainDispatcherRule.dispatcher) {
        stubLogin(jane)
        coEvery { libreLinkRepository.connect("u1", "a@b.com", auth, jane) } returns Result.success(session)
        val vm = buildViewModel()
        vm.enterCredentials()

        vm.login()
        assertEquals(LibreLinkConnectState.Connecting, vm.connectState.value)
        advanceUntilIdle()

        assertEquals(LibreLinkConnectState.Connected(session), vm.connectState.value)
        assertEquals("", vm.password.value)
        verify { syncCoordinator.start("u1") }
    }

    @Test
    fun `login with several connections asks which one to use`() = runTest(mainDispatcherRule.dispatcher) {
        stubLogin(jane, john)
        coEvery { libreLinkRepository.connect("u1", "a@b.com", auth, john) } returns
            Result.success(session.copy(patientId = "p2"))
        val vm = buildViewModel()
        vm.enterCredentials()

        vm.login()
        advanceUntilIdle()

        assertEquals(
            LibreLinkConnectState.ChoosingConnection("a@b.com", auth, listOf(jane, john)),
            vm.connectState.value
        )
        coVerify(exactly = 0) { libreLinkRepository.connect(any(), any(), any(), any()) }

        vm.selectConnection(john)
        advanceUntilIdle()

        assertEquals(LibreLinkConnectState.Connected(session.copy(patientId = "p2")), vm.connectState.value)
        verify { syncCoordinator.start("u1") }
    }

    @Test
    fun `cancelChoosingConnection returns to disconnected`() = runTest(mainDispatcherRule.dispatcher) {
        stubLogin(jane, john)
        val vm = buildViewModel()
        vm.enterCredentials()
        vm.login()
        advanceUntilIdle()

        vm.cancelChoosingConnection()

        assertEquals(LibreLinkConnectState.Disconnected, vm.connectState.value)
    }

    @Test
    fun `login failure shows the error message`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { libreLinkRepository.login("a@b.com", "secret") } returns
            Result.failure(IllegalStateException("Bad credentials"))
        val vm = buildViewModel()
        vm.enterCredentials()

        vm.login()
        advanceUntilIdle()

        assertEquals(LibreLinkConnectState.Error("Bad credentials"), vm.connectState.value)
    }

    @Test
    fun `connect failure shows the error message and does not start syncing`() =
        runTest(mainDispatcherRule.dispatcher) {
            stubLogin(jane)
            coEvery { libreLinkRepository.connect("u1", "a@b.com", auth, jane) } returns
                Result.failure(IllegalStateException("Storage failed"))
            val vm = buildViewModel()
            vm.enterCredentials()

            vm.login()
            advanceUntilIdle()

            assertEquals(LibreLinkConnectState.Error("Storage failed"), vm.connectState.value)
            verify(exactly = 0) { syncCoordinator.start(any()) }
        }

    @Test
    fun `disconnect clears the session and stops syncing`() {
        every { libreLinkRepository.getSession("u1") } returns session
        val vm = buildViewModel()
        vm.setEmail("a@b.com")

        vm.disconnect()

        verify { libreLinkRepository.disconnect("u1") }
        verify { syncCoordinator.stop() }
        assertEquals(LibreLinkConnectState.Disconnected, vm.connectState.value)
        assertEquals("", vm.email.value)
    }

    @Test
    fun `syncNow syncs readings and clears the syncing flag`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { libreLinkRepository.syncLatestReadings("u1") } returns Result.success(3)
        val vm = buildViewModel()

        vm.syncNow()
        assertTrue(vm.isSyncing.value)
        advanceUntilIdle()

        coVerify { libreLinkRepository.syncLatestReadings("u1") }
        assertFalse(vm.isSyncing.value)
    }

    @Test
    fun `syncNow clears the syncing flag even when the sync throws`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { libreLinkRepository.syncLatestReadings("u1") } throws IllegalStateException("offline")
        val vm = buildViewModel()

        vm.syncNow()
        advanceUntilIdle()

        assertFalse(vm.isSyncing.value)
    }
}
