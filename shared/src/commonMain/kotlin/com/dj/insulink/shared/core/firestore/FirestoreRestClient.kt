package com.dj.insulink.shared.core.firestore

import com.dj.insulink.shared.core.config.FIREBASE_PROJECT_ID
import com.dj.insulink.shared.core.network.createCoreHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1"

class FirestoreRestClient(
    private val httpClient: HttpClient = createCoreHttpClient()
) {
    private fun documentUrl(collection: String, documentId: String) =
        "$FIRESTORE_BASE_URL/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/$collection/$documentId"

    private fun collectionUrl(collection: String) =
        "$FIRESTORE_BASE_URL/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/$collection"

    suspend fun getDocumentFields(collection: String, documentId: String, idToken: String): JsonObject? {
        val response = httpClient.get(documentUrl(collection, documentId)) {
            header("Authorization", "Bearer $idToken")
        }
        if (response.status == HttpStatusCode.NotFound) return null
        requireSuccess(response) { "Firestore get failed: ${response.status}" }
        return response.body<JsonObject>()["fields"]?.jsonObject
    }

    suspend fun createDocument(
        collection: String,
        documentId: String,
        fields: Map<String, FirestoreValue>,
        idToken: String
    ) {
        val response = httpClient.post(collectionUrl(collection)) {
            parameter("documentId", documentId)
            header("Authorization", "Bearer $idToken")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("fields", buildJsonObject { fields.forEach { (key, value) -> put(key, value.toJson()) } })
            })
        }
        requireSuccess(response) { "Firestore create failed: ${response.status}" }
    }

    suspend fun setArrayField(
        collection: String,
        documentId: String,
        fieldName: String,
        elements: List<JsonElement>,
        idToken: String
    ) {
        val response = httpClient.patch(documentUrl(collection, documentId)) {
            parameter("updateMask.fieldPaths", fieldName)
            header("Authorization", "Bearer $idToken")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("fields", buildJsonObject { put(fieldName, arrayValueJson(elements)) }) })
        }
        requireSuccess(response) { "Firestore update failed: ${response.status}" }
    }

    suspend fun getArrayField(
        collection: String,
        documentId: String,
        fieldName: String,
        idToken: String
    ): List<JsonElement> {
        val fields = getDocumentFields(collection, documentId, idToken)
        return FirestoreValue.arrayElements(fields, fieldName)
    }

    suspend fun queryEqual(
        collection: String,
        field: String,
        value: String,
        idToken: String,
        limit: Int = 10
    ): List<Pair<String, JsonObject>> {
        val response = httpClient.post("$FIRESTORE_BASE_URL/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents:runQuery") {
            header("Authorization", "Bearer $idToken")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("structuredQuery", buildJsonObject {
                    put("from", buildJsonArray { add(buildJsonObject { put("collectionId", collection) }) })
                    put("where", buildJsonObject {
                        put("fieldFilter", buildJsonObject {
                            put("field", buildJsonObject { put("fieldPath", field) })
                            put("op", "EQUAL")
                            put("value", buildJsonObject { put("stringValue", value) })
                        })
                    })
                    put("limit", limit)
                })
            })
        }
        requireSuccess(response) { "Firestore query failed: ${response.status}" }
        val results = response.body<JsonArray>()
        return results.mapNotNull { entry ->
            val doc = entry.jsonObject["document"]?.jsonObject ?: return@mapNotNull null
            val name = doc["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val docId = name.substringAfterLast('/')
            val fields = doc["fields"]?.jsonObject ?: JsonObject(emptyMap())
            docId to fields
        }
    }

    private fun requireSuccess(response: HttpResponse, message: () -> String) {
        if (!response.status.isSuccess()) error(message())
    }
}
