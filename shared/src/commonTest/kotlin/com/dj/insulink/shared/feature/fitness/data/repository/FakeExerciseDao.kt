package com.dj.insulink.shared.feature.fitness.data.repository

import com.dj.insulink.shared.feature.fitness.data.local.dao.ExerciseDao
import com.dj.insulink.shared.feature.fitness.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeExerciseDao : ExerciseDao {
    val insertedExercises = mutableListOf<ExerciseEntity>()
    var insertAllCalledWith: List<ExerciseEntity>? = null
    var deleteAllForUserCalledWith: String? = null
    var insertReturns: Long = 0L

    var allExercisesFlow: Flow<List<ExerciseEntity>> = flowOf(emptyList())
    var exercisesBySportFlow: Flow<List<ExerciseEntity>> = flowOf(emptyList())
    var allExercisesOnce: List<ExerciseEntity> = emptyList()

    override fun getAllExercisesForUser(userId: String): Flow<List<ExerciseEntity>> = allExercisesFlow

    override suspend fun getAllExercisesForUserOnce(userId: String): List<ExerciseEntity> = allExercisesOnce

    override fun getExercisesBySportName(userId: String, sportName: String): Flow<List<ExerciseEntity>> = exercisesBySportFlow

    override suspend fun insert(exercise: ExerciseEntity): Long {
        insertedExercises += exercise
        return insertReturns
    }

    override suspend fun insertAll(exercises: List<ExerciseEntity>) {
        insertAllCalledWith = exercises
    }

    override suspend fun deleteAllForUser(userId: String) {
        deleteAllForUserCalledWith = userId
    }
}
