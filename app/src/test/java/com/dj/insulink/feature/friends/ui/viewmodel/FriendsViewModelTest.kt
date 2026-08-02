package com.dj.insulink.feature.friends.ui.viewmodel

import app.cash.turbine.test
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.feature.settings.data.SettingsPreferences
import com.dj.insulink.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.friends.data.repository.FriendRepository
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val friendRepository: FriendRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val settingsPreferences: SettingsPreferences = mockk()

    private fun buildViewModel(
        unit: GlucoseUnit = GlucoseUnit.MG_DL,
        friends: List<Friend> = emptyList(),
        userId: String? = "u1"
    ): FriendsViewModel {
        every { settingsPreferences.getGlucoseUnit() } returns unit
        every { authRepository.getCurrentUserFlow() } returns flowOf(userId)
        every { friendRepository.getAllFriendsForUser(any()) } returns flowOf(friends)
        return FriendsViewModel(friendRepository, authRepository, settingsPreferences)
    }

    @Test
    fun `setEnteredCode uppercases and caps at six characters`() {
        val vm = buildViewModel()
        vm.setEnteredCode("abc")
        assertEquals("ABC", vm.enteredCode.value)

        vm.setEnteredCode("abcdefg") // 7 chars -> rejected
        assertEquals("ABC", vm.enteredCode.value)
    }

    @Test
    fun `setShowAddNewFriendDialog updates state`() {
        val vm = buildViewModel()
        vm.setShowAddNewFriendDialog(true)
        assertEquals(true, vm.showAddNewFriendDialog.value)
    }

    @Test
    fun `refreshGlucoseUnit re-reads from preferences`() {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        every { settingsPreferences.getGlucoseUnit() } returns GlucoseUnit.MMOL_L
        vm.refreshGlucoseUnit()
        assertEquals(GlucoseUnit.MMOL_L, vm.glucoseUnit.value)
    }

    @Test
    fun `onAddFriendClick adds the found user and pushes to firestore both ways`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setEnteredCode("ABC123")
        val candidate = FriendCandidate(
            uid = "f1",
            firstName = "Jane",
            lastName = "Doe",
            latestReading = GlucoseReading(id = 9, userId = "f1", timestamp = 5L, value = 99, comment = "")
        )
        coEvery { friendRepository.findFriendCandidateByFriendCode("ABC123") } returns candidate

        vm.onAddFriendClick("u1")
        advanceUntilIdle()

        coVerify {
            friendRepository.addFriend(
                match {
                    it.userId == "u1" && it.friendId == "f1" && it.friendName == "Jane Doe" &&
                        it.friendLastGlucoseReadingValue == 99 && it.friendsLastGlucoseReadingTime == 5L
                }
            )
            friendRepository.pushFriendToFirestoreForUser("u1", "f1")
            friendRepository.pushFriendToFirestoreForUser("f1", "u1")
        }
        assertEquals(false, vm.showAddNewFriendDialog.value)
    }

    @Test
    fun `onAddFriendClick with no matching user does not add a friend`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setEnteredCode("NOPE")
        coEvery { friendRepository.findFriendCandidateByFriendCode("NOPE") } returns null

        vm.onAddFriendClick("u1")
        advanceUntilIdle()

        coVerify(exactly = 0) { friendRepository.addFriend(any()) }
        coVerify(exactly = 0) { friendRepository.pushFriendToFirestoreForUser(any(), any()) }
    }

    @Test
    fun `fetchFriendDataAndUpdateDatabase delegates to repository`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.fetchFriendDataAndUpdateDatabase("u1")
        advanceUntilIdle()
        coVerify { friendRepository.fetchFriendDataAndUpdateDatabase("u1") }
    }

    @Test
    fun `allFriendsForUser surfaces the repository data`() = runTest(mainDispatcherRule.dispatcher) {
        val friends = listOf(Friend(1, "u1", "f1", "Jane Doe", 100, 5L))
        val vm = buildViewModel(friends = friends)

        vm.allFriendsForUser.test {
            // initial empty value then the loaded list
            var item = awaitItem()
            while (item.isEmpty()) item = awaitItem()
            assertEquals(friends, item)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
