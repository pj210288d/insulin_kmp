package com.dj.insulink.shared.feature.friends.data.local

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<FriendsDatabase>
}
