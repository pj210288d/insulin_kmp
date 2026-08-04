package com.dj.insulink.shared.feature.librelink.data.mapper

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.librelink.data.remote.LibreLinkGlucoseReading

const val LIBRELINK_READING_COMMENT = "LibreLinkUp"

fun LibreLinkGlucoseReading.toGlucoseReading(userId: String): GlucoseReading {
    return GlucoseReading(
        id = 0L,
        userId = userId,
        timestamp = timestamp,
        value = valueMgDl,
        comment = LIBRELINK_READING_COMMENT
    )
}
