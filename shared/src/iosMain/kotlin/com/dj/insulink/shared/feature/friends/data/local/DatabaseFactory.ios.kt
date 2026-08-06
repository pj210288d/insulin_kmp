package com.dj.insulink.shared.feature.friends.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabaseFactory {
    @OptIn(ExperimentalForeignApi::class)
    actual fun create(): RoomDatabase.Builder<FriendsDatabase> {
        val documentDirectory = requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
        )
        val dbFilePath = documentDirectory.path + "/$FRIENDS_DATABASE_FILE_NAME"
        return Room.databaseBuilder<FriendsDatabase>(name = dbFilePath)
    }
}
