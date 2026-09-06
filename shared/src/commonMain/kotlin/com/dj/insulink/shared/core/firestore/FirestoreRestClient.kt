package com.dj.insulink.shared.core.firestore

import com.dj.insulink.shared.core.config.FIREBASE_PROJECT_ID
import com.dj.insulink.shared.core.network.createCoreHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

private const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1"

// Zajednički Firestore REST klijent - koristi ga iOS Auth actual (Faza 1, samo
// get/createDocument za users/{uid}) i iOS-ovi FirestoreRestXRemoteDataSource actual-i (Faza 2,
// dodaje patch/query metode kad na njih dođe red). Android NE koristi ovo - i dalje ide preko
// pravog Firebase GMS SDK-a (FirebaseFirestore), nepromenjeno.
class FirestoreRestClient(
    private val httpClient: HttpClient = createCoreHttpClient()
) {
    private fun documentUrl(collection: String, documentId: String) =
        "$FIRESTORE_BASE_URL/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/$collection/$documentId"

    private fun collectionUrl(collection: String) =
        "$FIRESTORE_BASE_URL/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/$collection"

    /** Vraća "fields" mapu dokumenta (sirov Firestore REST JSON), ili null ako ne postoji (404). */
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

    private fun requireSuccess(response: HttpResponse, message: () -> String) {
        if (!response.status.isSuccess()) error(message())
    }
}
