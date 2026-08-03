package com.dj.insulink.shared.feature.settings.domain.model

enum class AppLanguage(
    val key: String,
    val displayName: String
) {
    ENGLISH("en", "English"),
    SERBIAN("sr-Latn", "Srpski");

    companion object {
        fun fromKey(key: String): AppLanguage =
            entries.find { it.key == key } ?: ENGLISH
    }
}
