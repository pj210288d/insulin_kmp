package com.dj.insulink.shared.feature.insulin.data.remote

import com.dj.insulink.shared.core.auth.IosAuthTokenProvider
import com.dj.insulink.shared.core.firestore.FirestoreRestClient
import com.dj.insulink.shared.core.firestore.FirestoreValue
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import kotlinx.serialization.json.JsonElement

private const val USERS_COLLECTION = "users"
private const val FIELD_INSULIN_TYPES = "insulinTypes"

// Faza 2 (cloud sync) - iOS ekvivalent Android-ovog FirebaseInsulinRemoteDataSource.kt, preko
// Firestore REST-a umesto GMS SDK-a (arhitektonska odluka iz plana). Insulin je prvi feature u
// redosledu (najprostiji model - samo id/userId/name). Isti neatomski get-modifikuj-upiši
// obrazac kao Android-ov update/delete (push tamo koristi arrayUnion, ovde nema REST
// field-transform ekvivalenta bez dodatnog :commit poziva - za ovaj obim dovoljno je i ovako,
// pošto nema konkurentnih pisanja sa više uređaja u ovoj MVP iteraciji).
class FirestoreRestInsulinRemoteDataSource(
    private val firestoreClient: FirestoreRestClient,
    private val tokenProvider: IosAuthTokenProvider
) : InsulinRemoteDataSource {

    override suspend fun pushInsulinType(userId: String, insulinType: InsulinType) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_INSULIN_TYPES, idToken)
        val updated = current + insulinType.toFirestoreValue().toJson()
        firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_INSULIN_TYPES, updated, idToken)
    }

    override suspend fun deleteInsulinType(userId: String, insulinType: InsulinType) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_INSULIN_TYPES, idToken)
        val updated = current.filterNot { FirestoreValue.longOf(it, "id") == insulinType.id }
        if (updated.size != current.size) {
            firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_INSULIN_TYPES, updated, idToken)
        }
    }

    override suspend fun fetchAllInsulinTypes(userId: String): List<InsulinType> {
        val idToken = tokenProvider.currentIdToken()
        val elements = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_INSULIN_TYPES, idToken)
        return elements.mapNotNull { it.toInsulinTypeOrNull() }
    }
}

private fun InsulinType.toFirestoreValue() = FirestoreValue.MapVal(
    mapOf(
        "id" to FirestoreValue.IntNum(id),
        "userId" to FirestoreValue.Str(userId),
        "name" to FirestoreValue.Str(name)
    )
)

private fun JsonElement.toInsulinTypeOrNull(): InsulinType? {
    val id = FirestoreValue.longOf(this, "id") ?: return null
    return InsulinType(
        id = id,
        userId = FirestoreValue.stringOf(this, "userId").orEmpty(),
        name = FirestoreValue.stringOf(this, "name").orEmpty()
    )
}
