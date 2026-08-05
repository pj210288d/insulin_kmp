package com.dj.insulink.shared.feature.insulin.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dj.insulink.shared.feature.insulin.data.local.entity.InsulinTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsulinTypeDao {

    @Query("SELECT * FROM insulin_types WHERE userId = :userId")
    fun getAllInsulinTypesForUser(userId: String): Flow<List<InsulinTypeEntity>>

    @Insert
    suspend fun insert(insulinTypeEntity: InsulinTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(insulinTypes: List<InsulinTypeEntity>)

    @Delete
    suspend fun delete(insulinTypeEntity: InsulinTypeEntity)

    @Query("DELETE FROM insulin_types WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
