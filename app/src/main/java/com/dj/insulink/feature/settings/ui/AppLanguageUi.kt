package com.dj.insulink.feature.settings.ui

import androidx.annotation.DrawableRes
import com.dj.insulink.R
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import java.util.Locale

val AppLanguage.locale: Locale
    get() = when (this) {
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.SERBIAN -> Locale.forLanguageTag("sr-Latn")
    }

@get:DrawableRes
val AppLanguage.flagIcon: Int
    get() = when (this) {
        AppLanguage.ENGLISH -> R.drawable.ic_flag_usa
        AppLanguage.SERBIAN -> R.drawable.ic_flag_serbia
    }
