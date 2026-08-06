package com.dj.insulink.shared.feature.friends.data.mapper

import com.dj.insulink.shared.feature.friends.data.local.entity.FriendEntity
import com.dj.insulink.shared.feature.friends.domain.model.Friend

fun FriendEntity.toDomain(): Friend {
    return Friend(
        id = id,
        userId = userId,
        friendId = friendId,
        friendName = friendName,
        friendLastGlucoseReadingValue = friendLastGlucoseReadingValue,
        friendsLastGlucoseReadingTime = friendsLastGlucoseReadingTime
    )
}

fun Friend.toEntity(): FriendEntity {
    return FriendEntity(
        id = id,
        userId = userId,
        friendId = friendId,
        friendName = friendName,
        friendLastGlucoseReadingValue = friendLastGlucoseReadingValue,
        friendsLastGlucoseReadingTime = friendsLastGlucoseReadingTime
    )
}

fun List<FriendEntity>.toDomain(): List<Friend> {
    return map { it.toDomain() }
}

fun List<Friend>.toEntity(): List<FriendEntity> {
    return map { it.toEntity() }
}
