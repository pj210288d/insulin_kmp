package com.dj.insulink.feature.friends.ui.viewmodel

import com.dj.insulink.shared.core.auth.AuthSession
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.friends.data.repository.FriendRepository
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate
import com.dj.insulink.shared.feature.friends.ui.viewmodel.FriendsViewModel
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.util.MainDispatcherRule
import com.dj.insulink.util.collectInBackground
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class FriendsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val friendRepository: FriendRepository = mockk(relaxed = true)
    private val settingsPreferences: SettingsPreferences = mockk(relaxed = true)

    private val candidate = FriendCandidate(
        uid = "u2",
        firstName = "Jane",
        lastName = "Doe",
        latestReading = GlucoseReading(id = 1, userId = "u2", timestamp = 1_000L, value = 120, comment = "")
    )

    @Before
    fun setUp() {
        AuthSession.setCurrentUser(
            AuthUser(uid = "u1", firstName = "John", lastName = "Smith", email = "j@s.com", friendCode = "ABC123")
        )
        every { friendRepository.getAllFriendsForUser(any()) } returns flowOf(emptyList())
        coEvery { friendRepository.isFriendAlready(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        AuthSession.setCurrentUser(null)
    }

    private fun buildViewModel() = FriendsViewModel(friendRepository, settingsPreferences)

    @Test
    fun `init fetches friend data for the logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        buildViewModel()
        advanceUntilIdle()

        coVerify { friendRepository.fetchFriendDataAndUpdateDatabase("u1") }
    }

    @Test
    fun `usersFriendCode and friends list come from the session and repository`() =
        runTest(mainDispatcherRule.dispatcher) {
            val friends = listOf(Friend(1, "u1", "u2", "Jane Doe", 120, 1_000L))
            every { friendRepository.getAllFriendsForUser("u1") } returns flowOf(friends)

            val vm = buildViewModel()
            collectInBackground(vm.usersFriendCode, vm.allFriendsForUser)
            advanceUntilIdle()

            assertEquals("ABC123", vm.usersFriendCode.value)
            assertEquals(friends, vm.allFriendsForUser.value)
        }

    @Test
    fun `setEnteredCode uppercases and ignores codes longer than six characters`() {
        val vm = buildViewModel()

        vm.setEnteredCode("abc12")
        assertEquals("ABC12", vm.enteredCode.value)

        vm.setEnteredCode("abc1234")
        assertEquals("ABC12", vm.enteredCode.value)
    }

    @Test
    fun `addFriend stores the friend and links both users`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { friendRepository.findFriendCandidateByFriendCode("XYZ789") } returns candidate
        val vm = buildViewModel()
        vm.setShowAddNewFriendDialog(true)
        vm.setEnteredCode("xyz789")

        vm.addFriend()
        advanceUntilIdle()

        coVerify {
            friendRepository.addFriend(
                Friend(
                    id = 0,
                    userId = "u1",
                    friendId = "u2",
                    friendName = "Jane Doe",
                    friendLastGlucoseReadingValue = 120,
                    friendsLastGlucoseReadingTime = 1_000L
                )
            )
            friendRepository.pushFriendToFirestoreForUser("u1", "u2")
            friendRepository.pushFriendToFirestoreForUser("u2", "u1")
        }
        assertEquals("", vm.enteredCode.value)
        assertFalse(vm.showAddNewFriendDialog.value)
    }

    @Test
    fun `addFriend does not add someone who is already a friend`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { friendRepository.findFriendCandidateByFriendCode("XYZ789") } returns candidate
        coEvery { friendRepository.isFriendAlready("u1", "u2") } returns true
        val vm = buildViewModel()
        vm.setEnteredCode("XYZ789")

        vm.addFriend()
        advanceUntilIdle()

        coVerify(exactly = 0) { friendRepository.addFriend(any()) }
        coVerify(exactly = 0) { friendRepository.pushFriendToFirestoreForUser(any(), any()) }
    }

    @Test
    fun `addFriend with an unknown code adds nothing`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { friendRepository.findFriendCandidateByFriendCode(any()) } returns null
        val vm = buildViewModel()
        vm.setEnteredCode("NOPE00")

        vm.addFriend()
        advanceUntilIdle()

        coVerify(exactly = 0) { friendRepository.addFriend(any()) }
    }

    @Test
    fun `setShowAddNewFriendDialog updates state`() {
        val vm = buildViewModel()

        vm.setShowAddNewFriendDialog(true)

        assertTrue(vm.showAddNewFriendDialog.value)
    }

    @Test
    fun `removeFriend deletes the friend for the logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.removeFriend(Friend(1, "u1", "u2", "Jane Doe", 120, 1_000L))
        advanceUntilIdle()

        coVerify { friendRepository.deleteFriend("u1", "u2") }
    }
}
