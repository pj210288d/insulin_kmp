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

    // Dodato 2026-09-07 na zahtev korisnika (uklanjanje prijatelja + sprečavanje duplikata) - vidi
    // FriendsViewModel.removeFriend/addFriend komentare zašto poziv iz UI-ja ostaje isključen
    // (namerno, do kraja beta testiranja - korisnik želi da ovo bude bug koji beta korisnici sami
    // otkriju). DAO metoda sama po sebi je inertna dok se ne pozove.
    @Query("DELETE FROM friends WHERE userId = :userId AND friendId = :friendId")
    suspend fun deleteFriend(userId: String, friendId: String)

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
