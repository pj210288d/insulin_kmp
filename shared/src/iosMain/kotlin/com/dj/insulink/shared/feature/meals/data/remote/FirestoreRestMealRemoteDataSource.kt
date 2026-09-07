package com.dj.insulink.shared.feature.meals.data.remote

import com.dj.insulink.shared.core.auth.IosAuthTokenProvider
import com.dj.insulink.shared.core.firestore.FirestoreRestClient
import com.dj.insulink.shared.core.firestore.FirestoreValue
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.domain.model.MealIngredient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private const val USERS_COLLECTION = "users"
private const val FIELD_MEALS = "meals"

// Faza 2 (cloud sync), poslednji od pet - iOS ekvivalent Android-ovog
// FirebaseMealRemoteDataSource.kt. Isti ugnježdeni oblik (meal -> ingredients[] ->
// ingredient mapValue), namerno BEZ LogMeal foto prepoznavanja (Faza 5, van obima ovog dela) -
// samo ručni unos/lista/brisanje kao i postojeći shared Meals ekran. Firestore REST PATCH je
// upsert (kreira dokument ako ne postoji) - nema potrebe za Android-ovim eksplicitnim
// snapshot.exists() granjanjem set/update.
class FirestoreRestMealRemoteDataSource(
    private val firestoreClient: FirestoreRestClient,
    private val tokenProvider: IosAuthTokenProvider
) : MealRemoteDataSource {

    override suspend fun pushMeal(userId: String, meal: Meal) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_MEALS, idToken)
        val updated = current + meal.toFirestoreValue().toJson()
        firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_MEALS, updated, idToken)
    }

    override suspend fun updateMeal(userId: String, meal: Meal) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_MEALS, idToken)
        val updated = current.map { element ->
            if (FirestoreValue.longOf(element, "id") == meal.id) meal.toFirestoreValue().toJson() else element
        }
        firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_MEALS, updated, idToken)
    }

    override suspend fun deleteMeal(userId: String, meal: Meal) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_MEALS, idToken)
        val updated = current.filterNot { FirestoreValue.longOf(it, "id") == meal.id }
        if (updated.size != current.size) {
            firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_MEALS, updated, idToken)
        }
    }

    override suspend fun fetchAllMeals(userId: String): List<Meal> {
        val idToken = tokenProvider.currentIdToken()
        val elements = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_MEALS, idToken)
        return elements.mapNotNull { it.toMealOrNull() }
    }
}

private fun Ingredient.toFirestoreValue() = FirestoreValue.MapVal(
    mapOf(
        "id" to FirestoreValue.IntNum(id),
        "name" to FirestoreValue.Str(name),
        "caloriesPer100g" to FirestoreValue.DoubleNum(caloriesPer100g),
        "proteinPer100g" to FirestoreValue.DoubleNum(proteinPer100g),
        "carbsPer100g" to FirestoreValue.DoubleNum(carbsPer100g),
        "fatPer100g" to FirestoreValue.DoubleNum(fatPer100g),
        "sugarPer100g" to FirestoreValue.DoubleNum(sugarPer100g),
        "saltPer100g" to FirestoreValue.DoubleNum(saltPer100g),
        "userId" to FirestoreValue.nullableStr(userId),
        "firebaseId" to FirestoreValue.nullableStr(firebaseId),
        "createdAt" to FirestoreValue.IntNum(createdAt)
    )
)

private fun MealIngredient.toFirestoreValue() = FirestoreValue.MapVal(
    mapOf(
        "id" to FirestoreValue.IntNum(id),
        "mealId" to FirestoreValue.IntNum(mealId),
        "ingredient" to ingredient.toFirestoreValue(),
        "quantity" to FirestoreValue.DoubleNum(quantity),
        "firebaseId" to FirestoreValue.nullableStr(firebaseId),
        "createdAt" to FirestoreValue.IntNum(createdAt)
    )
)

