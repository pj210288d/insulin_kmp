package com.dj.insulink.shared.feature.insulin.data.repository

import com.dj.insulink.shared.feature.insulin.data.local.dao.InsulinTypeDao
import com.dj.insulink.shared.feature.insulin.data.local.entity.InsulinTypeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeInsulinTypeDao : InsulinTypeDao {
    val insertedEntities = mutableListOf<InsulinTypeEntity>()
    val deletedEntities = mutableListOf<InsulinTypeEntity>()
    var insertAllCalledWith: List<InsulinTypeEntity>? = null
    var deleteAllForUserCalledWith: String? = null

    var allInsulinTypesFlow: Flow<List<InsulinTypeEntity>> = flowOf(emptyList())

    override fun getAllInsulinTypesForUser(userId: String): Flow<List<InsulinTypeEntity>> = allInsulinTypesFlow

    override suspend fun insert(insulinTypeEntity: InsulinTypeEntity): Long {
        insertedEntities += insulinTypeEntity
        return insulinTypeEntity.id
    }

    override suspend fun insertAll(insulinTypes: List<InsulinTypeEntity>) {
        insertAllCalledWith = insulinTypes
    }

    override suspend fun delete(insulinTypeEntity: InsulinTypeEntity) {
        deletedEntities += insulinTypeEntity
    }

    override suspend fun deleteAllForUser(userId: String) {
        deleteAllForUserCalledWith = userId
    }
}
