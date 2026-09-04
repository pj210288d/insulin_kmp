package com.dj.insulink.shared.core.di

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.glucose.di.glucoseModule
import com.dj.insulink.shared.feature.settings.di.settingsModule
import org.koin.core.context.startKoin

// Pokreće Koin za iOS - trenutno samo modul-i koje MVP Glucose ekran zahteva (vidi
// GlucoseViewModel u feature/glucose/ui/viewmodel). Poziva se jednom iz MainViewController.kt
// pre prvog Compose ekrana - `started` čuva od dvostrukog startKoin poziva (Koin baca ako se
// pozove dva puta u istom procesu) ako bi MainViewController() ikad bio pozvan više puta.
//
// Firebase Auth još nije povezan na iOS strani (faza 4 MVP, vidi CLAUDE.md - GitLive Firebase
// KMP ili sličan wrapper je kandidat za sledeću iteraciju) - zato se ovde odmah postavlja
// fiksni lokalni demo korisnik umesto prave prijave. Podaci ostaju samo lokalno (Room preko
// SQLite bundled drajvera - vidi DatabaseFactory.ios.kt), bez cloud sinhronizacije, isto kao
// za sve ostale feature-e na iOS-u za sada (vidi NotImplemented*RemoteDataSource fajlove).
private var started = false

fun initKoinIOS() {
    if (started) return
    started = true
    startKoin {
        modules(glucoseModule, settingsModule)
    }
    UserSession.setCurrentUserId(IOS_DEMO_USER_ID)
}

private const val IOS_DEMO_USER_ID = "ios-demo-user"
