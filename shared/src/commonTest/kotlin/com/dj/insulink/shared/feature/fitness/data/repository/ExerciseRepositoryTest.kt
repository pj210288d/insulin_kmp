package com.dj.insulink.shared.feature.fitness.data.repository

import com.dj.insulink.shared.feature.fitness.data.local.entity.ExerciseEntity
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ExerciseRepositoryTest {

    private val dao = FakeExerciseDao()
    private val remote = FakeExerciseRemoteDataSource()
    private val repository = ExerciseRepository(dao, remote)

    @Test
    fun getAllExercisesForUser_mapsEntitiesToDomain() = runTest {
        val entity = ExerciseEntity(1, "u1", "Running", 1, 0, 150, 110)
        dao.allExercisesFlow = flowOf(listOf(entity))

        val result = repository.getAllExercisesForUser("u1").first()

        assertEquals(listOf(Exercise(1, "u1", "Running", 1, 0, 150, 110)), result)
    }

    @Test
    fun getExercisesBySportName_mapsEntitiesToDomain() = runTest {
        val entity = ExerciseEntity(2, "u1", "Cycling", 0, 45, 130, 120)
        dao.exercisesBySportFlow = flowOf(listOf(entity))

        val result = repository.getExercisesBySportName("u1", "Cycling").first()

        assertEquals(listOf(Exercise(2, "u1", "Cycling", 0, 45, 130, 120)), result)
    }

    @Test
    fun insert_assignsGeneratedIdForANewExercise() = runTest {
        val exercise = Exercise(0, "u1", "Running", 1, 0, 150, 110)

        repository.insert("u1", exercise)

        val inserted = dao.insertedExercises.single()
        assertEquals(true, inserted.id != 0L)
        assertEquals("Running", inserted.sportName)
        assertEquals("u1", remote.pushed.single().first)
    }

    @Test
    fun insert_keepsAnExistingId() = runTest {
        val exercise = Exercise(9, "u1", "Running", 1, 0, 150, 110)

        repository.insert("u1", exercise)

        assertEquals(9L, dao.insertedExercises.single().id)
    }

    @Test
    fun fetchAllExercisesForUserAndUpdateDatabase_replacesLocalCache() = runTest {
        remote.fetchAllResult = listOf(Exercise(1, "u1", "Running", 1, 0, 150, 110))

        repository.fetchAllExercisesForUserAndUpdateDatabase("u1")

        assertEquals("u1", dao.deleteAllForUserCalledWith)
        assertEquals(listOf(ExerciseEntity(1, "u1", "Running", 1, 0, 150, 110)), dao.insertAllCalledWith)
    }
}
