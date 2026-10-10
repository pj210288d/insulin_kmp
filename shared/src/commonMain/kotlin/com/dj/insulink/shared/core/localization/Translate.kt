package com.dj.insulink.shared.core.localization

import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage

fun tr(language: AppLanguage, sr: String, en: String): String =
    if (language == AppLanguage.ENGLISH) en else sr
