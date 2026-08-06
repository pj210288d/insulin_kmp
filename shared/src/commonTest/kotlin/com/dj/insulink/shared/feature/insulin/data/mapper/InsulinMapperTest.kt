package com.dj.insulink.shared.feature.insulin.data.mapper

import com.dj.insulink.shared.feature.insulin.data.local.entity.InsulinTypeEntity
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import kotlin.test.Test
import kotlin.test.assertEquals

class InsulinMapperTest {

    private val domain = InsulinType(id = 1, userId = "u1", name = "NovoRapid")
    private val entity = InsulinTypeEntity(id = 1, userId = "u1", name = "NovoRapid")

    @Test
    fun entityMapsToDomainFieldForField() {
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun domainMapsToEntityFieldForField() {
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun roundTripPreservesTheInsulinType() {
        assertEquals(domain, domain.toEntity().toDomain())
    }

    @Test
    fun listMappersMapEveryElement() {
        val entities = listOf(entity, entity.copy(id = 2, name = "Lantus"))
        val domains = entities.toDomain()
        assertEquals(2, domains.size)
        assertEquals(entities, domains.toEntity())
    }
}
