package com.dj.insulink.core.di

import android.content.Context
import androidx.room.Room
import com.dj.insulink.core.room.InsulinkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): InsulinkDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            InsulinkDatabase::class.java,
            "insulink_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideFriendDao(database: InsulinkDatabase) = database.friendDao()

    @Provides
    @Singleton
    fun provideReminderDao(database: InsulinkDatabase) = database.reminderDao()

    @Provides
    @Singleton
    fun provideMealDao(database: InsulinkDatabase) = database.mealDao()

    @Provides
    @Singleton
    fun provideIngredientDao(database: InsulinkDatabase) = database.ingredientDao()

    @Provides
    @Singleton
    fun provideMealIngredientDao(database: InsulinkDatabase) = database.mealIngredientDao()

    @Provides
    @Singleton
    fun provideExerciseDao(database: InsulinkDatabase) = database.exerciseDao()
}
