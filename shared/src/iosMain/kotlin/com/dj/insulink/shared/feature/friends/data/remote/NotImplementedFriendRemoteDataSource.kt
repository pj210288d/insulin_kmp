package com.dj.insulink.shared.feature.friends.data.remote

import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate

class NotImplementedFriendRemoteDataSource : FriendRemoteDataSource {
    override suspend fun findFriendCandidateByFriendCode(friendCode: String): FriendCandidate? =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun pushFriendToFirestoreForUser(userId: String, friendId: String): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun fetchFriendCandidates(userId: String): List<FriendCandidate> =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")
}
