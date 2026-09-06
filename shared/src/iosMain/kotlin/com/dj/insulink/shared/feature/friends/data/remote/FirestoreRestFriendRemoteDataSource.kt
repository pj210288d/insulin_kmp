package com.dj.insulink.shared.feature.friends.data.remote

import com.dj.insulink.shared.core.auth.IosAuthTokenProvider
import com.dj.insulink.shared.core.firestore.FirestoreRestClient
import com.dj.insulink.shared.core.firestore.FirestoreValue
import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import kotlinx.serialization.json.JsonObject

private const val USERS_COLLECTION = "users"
private const val FIELD_FRIENDS = "friends"
private const val FIELD_READINGS = "readings"

// Faza 3 (Friends) - iOS ekvivalent Android-ovog FirebaseFriendRemoteDataSource.kt.
// findFriendCandidateByFriendCode koristi PRAVI Firestore query (:runQuery, FirestoreRestClient.
// queryEqual) - jedino mesto u celom projektu gde iOS strana mora da pretraži celu kolekciju po
// proizvoljnom polju, ne po poznatom document id-u. fetchFriendCandidates namerno radi N
// pojedinačnih getDocumentFields poziva (po jedan po prijatelju) umesto Android-ovog
// whereIn(FieldPath.documentId(), ...) grupnog upita - liste prijatelja su tipično male, a
// izbegava se implementacija IN operatora za structured query (dodatna složenost bez stvarne
// potrebe u ovom obimu).
class FirestoreRestFriendRemoteDataSource(
    private val firestoreClient: FirestoreRestClient,
    private val tokenProvider: IosAuthTokenProvider
) : FriendRemoteDataSource {

    override suspend fun findFriendCandidateByFriendCode(friendCode: String): FriendCandidate? {
        val idToken = tokenProvider.currentIdToken()
        val (docId, fields) = firestoreClient
            .queryEqual(USERS_COLLECTION, "friendCode", friendCode, idToken, limit = 1)
            .firstOrNull() ?: return null
        return toFriendCandidate(docId, fields)
    }

    override suspend fun pushFriendToFirestoreForUser(userId: String, friendId: String) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_FRIENDS, idToken)
        if (current.any { FirestoreValue.plainStringOf(it) == friendId }) return
        val updated = current + FirestoreValue.Str(friendId).toJson()
        firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_FRIENDS, updated, idToken)
    }

    override suspend fun fetchFriendCandidates(userId: String): List<FriendCandidate> {
        val idToken = tokenProvider.currentIdToken()
        val friendIds = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_FRIENDS, idToken)
            .mapNotNull { FirestoreValue.plainStringOf(it) }
        if (friendIds.isEmpty()) return emptyList()
        return friendIds.mapNotNull { friendUid ->
            val fields = firestoreClient.getDocumentFields(USERS_COLLECTION, friendUid, idToken)
                ?: return@mapNotNull null
            toFriendCandidate(friendUid, fields)
        }
    }

    private fun toFriendCandidate(uid: String, fields: JsonObject): FriendCandidate {
        val latest = FirestoreValue.arrayElements(fields, FIELD_READINGS)
            .mapNotNull { element ->
                val id = FirestoreValue.longOf(element, "id") ?: return@mapNotNull null
                val timestamp = FirestoreValue.longOf(element, "timestamp") ?: return@mapNotNull null
                GlucoseReading(
                    id = id,
                    userId = FirestoreValue.stringOf(element, "userId").orEmpty(),
                    timestamp = timestamp,
                    value = (FirestoreValue.longOf(element, "value") ?: 0).toInt(),
                    comment = FirestoreValue.stringOf(element, "comment").orEmpty()
                )
            }
            .maxByOrNull { it.timestamp }
        return FriendCandidate(
            uid = uid,
            firstName = FirestoreValue.stringOrNull(fields, "firstName").orEmpty(),
            lastName = FirestoreValue.stringOrNull(fields, "lastName").orEmpty(),
            latestReading = latest
        )
    }
}
