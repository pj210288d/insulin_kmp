package com.dj.insulink.shared.feature.insulin.data.repository

import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.insulin.data.local.dao.InsulinTypeDao
import com.dj.insulink.shared.feature.insulin.data.mapper.toDomain
import com.dj.insulink.shared.feature.insulin.data.mapper.toEntity
import com.dj.insulink.shared.feature.insulin.data.remote.InsulinRemoteDataSource
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.shared.core.dispatcher.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InsulinTypeRepository(
    private val insulinTypeDao: InsulinTypeDao,
    private val remoteDataSource: InsulinRemoteDataSource
) {

    fun getAllInsulinTypesForUser(userId: String): Flow<List<InsulinType>> {
        return insulinTypeDao.getAllInsulinTypesForUser(userId).map { it.toDomain() }
    }

    suspend fun insert(userId: String, insulinType: InsulinType) {
        withContext(ioDispatcher) {
            try {
                val insulinTypeWithUniqueId = if (insulinType.id == 0L) {
                    insulinType.copy(id = currentTimeMillis())
                } else {
                    insulinType
                }

                insulinTypeDao.insert(insulinTypeWithUniqueId.toEntity())
                remoteDataSource.pushInsulinType(userId, insulinTypeWithUniqueId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun delete(userId: String, insulinType: InsulinType) {
        withContext(ioDispatcher) {
            try {
                insulinTypeDao.delete(insulinType.toEntity())
                remoteDataSource.deleteInsulinType(userId, insulinType)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAllInsulinTypesForUserAndUpdateDatabase(userId: String) {
        withContext(ioDispatcher) {
            val fetchedInsulinTypes = remoteDataSource.fetchAllInsulinTypes(userId)
            insulinTypeDao.deleteAllForUser(userId)
            insulinTypeDao.insertAll(fetchedInsulinTypes.map { it.toEntity() })
        }
    }
}
