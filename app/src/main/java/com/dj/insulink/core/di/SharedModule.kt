package com.dj.insulink.core.di

import com.dj.insulink.shared.feature.fitness.data.repository.ExerciseRepository
import com.dj.insulink.shared.feature.friends.data.repository.FriendRepository
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.koin.core.context.GlobalContext

// Bridge module: :shared feature dependency graphs are wired with Koin
// (startKoin call in InsulinkApplication), while the rest of the app still
// uses Hilt. This exposes Koin-managed shared repositories to Hilt injection
// sites until more feature modules move to :shared.
@Module
@InstallIn(SingletonComponent::class)
object SharedModule {
    @Provides
    @Singleton
    fun provideGlucoseReadingRepository(): GlucoseReadingRepository {
        return GlobalContext.get().get()
    }

    @Provides
    @Singleton
    fun provideMealRepository(): MealRepository {
        return GlobalContext.get().get()
    }

    @Provides
    @Singleton
    fun provideExerciseRepository(): ExerciseRepository {
        return GlobalContext.get().get()
    }

    @Provides
    @Singleton
    fun provideReminderRepository(): ReminderRepository {
        return GlobalContext.get().get()
    }

    @Provides
    @Singleton
    fun provideFriendRepository(): FriendRepository {
        return GlobalContext.get().get()
    }

    @Provides
    @Singleton
    fun provideSettingsPreferences(): SettingsPreferences {
        return GlobalContext.get().get()
    }
}
