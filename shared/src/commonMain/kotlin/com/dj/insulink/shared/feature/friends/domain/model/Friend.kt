package com.dj.insulink.shared.feature.friends.domain.model

data class Friend(
    val id: Long,
    val userId: String,
    val friendId: String,
    val friendName: String,
    val friendLastGlucoseReadingValue: Int?,
    val friendsLastGlucoseReadingTime: Long?
)