private fun Meal.toFirestoreValue() = FirestoreValue.MapVal(
    mapOf(
        "id" to FirestoreValue.IntNum(id),
        "name" to FirestoreValue.Str(name),
        "timestamp" to FirestoreValue.IntNum(timestamp),
        "calories" to FirestoreValue.nullableIntNum(calories?.toLong()),
        "carbs" to FirestoreValue.nullableDoubleNum(carbs),
        "protein" to FirestoreValue.nullableDoubleNum(protein),
        "fat" to FirestoreValue.nullableDoubleNum(fat),
        "sugar" to FirestoreValue.nullableDoubleNum(sugar),
        "salt" to FirestoreValue.nullableDoubleNum(salt),
        "comment" to FirestoreValue.nullableStr(comment),
        "userId" to FirestoreValue.Str(userId),
        "firebaseId" to FirestoreValue.nullableStr(firebaseId),
        "ingredients" to FirestoreValue.Arr(ingredients.map { it.toFirestoreValue() }),
        "createdAt" to FirestoreValue.IntNum(createdAt),
        "updatedAt" to FirestoreValue.IntNum(updatedAt)
    )
)

private fun JsonElement.toIngredientOrNull(): Ingredient? {
    val fields = FirestoreValue.mapFieldsOf(this) ?: return null
    return Ingredient(
        id = FirestoreValue.longOrNull(fields, "id") ?: 0,
        name = FirestoreValue.stringOrNull(fields, "name").orEmpty(),
        caloriesPer100g = FirestoreValue.doubleOrNull(fields, "caloriesPer100g") ?: 0.0,
        proteinPer100g = FirestoreValue.doubleOrNull(fields, "proteinPer100g") ?: 0.0,
        carbsPer100g = FirestoreValue.doubleOrNull(fields, "carbsPer100g") ?: 0.0,
        fatPer100g = FirestoreValue.doubleOrNull(fields, "fatPer100g") ?: 0.0,
        sugarPer100g = FirestoreValue.doubleOrNull(fields, "sugarPer100g") ?: 0.0,
        saltPer100g = FirestoreValue.doubleOrNull(fields, "saltPer100g") ?: 0.0,
        userId = FirestoreValue.stringOrNull(fields, "userId"),
        firebaseId = FirestoreValue.stringOrNull(fields, "firebaseId"),
        createdAt = FirestoreValue.longOrNull(fields, "createdAt") ?: 0
    )
}

private fun JsonElement.toMealIngredientOrNull(): MealIngredient? {
    val fields = FirestoreValue.mapFieldsOf(this) ?: return null
    val ingredientElement = fields["ingredient"] ?: return null
    val ingredient = ingredientElement.toIngredientOrNull() ?: return null
    return MealIngredient(
        id = FirestoreValue.longOrNull(fields, "id") ?: 0,
        mealId = FirestoreValue.longOrNull(fields, "mealId") ?: 0,
        ingredient = ingredient,
        quantity = FirestoreValue.doubleOrNull(fields, "quantity") ?: 0.0,
        firebaseId = FirestoreValue.stringOrNull(fields, "firebaseId"),
        createdAt = FirestoreValue.longOrNull(fields, "createdAt") ?: 0
    )
}

private fun JsonElement.toMealOrNull(): Meal? {
    val fields: JsonObject = FirestoreValue.mapFieldsOf(this) ?: return null
    val id = FirestoreValue.longOrNull(fields, "id") ?: return null
    val ingredients = FirestoreValue.arrayElements(fields, "ingredients").mapNotNull { it.toMealIngredientOrNull() }

    return Meal(
        id = id,
        name = FirestoreValue.stringOrNull(fields, "name").orEmpty(),
        timestamp = FirestoreValue.longOrNull(fields, "timestamp") ?: 0,
        calories = FirestoreValue.longOrNull(fields, "calories")?.toInt(),
        carbs = FirestoreValue.doubleOrNull(fields, "carbs"),
        protein = FirestoreValue.doubleOrNull(fields, "protein"),
        fat = FirestoreValue.doubleOrNull(fields, "fat"),
        sugar = FirestoreValue.doubleOrNull(fields, "sugar"),
        salt = FirestoreValue.doubleOrNull(fields, "salt"),
        comment = FirestoreValue.stringOrNull(fields, "comment"),
        userId = FirestoreValue.stringOrNull(fields, "userId").orEmpty(),
        firebaseId = FirestoreValue.stringOrNull(fields, "firebaseId"),
        ingredients = ingredients,
        createdAt = FirestoreValue.longOrNull(fields, "createdAt") ?: 0,
        updatedAt = FirestoreValue.longOrNull(fields, "updatedAt") ?: 0
    )
}
