package com.dj.insulink.shared.feature.fitness.data.remote

import com.dj.insulink.shared.feature.fitness.domain.model.Exercise

interface ExerciseRemoteDataSource {
    suspend fun pushExercise(userId: String, exercise: Exercise)
    suspend fun fetchAllExercises(userId: String): List<Exercise>
}
