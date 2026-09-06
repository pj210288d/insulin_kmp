package com.dj.insulink.shared.core.firestore

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// Firestore REST API predstavlja svaku vrednost dokumenta kao typed-value JSON envelope
// (npr. {"stringValue": "x"}, ne prosto "x") - vidi
// https://firebase.google.com/docs/firestore/reference/rest/v1/Value. Ovo pokriva samo tipove
// koje ovaj projekat stvarno koristi (users/{uid} dokument: string polja, friendCode, prazni
// nizovi pri registraciji - Faza 2 dodaje integer/timestamp/map po potrebi za svaki feature).
sealed class FirestoreValue {
    data class Str(val value: String) : FirestoreValue()
    data class IntNum(val value: Long) : FirestoreValue()
    data class Timestamp(val iso8601Utc: String) : FirestoreValue()
    data class Arr(val values: List<FirestoreValue> = emptyList()) : FirestoreValue()

    fun toJson(): JsonElement = when (this) {
        is Str -> buildJsonObject { put("stringValue", value) }
        is IntNum -> buildJsonObject { put("integerValue", value.toString()) }
        is Timestamp -> buildJsonObject { put("timestampValue", iso8601Utc) }
        is Arr -> buildJsonObject {
            put("arrayValue", buildJsonObject {
                put("values", buildJsonArray { values.forEach { add(it.toJson()) } })
            })
        }
    }

    companion object {
        fun stringOrNull(fields: JsonObject?, key: String): String? =
            fields?.get(key)?.jsonObject?.get("stringValue")?.jsonPrimitive?.content
    }
}
