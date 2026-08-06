package com.dj.insulink.shared.feature.fitness.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val userId: String,
    val sportName: String,
    val durationHours: Int,
    val durationMinutes: Int,
    val glucoseBefore: Int,
    val glucoseAfter: Int
)
