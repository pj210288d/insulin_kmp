# Insulink KMP — kontekst za Claude Code

## O projektu

Diplomski rad: migracija postojeće Android aplikacije **Insulink** (praćenje dijabetesa — glukoza, obroci, insulin, fizička aktivnost, podsetnici, prijatelji, izveštaji) na **Kotlin Multiplatform** arhitekturu sa podrškom za Android, iOS, Wear OS i (opciono) web.

Puna specifikacija rada nalazi se u `specifikacija.md` u korenu repoa — **pre bilo kakvih većih arhitekturnih odluka, proveri taj fajl.** Sadrži: analizu postojećeg rešenja, izbor KMP biblioteka, predloženu Gradle strukturu modula, sve funkcionalne i nefunkcionalne zahteve (FZ-1 do FZ-14), model podataka i plan realizacije po fazama.

## Trenutno stanje (ažurirano)

- Repo: `insulin_kmp` na GitHub-u (prebačen iz originalnog Insulink repoa uz zadržanu git istoriju, ne fork)
- Postojeći `androidApp` (ranije `app`) modul i dalje radi nepromenjen — Hilt, Room, Retrofit/Gson, Firebase
- Dodat je novi `:shared` Gradle modul (Kotlin Multiplatform + Compose Multiplatform + Android KMP library plugin)
- **`shared` modul sada uspešno builduje** (Gradle sync i build prolaze)
- iOS strana (`iosApp`) još nije testirana — planirano kad korisnik obezbedi pristup Mac računaru

## Rešeni problemi pri postavljanju (da se ne ponavljaju)

1. **`android {}` vs `androidLibrary {}` blok** — za AGP ispod 8.12.0 koristi se `androidLibrary {}`, ne `android {}` (real projekat ima AGP 8.11.1). Ne diraj ovo bez razloga da ne izazoveš regresiju.
2. **Version catalog (`gradle/libs.versions.toml`)** — kad se dodaju novi KMP/Compose alias-i, moraju se dodati i odgovarajući `[libraries]` unosi, ne samo `[versions]`/`[plugins]`. Crtice u ključu (`compose-components-resources`) postaju ugnježdeni pristup u kodu (`libs.compose.components.resources`).
3. **Material3 ima poseban verzioni ciklus** — u Compose Multiplatform 1.11.1, `material3` artefakt je ostao na `1.11.0-alpha07` (nije stigao do 1.11.1 kao runtime/ui/foundation). Ima svoj `composeMaterial3` version key u tomlu, odvojen od `composeMultiplatform`. Ako se compose verzija ikad podigne, proveri zvanične JetBrains release notes za tačnu material3 verziju pre nego što je uskladiš sa ostatkom.
4. Ne diraj AGP verziju na 9.0+ dok se ne planira namerna migracija — AGP 9.x zahteva potpuno razdvajanje Android app modula od KMP shared modula i menja ceo pristup (built-in Kotlin, `com.android.kotlin.multiplatform.library` obavezan). Trenutno radimo sa AGP 8.11.1 i `androidLibrary {}` sintaksom namerno.

## Plan migracije (iz specifikacije, poglavlje 9)

1. ✅ Priprema — čišćenje `dataREMOVE` referenci, KMP Gradle struktura
2. **U TOKU** — Migracija modela i poslovne logike u `commonMain` (Room→Room Multiplatform ili SQLDelight — odluka još nije doneta, treba spike)
3. Migracija Android UI-ja na Compose Multiplatform
4. iOS aplikacija
5. Nove funkcionalnosti (FZ-9 insulin doze, FZ-10 real-time sync, FZ-12 statistika, FZ-14 LibreLinkUp)
6. Wear OS aplikacija
7. Web aplikacija (opciono)
8. Testiranje, evaluacija, pisanje rada

**Preporučen pristup za korak 2:** ne migrirati sve feature-e odjednom. Prvo prebaciti JEDAN feature (predlog: `glucose`, najjednostavniji) kroz ceo lanac — entitet → repozitorijum → Koin modul → Ktor poziv — u `shared/commonMain`, potvrditi da `androidApp` i dalje radi identično, pa tek onda ponoviti isti obrazac za `meals`, `fitness`, `reminders`, itd.

## Napomene o razvojnom okruženju

- Developer radi na Windows laptopu — nema pristup macOS/Xcode za iOS build, planira da pozajmi Mac od druga kad dođe vreme za iOS deo (faza 4)
- Git remote koristi Personal Access Token (ne SSH) za push na GitHub
