package com.dj.insulink.shared.feature.glucose.data.mapper

import com.dj.insulink.shared.feature.glucose.data.local.entity.GlucoseReadingEntity
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import kotlin.test.Test
import kotlin.test.assertEquals

class GlucoseMapperTest {

    private val domain = GlucoseReading(id = 7, userId = "u1", timestamp = 1000L, value = 120, comment = "after lunch")
    private val entity = GlucoseReadingEntity(id = 7, userId = "u1", timestamp = 1000L, value = 120, comment = "after lunch")

    @Test
    fun entityMapsToDomainFieldForField() {
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun domainMapsToEntityFieldForField() {
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun roundTripPreservesTheReading() {
        assertEquals(domain, domain.toEntity().toDomain())
    }

    @Test
    fun listMappersMapEveryElement() {
        val entities = listOf(entity, entity.copy(id = 8, value = 90))
        val domains = entities.toDomain()
        assertEquals(2, domains.size)
        assertEquals(entities, domains.toEntity())
    }

    @Test
    fun insulinAndMealLinkFieldsRoundTrip() {
        val domainWithLinks = domain.copy(insulinTypeId = 3, insulinUnits = 4.5, linkedMealId = 9)
        val entityWithLinks = entity.copy(insulinTypeId = 3, insulinUnits = 4.5, linkedMealId = 9)

        assertEquals(domainWithLinks, entityWithLinks.toDomain())
        assertEquals(entityWithLinks, domainWithLinks.toEntity())
    }
}
