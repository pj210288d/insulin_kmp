package com.dj.insulink.shared.feature.fitness.data.repository

import com.dj.insulink.shared.feature.fitness.data.remote.ExerciseRemoteDataSource
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise

class FakeExerciseRemoteDataSource : ExerciseRemoteDataSource {
    val pushed = mutableListOf<Pair<String, Exercise>>()
    var fetchAllResult: List<Exercise> = emptyList()

    override suspend fun pushExercise(userId: String, exercise: Exercise) {
        pushed += userId to exercise
    }

    override suspend fun fetchAllExercises(userId: String): List<Exercise> = fetchAllResult
}
