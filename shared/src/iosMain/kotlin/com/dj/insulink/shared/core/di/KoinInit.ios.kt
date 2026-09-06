package com.dj.insulink.shared.core.di

import com.dj.insulink.shared.feature.auth.di.authModule
import com.dj.insulink.shared.feature.fitness.di.fitnessModule
import com.dj.insulink.shared.feature.insulin.di.insulinModule
import com.dj.insulink.shared.feature.librelink.di.librelinkModule
import com.dj.insulink.shared.feature.meals.di.mealsModule
import com.dj.insulink.shared.feature.reminders.di.remindersModule
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
            // LogMeal/USDA/Spoonacular ključevi nisu potrebni - deljeni Meals MVP ekran (vidi
            // MealsViewModel) je namerno samo ručni unos, ne poziva analyzeFoodImage ni
            // searchIngredients.
            mealsModule(usdaApiKey = "", spoonacularApiKey = "", logMealApiKey = "")
        )
    }
}
