package com.dj.insulink.shared.feature.friends.data.repository

import com.dj.insulink.shared.feature.friends.data.local.entity.FriendEntity
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class FriendRepositoryTest {

    private val dao = FakeFriendDao()
    private val remote = FakeFriendRemoteDataSource()
    private val repository = FriendRepository(dao, remote)

    @Test
    fun getAllFriendsForUser_mapsEntitiesToDomain() = runTest {
        val entity = FriendEntity(1, "u1", "f1", "Jane Doe", 100, 5L)
        dao.allFriendsFlow = flowOf(listOf(entity))

        val result = repository.getAllFriendsForUser("u1").first()

        assertEquals(listOf(Friend(1, "u1", "f1", "Jane Doe", 100, 5L)), result)
    }

    @Test
    fun addFriend_storesTheFriendLocally() = runTest {
        val friend = Friend(0, "u1", "f1", "Jane Doe", 100, 5L)

        repository.addFriend(friend)

        assertEquals("f1", dao.insertedFriends.single().friendId)
    }

    @Test
    fun findFriendCandidateByFriendCode_delegatesToRemote() = runTest {
        val candidate = FriendCandidate("f1", "Jane", "Doe", null)
        remote.findResult = candidate

        val result = repository.findFriendCandidateByFriendCode("ABC123")

        assertEquals(candidate, result)
    }

    @Test
    fun findFriendCandidateByFriendCode_returnsNullWhenNotFound() = runTest {
        remote.findResult = null

        val result = repository.findFriendCandidateByFriendCode("NOPE")

        assertNull(result)
    }

    @Test
    fun pushFriendToFirestoreForUser_delegatesToRemote() = runTest {
        repository.pushFriendToFirestoreForUser("u1", "f1")

        assertEquals(listOf("u1" to "f1"), remote.pushedPairs)
    }

    @Test
    fun fetchFriendDataAndUpdateDatabase_insertsNewFriendCandidates() = runTest {
        remote.fetchCandidatesResult = listOf(
            FriendCandidate("f1", "Jane", "Doe", GlucoseReading(9, "f1", 5L, 99, ""))
        )

        repository.fetchFriendDataAndUpdateDatabase("u1")

        val inserted = dao.insertedFriends.single()
        assertEquals("f1", inserted.friendId)
        assertEquals("Jane Doe", inserted.friendName)
        assertEquals(99, inserted.friendLastGlucoseReadingValue)
        assertEquals(5L, inserted.friendsLastGlucoseReadingTime)
    }

    @Test
    fun fetchFriendDataAndUpdateDatabase_updatesExistingFriendReading() = runTest {
        dao.allFriendsOnce = listOf(FriendEntity(1, "u1", "f1", "Jane Doe", 80, 1L))
        remote.fetchCandidatesResult = listOf(
            FriendCandidate("f1", "Jane", "Doe", GlucoseReading(9, "f1", 5L, 99, ""))
        )

        repository.fetchFriendDataAndUpdateDatabase("u1")

        assertEquals(0, dao.insertedFriends.size)
        val update = dao.updateLatestReadingCalls.single()
        assertEquals("u1", update.userId)
        assertEquals("f1", update.friendId)
        assertEquals(99, update.readingValue)
        assertEquals(5L, update.timestamp)
    }
}
