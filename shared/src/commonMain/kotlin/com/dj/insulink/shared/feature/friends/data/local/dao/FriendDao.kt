package com.dj.insulink.shared.feature.friends.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dj.insulink.shared.feature.friends.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends WHERE userId = :userId")
    fun getAllFriendsForUser(userId: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE userId = :userId")
    suspend fun getAllFriendsForUserOnce(userId: String): List<FriendEntity>

    @Insert
    suspend fun insert(friend: FriendEntity): Long

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(friends: List<FriendEntity>)

    @Query("""
        UPDATE friends
        SET friendLastGlucoseReadingValue = :readingValue,
            friendsLastGlucoseReadingTime = :timestamp
        WHERE friendId = :friendId AND userId = :userId
    """)
    suspend fun updateLatestReading(
        userId: String,
        friendId: String,
        readingValue: Int,
        timestamp: Long
    )
}
