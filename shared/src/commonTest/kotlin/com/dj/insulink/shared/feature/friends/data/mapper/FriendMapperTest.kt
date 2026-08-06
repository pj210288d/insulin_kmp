package com.dj.insulink.shared.feature.friends.data.mapper

import com.dj.insulink.shared.feature.friends.data.local.entity.FriendEntity
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import kotlin.test.Test
import kotlin.test.assertEquals

class FriendMapperTest {

    private val domain = Friend(
        id = 1, userId = "u1", friendId = "f1", friendName = "Jane Doe",
        friendLastGlucoseReadingValue = 105, friendsLastGlucoseReadingTime = 2000L
    )
    private val entity = FriendEntity(
        id = 1, userId = "u1", friendId = "f1", friendName = "Jane Doe",
        friendLastGlucoseReadingValue = 105, friendsLastGlucoseReadingTime = 2000L
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
    fun nullableReadingFieldsArePreserved() {
        val noReading = domain.copy(friendLastGlucoseReadingValue = null, friendsLastGlucoseReadingTime = null)
        assertEquals(noReading, noReading.toEntity().toDomain())
    }

    @Test
    fun listMappersMapEveryElement() {
        val entities = listOf(entity, entity.copy(id = 2, friendName = "John Roe"))
        val domains = entities.toDomain()
        assertEquals(2, domains.size)
        assertEquals(entities, domains.toEntity())
    }
}
