package com.dj.insulink.shared.feature.friends.data.repository

import com.dj.insulink.shared.feature.friends.data.remote.FriendRemoteDataSource
import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate

class FakeFriendRemoteDataSource : FriendRemoteDataSource {
    val pushedPairs = mutableListOf<Pair<String, String>>()
    var findResult: FriendCandidate? = null
    var fetchCandidatesResult: List<FriendCandidate> = emptyList()

    override suspend fun findFriendCandidateByFriendCode(friendCode: String): FriendCandidate? = findResult

    override suspend fun pushFriendToFirestoreForUser(userId: String, friendId: String) {
        pushedPairs += userId to friendId
    }

    override suspend fun fetchFriendCandidates(userId: String): List<FriendCandidate> = fetchCandidatesResult
}
