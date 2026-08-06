package com.dj.insulink.shared.feature.friends.data.repository

import com.dj.insulink.shared.feature.friends.data.local.dao.FriendDao
import com.dj.insulink.shared.feature.friends.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFriendDao : FriendDao {
    val insertedFriends = mutableListOf<FriendEntity>()
    var insertAllCalledWith: List<FriendEntity>? = null
    var insertReturns: Long = 0L

    var allFriendsFlow: Flow<List<FriendEntity>> = flowOf(emptyList())
    var allFriendsOnce: List<FriendEntity> = emptyList()

    data class UpdateLatestReadingCall(
        val userId: String,
        val friendId: String,
        val readingValue: Int,
        val timestamp: Long
    )

    val updateLatestReadingCalls = mutableListOf<UpdateLatestReadingCall>()

    override fun getAllFriendsForUser(userId: String): Flow<List<FriendEntity>> = allFriendsFlow

    override suspend fun getAllFriendsForUserOnce(userId: String): List<FriendEntity> = allFriendsOnce

    override suspend fun insert(friend: FriendEntity): Long {
        insertedFriends += friend
        return insertReturns
    }

    override suspend fun insertAll(friends: List<FriendEntity>) {
        insertAllCalledWith = friends
    }

    override suspend fun updateLatestReading(
        userId: String,
        friendId: String,
        readingValue: Int,
        timestamp: Long
    ) {
        updateLatestReadingCalls += UpdateLatestReadingCall(userId, friendId, readingValue, timestamp)
    }
}
