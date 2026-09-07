package com.dj.insulink.shared.feature.fitness.data.remote

import com.dj.insulink.shared.core.auth.IosAuthTokenProvider
import com.dj.insulink.shared.core.firestore.FirestoreRestClient
import com.dj.insulink.shared.core.firestore.FirestoreValue
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import kotlinx.serialization.json.JsonElement

private const val USERS_COLLECTION = "users"
private const val FIELD_EXERCISES = "exercises"

// Faza 2 (cloud sync) - iOS ekvivalent Android-ovog FirebaseExerciseRemoteDataSource.kt. Nema
// delete/update na Android strani (ExerciseDao nema per-item delete - vidi dnevnik 2026-09-05),
// pa ni ovde - samo push/fetch, isti stvarni paritet kao Android.
class FirestoreRestExerciseRemoteDataSource(
    private val firestoreClient: FirestoreRestClient,
    private val tokenProvider: IosAuthTokenProvider
) : ExerciseRemoteDataSource {

    override suspend fun pushExercise(userId: String, exercise: Exercise) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_EXERCISES, idToken)
        val updated = current + exercise.toFirestoreValue().toJson()
        firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_EXERCISES, updated, idToken)
    }

    override suspend fun fetchAllExercises(userId: String): List<Exercise> {
        val idToken = tokenProvider.currentIdToken()
        val elements = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_EXERCISES, idToken)
        return elements.mapNotNull { it.toExerciseOrNull() }
    }
}

private fun Exercise.toFirestoreValue() = FirestoreValue.MapVal(
    mapOf(
        "id" to FirestoreValue.IntNum(id),
        "userId" to FirestoreValue.Str(userId),
        "sportName" to FirestoreValue.Str(sportName),
        "durationHours" to FirestoreValue.IntNum(durationHours.toLong()),
        "durationMinutes" to FirestoreValue.IntNum(durationMinutes.toLong()),
        "glucoseBefore" to FirestoreValue.IntNum(glucoseBefore.toLong()),
        "glucoseAfter" to FirestoreValue.IntNum(glucoseAfter.toLong())
    )
)

private fun JsonElement.toExerciseOrNull(): Exercise? {
    val id = FirestoreValue.longOf(this, "id") ?: return null
    return Exercise(
        id = id,
        userId = FirestoreValue.stringOf(this, "userId").orEmpty(),
        sportName = FirestoreValue.stringOf(this, "sportName").orEmpty(),
        durationHours = (FirestoreValue.longOf(this, "durationHours") ?: 0).toInt(),
        durationMinutes = (FirestoreValue.longOf(this, "durationMinutes") ?: 0).toInt(),
        glucoseBefore = (FirestoreValue.longOf(this, "glucoseBefore") ?: 0).toInt(),
        glucoseAfter = (FirestoreValue.longOf(this, "glucoseAfter") ?: 0).toInt()
    )
}
