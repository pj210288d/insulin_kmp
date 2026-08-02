package com.dj.insulink.core.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dj.insulink.feature.friends.data.room.dao.FriendDao
import com.dj.insulink.feature.friends.data.room.entity.FriendEntity

@Database(
    entities = [
        FriendEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InsulinkDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
}
