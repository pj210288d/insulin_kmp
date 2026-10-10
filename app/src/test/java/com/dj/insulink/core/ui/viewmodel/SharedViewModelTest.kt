package com.dj.insulink.core.ui.viewmodel

import android.content.Context
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import com.dj.insulink.util.MainDispatcherRule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val firebaseAuth: FirebaseAuth = mockk()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val authUser = AuthUser(
        uid = "u1",
        firstName = "Jane",
        lastName = "Doe",
        email = "j@d.com",
        friendCode = "ABC123",
        isEmailVerified = true
    )

    private lateinit var viewModel: SharedViewModel

    @Before
    fun setUp() {
        startKoin { modules(module { single<AuthRepository> { authRepository } }) }
        viewModel = SharedViewModel(firebaseAuth)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `isUserLoggedIn reflects the Firebase user`() {
        every { firebaseAuth.currentUser } returns mockk<FirebaseUser>()
        assertTrue(viewModel.isUserLoggedIn())

        every { firebaseAuth.currentUser } returns null
        assertFalse(viewModel.isUserLoggedIn())
    }

    @Test
    fun `currentUser starts null`() {
        assertNull(viewModel.currentUser.value)
    }

    @Test
    fun `getCurrentUser loads the restored session into state`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { authRepository.restoreSession() } returns authUser

        viewModel.getCurrentUser()
        advanceUntilIdle()

        assertEquals(
            User(
                uid = "u1",
                firstName = "Jane",
                lastName = "Doe",
                email = "j@d.com",
                friendCode = "ABC123",
                isEmailVerified = true
            ),
            viewModel.currentUser.value
        )
    }

    @Test
    fun `getCurrentUser leaves the user null when restoring fails`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { authRepository.restoreSession() } throws IllegalStateException("offline")

        viewModel.getCurrentUser()
        advanceUntilIdle()

        assertNull(viewModel.currentUser.value)
    }

    @Test
    fun `signOut signs out and clears the user`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { authRepository.restoreSession() } returns authUser
        viewModel.getCurrentUser()
        advanceUntilIdle()

        viewModel.signOut(mockk<Context>())
        advanceUntilIdle()

        coVerify { authRepository.signOut() }
        assertNull(viewModel.currentUser.value)
    }
}
