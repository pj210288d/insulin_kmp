package com.dj.insulink.shared.feature.friends.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val userId: String,
    val friendId: String,
    val friendName: String,
    val friendLastGlucoseReadingValue: Int?,
    val friendsLastGlucoseReadingTime: Long?
)
