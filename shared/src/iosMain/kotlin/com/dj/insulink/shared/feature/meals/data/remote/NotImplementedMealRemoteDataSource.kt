package com.dj.insulink.shared.feature.meals.data.remote

import com.dj.insulink.shared.feature.meals.domain.model.Meal

// Firestore sync za iOS je planiran za fazu 4 migracije (vidi CLAUDE.md/dnevnik.md).
// FoodApiRemoteDataSource (Ktor) NE mora ovde da se stubuje - to je čist REST poziv i
// već radi na iOS-u preko Darwin engine-a (data/remote/HttpClientEngine.ios.kt).
class NotImplementedMealRemoteDataSource : MealRemoteDataSource {
    override suspend fun pushMeal(userId: String, meal: Meal): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun deleteMeal(userId: String, meal: Meal): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun updateMeal(userId: String, meal: Meal): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun fetchAllMeals(userId: String): List<Meal> =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")
}
