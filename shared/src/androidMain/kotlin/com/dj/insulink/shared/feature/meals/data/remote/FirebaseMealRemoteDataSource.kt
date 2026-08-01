package com.dj.insulink.shared.feature.meals.data.remote

import android.util.Log
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.domain.model.MealIngredient
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseMealRemoteDataSource(
    private val firestore: FirebaseFirestore
) : MealRemoteDataSource {

    override suspend fun pushMeal(userId: String, meal: Meal) {
        try {
            val userDocumentRef = firestore.collection(COLLECTION_NAME_USERS).document(userId)

            val snapshot = userDocumentRef.get().await()

            if (snapshot.exists()) {
                userDocumentRef.update(DOCUMENT_FIELD_MEALS, FieldValue.arrayUnion(meal)).await()
            } else {
                val userData = mapOf(DOCUMENT_FIELD_MEALS to listOf(meal))
                userDocumentRef.set(userData).await()
            }
        } catch (e: Exception) {
            Log.e("MealRemoteDataSource", "Error pushing meal to Firestore", e)
        }
    }

    override suspend fun deleteMeal(userId: String, meal: Meal) {
        try {
            val userDocumentRef = firestore.collection(COLLECTION_NAME_USERS).document(userId)

            val snapshot = userDocumentRef.get().await()
            val meals = snapshot.get(DOCUMENT_FIELD_MEALS) as? List<Map<String, Any>> ?: emptyList()

            val updatedMeals = meals.filter { mealMap ->
                (mealMap["id"] as? Number)?.toLong() != meal.id
            }

            if (meals.size != updatedMeals.size) {
                userDocumentRef.update(DOCUMENT_FIELD_MEALS, updatedMeals).await()
            } else {
                Log.w("MealRemoteDataSource", "No meal found with ID ${meal.id} to delete")
            }
        } catch (e: Exception) {
            Log.e("MealRemoteDataSource", "Error deleting meal from Firestore", e)
        }
    }

    override suspend fun updateMeal(userId: String, meal: Meal) {
        try {
            val userDocumentRef = firestore.collection(COLLECTION_NAME_USERS).document(userId)

            val snapshot = userDocumentRef.get().await()

            if (snapshot.exists()) {
                val meals = snapshot.get(DOCUMENT_FIELD_MEALS) as? List<Map<String, Any>> ?: emptyList()

                val updatedMeals = meals.map { mealMap ->
                    if ((mealMap["id"] as? Number)?.toLong() == meal.id) meal else mealMap
                }

                userDocumentRef.update(DOCUMENT_FIELD_MEALS, updatedMeals).await()
            } else {
                val userData = mapOf(DOCUMENT_FIELD_MEALS to listOf(meal))
                userDocumentRef.set(userData).await()
            }
        } catch (e: Exception) {
            Log.e("MealRemoteDataSource", "Error updating meal in Firestore", e)
        }
    }

    override suspend fun fetchAllMeals(userId: String): List<Meal> {
        val document = firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .get()
            .await()

        if (!document.exists()) {
            return emptyList()
        }

        val mealsData = document.get(DOCUMENT_FIELD_MEALS) as? List<Map<String, Any>> ?: emptyList()

        return mealsData.map { mealMap ->
            val ingredientsData = mealMap["ingredients"] as? List<Map<String, Any>> ?: emptyList()
            val ingredients = ingredientsData.map { ingredientMap ->
                val ingredientData = ingredientMap["ingredient"] as? Map<String, Any> ?: emptyMap()
                val ingredient = Ingredient(
                    id = (ingredientData["id"] as? Number)?.toLong() ?: 0,
                    name = ingredientData["name"] as? String ?: "",
                    caloriesPer100g = (ingredientData["caloriesPer100g"] as? Number)?.toDouble() ?: 0.0,
                    proteinPer100g = (ingredientData["proteinPer100g"] as? Number)?.toDouble() ?: 0.0,
                    carbsPer100g = (ingredientData["carbsPer100g"] as? Number)?.toDouble() ?: 0.0,
                    fatPer100g = (ingredientData["fatPer100g"] as? Number)?.toDouble() ?: 0.0,
                    sugarPer100g = (ingredientData["sugarPer100g"] as? Number)?.toDouble() ?: 0.0,
                    saltPer100g = (ingredientData["saltPer100g"] as? Number)?.toDouble() ?: 0.0,
                    userId = ingredientData["userId"] as? String,
                    firebaseId = ingredientData["firebaseId"] as? String,
                    createdAt = (ingredientData["createdAt"] as? Number)?.toLong() ?: 0
                )
                MealIngredient(
                    id = (ingredientMap["id"] as? Number)?.toLong() ?: 0,
                    mealId = (ingredientMap["mealId"] as? Number)?.toLong() ?: 0,
                    ingredient = ingredient,
                    quantity = (ingredientMap["quantity"] as? Number)?.toDouble() ?: 0.0,
                    firebaseId = ingredientMap["firebaseId"] as? String,
                    createdAt = (ingredientMap["createdAt"] as? Number)?.toLong() ?: 0
                )
            }

            Meal(
                id = (mealMap["id"] as? Number)?.toLong() ?: 0,
                name = mealMap["name"] as? String ?: "",
                timestamp = (mealMap["timestamp"] as? Number)?.toLong() ?: 0,
                calories = (mealMap["calories"] as? Number)?.toInt(),
                carbs = (mealMap["carbs"] as? Number)?.toDouble(),
                protein = (mealMap["protein"] as? Number)?.toDouble(),
                fat = (mealMap["fat"] as? Number)?.toDouble(),
                sugar = (mealMap["sugar"] as? Number)?.toDouble(),
                salt = (mealMap["salt"] as? Number)?.toDouble(),
                comment = mealMap["comment"] as? String,
                userId = mealMap["userId"] as? String ?: "",
                firebaseId = mealMap["firebaseId"] as? String,
                ingredients = ingredients,
                createdAt = (mealMap["createdAt"] as? Number)?.toLong() ?: 0,
                updatedAt = (mealMap["updatedAt"] as? Number)?.toLong() ?: 0
            )
        }
    }
}

private const val COLLECTION_NAME_USERS = "users"
private const val DOCUMENT_FIELD_MEALS = "meals"
