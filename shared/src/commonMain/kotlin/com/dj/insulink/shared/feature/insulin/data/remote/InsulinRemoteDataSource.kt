package com.dj.insulink.shared.feature.insulin.data.remote

import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType

interface InsulinRemoteDataSource {
    suspend fun pushInsulinType(userId: String, insulinType: InsulinType)
    suspend fun deleteInsulinType(userId: String, insulinType: InsulinType)
    suspend fun fetchAllInsulinTypes(userId: String): List<InsulinType>
}
