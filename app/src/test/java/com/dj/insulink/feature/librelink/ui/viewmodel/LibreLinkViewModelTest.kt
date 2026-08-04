package com.dj.insulink.feature.librelink.ui.viewmodel

import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.sync.LibreLinkSyncScheduler
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
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

    @Before
    fun setUp() {
        every { libreLinkRepository.getSession() } returns null
        every { libreLinkRepository.getLastSyncedTimestamp() } returns null
        every { libreLinkRepository.getLastSyncError() } returns null
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

        coVerify(exactly = 0) { libreLinkRepository.connect(any(), any()) }
    }

    @Test
    fun `connect success updates session, clears password, triggers a sync, and schedules periodic work`() =
        runTest(mainDispatcherRule.dispatcher) {
            val session = LibreLinkSession("a@b.com", "tok", "https://api.libreview.io", "hash", "p1")
            every { authRepository.getCurrentUserFlow() } returns flowOf("u1")
            coEvery { libreLinkRepository.connect("a@b.com", "secret") } returns Result.success(session)
            coEvery { libreLinkRepository.syncLatestReadings("u1") } returns Result.success(2)
            // refreshStatus() re-reads from the repository after connecting, so the mock needs
            // to reflect the post-connect state rather than the default(null) from setUp().
            every { libreLinkRepository.getSession() } returns session

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
    fun `connect failure surfaces an error state`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { libreLinkRepository.connect("a@b.com", "wrong") } returns Result.failure(RuntimeException("bad creds"))

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
    fun `disconnect clears the session and cancels scheduled work`() {
        val vm = buildViewModel()

        vm.disconnect()

        verify { libreLinkRepository.disconnect() }
        verify { syncScheduler.cancel() }
        assertEquals(LibreLinkConnectState.Idle, vm.connectState.value)
    }
}
