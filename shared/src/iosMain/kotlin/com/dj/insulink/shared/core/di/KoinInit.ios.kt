package com.dj.insulink.shared.core.di

import com.dj.insulink.shared.feature.auth.di.authModule
import com.dj.insulink.shared.feature.fitness.di.fitnessModule
import com.dj.insulink.shared.feature.friends.di.friendsModule
import com.dj.insulink.shared.feature.insulin.di.insulinModule
import com.dj.insulink.shared.feature.librelink.di.librelinkModule
import com.dj.insulink.shared.feature.meals.config.MEAL_LOGMEAL_API_KEY
import com.dj.insulink.shared.feature.meals.config.MEAL_SPOONACULAR_API_KEY
import com.dj.insulink.shared.feature.meals.config.MEAL_USDA_API_KEY
import com.dj.insulink.shared.feature.meals.di.mealsModule
import com.dj.insulink.shared.feature.reminders.di.remindersModule
import com.dj.insulink.shared.feature.reports.di.reportsModule
import com.dj.insulink.shared.feature.statistics.di.statisticsModule
import org.koin.core.context.startKoin

private var started = false

fun initKoinIOS() {
    if (started) return
    started = true
    startKoin {
        modules(
            authModule,
            statisticsModule,
            insulinModule,
            remindersModule,
            fitnessModule,
            librelinkModule,
            friendsModule,
            reportsModule,
            mealsModule(
                usdaApiKey = MEAL_USDA_API_KEY,
                spoonacularApiKey = MEAL_SPOONACULAR_API_KEY,
                logMealApiKey = MEAL_LOGMEAL_API_KEY
            )
        )
    }
}
