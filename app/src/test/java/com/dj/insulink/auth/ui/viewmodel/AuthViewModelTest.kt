package com.dj.insulink.auth.ui.viewmodel

import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import com.dj.insulink.shared.feature.auth.ui.viewmodel.AuthViewModel
import com.dj.insulink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val currentUserFlow = MutableStateFlow<AuthUser?>(null)
    private val user = AuthUser(
        uid = "u1",
        firstName = "Jane",
        lastName = "Doe",
        email = "j@d.com",
        friendCode = "ABC123"
    )

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        every { authRepository.currentUserFlow } returns currentUserFlow
        coEvery { authRepository.login(any(), any()) } returns user
        coEvery { authRepository.register(any(), any(), any(), any()) } returns user
        coEvery { authRepository.signInWithGoogle() } returns user
        viewModel = AuthViewModel(authRepository)
    }

    private fun register(
        password: String = "password1",
        confirmPassword: String = password,
        termsAccepted: Boolean = true
    ) {
        viewModel.register(" Jane ", " Doe ", " j@d.com ", password, confirmPassword, termsAccepted)
    }

    @Test
    fun `currentUser mirrors the repository flow`() {
        assertNull(viewModel.currentUser.value)

        currentUserFlow.value = user

        assertEquals(user, viewModel.currentUser.value)
    }

    @Test
    fun `restoreSession finishes restoring`() = runTest(mainDispatcherRule.dispatcher) {
        assertTrue(viewModel.isRestoringSession.value)

        viewModel.restoreSession()
        advanceUntilIdle()

        coVerify { authRepository.restoreSession() }
        assertFalse(viewModel.isRestoringSession.value)
    }

    @Test
    fun `restoreSession finishes restoring even when it fails`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { authRepository.restoreSession() } throws IllegalStateException("offline")

        viewModel.restoreSession()
        advanceUntilIdle()

        assertFalse(viewModel.isRestoringSession.value)
    }

    @Test
    fun `login trims the email and clears loading`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.login(" j@d.com ", "password1")
        advanceUntilIdle()

        coVerify { authRepository.login("j@d.com", "password1") }
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `login failure shows the error message`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { authRepository.login(any(), any()) } throws IllegalStateException("Wrong password")

        viewModel.login("j@d.com", "nope")
        advanceUntilIdle()

        assertEquals("Wrong password", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `register rejects a password shorter than eight characters`() = runTest(mainDispatcherRule.dispatcher) {
        register(password = "short")
        advanceUntilIdle()

        assertEquals("Lozinka mora imati najmanje 8 karaktera.", viewModel.errorMessage.value)
        coVerify(exactly = 0) { authRepository.register(any(), any(), any(), any()) }
    }

    @Test
    fun `register rejects mismatched passwords`() = runTest(mainDispatcherRule.dispatcher) {
        register(password = "password1", confirmPassword = "password2")
        advanceUntilIdle()

        assertEquals("Lozinke se ne poklapaju.", viewModel.errorMessage.value)
        coVerify(exactly = 0) { authRepository.register(any(), any(), any(), any()) }
    }

    @Test
    fun `register requires accepting the terms`() = runTest(mainDispatcherRule.dispatcher) {
        register(termsAccepted = false)
        advanceUntilIdle()

        assertEquals(
            "Morate prihvatiti Uslove korišćenja i Politiku privatnosti",
            viewModel.errorMessage.value
        )
        coVerify(exactly = 0) { authRepository.register(any(), any(), any(), any()) }
    }

    @Test
    fun `register trims names and email`() = runTest(mainDispatcherRule.dispatcher) {
        register()
        advanceUntilIdle()

        coVerify { authRepository.register("Jane", "Doe", "j@d.com", "password1") }
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `signInWithGoogle delegates to the repository`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signInWithGoogle()
        advanceUntilIdle()

        coVerify { authRepository.signInWithGoogle() }
    }

    @Test
    fun `sendPasswordResetEmail shows a confirmation`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.sendPasswordResetEmail(" j@d.com ")
        advanceUntilIdle()

        coVerify { authRepository.sendPasswordResetEmail("j@d.com") }
        assertEquals("Email za reset lozinke je poslat.", viewModel.infoMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendPasswordResetEmail failure shows the error message`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { authRepository.sendPasswordResetEmail(any()) } throws IllegalStateException("Unknown email")

        viewModel.sendPasswordResetEmail("x@y.com")
        advanceUntilIdle()

        assertEquals("Unknown email", viewModel.errorMessage.value)
        assertNull(viewModel.infoMessage.value)
    }

    @Test
    fun `clearMessages resets error and info`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.sendPasswordResetEmail("j@d.com")
        register(password = "short")
        advanceUntilIdle()

        viewModel.clearMessages()

        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.infoMessage.value)
    }

    @Test
    fun `signOut delegates to the repository`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signOut()
        advanceUntilIdle()

        coVerify { authRepository.signOut() }
    }
}
