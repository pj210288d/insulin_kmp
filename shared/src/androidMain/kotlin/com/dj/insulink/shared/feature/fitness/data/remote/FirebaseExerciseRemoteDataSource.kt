package com.dj.insulink.shared.feature.fitness.data.remote

import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseExerciseRemoteDataSource(
    private val firestore: FirebaseFirestore
) : ExerciseRemoteDataSource {

    override suspend fun pushExercise(userId: String, exercise: Exercise) {
        firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .update(DOCUMENT_FIELD_EXERCISES, FieldValue.arrayUnion(exercise))
            .await()
    }

    override suspend fun fetchAllExercises(userId: String): List<Exercise> {
        val document = firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .get()
            .await()

        val exercisesData = document.get(DOCUMENT_FIELD_EXERCISES) as? List<Map<String, Any>> ?: emptyList()

        return exercisesData.map { exerciseMap ->
            Exercise(
                id = (exerciseMap["id"] as? Number)?.toLong() ?: 0,
                sportName = exerciseMap["sportName"] as? String ?: "",
                durationHours = (exerciseMap["durationHours"] as? Number)?.toInt() ?: 0,
                durationMinutes = (exerciseMap["durationMinutes"] as? Number)?.toInt() ?: 0,
                glucoseBefore = (exerciseMap["glucoseBefore"] as? Number)?.toInt() ?: 0,
                glucoseAfter = (exerciseMap["glucoseAfter"] as? Number)?.toInt() ?: 0,
                userId = exerciseMap["userId"] as? String ?: ""
            )
        }
    }
}

private const val COLLECTION_NAME_USERS = "users"
private const val DOCUMENT_FIELD_EXERCISES = "exercises"
