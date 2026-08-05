package com.dj.insulink.shared.feature.insulin.data.repository

import com.dj.insulink.shared.feature.insulin.data.local.entity.InsulinTypeEntity
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class InsulinTypeRepositoryTest {

    private val dao = FakeInsulinTypeDao()
    private val remote = FakeInsulinRemoteDataSource()
    private val repository = InsulinTypeRepository(dao, remote)

    @Test
    fun getAllInsulinTypesForUser_mapsEntitiesToDomain() = runTest {
        val entity = InsulinTypeEntity(1, "u1", "NovoRapid")
        dao.allInsulinTypesFlow = flowOf(listOf(entity))

        val result = repository.getAllInsulinTypesForUser("u1").first()

        assertEquals(listOf(InsulinType(1, "u1", "NovoRapid")), result)
    }

    @Test
    fun insert_assignsGeneratedId_forNewInsulinType_andSyncsRemote() = runTest {
        val insulinType = InsulinType(id = 0, userId = "u1", name = "NovoRapid")

        repository.insert("u1", insulinType)

        assertTrue(dao.insertedEntities.single().id != 0L)
        assertEquals("NovoRapid", dao.insertedEntities.single().name)
        assertEquals("u1", remote.pushed.single().first)
    }

    @Test
    fun insert_keepsExistingId() = runTest {
        val insulinType = InsulinType(id = 7, userId = "u1", name = "Lantus")

        repository.insert("u1", insulinType)

        assertEquals(7L, dao.insertedEntities.single().id)
    }

    @Test
    fun delete_removesLocallyAndRemotely() = runTest {
        val insulinType = InsulinType(id = 7, userId = "u1", name = "Lantus")

        repository.delete("u1", insulinType)

        assertEquals(7L, dao.deletedEntities.single().id)
        assertEquals(7L, remote.deleted.single().second.id)
    }

    @Test
    fun fetchAllInsulinTypesForUserAndUpdateDatabase_replacesLocalCache() = runTest {
        remote.fetchAllResult = listOf(InsulinType(1, "u1", "NovoRapid"))

        repository.fetchAllInsulinTypesForUserAndUpdateDatabase("u1")

        assertEquals("u1", dao.deleteAllForUserCalledWith)
        assertEquals(listOf(InsulinTypeEntity(1, "u1", "NovoRapid")), dao.insertAllCalledWith)
    }
}
