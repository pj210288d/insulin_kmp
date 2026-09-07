package com.dj.insulink.shared.feature.friends.data.repository

import com.dj.insulink.shared.feature.friends.data.local.dao.FriendDao
import com.dj.insulink.shared.feature.friends.data.mapper.toDomain
import com.dj.insulink.shared.feature.friends.data.mapper.toEntity
import com.dj.insulink.shared.feature.friends.data.remote.FriendRemoteDataSource
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate
import com.dj.insulink.shared.core.dispatcher.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FriendRepository(
    private val friendDao: FriendDao,
    private val remoteDataSource: FriendRemoteDataSource
) {

    fun getAllFriendsForUser(userId: String): Flow<List<Friend>> {
        return friendDao.getAllFriendsForUser(userId).map {
            it.toDomain()
        }
    }

    suspend fun findFriendCandidateByFriendCode(friendCode: String): FriendCandidate? {
        return remoteDataSource.findFriendCandidateByFriendCode(friendCode)
    }

    suspend fun addFriend(friend: Friend) {
        withContext(ioDispatcher) {
            friendDao.insert(friend.toEntity())
        }
    }

    /** true ako je friendId već u korisnikovoj listi prijatelja - vidi FriendsViewModel.addFriend(). */
    suspend fun isFriendAlready(userId: String, friendId: String): Boolean {
        return withContext(ioDispatcher) {
            friendDao.getAllFriendsForUserOnce(userId).any { it.friendId == friendId }
        }
    }

    // Dodato 2026-09-07 na zahtev korisnika (uklanjanje prijatelja) - vidi FriendsViewModel za
    // razlog zašto poziv iz UI-ja ostaje isključen za sada (namerno, do kraja beta testiranja).
    suspend fun deleteFriend(userId: String, friendId: String) {
        withContext(ioDispatcher) {
            friendDao.deleteFriend(userId, friendId)
            remoteDataSource.removeFriendFromFirestoreForUser(userId, friendId)
        }
    }

    suspend fun pushFriendToFirestoreForUser(userId: String, friendId: String) {
        withContext(ioDispatcher) {
            remoteDataSource.pushFriendToFirestoreForUser(userId, friendId)
        }
    }

    suspend fun fetchFriendDataAndUpdateDatabase(userId: String) {
        withContext(ioDispatcher) {
            val friendsList = friendDao.getAllFriendsForUserOnce(userId)
            val friendCandidates = remoteDataSource.fetchFriendCandidates(userId)

            friendCandidates.forEach { candidate ->
                if (friendsList.map { it.friendId }.contains(candidate.uid)) {
                    candidate.latestReading?.let {
                        friendDao.updateLatestReading(
                            userId,
                            candidate.uid,
                            it.value,
                            it.timestamp
                        )
                    }
                } else {
                    addFriend(
                        Friend(
                            id = 0,
                            userId = userId,
                            friendId = candidate.uid,
                            friendName = "${candidate.firstName} ${candidate.lastName}",
                            friendLastGlucoseReadingValue = candidate.latestReading?.value,
                            friendsLastGlucoseReadingTime = candidate.latestReading?.timestamp
                        )
                    )
                }
            }
        }
    }
}
