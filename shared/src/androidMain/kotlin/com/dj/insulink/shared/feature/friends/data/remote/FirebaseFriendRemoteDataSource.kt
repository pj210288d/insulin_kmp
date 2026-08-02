package com.dj.insulink.shared.feature.friends.data.remote

import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseFriendRemoteDataSource(
    private val firestore: FirebaseFirestore
) : FriendRemoteDataSource {

    override suspend fun findFriendCandidateByFriendCode(friendCode: String): FriendCandidate? {
        val snapshot = firestore.collection(COLLECTION_NAME_USERS)
            .whereEqualTo(DOCUMENT_FIELD_FRIEND_CODE, friendCode)
            .get()
            .await()

        val document = snapshot.documents.firstOrNull() ?: return null
        return convertDocumentToFriendCandidate(document)
    }

    override suspend fun pushFriendToFirestoreForUser(userId: String, friendId: String) {
        firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .update(DOCUMENT_FIELD_FRIENDS, FieldValue.arrayUnion(friendId))
            .await()
    }

    override suspend fun fetchFriendCandidates(userId: String): List<FriendCandidate> {
        val userSnapshot = firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .get()
            .await()

        val friendIds = userSnapshot.get(DOCUMENT_FIELD_FRIENDS) as? List<String> ?: emptyList()

        if (friendIds.isEmpty()) return emptyList()

        val friendSnapshots = firestore.collection(COLLECTION_NAME_USERS)
            .whereIn(FieldPath.documentId(), friendIds)
            .get()
            .await()

        return friendSnapshots.documents.mapNotNull { doc ->
            convertDocumentToFriendCandidate(doc)
        }
    }

    private fun convertDocumentToFriendCandidate(document: DocumentSnapshot): FriendCandidate? {
        val readings =
            document.get(COLLECTION_NAME_READINGS) as? List<Map<String, Any>> ?: emptyList()
        return try {
            FriendCandidate(
                uid = document.id,
                firstName = document.getString("firstName") ?: "",
                lastName = document.getString("lastName") ?: "",
                latestReading = readings.maxByOrNull { reading ->
                    (reading["timestamp"] as? Number)?.toLong() ?: 0L
                }?.let { map ->
                    GlucoseReading(
                        id = (map["id"] as? Number)?.toLong() ?: 0,
                        value = (map["value"] as? Number)?.toInt() ?: 0,
                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0,
                        comment = map["comment"] as? String ?: "",
                        userId = map["userId"] as? String ?: ""
                    )
                }
            )
        } catch (e: Exception) {
            null
        }
    }
}

private const val COLLECTION_NAME_USERS = "users"
private const val COLLECTION_NAME_READINGS = "readings"
private const val DOCUMENT_FIELD_FRIENDS = "friends"
private const val DOCUMENT_FIELD_FRIEND_CODE = "friendCode"
