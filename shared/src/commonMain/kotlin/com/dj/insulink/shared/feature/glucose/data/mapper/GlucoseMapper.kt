package com.dj.insulink.shared.feature.glucose.data.mapper

import com.dj.insulink.shared.feature.glucose.data.local.entity.GlucoseReadingEntity
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading

// Entity -> Domain
fun GlucoseReadingEntity.toDomain(): GlucoseReading {
    return GlucoseReading(
        id = id,
        userId = userId,
        timestamp = timestamp,
        value = value,
        comment = comment,
        insulinTypeId = insulinTypeId,
        insulinUnits = insulinUnits,
        linkedMealId = linkedMealId
    )
}

// Domain -> Entity
fun GlucoseReading.toEntity(): GlucoseReadingEntity {
    return GlucoseReadingEntity(
        id = id,
        userId = userId,
        timestamp = timestamp,
        value = value,
        comment = comment,
        insulinTypeId = insulinTypeId,
        insulinUnits = insulinUnits,
        linkedMealId = linkedMealId
    )
}

fun List<GlucoseReadingEntity>.toDomain(): List<GlucoseReading> {
    return map { it.toDomain() }
}

fun List<GlucoseReading>.toEntity(): List<GlucoseReadingEntity> {
    return map { it.toEntity() }
}
