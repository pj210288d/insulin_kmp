package com.dj.insulink.shared.feature.fitness.domain.model

data class Exercise(
    val id: Long,
    val userId: String,
    val sportName: String,
    val durationHours: Int,
    val durationMinutes: Int,
    val glucoseBefore: Int,
    val glucoseAfter: Int
)
