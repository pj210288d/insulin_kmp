package com.dj.insulink.shared.feature.fitness.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dj.insulink.shared.feature.fitness.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE userId = :userId")
    fun getAllExercisesForUser(userId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE userId = :userId")
    suspend fun getAllExercisesForUserOnce(userId: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE userId = :userId AND sportName = :sportName")
    fun getExercisesBySportName(userId: String, sportName: String): Flow<List<ExerciseEntity>>

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
