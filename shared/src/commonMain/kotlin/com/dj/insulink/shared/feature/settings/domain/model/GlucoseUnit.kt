package com.dj.insulink.shared.feature.settings.domain.model

import kotlin.math.round

enum class GlucoseUnit(
    val key: String,
    val label: String,
    val suffix: String
) {
    MG_DL("mg_dl", "mg/dL", "mg/dL"),
    MMOL_L("mmol_l", "mmol/L", "mmol/L");

    fun formatValue(mgDlValue: Int): String = formatValue(mgDlValue.toDouble())

    fun formatValue(mgDlValue: Double): String {
        return when (this) {
            MG_DL -> mgDlValue.toInt().toString()
            MMOL_L -> formatOneDecimal(convertMgDlToMmolL(mgDlValue))
        }
    }

    companion object {
        fun fromKey(key: String): GlucoseUnit =
            entries.find { it.key == key } ?: MG_DL

        fun convertMgDlToMmolL(mgDl: Double): Double = mgDl / CONVERSION_FACTOR

        fun convertMmolLToMgDl(mmolL: Double): Double = mmolL * CONVERSION_FACTOR

        private const val CONVERSION_FACTOR = 18.0182

        // Locale-independent replacement for String.format(Locale.US, "%.1f", value),
        // which isn't available in commonMain.
        private fun formatOneDecimal(value: Double): String {
            val scaled = round(value * 10).toLong()
            val whole = scaled / 10
            val fraction = kotlin.math.abs(scaled % 10)
            return "$whole.$fraction"
        }
    }
}
