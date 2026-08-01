package com.dj.insulink.shared.feature.fitness.data.remote

import com.dj.insulink.shared.feature.fitness.domain.model.Exercise

class NotImplementedExerciseRemoteDataSource : ExerciseRemoteDataSource {
    override suspend fun pushExercise(userId: String, exercise: Exercise): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun fetchAllExercises(userId: String): List<Exercise> =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")
}
