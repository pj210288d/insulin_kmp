package com.dj.insulink.shared.core.firestore

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// Firestore REST API predstavlja svaku vrednost dokumenta kao typed-value JSON envelope
// (npr. {"stringValue": "x"}, ne prosto "x") - vidi
// https://firebase.google.com/docs/firestore/reference/rest/v1/Value. Pokriva tipove koje ovaj
// projekat stvarno koristi: string/integer/timestamp polja, prazni nizovi (Faza 1 registracija),
// i (Faza 2) nizovi objekata (mapValue) za GlucoseReading/InsulinType/Exercise/Reminder/Meal.
sealed class FirestoreValue {
    data class Str(val value: String) : FirestoreValue()
    data class IntNum(val value: Long) : FirestoreValue()
    data class DoubleNum(val value: Double) : FirestoreValue()
    data class Timestamp(val iso8601Utc: String) : FirestoreValue()
    data class MapVal(val fields: Map<String, FirestoreValue>) : FirestoreValue()
    data class Arr(val values: List<FirestoreValue> = emptyList()) : FirestoreValue()
    object Null : FirestoreValue()

    fun toJson(): JsonElement = when (this) {
        is Str -> buildJsonObject { put("stringValue", value) }
        is IntNum -> buildJsonObject { put("integerValue", value.toString()) }
        is DoubleNum -> buildJsonObject { put("doubleValue", value) }
        is Timestamp -> buildJsonObject { put("timestampValue", iso8601Utc) }
        is MapVal -> buildJsonObject {
            put("mapValue", buildJsonObject {
                put("fields", buildJsonObject { fields.forEach { (key, value) -> put(key, value.toJson()) } })
            })
        }
        is Arr -> buildJsonObject {
            put("arrayValue", buildJsonObject {
                put("values", buildJsonArray { values.forEach { add(it.toJson()) } })
            })
        }
        is Null -> buildJsonObject { put("nullValue", kotlinx.serialization.json.JsonNull) }
    }

    companion object {
        /** `nullable` polje -> `Null` FirestoreValue umesto da se izostavi (izostavljanje bi na
         * update-u ostavilo staru vrednost iz prethodnog upisa, vidi setArrayField). */
        fun nullableStr(value: String?): FirestoreValue = value?.let(::Str) ?: Null
        fun nullableIntNum(value: Long?): FirestoreValue = value?.let(::IntNum) ?: Null
        fun nullableDoubleNum(value: Double?): FirestoreValue = value?.let(::DoubleNum) ?: Null

        fun stringOrNull(fields: JsonObject?, key: String): String? =
            fields?.get(key)?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull

        fun longOrNull(fields: JsonObject?, key: String): Long? =
            fields?.get(key)?.jsonObject?.get("integerValue")?.jsonPrimitive?.contentOrNull?.toLongOrNull()

        fun doubleOrNull(fields: JsonObject?, key: String): Double? =
            fields?.get(key)?.jsonObject?.get("doubleValue")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

        /** Vrednosti niza pod `key`-em, kao "sirovi" JsonElement-i (svaki je jedan mapValue objekat). */
        fun arrayElements(fields: JsonObject?, key: String): List<JsonElement> =
            fields?.get(key)?.jsonObject?.get("arrayValue")?.jsonObject?.get("values")?.jsonArray
                ?.toList().orEmpty()

        /** "fields" mapa unutar jednog mapValue elementa niza (rezultat arrayElements). */
        fun mapFieldsOf(element: JsonElement): JsonObject? =
            element.jsonObject["mapValue"]?.jsonObject?.get("fields")?.jsonObject

        fun stringOf(element: JsonElement, key: String): String? = stringOrNull(mapFieldsOf(element), key)
        fun longOf(element: JsonElement, key: String): Long? = longOrNull(mapFieldsOf(element), key)
        fun doubleOf(element: JsonElement, key: String): Double? = doubleOrNull(mapFieldsOf(element), key)

        /** Za nizove golih stringova (npr. "friends" polje - lista uid-ova, ne mapValue objekata). */
        fun plainStringOf(element: JsonElement): String? =
            element.jsonObject["stringValue"]?.jsonPrimitive?.contentOrNull
    }
}

/** Pomoćna funkcija: builder za arrayValue JSON telo direktno iz sirovih JsonElement-a (bez re-enkodiranja). */
fun arrayValueJson(elements: List<JsonElement>): JsonObject = buildJsonObject {
    put("arrayValue", buildJsonObject { put("values", JsonArray(elements)) })
}
