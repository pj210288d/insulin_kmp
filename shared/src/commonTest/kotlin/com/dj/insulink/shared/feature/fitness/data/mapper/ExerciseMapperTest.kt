package com.dj.insulink.shared.feature.fitness.data.mapper

import com.dj.insulink.shared.feature.fitness.data.local.entity.ExerciseEntity
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseMapperTest {

    private val domain = Exercise(
        id = 3, userId = "u1", sportName = "Running",
        durationHours = 1, durationMinutes = 30, glucoseBefore = 150, glucoseAfter = 110
    )
    private val entity = ExerciseEntity(
        id = 3, userId = "u1", sportName = "Running",
        durationHours = 1, durationMinutes = 30, glucoseBefore = 150, glucoseAfter = 110
    )

    @Test
    fun entityMapsToDomain() {
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun domainMapsToEntity() {
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun roundTripPreservesTheExercise() {
        assertEquals(domain, domain.toEntity().toDomain())
    }

    @Test
    fun listMappersMapEveryElement() {
        val entities = listOf(entity, entity.copy(id = 4, sportName = "Cycling"))
        val domains = entities.toDomain()
        assertEquals(2, domains.size)
        assertEquals(entities, domains.toEntity())
    }
}
