package com.dj.insulink.shared.feature.settings.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {

    @Test
    fun fromKeyReturnsMatchingLanguageForKnownKeys() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromKey("en"))
        assertEquals(AppLanguage.SERBIAN, AppLanguage.fromKey("sr-Latn"))
    }

    @Test
    fun fromKeyFallsBackToEnglishForUnknownKey() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromKey("de"))
    }
}
