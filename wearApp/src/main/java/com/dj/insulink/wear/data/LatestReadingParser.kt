package com.dj.insulink.wear.data

import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem

fun DataItem.toLatestReadingOrNull(): LatestReading? {
    val dataMap = DataMapItem.fromDataItem(this).dataMap
    if (!dataMap.getBoolean(WearDataLayerContract.KEY_HAS_READING, false)) return null

    return LatestReading(
        value = dataMap.getInt(WearDataLayerContract.KEY_VALUE),
        formattedValue = dataMap.getString(WearDataLayerContract.KEY_FORMATTED_VALUE).orEmpty(),
        rangeStatus = dataMap.getString(WearDataLayerContract.KEY_RANGE_STATUS).orEmpty(),
        timestampMillis = dataMap.getLong(WearDataLayerContract.KEY_TIMESTAMP)
    )
}
