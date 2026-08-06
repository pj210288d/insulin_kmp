package com.dj.insulink.shared.feature.friends.domain.model

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading

data class FriendCandidate(
    val uid: String,
    val firstName: String,
    val lastName: String,
    val latestReading: GlucoseReading?
)
