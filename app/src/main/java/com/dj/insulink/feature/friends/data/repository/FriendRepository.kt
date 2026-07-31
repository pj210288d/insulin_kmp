package com.dj.insulink.feature.friends.data.repository

import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.feature.friends.data.room.dao.FriendDao
import com.dj.insulink.feature.friends.domain.mappers.toDomain
import com.dj.insulink.feature.friends.domain.mappers.toEntity
import com.dj.insulink.feature.friends.domain.models.Friend
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FriendRepository @Inject constructor(
    private val friendDao: FriendDao,
    private val firestore: FirebaseFirestore
) {

    fun getAllFriendsForUser(userId: String): Flow<List<Friend>> {
        return friendDao.getAllFriendsForUser(userId).map {
            it.toDomain()
        }
    }

    suspend fun findUserByFriendCode(friendCode: String): UserWithLatestReading? {
        val snapshot = firestore.collection(COLLECTION_NAME_USERS)
            .whereEqualTo(DOCUMENT_FIELD_FRIEND_CODE, friendCode)
            .get()
            .await()

        val document = snapshot.documents.first()
        return convertDocumentToUserWithGlucoseReading(document)
    }

    suspend fun addFriend(friend: Friend) {
        withContext(Dispatchers.IO) {
            friendDao.insert(friend.toEntity())
        }
    }

    suspend fun pushFriendToFirestoreForUser(userId: String, friendId: String) {
        withContext(Dispatchers.IO) {
            firestore.collection(COLLECTION_NAME_USERS)
                .document(userId)
                .update(DOCUMENT_FIELD_FRIENDS, FieldValue.arrayUnion(friendId))
                .await()
        }
    }

    suspend fun fetchFriendDataAndUpdateDatabase(userId: String) {
        withContext(Dispatchers.IO) {
            val friendsList = friendDao.getAllFriendsForUserOnce(userId)
            val friendsData = fetchFriendData(userId)

            friendsData.forEach { friend ->
                if (friendsList.map { it.friendId }.contains(friend.user.uid)) {
                    friend.latestReading?.let {
                        friendDao.updateLatestReading(
                            userId,
                            friend.user.uid,
                            it.value,
                            it.timestamp
                        )
                    }
                } else {
                    addFriend(
                        Friend(
                            id = 0,
                            userId = userId,
                            friendId = friend.user.uid,
                            friendName = "${friend.user.firstName} ${friend.user.lastName}",
                            friendLastGlucoseReadingValue = friend.latestReading?.value,
                            friendsLastGlucoseReadingTime = friend.latestReading?.timestamp
                        )
                    )
                }
            }
        }
    }

    private suspend fun fetchFriendData(userId: String): List<UserWithLatestReading> {
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
            convertDocumentToUserWithGlucoseReading(doc)
        }
    }

    private fun convertDocumentToUserWithGlucoseReading(document: DocumentSnapshot): UserWithLatestReading? {
        val readings =
            document.get(COLLECTION_NAME_READINGS) as? List<Map<String, Any>> ?: emptyList()
        return try {
            UserWithLatestReading(
                user = User(
                    uid = document.id,
                    firstName = document.getString("firstName") ?: "",
                    lastName = document.getString("lastName") ?: "",
                    email = document.getString("email") ?: "",
                    friendCode = document.getString("friendCode") ?: "",
                    isEmailVerified = document.getBoolean("isEmailVerified") ?: false,
                ),
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
            return null
        }
    }
}

data class UserWithLatestReading(
    val user: User,
    val latestReading: GlucoseReading?
)

private const val COLLECTION_NAME_USERS = "users"
private const val COLLECTION_NAME_READINGS = "readings"
private const val DOCUMENT_FIELD_FRIENDS = "friends"
private const val DOCUMENT_FIELD_FRIEND_CODE = "friendCode"
