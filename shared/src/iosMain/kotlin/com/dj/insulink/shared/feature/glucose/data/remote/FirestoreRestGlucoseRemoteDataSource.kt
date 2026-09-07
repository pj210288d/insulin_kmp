package com.dj.insulink.shared.feature.glucose.data.remote

import com.dj.insulink.shared.core.auth.IosAuthTokenProvider
import com.dj.insulink.shared.core.firestore.FirestoreRestClient
import com.dj.insulink.shared.core.firestore.FirestoreValue
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import kotlinx.serialization.json.JsonElement

private const val USERS_COLLECTION = "users"
private const val FIELD_READINGS = "readings"

// Faza 2 (cloud sync) - iOS ekvivalent Android-ovog FirebaseGlucoseRemoteDataSource.kt. Android
// koristi arrayUnion za push, get+map/filter+set za update/delete (neatomski) - ovde je SVE
// get+modifikuj+set (nema REST field-transform ekvivalent za arrayUnion bez dodatnog :commit
// poziva), ali krajnji rezultat je isti jer se u ovoj MVP iteraciji ne piše konkurentno sa više
// uređaja istovremeno.
class FirestoreRestGlucoseRemoteDataSource(
    private val firestoreClient: FirestoreRestClient,
    private val tokenProvider: IosAuthTokenProvider
) : GlucoseRemoteDataSource {

    override suspend fun pushReading(userId: String, reading: GlucoseReading) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_READINGS, idToken)
        val updated = current + reading.toFirestoreValue().toJson()
        firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_READINGS, updated, idToken)
    }

    override suspend fun updateReading(userId: String, reading: GlucoseReading) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_READINGS, idToken)
        val updated = current.map { element ->
            if (FirestoreValue.longOf(element, "id") == reading.id) reading.toFirestoreValue().toJson() else element
        }
        firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_READINGS, updated, idToken)
    }

    override suspend fun deleteReading(userId: String, reading: GlucoseReading) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_READINGS, idToken)
        val updated = current.filterNot { FirestoreValue.longOf(it, "id") == reading.id }
        if (updated.size != current.size) {
            firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_READINGS, updated, idToken)
        }
    }

    override suspend fun fetchAllReadings(userId: String): List<GlucoseReading> {
        val idToken = tokenProvider.currentIdToken()
        val elements = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_READINGS, idToken)
        return elements.mapNotNull { it.toGlucoseReadingOrNull() }
    }
}

private fun GlucoseReading.toFirestoreValue() = FirestoreValue.MapVal(
    mapOf(
        "id" to FirestoreValue.IntNum(id),
        "userId" to FirestoreValue.Str(userId),
        "timestamp" to FirestoreValue.IntNum(timestamp),
        "value" to FirestoreValue.IntNum(value.toLong()),
        "comment" to FirestoreValue.Str(comment),
        "insulinTypeId" to FirestoreValue.nullableIntNum(insulinTypeId),
        "insulinUnits" to FirestoreValue.nullableDoubleNum(insulinUnits),
        "linkedMealId" to FirestoreValue.nullableIntNum(linkedMealId)
    )
)

private fun JsonElement.toGlucoseReadingOrNull(): GlucoseReading? {
    val id = FirestoreValue.longOf(this, "id") ?: return null
    return GlucoseReading(
        id = id,
        userId = FirestoreValue.stringOf(this, "userId").orEmpty(),
        timestamp = FirestoreValue.longOf(this, "timestamp") ?: 0L,
        value = (FirestoreValue.longOf(this, "value") ?: 0).toInt(),
        comment = FirestoreValue.stringOf(this, "comment").orEmpty(),
        insulinTypeId = FirestoreValue.longOf(this, "insulinTypeId"),
        insulinUnits = FirestoreValue.doubleOf(this, "insulinUnits"),
        linkedMealId = FirestoreValue.longOf(this, "linkedMealId")
    )
}
