package com.dj.insulink.shared.feature.insulin.data.repository

import com.dj.insulink.shared.feature.insulin.data.remote.InsulinRemoteDataSource
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType

class FakeInsulinRemoteDataSource : InsulinRemoteDataSource {
    val pushed = mutableListOf<Pair<String, InsulinType>>()
    val deleted = mutableListOf<Pair<String, InsulinType>>()
    var fetchAllResult: List<InsulinType> = emptyList()

    override suspend fun pushInsulinType(userId: String, insulinType: InsulinType) {
        pushed += userId to insulinType
    }

    override suspend fun deleteInsulinType(userId: String, insulinType: InsulinType) {
        deleted += userId to insulinType
    }

    override suspend fun fetchAllInsulinTypes(userId: String): List<InsulinType> = fetchAllResult
}
