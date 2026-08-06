package com.dj.insulink.shared.feature.friends.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dj.insulink.shared.feature.friends.data.local.dao.FriendDao
import com.dj.insulink.shared.feature.friends.data.local.entity.FriendEntity
import kotlinx.coroutines.Dispatchers

internal const val FRIENDS_DATABASE_FILE_NAME = "friends.db"

@Database(entities = [FriendEntity::class], version = 1, exportSchema = false)
@ConstructedBy(FriendsDatabaseConstructor::class)
abstract class FriendsDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
}

// Room's KSP compiler generates the platform `actual` for this object.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FriendsDatabaseConstructor : RoomDatabaseConstructor<FriendsDatabase> {
    override fun initialize(): FriendsDatabase
}

fun buildFriendsDatabase(builder: RoomDatabase.Builder<FriendsDatabase>): FriendsDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
