package com.dj.insulink.shared.feature.meals.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dj.insulink.shared.feature.meals.data.local.dao.IngredientDao
import com.dj.insulink.shared.feature.meals.data.local.dao.MealDao
import com.dj.insulink.shared.feature.meals.data.local.dao.MealIngredientDao
import com.dj.insulink.shared.feature.meals.data.local.entity.IngredientEntity
import com.dj.insulink.shared.feature.meals.data.local.entity.MealEntity
import com.dj.insulink.shared.feature.meals.data.local.entity.MealIngredientEntity
import com.dj.insulink.shared.core.dispatcher.ioDispatcher

internal const val MEALS_DATABASE_FILE_NAME = "meals.db"

@Database(
    entities = [MealEntity::class, IngredientEntity::class, MealIngredientEntity::class],
    version = 1,
    exportSchema = false
)
@ConstructedBy(MealsDatabaseConstructor::class)
abstract class MealsDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun mealIngredientDao(): MealIngredientDao
}

// Room's KSP compiler generates the platform `actual` for this object.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MealsDatabaseConstructor : RoomDatabaseConstructor<MealsDatabase> {
    override fun initialize(): MealsDatabase
}

fun buildMealsDatabase(builder: RoomDatabase.Builder<MealsDatabase>): MealsDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .build()
}
