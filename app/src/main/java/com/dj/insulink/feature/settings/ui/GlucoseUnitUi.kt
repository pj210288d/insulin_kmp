package com.dj.insulink.feature.settings.ui

import androidx.annotation.DrawableRes
import com.dj.insulink.R
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

@get:DrawableRes
val GlucoseUnit.flagIcon: Int
    get() = when (this) {
        GlucoseUnit.MG_DL -> R.drawable.ic_flag_usa
        GlucoseUnit.MMOL_L -> R.drawable.ic_flag_eu
    }
