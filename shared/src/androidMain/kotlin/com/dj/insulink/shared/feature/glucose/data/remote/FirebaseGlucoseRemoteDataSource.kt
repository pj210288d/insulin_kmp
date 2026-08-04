package com.dj.insulink.shared.feature.glucose.data.remote

import android.util.Log
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseGlucoseRemoteDataSource(
    private val firestore: FirebaseFirestore
) : GlucoseRemoteDataSource {

    override suspend fun pushReading(userId: String, reading: GlucoseReading) {
        firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .update(DOCUMENT_FIELD_READINGS, FieldValue.arrayUnion(reading))
            .await()
    }

    override suspend fun deleteReading(userId: String, reading: GlucoseReading) {
        try {
            val userDocumentRef = firestore.collection(COLLECTION_NAME_USERS).document(userId)

            val snapshot = userDocumentRef.get().await()
            val readings =
                snapshot.get(DOCUMENT_FIELD_READINGS) as? List<Map<String, Any>> ?: emptyList()

            val updatedReadings = readings.filter { readingMap ->
                val idMatches = (readingMap["id"] as? Number)?.toLong() == reading.id

                !idMatches
            }

            if (readings.size != updatedReadings.size) {
                userDocumentRef.update(DOCUMENT_FIELD_READINGS, updatedReadings).await()
            } else {
                Log.w("GlucoseRemoteDataSource", "No reading found with ID ${reading.id} to delete")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun fetchAllReadings(userId: String): List<GlucoseReading> {
        val document = firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .get()
            .await()

        val readingsData = document.get(DOCUMENT_FIELD_READINGS) as? List<Map<String, Any>> ?: emptyList()

        return readingsData.map { readingMap ->
            GlucoseReading(
                id = (readingMap["id"] as? Number)?.toLong() ?: 0,
                value = (readingMap["value"] as? Number)?.toInt() ?: 0,
                timestamp = (readingMap["timestamp"] as? Number)?.toLong() ?: 0,
                comment = readingMap["comment"] as? String ?: "",
                userId = readingMap["userId"] as? String ?: "",
                insulinTypeId = (readingMap["insulinTypeId"] as? Number)?.toLong(),
                insulinUnits = (readingMap["insulinUnits"] as? Number)?.toDouble(),
                linkedMealId = (readingMap["linkedMealId"] as? Number)?.toLong(),
            )
        }
    }
}

private const val COLLECTION_NAME_USERS = "users"
private const val DOCUMENT_FIELD_READINGS = "readings"
