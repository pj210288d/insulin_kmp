package com.dj.insulink.shared.feature.friends.data.remote

import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate

interface FriendRemoteDataSource {
    suspend fun findFriendCandidateByFriendCode(friendCode: String): FriendCandidate?
    suspend fun pushFriendToFirestoreForUser(userId: String, friendId: String)
    suspend fun fetchFriendCandidates(userId: String): List<FriendCandidate>
}
