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

// Pokreće Koin za iOS - trenutno samo modul-i koje deljeni MVP ekrani zahtevaju
// (statisticsModule uključuje i glucoseModule/settingsModule - vidi StatisticsModule.kt).
// Poziva se jednom iz MainViewController.kt
// pre prvog Compose ekrana - `started` čuva od dvostrukog startKoin poziva (Koin baca ako se
// pozove dva puta u istom procesu) ako bi MainViewController() ikad bio pozvan više puta.
//
// Faza 1 (Auth): authModule dodat - App() sada gate-uje na pravu prijavu (Ktor REST klijent ka
// Firebase Identity Toolkit/Firestore, vidi RestAuthRepository) umesto fiksnog demo id-a koji je
// ranije ovde stajao. UserSession se sada puni SAMO preko AuthSession.setCurrentUser(), pozvano
// iz RestAuthRepository posle uspešnog restoreSession()/login()/register() poziva iz App()-a -
// ne više ovde direktno.
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
            // Meals ekran je sada u punom paritetu sa Android-om (pretraga sastojaka + LogMeal
            // foto-prepoznavanje, vidi MealsViewModel/MealsScreen) - ključevi dolaze iz istog
            // root local.properties koji Android čita preko BuildConfig, generisano preko
            // :shared:generateMealApiConfig (vidi shared/build.gradle.kts). Ako local.properties
            // na ovoj mašini nema SPOONACULAR_API_KEY/USDA_API_KEY/LOGMEAL_API_KEY, ovo su prazni
            // stringovi i pretraga/analiza tiho padaju nazad na lokalnu bazu (vidi MealApiConfig.kt).
            mealsModule(
                usdaApiKey = MEAL_USDA_API_KEY,
                spoonacularApiKey = MEAL_SPOONACULAR_API_KEY,
                logMealApiKey = MEAL_LOGMEAL_API_KEY
            )
        )
    }
}
