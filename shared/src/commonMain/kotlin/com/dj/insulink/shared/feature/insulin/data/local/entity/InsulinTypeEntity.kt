package com.dj.insulink.shared.feature.insulin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("insulin_types")
data class InsulinTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val userId: String,
    val name: String
)
