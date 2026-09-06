package com.dj.insulink.shared.core.localization

import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage

// Prevodilački helper za PUNU lokalizaciju deljenog UI-ja (odluka korisnika 2026-09-07 - "ima
// vremena", proširenje ranije namerno ograničene promene jezika na samo navigaciju/Settings).
// Namerno BEZ posebnog resource sistema (Compose Multiplatform composeResources runtime-override
// jezika nije proveren u ovoj pinovanoj CMP 1.10 verziji - isti oprezan princip kao ostale gotcha
// odluke u CLAUDE.md, npr. izbegavanje novijih/neproverenih API-ja). Svaki string na svakom
// deljenom ekranu se prevodi pozivom `tr(language, "srpski tekst", "english text")` na mestu
// upotrebe - `language` se čita JEDNOM po ekranu (`val language by
// LocalizationSession.currentLanguage.collectAsState()`) i prosleđuje dalje, umesto da svaki
// poziv sam pretplaćuje novi State (jeftinije, i radi i van @Composable konteksta - npr. u
// pomoćnim formatting funkcijama).
fun tr(language: AppLanguage, sr: String, en: String): String =
    if (language == AppLanguage.ENGLISH) en else sr
