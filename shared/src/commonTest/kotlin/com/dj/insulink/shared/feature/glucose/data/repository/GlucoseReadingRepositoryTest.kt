package com.dj.insulink.shared.feature.glucose.data.repository

import com.dj.insulink.shared.feature.glucose.data.local.entity.GlucoseReadingEntity
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class GlucoseReadingRepositoryTest {

    private val dao = FakeGlucoseReadingDao()
    private val remote = FakeGlucoseRemoteDataSource()
    private val repository = GlucoseReadingRepository(dao, remote)

    @Test
    fun getAllGlucoseReadingsForUser_mapsEntitiesToDomain() = runTest {
        val entity = GlucoseReadingEntity(1, "u1", 1000L, 120, "c")
        dao.allReadingsFlow = flowOf(listOf(entity))

        val result = repository.getAllGlucoseReadingsForUser("u1").first()

        assertEquals(listOf(GlucoseReading(1, "u1", 1000L, 120, "c")), result)
    }

    @Test
    fun getGlucoseReadingsByDateRange_mapsEntitiesToDomain() = runTest {
        val entity = GlucoseReadingEntity(2, "u1", 1500L, 90, "")
        dao.dateRangeFlow = flowOf(listOf(entity))

        val result = repository.getGlucoseReadingsByDateRange("u1", 0L, 2000L).first()

        assertEquals(listOf(GlucoseReading(2, "u1", 1500L, 90, "")), result)
    }

    @Test
    fun getDateRange_returnsEarliestAndLatestTimestamps() = runTest {
        dao.earliestTimestamp = 100L
        dao.latestTimestamp = 900L

        assertEquals(Pair(100L, 900L), repository.getDateRange("u1"))
    }

    @Test
    fun insert_assignsGeneratedId_forNewReading_andSyncsRemote() = runTest {
        val reading = GlucoseReading(id = 0, userId = "u1", timestamp = 1000L, value = 120, comment = "")

        repository.insert("u1", reading)

        assertTrue(dao.insertedEntities.single().id != 0L)
        assertEquals(120, dao.insertedEntities.single().value)
        assertEquals("u1", remote.pushed.single().first)
    }

    @Test
    fun insert_keepsExistingId() = runTest {
        val reading = GlucoseReading(id = 7, userId = "u1", timestamp = 1000L, value = 120, comment = "")

        repository.insert("u1", reading)

        assertEquals(7L, dao.insertedEntities.single().id)
    }

    @Test
    fun update_updatesLocallyAndRemotely() = runTest {
        val reading = GlucoseReading(
            id = 7,
            userId = "u1",
            timestamp = 1000L,
            value = 130,
            comment = "",
            insulinTypeId = 3,
            insulinUnits = 4.0,
            linkedMealId = 9
        )

        repository.update("u1", reading)

        assertEquals(7L, dao.updatedEntities.single().id)
        assertEquals(3L, dao.updatedEntities.single().insulinTypeId)
        assertEquals("u1", remote.updated.single().first)
        assertEquals(reading, remote.updated.single().second)
    }

    @Test
    fun delete_removesLocallyAndRemotely() = runTest {
        val reading = GlucoseReading(id = 7, userId = "u1", timestamp = 1000L, value = 120, comment = "")

        repository.delete("u1", reading)

        assertEquals(7L, dao.deletedEntities.single().id)
        assertEquals(7L, remote.deleted.single().second.id)
    }

    @Test
    fun fetchAllGlucoseReadingsForUserAndUpdateDatabase_replacesLocalCache() = runTest {
        remote.fetchAllResult = listOf(GlucoseReading(1, "u1", 1000L, 120, ""))

        repository.fetchAllGlucoseReadingsForUserAndUpdateDatabase("u1")

        assertEquals("u1", dao.deleteAllForUserCalledWith)
        assertEquals(listOf(GlucoseReadingEntity(1, "u1", 1000L, 120, "")), dao.insertAllCalledWith)
    }
}
