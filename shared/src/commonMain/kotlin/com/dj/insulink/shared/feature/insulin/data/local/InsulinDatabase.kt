package com.dj.insulink.shared.feature.insulin.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dj.insulink.shared.feature.insulin.data.local.dao.InsulinTypeDao
import com.dj.insulink.shared.feature.insulin.data.local.entity.InsulinTypeEntity
import com.dj.insulink.shared.core.dispatcher.ioDispatcher

internal const val INSULIN_DATABASE_FILE_NAME = "insulin_types.db"

@Database(entities = [InsulinTypeEntity::class], version = 1, exportSchema = false)
@ConstructedBy(InsulinDatabaseConstructor::class)
abstract class InsulinDatabase : RoomDatabase() {
    abstract fun insulinTypeDao(): InsulinTypeDao
}

// Room's KSP compiler generates the platform `actual` for this object.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object InsulinDatabaseConstructor : RoomDatabaseConstructor<InsulinDatabase> {
    override fun initialize(): InsulinDatabase
}

fun buildInsulinDatabase(builder: RoomDatabase.Builder<InsulinDatabase>): InsulinDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .build()
}
