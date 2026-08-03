package com.dj.insulink.core

import android.app.Application
import com.dj.insulink.BuildConfig
import com.dj.insulink.shared.core.di.firebaseModule
import com.dj.insulink.shared.feature.fitness.di.fitnessModule
import com.dj.insulink.shared.feature.friends.di.friendsModule
import com.dj.insulink.shared.feature.glucose.di.glucoseModule
import com.dj.insulink.shared.feature.meals.di.mealsModule
import com.dj.insulink.shared.feature.reminders.di.remindersModule
import com.dj.insulink.shared.feature.settings.di.settingsModule
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

@HiltAndroidApp
class InsulinkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@InsulinkApplication)
            modules(
                firebaseModule,
                glucoseModule,
                mealsModule(
                    usdaApiKey = BuildConfig.USDA_API_KEY ?: "",
                    spoonacularApiKey = BuildConfig.SPOONACULAR_API_KEY ?: ""
                ),
                fitnessModule,
                remindersModule,
                friendsModule,
                settingsModule
            )
        }
    }
}