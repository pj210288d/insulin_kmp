package com.dj.insulink.shared.feature.meals.data.local

import androidx.room.RoomDatabase

expect class MealsDatabaseFactory {
    fun create(): RoomDatabase.Builder<MealsDatabase>
}
