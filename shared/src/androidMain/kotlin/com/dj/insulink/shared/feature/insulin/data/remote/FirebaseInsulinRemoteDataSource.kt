package com.dj.insulink.shared.feature.insulin.data.remote

import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseInsulinRemoteDataSource(
    private val firestore: FirebaseFirestore
) : InsulinRemoteDataSource {

    override suspend fun pushInsulinType(userId: String, insulinType: InsulinType) {
        firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .update(DOCUMENT_FIELD_INSULIN_TYPES, FieldValue.arrayUnion(insulinType))
            .await()
    }

    override suspend fun deleteInsulinType(userId: String, insulinType: InsulinType) {
        try {
            val userDocumentRef = firestore.collection(COLLECTION_NAME_USERS).document(userId)

            val snapshot = userDocumentRef.get().await()
            val insulinTypes =
                snapshot.get(DOCUMENT_FIELD_INSULIN_TYPES) as? List<Map<String, Any>> ?: emptyList()

            val updatedInsulinTypes = insulinTypes.filter { insulinTypeMap ->
                val idMatches = (insulinTypeMap["id"] as? Number)?.toLong() == insulinType.id

                !idMatches
            }

            if (insulinTypes.size != updatedInsulinTypes.size) {
                userDocumentRef.update(DOCUMENT_FIELD_INSULIN_TYPES, updatedInsulinTypes).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun fetchAllInsulinTypes(userId: String): List<InsulinType> {
        val document = firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .get()
            .await()

        val insulinTypesData =
            document.get(DOCUMENT_FIELD_INSULIN_TYPES) as? List<Map<String, Any>> ?: emptyList()

        return insulinTypesData.map { insulinTypeMap ->
            InsulinType(
                id = (insulinTypeMap["id"] as? Number)?.toLong() ?: 0,
                userId = insulinTypeMap["userId"] as? String ?: "",
                name = insulinTypeMap["name"] as? String ?: "",
            )
        }
    }
}

private const val COLLECTION_NAME_USERS = "users"
private const val DOCUMENT_FIELD_INSULIN_TYPES = "insulinTypes"
