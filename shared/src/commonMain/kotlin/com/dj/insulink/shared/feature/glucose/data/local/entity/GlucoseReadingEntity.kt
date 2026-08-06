package com.dj.insulink.shared.feature.glucose.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_readings")
data class GlucoseReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val userId: String,
    val timestamp: Long,
    val value: Int,
    val comment: String,
    val insulinTypeId: Long? = null,
    val insulinUnits: Double? = null,
    val linkedMealId: Long? = null
)
