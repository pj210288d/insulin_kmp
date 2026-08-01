package com.dj.insulink.shared.feature.fitness.data.repository

import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.fitness.data.local.dao.ExerciseDao
import com.dj.insulink.shared.feature.fitness.data.mapper.toDomain
import com.dj.insulink.shared.feature.fitness.data.mapper.toEntity
import com.dj.insulink.shared.feature.fitness.data.remote.ExerciseRemoteDataSource
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val remoteDataSource: ExerciseRemoteDataSource
) {

    fun getAllExercisesForUser(userId: String): Flow<List<Exercise>> {
        return exerciseDao.getAllExercisesForUser(userId).map {
            it.toDomain()
        }
    }

    suspend fun insert(userId: String, exercise: Exercise) {
        withContext(Dispatchers.IO) {
            try {
                val exerciseWithUniqueId = if (exercise.id == 0L) {
                    exercise.copy(id = currentTimeMillis())
                } else {
                    exercise
                }

                exerciseDao.insert(exerciseWithUniqueId.toEntity())
                remoteDataSource.pushExercise(userId, exerciseWithUniqueId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAllExercisesForUserAndUpdateDatabase(userId: String) {
        withContext(Dispatchers.IO) {
            val fetchedExercises = remoteDataSource.fetchAllExercises(userId)
            exerciseDao.deleteAllForUser(userId)
            exerciseDao.insertAll(fetchedExercises.map { it.toEntity() })
        }
    }

    fun getExercisesBySportName(userId: String, sportName: String): Flow<List<Exercise>> {
        return exerciseDao.getExercisesBySportName(userId, sportName).map {
            it.toDomain()
        }
    }
}
