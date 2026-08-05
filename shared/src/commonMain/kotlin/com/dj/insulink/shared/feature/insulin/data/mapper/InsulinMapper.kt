package com.dj.insulink.shared.feature.insulin.data.mapper

import com.dj.insulink.shared.feature.insulin.data.local.entity.InsulinTypeEntity
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType

fun InsulinTypeEntity.toDomain(): InsulinType {
    return InsulinType(
        id = id,
        userId = userId,
        name = name
    )
}

fun InsulinType.toEntity(): InsulinTypeEntity {
    return InsulinTypeEntity(
        id = id,
        userId = userId,
        name = name
    )
}

fun List<InsulinTypeEntity>.toDomain(): List<InsulinType> {
    return map { it.toDomain() }
}

fun List<InsulinType>.toEntity(): List<InsulinTypeEntity> {
    return map { it.toEntity() }
}
