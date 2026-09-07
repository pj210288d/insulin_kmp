package com.dj.insulink.shared.feature.friends.data.remote

import com.dj.insulink.shared.feature.friends.domain.model.FriendCandidate

interface FriendRemoteDataSource {
    suspend fun findFriendCandidateByFriendCode(friendCode: String): FriendCandidate?
    suspend fun pushFriendToFirestoreForUser(userId: String, friendId: String)
    suspend fun fetchFriendCandidates(userId: String): List<FriendCandidate>

    // Dodato 2026-09-07 na zahtev korisnika (uklanjanje prijatelja) - vidi FriendsViewModel za
    // razlog zašto UI poziv ostaje isključen za sada (namerno, do kraja beta testiranja).
    suspend fun removeFriendFromFirestoreForUser(userId: String, friendId: String)
}
