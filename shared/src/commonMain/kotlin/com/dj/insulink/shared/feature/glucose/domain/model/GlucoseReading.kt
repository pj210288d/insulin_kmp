package com.dj.insulink.shared.feature.glucose.domain.model

data class GlucoseReading(
    val id: Long,
    val userId: String,
    val timestamp: Long,
    val value: Int,
    val comment: String
)
