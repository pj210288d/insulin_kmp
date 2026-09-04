# Dnevnik migracije — Insulink KMP

Dnevnik napretka na diplomskom radu (migracija Insulink aplikacije na Kotlin Multiplatform).
Nova stavka se dodaje posle svakog završenog feature-a/koraka: šta je dodato, koje odluke su
donete (i zašto), i šta ostaje do kraja rada. Redosled je hronološki (najstarije na vrhu).
Kontekst i plan po fazama: `CLAUDE.md` i `specifikacija.md` u korenu repoa.

---

## 2026-07-30 — Priprema: `:shared` KMP modul

### Šta smo dodali
- Novi Gradle modul `:shared` (Kotlin Multiplatform + Compose Multiplatform + Android KMP
  library plugin), targete: `androidLibrary`, `iosArm64`, `iosSimulatorArm64`.
- Sređen `gradle/libs.versions.toml` — dodati version/library unosi za KMP/Compose
  alias-e (uključujući poseban `composeMaterial3` version key).
- `androidApp` (`app`) modul ostaje nepromenjen i dalje radi identično (Hilt, Room,
  Retrofit/Gson, Firebase).

### Odluke
- Korišćen je `androidLibrary {}` DSL blok (ne `android {}`), jer je AGP 8.11.1 < 8.12.0.
- AGP ostaje na 8.11.1 — namerno se ne diže na 9.0+ dok se ne planira posebna migracija
  (AGP 9.x zahteva potpuno razdvajanje Android app modula od KMP shared modula).

### Šta je ostalo
- Dodavanje prve stvarne poslovne logike u `commonMain` (urađeno u sledećoj stavci —
  glucose feature).
- iOS strana (`iosApp`) nije testirana — nema pristupa Mac računaru.

---

## 2026-07-31 — Feature: Glucose migriran u `:shared`

### Šta smo dodali
- Ceo glucose lanac prebačen iz `app` u `shared/commonMain`: domen model
  (`GlucoseReading`), Room entitet/DAO/baza (Room Multiplatform + KSP), mapper,
  `GlucoseReadingRepository`, `GlucoseRemoteDataSource` interfejs, Koin modul.
- `shared/androidMain`: `DatabaseFactory` (Room builder preko `Context`),
  `FirebaseGlucoseRemoteDataSource` (identična logika kao stari kod, samo iza interfejsa).
- `shared/iosMain`: `DatabaseFactory` (NSDocumentDirectory), `NotImplementedGlucoseRemoteDataSource`
  (eksplicitno baca grešku — nema still Firestore na iOS-u). Nije build-verifikovano (nema Mac-a).
- U `app` modulu: `InsulinkApplication` sad pokreće Koin pored Hilt-a; novi `SharedModule.kt`
  (Hilt `@Module`) premošćuje Koin-om upravljanu `GlucoseReadingRepository` u Hilt graf, tako
  da `GlucoseViewModel`/`ReportsViewModel` ostaju nepromenjeni (samo import).
- Stari app-modul fajlovi obrisani (`GlucoseReadingRepository`, `GlucoseReadingDao`,
  `GlucoseReadingEntity`, `GlucoseMapper`, stari `GlucoseReading` model), `InsulinkDatabase`
  i `DatabaseModule` više ne sadrže glucose tabelu/DAO.
- Testovi: `GlucoseMapperTest` i `GlucoseReadingRepositoryTest` premešteni u
  `shared/commonTest` (ručno pisani fake-ovi za DAO/remote izvor, jer MockK nije
  multiplatform). `GlucoseViewModelTest`, `ReportsViewModelTest`, `FriendsViewModelTest`
  ažurirani samo u importima.

### Odluke
- **Firestore u commonMain**: umesto uvođenja GitLive Firebase KMP SDK-a odmah, remote sync
  je iza `GlucoseRemoteDataSource` interfejsa — implementiran samo na Androidu (postojeći
  Firebase Android SDK, nepromenjeno ponašanje), a iOS actual baca `NotImplementedError` do
  faze 4 (kad bude dostupan Mac za testiranje). Nisko rizično, ništa novo za testirati sada.
- **Odvojena Room baza**: glucose sad živi u sopstvenom fajlu (`glucose_readings.db`) umesto
  u zajedničkoj `insulink_database`. Prihvaćen rizik gubitka lokalnog keša kod postojećih
  instalacija — self-heal-uje se jer `GlucoseWrapper` zove
  `fetchAllGlucoseReadingsForUserAndUpdateDatabase` pri otvaranju ekrana (re-sync sa
  Firestore-a). Prihvatljivo jer je projekat u dev fazi, bez produkcionih korisnika.
- **Koin + Hilt paralelno**: umesto potpune zamene Hilt-a odjednom, Koin se koristi samo za
  `:shared` feature-e, a most (`SharedModule.kt`, `GlobalContext.get().get()`) ubacuje
  Koin-om kreirane instance u Hilt graf. Ovaj obrazac se ponavlja za svaki sledeći feature.
- **Paket shared koda**: novi kod ide pod `com.dj.insulink.shared.*` (ne pod template-ski
  `org.example.project`), radi konzistentnosti sa app modulom (`com.dj.insulink.*`).

### Verifikacija
- `:shared:compileAndroidMain`, `:app:assembleDebug`, `:shared:testAndroidHostTest`,
  `:app:testDebugUnitTest` — svi BUILD SUCCESSFUL.
- Ručni smoke test na fizičkom telefonu (Samsung S942B): `logcat` bez padova/Koin grešaka;
  potvrđeno preko `adb shell run-as ... ls databases` da postoji novi `glucose_readings.db`
  fajl (dokaz da je nova putanja aktivna, ne stara).
- PR otvoren: grana `jovan/glucose-shared-migration` → `master` (3 commit-a).

### Šta je ostalo
- Ostali feature-i po istom obrascu, redosled iz CLAUDE.md: `meals`, `fitness`,
  `reminders`, `friends`, `settings`, `reports`.
- Poznat, nepovezan bug primećen tokom testiranja (nije naš regres): `SecurityException`
  pri otvaranju/deljenju PDF izveštaja (`ReportsScreen.kt` — `FileProvider` nema write grant).
  Nije popravljeno, čeka odluku da li se radi sad ili kasnije.
- Faza 3: migracija Android UI-ja na Compose Multiplatform.
- Faza 4: iOS aplikacija (čeka pristup Mac računaru; tad i pravi Firestore za iOS).
- Faza 5: nove funkcionalnosti — FZ-9 (insulin doze), FZ-10 (real-time sync), FZ-12
  (statistika), FZ-14 (LibreLinkUp).
- Faza 6: Wear OS aplikacija.
- Faza 7: Web aplikacija (opciono).
- Faza 8: Testiranje, evaluacija, pisanje rada.

---

## 2026-07-31 — Feature: Meals migriran u `:shared` (uveden Ktor)

### Šta smo dodali
- Ceo meals lanac prebačen u `shared/commonMain`: domen modeli (`Meal`, `MealIngredient`,
  `Ingredient`, `DailyNutrition`), 3 Room entiteta/DAO-a (`MealEntity`, `IngredientEntity`,
  `MealIngredientEntity`) u JEDNOJ `MealsDatabase` (za razliku od glucose, ova tri entiteta
  ostaju zajedno jer se relaciono koriste zajedno — isti obrazac kao stara `InsulinkDatabase`),
  mapperi, `MealRepository`, Koin modul.
- **Prvi pravi multiplatform mrežni poziv**: USDA/Spoonacular food-search API (ranije
  Retrofit+Gson, Android-only) prebačen na **Ktor Client** (`io.ktor:ktor-client-core` 3.4.0)
  + **kotlinx.serialization** (1.9.0) umesto Gson. `KtorFoodApiRemoteDataSource` živi u
  `commonMain` i radi identično na Android i iOS (samo se HTTP engine razlikuje: OkHttp na
  Androidu, Darwin na iOS-u) — ovo je konkretno taj "Ktor poziv" korak iz CLAUDE.md plana.
- Firestore deo (`MealRemoteDataSource`) i dalje prati glucose obrazac: Android implementacija
  identična staroj logici, iOS `NotImplementedError` do faze 4.
- Nova deljena `shared/androidMain` `core/di/FirebaseModule.kt` — `FirebaseFirestore` singleton
  se sad registruje NA JEDNOM MESTU (ne po feature-u), da ne dođe do Koin konflikta kad se
  glucose i meals moduli učitaju zajedno.
- U `app` modulu: `InsulinkApplication` sad prosleđuje USDA/Spoonacular API ključeve
  (iz `BuildConfig`, i dalje čitani iz `local.properties`) u `mealsModule(usdaApiKey, spoonacularApiKey)`
  pri `startKoin`. `SharedModule.kt` dobija bridge i za `MealRepository`.
- Stari app-modul fajlovi obrisani: `MealRepository`, `FoodApiRepository`, `FoodApiService`,
  `FoodApiModels`, 3 DAO-a, 3 entiteta, `MealMappers`, stari `Meal.kt` model, i ceo
  `NetworkModule.kt` (Retrofit/Gson/OkHttp Hilt provideri — više se nigde ne koriste).
  `InsulinkDatabase`/`DatabaseModule` više ne sadrže meal/ingredient/mealIngredient.
  Retrofit/Gson/OkHttp uklonjeni i iz `app/build.gradle.kts` (potvrđeno grep-om da ih niko
  drugi u app modulu ne koristi).
- Testovi premešteni/prepisani u `shared/commonTest`: `MealMappersTest`, `MealRepositoryTest`
  (ručni fake-ovi za 3 DAO-a + oba remote izvora), i novi `KtorFoodApiRemoteDataSourceTest`
  koji koristi Ktor-ov `MockEngine` (`ktor-client-mock`) umesto Retrofit/OkHttp mock-ovanja —
  čist multiplatform test bez ijedne Android-specifične zavisnosti.

### Odluke
- **Ktor umesto Retrofit-a za food-search API**: pošto USDA/Spoonacular nisu vezani ni za
  jednu platformu (čist REST), nije bilo razloga da ostanu Android-only kao Firestore. Ovo je
  ujedno i prva potvrda da "commonMain networking" stvarno radi u ovom projektu.
- **Verzije Ktor-a/kotlinx.serialization proverene uživo** (WebSearch) pre pisanja koda —
  Ktor 3.4.0, kotlinx.serialization 1.9.0 (kompatibilno sa Kotlin 2.2.20) — da se izbegne
  pogađanje verzije i pokvaren build.
- **Jedna Room baza za sva 3 meals entiteta** (za razliku od "1 feature = 1 baza" pravila iz
  glucose stavke) — jer se `Meal`/`Ingredient`/`MealIngredient` relaciono koriste zajedno u
  repository sloju (ručni join-ovi), pa razdvajanje u 3 fajla ne bi imalo smisla.
- **FirebaseFirestore centralizovan** u `shared/androidMain/core/di/FirebaseModule.kt` — otkriveno
  tokom pisanja da bi svaki feature koji ga zasebno registruje u Koin-u pukao pri startup-u čim
  se učita više od jednog feature Koin modula istovremeno (Koin ne dozvoljava duplu registraciju
  istog tipa). Retroaktivno izbačen i iz `GlucoseModule.android.kt`.
- **API ključevi ostaju u Android `BuildConfig`** (ne dupliraju se u `:shared` modulu) — prosleđuju
  se kao obični `String` parametri u `mealsModule(usdaApiKey, spoonacularApiKey)` pri `startKoin`,
  umesto da `:shared` sam čita `local.properties`. Jednostavnije, bez rizika po postojeći setup.
- **Cross-module smart cast gotcha**: `MealItem.kt` (`meal.calories != null` pa direktno
  `meal.calories` u `stringResource(...)`) prestao je da se kompajlira kad je `Meal` prešao u
  `:shared` — Kotlin ne radi smart-cast na `val` iz DRUGOG modula za pozive koji očekuju
  non-null `Any` (npr. `stringResource` vararg). Rešenje: lokalni `val calories = meal.calories`
  pre provere. **Ovo je opšti obrazac koji treba očekivati i kod narednih feature-a** (fitness,
  reminders...) čim se njihovi domain modeli presele u `:shared` — potraži slične
  `model.nullableField != null` + direktno korišćenje u pozivima koji traže `Any`/non-null tip.

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:assembleDebug`,
  `:app:testDebugUnitTest` — svi BUILD SUCCESSFUL.
- Ručni smoke test na fizičkom telefonu (Samsung S942B): app se pokreće bez pada, Koin
  bootstrap čist (firebaseModule + glucoseModule + mealsModule zajedno, bez "already exists"
  konflikta — potvrđuje da je centralizacija FirebaseFirestore-a rešila problem). Dodavanje/
  brisanje obroka radi, dnevni nutritivni pregled radi.
- **USDA/Spoonacular pretraga**: prvi pokušaj nije vratio ništa — ispostavilo se da
  `local.properties` nikad nije imao `USDA_API_KEY`/`SPOONACULAR_API_KEY` (pre-postojeće stanje,
  ne regresija; stari Retrofit kod je imao identičnu "tiho vrati prazno" granu za prazne
  ključeve). Korisnik je dao oba ključa, dodati u `local.properties` (nisu u git-u). Posle
  reinstalacije: Ktor pozivi vidljivo idu na mrežu (potvrđeno u logcat-u), pretraga radi.
  Jedino što se vidi u logu je bezopasan `CancellationException` iz Ktor-a kad debounce/
  `flatMapLatest` otkaže prethodni zahtev pri kucanju sledećeg karaktera — očekivano ponašanje.
- Baza potvrđena na uređaju (`adb shell run-as ... ls databases`): `meals.db` postoji kao
  odvojen fajl, `insulink_database` i dalje postoji (za friends/reminders/fitness).

### Šta je ostalo
- Isti kao u prethodnoj stavci, minus meals: `fitness`, `reminders`, `friends`, `settings`,
  `reports` po istom obrascu. Kod fitness/reminders/friends/reports očekuj isti
  cross-module smart-cast gotcha opisan gore.
- Firestore ostaje jedini deo koji nije multiplatform (Android-only iza interfejsa) —
  food-search API sada JESTE multiplatform zahvaljujući Ktor-u, što je dobar presedan za dalje.
- Isto što i pre za faze 3–8 (Compose Multiplatform UI, iOS, nove funkcionalnosti, Wear OS,
  Web, testiranje/pisanje rada).

---

## 2026-08-01 — Feature: Fitness migriran u `:shared`

### Šta smo dodali
- `Exercise` domen model + Room entitet/DAO/baza (`ExerciseDatabase`, sopstveni fajl
  `exercises.db` — jednostavan slučaj kao glucose, jedan entitet) + mapper + `ExerciseRepository`
  + `ExerciseRemoteDataSource` (Firestore, Android-only implementacija/iOS stub) + Koin modul
  (`fitnessModule`), sve po identičnom obrascu kao glucose.
- `Sport` domen model (izračunata statistika — prosečan/poslednji pad glukoze po satu po
  sportu) i `calculateSportsFromExercises`/`calculateDropPerHour` **ostaju u `app` modulu**,
  u `FitnessViewModel` — nisu persistovani, čisto prezentacioni sloj (isti princip kao
  `GlucoseReadingTimespan`/filter logika koja je ostala u `GlucoseViewModel`).
- `InsulinkApplication` sad učitava i `fitnessModule` (4. feature Koin modul, uz firebase/
  glucose/meals). `SharedModule.kt` dobija bridge i za `ExerciseRepository`.
- Stari app-modul fajlovi obrisani (`ExerciseRepository`, `ExerciseDao`, `ExerciseEntity`,
  `ExerciseMapper`, stari `Exercise.kt` model); `InsulinkDatabase`/`DatabaseModule` više ne
  sadrže exercise tabelu/DAO.
- Testovi (`ExerciseMapperTest`, `ExerciseRepositoryTest`) premešteni u `shared/commonTest`
  sa fake-ovima; `FitnessViewModelTest` ažuriran samo u importima.

### Odluke
- Nije bilo potrebe za novom arhitekturnom odlukom — fitness je čisto ponovio glucose obrazac
  1:1 (jedan entitet, jedna baza, Firestore iza interfejsa). Ovo je dobar znak da se obrazac
  iz prve dve migracije stabilizovao i da se sledeći feature-i (reminders, friends) mogu raditi
  još brže.
- Cross-module smart-cast gotcha (najavljen u meals stavci) se OVOG PUTA nije pojavio —
  `FitnessScreen.kt`/`AddSportsActivityDialog.kt` ne rade `if (model.nullableField != null)`
  pa direktno prosleđivanje u `Any`-tražeći poziv. I dalje treba paziti na to kod reminders/
  friends/reports.

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:assembleDebug`,
  `:app:testDebugUnitTest` — svi BUILD SUCCESSFUL.
- Ručni smoke test na telefonu (ovog puta Samsung A528B — svež install, drugi uređaj/UID
  nego glucose/meals testovi): app se pokreće čisto, 4 Koin modula (firebase+glucose+meals+
  fitness) učitana bez konflikta. Baze se prave lenjo (Koin `single`) — na svežem install-u
  `databases/` prazna dok se ne otvori odgovarajući ekran, to je očekivano, ne bug.
  Korisnik potvrdio da dodavanje aktivnosti i fitness statistika rade na uređaju.

### Šta je ostalo
- `reminders`, `friends`, `settings`, `reports` po istom obrascu (redosled iz CLAUDE.md).
- Isto što i pre za Firestore-only-Android ograničenje i faze 3–8.

---

## 2026-08-03 — Feature: Reminders migriran u `:shared`

### Šta smo dodali
- `Reminder` domen model + `ReminderType` enum (`MEAL_REMINDER`, `INSULIN_REMINDER`,
  `BLOOD_SUGAR_CHECK_REMINDER`) + Room entitet/DAO/baza (sopstveni fajl `reminders.db`) +
  mapper + `ReminderRepository` + `ReminderRemoteDataSource` (Firestore, Android-only
  implementacija/iOS stub) + Koin modul (`remindersModule`) — isti obrazac kao glucose/fitness.
- `InsulinkApplication` sad učitava i `remindersModule` (5. feature Koin modul). `SharedModule.kt`
  dobija bridge i za `ReminderRepository`.
- Stari app-modul fajlovi obrisani (`ReminderRepository`, `ReminderDao`, `ReminderEntity`,
  `ReminderMapper`, stari `Reminder.kt`/`ReminderType` model); `InsulinkDatabase`/`DatabaseModule`
  više ne sadrže reminder tabelu/DAO. `ReminderScheduler` (AlarmManager) ostaje nepromenjen u
  `app` modulu — Android-only sistemski API, nema smisla da ide u `commonMain`.
- Testovi (`ReminderMapperTest`, `ReminderRepositoryTest`, `ReminderTypeTest`) premešteni u
  `shared/commonTest` sa fake-ovima; `RemindersViewModelTest` ažuriran samo u importima.

### Odluke
- **`ReminderType` očišćen od Android resursa**: stari enum je imao `@StringRes`/`@DrawableRes`
  konstante direktno na svakoj vrednosti (`R.string.*`, `R.drawable.*`) — to ne može da postoji u
  `commonMain` (Android-specifični tipovi). Rešenje: enum u `:shared` je čist (samo `name`,
  `fromName`), a `displayNameRes`/`icon` postali su extension property-ji na `ReminderType` u
  NOVOM app-modul fajlu (`feature/reminders/ui/ReminderTypeUi.kt`). Ovo je isti princip kao
  ranija odluka da UI-vezani kod ostaje u `app` (npr. `Sport` statistika u fitness stavci), samo
  prvi put primenjen na sam domain enum, ne na posebnu klasu. **Očekivati isti obrazac za svaki
  budući domain enum/model koji nosi `@StringRes`/`@DrawableRes`** (proveriti `friends`/`settings`
  pre migracije).
- **java.time → kotlinx-datetime**: `isDoneForToday` logika (računanje da li je podsetnik već
  "odrađen danas" na osnovu trenutnog vremena u danu) je u starom kodu koristila `java.time.*`
  (JVM-only, ne postoji u `commonMain`). Dodata `kotlinx-datetime` 0.8.0 zavisnost (verzija
  potvrđena uživo pre pisanja koda, isti princip kao kod Ktor/kotlinx.serialization u meals
  stavci); nova `shared/commonMain/core/time/LocalTimeOfDay.kt` sa `localTimeOfDay(epochMillis)`/
  `currentLocalTimeOfDay()` helper funkcijama koje sad koriste `kotlinx.datetime.LocalTime`/
  `TimeZone`. **Ovo je prvi put da se java.time zavisnost morala zameniti** — dobar signal da
  treba unapred proveriti svaki feature na `import java.time.*` pre migracije (friends/settings/
  reports mogu imati sličnu logiku).
- **kotlinx-datetime 0.8.0 API gotcha**: `Instant`/`Clock` u ovoj verziji nisu više
  `kotlinx.datetime.Instant`/`Clock` nego reeksportovani iz Kotlin stdlib-a (`kotlin.time.Instant`,
  `kotlin.time.Clock`), koji su označeni `@ExperimentalTime`. Kompajler je bacio
  "This declaration needs opt-in" i "Unresolved reference 'System'" dok se koristio stari
  `kotlinx.datetime.Clock`/`Instant` import. Rešenje: import `kotlin.time.Clock`/`kotlin.time.Instant`/
  `kotlin.time.ExperimentalTime` + `@OptIn(ExperimentalTime::class)` na obe funkcije u
  `LocalTimeOfDay.kt`. **Vredi zapamtiti za bilo koji budući kod koji direktno zove
  `Clock.System.now()` ili `Instant.fromEpochMilliseconds(...)`.**

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:assembleDebug`,
  `:app:testDebugUnitTest` — svi BUILD SUCCESSFUL.
- Ručni smoke test na telefonu: app se pokreće čisto, 5 Koin modula (firebase+glucose+meals+
  fitness+reminders) učitana bez konflikta, logcat bez FATAL/AndroidRuntime/Koin grešaka.
  Korisnik potvrdio da dodavanje/brisanje podsetnika i AlarmManager notifikacije rade na uređaju.

### Šta je ostalo
- `friends`, `settings`, `reports` po istom obrascu (redosled iz CLAUDE.md). Kod `friends`
  posebno paziti — dvosmeran model prijateljskih zahteva (request/accept), verovatno nije
  prosto CRUD kao dosadašnji feature-i.
- Pre svakog narednog feature-a: proveriti `@StringRes`/`@DrawableRes` na domain modelima i
  `import java.time.*` upotrebu — oba gotcha-a otkrivena tek u ovoj stavci.
- Isto što i pre za Firestore-only-Android ograničenje i faze 3–8.

---

## 2026-08-03 — Feature: Friends migriran u `:shared`

### Šta smo dodali
- `Friend` domen model + Room entitet/DAO/baza (sopstveni fajl `friends.db`, umesto stare
  zajedničke `insulink_database`) + mapper + `FriendRepository` + `FriendRemoteDataSource`
  (Firestore, Android-only implementacija/iOS stub) + Koin modul (`friendsModule`) — isti
  obrazac kao glucose/fitness/reminders.
- Pregledom koda ispostavilo se da `friends` NIJE dvosmeran request/accept model kako je
  najavljeno u prošloj stavci — dodavanje prijatelja odmah upisuje prijateljstvo objema
  stranama (`pushFriendToFirestoreForUser` se poziva dva puta, jednom za svakog korisnika),
  bez posrednog "zahtev na čekanju" stanja. Migracija je ispala jednostavnija nego očekivano.
- `InsulinkApplication` sad učitava i `friendsModule` (6. i poslednji feature Koin modul za
  sada). `SharedModule.kt` dobija bridge i za `FriendRepository`.
- Stari app-modul fajlovi obrisani (`FriendRepository`, `FriendDao`, `FriendEntity`,
  `FriendMapper`, stari `Friend.kt` model). **Ceo `core/room/InsulinkDatabase.kt` i
  `core/di/DatabaseModule.kt` (Hilt) obrisani u potpunosti** — `friends` je bio poslednji
  entitet u toj bazi, pa isti kao `NetworkModule.kt` presedan iz meals stavke: kad feature-u
  migracija ukloni poslednjeg korisnika neke infrastrukture, ta infrastruktura ide u brisanje,
  ne u "ostavi za svaki slučaj".
- Testovi (`FriendMapperTest`, `FriendRepositoryTest`) premešteni u `shared/commonTest` sa
  fake-ovima (uključujući nove test slučajeve za `fetchFriendDataAndUpdateDatabase` — insert
  novog kandidata vs. update postojećeg, stari app-modul test to nije pokrivao);
  `FriendsViewModelTest` ažuriran za novi `FriendCandidate` API (vidi odluke ispod).

### Odluke
- **`FriendCandidate` umesto `UserWithLatestReading`/app-modulskog `User`-a**: stari
  `FriendRepository.findUserByFriendCode()` vraćao je `UserWithLatestReading` koji je unutra
  nosio `com.dj.insulink.auth.domain.models.User` — ali `auth` feature NIJE u planu migracije
  (nije na listi u CLAUDE.md), pa `:shared` ne sme da zavisi od app-modulskog `User` modela.
  Rešenje: nov, samostalan `FriendCandidate` domen model u `:shared`
  (`uid`/`firstName`/`lastName`/`latestReading: GlucoseReading?`) koji nosi samo ono što je
  friends feature-u zapravo potrebno iz "pronađenog korisnika". `GlucoseReading` je već u
  `:shared` (iz glucose stavke) pa se prirodno reuse-uje. **Ovo je prvi put da je migracija
  feature-a otkrila implicitnu zavisnost od NEmigriranog feature-a** — vredi proveriti i kod
  `settings`/`reports` da li nešto slično zavisi od `auth`/`User`.
- **Bug fix usput**: stari `findUserByFriendCode` je pozivao `snapshot.documents.first()` —
  da nema poklapanja po friend kodu, ovo baca `NoSuchElementException` umesto da vrati `null`,
  iako je `FriendsViewModel`/testovi već pretpostavljali da repozitorijum vraća `null` kad
  korisnik nije pronađen (postojeći test `onAddFriendClick with no matching user` mock-uje
  `null` povratnu vrednost, što se u produkciji nikad ne bi desilo — test i implementacija su
  bili neusklađeni). Ispravljeno na `.firstOrNull()` u novom
  `FirebaseFriendRemoteDataSource.findFriendCandidateByFriendCode`. Nije agresivna promena
  ponašanja (samo sprečava crash u retkom edge-case-u), ali vredi zabeležiti jer odudara od
  principa "1:1 port bez promena ponašanja" koji smo pratili do sada.
- **Cross-module smart-cast gotcha, drugi put**: `FriendsListItem.kt` je imao DVA mesta gde
  se nullable `Friend` polje direktno prosleđuje dalje nakon null-check-a
  (`friend.friendLastGlucoseReadingValue` u `stringResource(...)` i `GlucoseLevelTag(...)`, i
  `friend.friendsLastGlucoseReadingTime` u `Date(...)`) — oba popravljena istim obrascem kao u
  meals stavci (lokalni `val` pre null-provere). Ovog puta je gotcha pogodio i common
  konstruktor (`Date(Long)`), ne samo `Any`-vararg pozive kao u meals — potvrđuje da je
  bezbednije uvek raditi lokalni `val` bind za nullable cross-module polja, ne samo kad idu u
  `stringResource`.

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:assembleDebug`,
  `:app:testDebugUnitTest` — svi BUILD SUCCESSFUL.
- Ručni smoke test na telefonu: app se pokreće čisto, 6 Koin modula (firebase+glucose+meals+
  fitness+reminders+friends) učitana bez konflikta, logcat bez FATAL/AndroidRuntime/Koin
  grešaka. Korisnik potvrdio da prikaz friend koda, dodavanje prijatelja preko koda i prikaz
  liste prijatelja (sa poslednjim očitavanjem glukoze) rade na uređaju.

### Šta je ostalo
- `settings`, `reports` po istom obrascu (redosled iz CLAUDE.md) — ovo su poslednja dva
  feature-a iz faze 2 migracionog plana.
- Pre `settings`/`reports`: proveriti (a) `@StringRes`/`@DrawableRes` na domain modelima,
  (b) `import java.time.*`, (c) implicitnu zavisnost od `auth`/`User` modela — sva tri gotcha-a
  su se pojavila u poslednje dve stavke (reminders, friends).
- Isto što i pre za Firestore-only-Android ograničenje i faze 3–8.

---

## 2026-08-03 — Feature: Settings migriran u `:shared`

### Šta smo dodali
- `GlucoseUnit` i `AppLanguage` domen enumi (bez Android resursa/`Locale`, vidi odluke ispod)
  prebačeni u `shared/commonMain`.
- **Nema Room ni Firestore ovog puta** — settings je čisto lokalna key-value konfiguracija.
  Umesto toga: `SettingsPreferences` je postao `expect class` (isti obrazac kao `DatabaseFactory`
  u ostalim feature-ima, samo primenjen na SharedPreferences umesto Room builder-a) —
  `androidMain actual` koristi `android.content.SharedPreferences` (identična logika kao stari
  kod), `iosMain actual` koristi `NSUserDefaults` (nov kod, nije ranije postojao jer je stari
  app bio Android-only). Koin modul (`settingsModule`) prati identičan `expect/actual`
  `platformSettingsModule()` obrazac kao ostali feature-i.
- `InsulinkApplication` sad učitava i `settingsModule` (7. feature Koin modul). `SharedModule.kt`
  dobija bridge i za `SettingsPreferences`.
- Stari app-modul fajlovi obrisani (`SettingsPreferences`, `AppLanguage`, `GlucoseUnit`).
  Nova UI-only fajlovi u app modulu: `feature/settings/ui/AppLanguageUi.kt` (`locale`,
  `flagIcon` extension property-ji) i `feature/settings/ui/GlucoseUnitUi.kt` (`flagIcon`)
  — isti obrazac kao `ReminderTypeUi.kt` iz reminders stavke.
- `GlucoseUnit`/`SettingsPreferences` su najšire korišćeni tipovi do sada u migraciji — 20-ak
  fajlova u `app` modulu (glucose, fitness, friends, reports, settings ekrani/ViewModel-i)
  je trebalo da promeni import na `com.dj.insulink.shared.feature.settings...`; sve pronađeno
  jednim grep-om pre početka, ništa nije promaklo (build je to i potvrdio).
- Testovi (`AppLanguageTest`, `GlucoseUnitTest`) premešteni u `shared/commonTest`;
  `SettingsViewModelTest` i svi ViewModel testovi koji koriste `GlucoseUnit`/`SettingsPreferences`
  (fitness, friends, glucose, reports) ažurirani samo u importima.

### Odluke
- **`GlucoseUnit`/`AppLanguage` očišćeni od Android resursa, drugi i treći put**: isti obrazac
  kao `ReminderType` iz reminders stavke — `@DrawableRes flagIcon` na oba enuma prebačen u
  app-modulske extension property-je. `AppLanguage` je imao i `java.util.Locale` polje (JVM-only,
  ne postoji u commonMain) — ono je TAKOĐE prebačeno u app-modulski `AppLanguageUi.kt`, jer se
  jedino koristi u `SettingsWrapper.applyLocaleAndRecreate` (Android `Activity`/`Locale` API,
  čisto platformski kod). Enum u `:shared` sad nosi samo `key`/`displayName`.
- **`String.format(Locale.US, "%.1f", ...)` zamenjen ručnim zaokruživanjem**: `GlucoseUnit
  .formatValue()` je za mmol/L koristio `String.format` sa eksplicitnim `Locale.US` (baš zato da
  decimalni separator UVEK bude tačka, bez obzira na jezik uređaja) — ni `String.format` ni
  `java.util.Locale` ne postoje u commonMain. Napisana je čista Kotlin `formatOneDecimal` funkcija
  (zaokruži na jednu decimalu preko `kotlin.math.round`, pa ručno sastavi string) — Kotlin-ov
  `Long`/`Int` `toString()` je već lokalno-nezavisan (uvek tačka), tako da se isti efekat postiže
  bez ijedne platformske zavisnosti. Postojeći testovi (`GlucoseUnitTest`) su prevedeni 1:1 u
  `shared/commonTest` i i dalje prolaze sa identičnim očekivanim vrednostima (npr. "180 mg/dL →
  10.0 mmol/L"), što potvrđuje da se ponašanje nije promenilo.
- **`SettingsPreferences` kao `expect/actual class` umesto Firestore-repository obrasca**: ovo je
  prvi feature bez perzistentnog cloud/Room sloja — čista lokalna podešavanja. Umesto uvođenja
  nove multiplatform biblioteke (npr. `multiplatform-settings`), iskorišćen je već ustaljeni
  `expect class` obrazac iz `DatabaseFactory` (nema deklarisan konstruktor u `commonMain`, svaka
  platforma dodaje svoj konstruktor u `actual` — androidMain uzima `Context`, iOS ne treba ništa
  jer `NSUserDefaults.standardUserDefaults` je globalan). Manje novih zavisnosti, isti mentalni
  model kao ostatak koda.
- **Uklonjen mrtav kod iz `MainActivity.kt`**: `@Inject lateinit var settingsPreferences:
  SettingsPreferences` nikad nije bio čitan — `attachBaseContext()` (koji postavlja jezik pre
  Hilt injekcije) je oduvek ručno čitao raw `SharedPreferences` sa hardkodiranim ključevima,
  baš zato što Hilt injekcija još nije spremna u tom trenutku životnog ciklusa. Polje je bilo
  vizuelno mrtvo od početka (verovatno ostatak ranije verzije koda). Obrisano umesto premošćeno
  u Hilt bez razloga — analogno ranijim odlukama da se mrtva infrastruktura (`NetworkModule.kt`,
  `InsulinkDatabase.kt`) briše čim migracija to otkrije, ne ostavlja "za svaki slučaj".

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:assembleDebug`,
  `:app:testDebugUnitTest` — svi BUILD SUCCESSFUL.
- Ručni smoke test na telefonu: app se pokreće čisto, 7 Koin modula (firebase+glucose+meals+
  fitness+reminders+friends+settings) učitana bez konflikta, logcat bez FATAL/AndroidRuntime/
  Koin grešaka. Korisnik potvrdio da promena jedinice glukoze (mg/dL ↔ mmol/L) ispravno
  formatira vrednosti na svim ekranima (glucose, friends, fitness) i da promena jezika na
  srpski radi i ostaje posle `recreate()`.

### Šta je ostalo
- **`reports`** — poslednji feature iz faze 2 migracionog plana (CLAUDE.md: glucose, meals,
  fitness, reminders, friends, settings, reports — svi osim `reports` sada gotovi).
  Prilikom `reports` posebno pogledati poznati, nepovezan `SecurityException` bug kod deljenja
  PDF izveštaja (zabeležen još u glucose stavci, nikad popravljen) — odlučiti da li se rešava
  usput ili ostaje van obima migracije.
- Nakon `reports`: faza 3 (Compose Multiplatform UI), faza 4 (iOS — čeka Mac), faza 5 (nove
  funkcionalnosti: FZ-9 insulin doze, FZ-10 real-time sync, FZ-12 statistika, FZ-14 LibreLinkUp),
  faza 6 (Wear OS), faza 7 (Web, opciono), faza 8 (testiranje/pisanje rada).
- Ista tri gotcha-a i dalje vrede proveriti kod `reports`: `@StringRes`/`@DrawableRes` na
  domain modelima, `import java.time.*`, implicitna zavisnost od `auth`/`User` modela.

---

## 2026-08-03 — Feature: Reports — **faza 2 migracije završena** (bez novog `:shared` koda) + 3 popravljena buga

### Šta smo dodali
- **Reports NEMA sopstveni perzistentni domain model** — ne postoji Room baza ni Firestore
  kolekcija koja pripada reports feature-u. Ekran samo orkestrira već migrirane servise
  (`GlucoseReadingRepository`, `SettingsPreferences`) i generiše PDF preko `GlucoseReportPdfGenerator`
  (iText + `android.graphics.Bitmap` za grafikon — suštinski Android-only, kao što je Firestore
  bio pre uvođenja interfejsa, samo ovde nema smisla ni praviti `expect/actual` fasadu jer je CEO
  posao (bitmap renderovanje grafikona) platformski specifičan, ne samo pristup podacima).
  `ReportsViewModel`/`ReportsScreen`/`ReportsWrapper` su već koristili isključivo shared tipove
  (`GlucoseReadingRepository`, `GlucoseUnit`, `SettingsPreferences`) zahvaljujući ranijim
  migracijama (glucose, settings) — grep pre početka nije našao nijedan preostali stari import.
  **Zaključak**: migracija reports-a je suštinski već bila završena kao nusprodukt prethodnih
  stavki; ova stavka je samo potvrdila to (pun build+test prolazi) i iskoristila priliku da se
  poprave tri stvarna, nepovezana bug-a otkrivena/tražena tokom sesije.
- Ovim je **faza 2 migracionog plana (CLAUDE.md, poglavlje 9) završena** — svih 7 feature-a
  (glucose, meals, fitness, reminders, friends, settings, reports) sada žive u `:shared`.

### Popravljeni bug-ovi (van uskog obima "migracije", ali svi vezani za reports ekran)
1. **`SecurityException` pri otvaranju/deljenju PDF izveštaja** (poznat od glucose stavke, nikad
   ranije popravljen): `openPdfFile`/`sharePdfFile` u `ReportsScreen.kt` su intent-u dodavali samo
   `FLAG_GRANT_READ_URI_PERMISSION`. Neki share/view target-i (posebno na Samsung uređajima) traže
   i write grant, a `Intent.createChooser()` ne prosleđuje URI grant flagove pouzdano SVIM
   rezolvovanim target-ima u chooser listi bez eksplicitnog `ClipData`. Popravljeno dodavanjem
   `FLAG_GRANT_WRITE_URI_PERMISSION` i `clipData = ClipData.newRawUri("", uri)` na oba intent-a —
   standardan, dobro poznat Android obrazac za pouzdano `content://` deljenje kroz chooser.
2. **`NullPointerException` pri biranju datuma u izveštaju** (NOVO otkriven tokom ručnog testiranja
   ovog session-a, ne postoji u ranijim beleškama): `ReportDatePickerDialog`-ov confirm handler je
   koristio `params.selectedMaxDate!!` / `params.selectedMinDate!!` — ako korisnik otvori date
   picker PRE nego što je bilo koja strana opsega ikad postavljena (npr. prvi put), `!!` puca.
   Popravljeno sigurnim fallback lancem: `selectedX ?: fullRangeX ?: millis` (koristi već učitani
   pun opseg kao razumnu podrazumevanu vrednost umesto crash-a).
3. **Potvrda opsega nije koristila IZABRANI opseg** (otkriven dok je ispravljan bug #2, direktno
   povezan): `ReportsViewModel.filterReadingsByCurrentDateRange()` je filtrirao očitavanja po
   `_minDate`/`_maxDate` (CEO dostupan opseg), a ne po `_selectedMinDate`/`_selectedMaxDate` (opseg
   koji je korisnik STVARNO izabrao u date picker-u). Efektivno: biranje užeg opsega u UI-ju nikad
   nije menjalo šta ulazi u izveštaj — uvek se koristio ceo dostupan opseg. Popravljeno da koristi
   `_selectedMinDate`/`_selectedMaxDate`. Dodat regresioni test
   (`filterReadingsByCurrentDateRange uses the selected sub-range, not the full available range`)
   koji bi uhvatio ovaj bug da je postojao ranije.
4. **"Ništa se ne dešava" kad generisanje izveštaja tiho ne uspe** (otkriven tokom testiranja
   popravke #2/#3 — korisnik je testirao na uređaju bez merenja glukoze u izabranom opsegu):
   `ReportsViewModel.generatePdfReport()` ispravno postavlja `PdfGenerationState.Error(poruka)`
   kad nema podataka, ali `ReportsScreen.kt` NIJE imao nijedan UI element koji prikazuje tu poruku
   — korisnik je video praznu dugmad bez ikakve reakcije, bez traga zašto. Popravljeno dodavanjem
   `Text` sa porukom greške (`MaterialTheme.colorScheme.error`) ispod "Generate report" dugmeta,
   vidljivim preko `AnimatedVisibility` kad god je stanje `Error`. **Ovo je bio stvarni uzrok
   "ništa se ne dešava" prijave korisnika** — nije bilo podataka za izabrani opseg, ali aplikacija
   to nije komunicirala.

### Odluke
- **Nema `:shared` interfejsa za PDF generisanje**: za razliku od Firestore-a (gde je apstrakcija
  isplativa jer je "šta" isto na obe platforme, samo "kako" različito preko Firebase SDK-a), PDF
  generisanje ovde zavisi od `android.graphics.Bitmap` za renderovanje grafikona — na iOS-u bi
  ekvivalent bio potpuno drugačiji API (CoreGraphics/UIKit), ne samo drugi "actual". Pravljenje
  prazne `expect`/`NotImplementedError` fasade ne bi dodalo nikakvu vrednost sada, pa je
  `GlucoseReportPdfGenerator` ostao u `app` modulu — isti princip kao `ReminderScheduler`
  (AlarmManager) koji je ostao Android-only jer je ceo API inherentno platformski.
- **Bug fix-ovi izvan striktnog obima migracije, ali opravdani**: korisnik je eksplicitno tražio
  da se SecurityException bug popravi u ovoj sesiji, a preostala tri bug-a su otkrivena USPUT dok
  se testirala baš ta popravka (klasičan slučaj — jedan popravljen bug otkrio je sledeći). Sva
  četiri su nezavisna od KMP migracije (postojala bi i da je app ostao čisto Android), pa su
  popravljena direktno umesto odlaganja, u skladu sa eksplicitnim uputstvom korisnika ("radi sve
  komande kao da sam ih odobrio").

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest` (bez izmena — potvrđeno da i dalje
  prolaze), `:app:assembleDebug`, `:app:testDebugUnitTest` (uključujući novi regresioni test za
  bug #3) — svi BUILD SUCCESSFUL.
- Ručni smoke test na telefonu (Samsung A528B): sva 4 bug-a potvrđena kao ispravljena od strane
  korisnika — generisanje izveštaja, Preview (otvaranje PDF-a) i Share (deljenje PDF-a) sada rade
  bez `SecurityException`; biranje datuma više ne ruši aplikaciju; poruka o grešci ("nema
  merenja za izabrani period") se sada ispravno prikazuje.

### Šta je ostalo
- **Faza 2 migracionog plana je ZAVRŠENA** — svih 7 feature-a (glucose, meals, fitness, reminders,
  friends, settings, reports) su u `:shared`.
- Sledeće po CLAUDE.md planu (poglavlje 9): **faza 3** — migracija Android UI-ja na Compose
  Multiplatform. Ovo je veći, drugačiji tip posla od dosadašnjeg (UI slojevi, ne domain/data),
  vredi razgovarati sa korisnikom o pristupu/redosledu pre početka, ne pretpostavljati.
  Posle faze 3: faza 4 (iOS — čeka Mac), faza 5 (nove funkcionalnosti: FZ-9 insulin doze, FZ-10
  real-time sync, FZ-12 statistika, FZ-14 LibreLinkUp), faza 6 (Wear OS), faza 7 (Web, opciono),
  faza 8 (testiranje/pisanje rada).
- iOS strana svih 7 feature-a i dalje nije build-verifikovana (nema Mac pristupa).

---

## 2026-08-04 — FZ-14: LibreLinkUp integracija (van redosleda faza, urađeno pre faze 3)

### Šta smo dodali
- Ceo LibreLinkUp lanac u `shared/commonMain`: domen modeli (`LibreLinkAuth`, `LibreLinkSession`,
  `LibreLinkConnection`), Ktor klijent (`KtorLibreLinkApiClient`) koji radi login (uz redirect na
  regionalni host), `fetchConnections`, `fetchGlucoseReadings` (spaja `graphData` istoriju sa
  poslednjim `connection.glucoseMeasurement`, bez duplikata unutar samog odgovora), ručni parser
  za LibreLinkUp-ov ne-ISO-8601 timestamp format (`LibreLinkTimestampParser`, pošto `java.time`
  nije dostupan u commonMain), `LibreLinkSessionStorage` interfejs, `LibreLinkRepository`
  (connect/disconnect/syncLatestReadings sa dedup preko `lastSyncedTimestamp` cursor-a), Koin
  modul.
- `shared/androidMain`: `AndroidLibreLinkSessionStorage` (SharedPreferences + Keystore-enkriptovan
  token), Ktor Android engine, SHA256 preko `java.security.MessageDigest`.
- `shared/iosMain`: `IosLibreLinkSessionStorage` (NSUserDefaults, neenkriptovano za sada — isti
  privremeni pristup kao `SettingsPreferences`), Ktor Darwin engine, SHA256 preko CryptoKit-a.
- App strana: `LibreLinkSection` UI ugrađen u Settings ekran (email/password unos, connect/
  disconnect, prikaz poslednjeg sync-a i greške), `LibreLinkViewModel`, `LibreLinkSyncWorker` +
  `LibreLinkSyncScheduler` (WorkManager periodic sync na 15 min — OS-ov minimum).
- Testovi: 20 u `shared/commonTest` (mapper, timestamp parser, Ktor klijent preko
  `MockEngine`, repository preko fake-ova), 5 u `app/test` (ViewModel preko MockK).

### Odluke
- LibreLinkUp REST API nije Firebase-specifičan, pa ide kroz Ktor + kotlinx.serialization u
  commonMain (isti pristup kao meals-ov USDA/Spoonacular poziv), radi i na iOS-u preko Darwin
  engine-a.
- Session token čuva se enkriptovano na Androidu (Keystore cipher) — LibreLinkUp auth token je
  osetljiv podatak, ne čuva se u plain SharedPreferences.

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest` (20/20), `:app:assembleDebug`,
  `:app:testDebugUnitTest` (5/5 za LibreLink) — svi BUILD SUCCESSFUL.
- Ručni smoke test na telefonu (Samsung A528B → S942B): connect sa pravim LibreLinkUp nalogom
  uspešan, očitavanja se povlače i prikazuju u glucose grafiku/listi.
- **Uporedio grafik u Insulink-u sa zvaničnom LibreLinkUp aplikacijom preko screenshot-ova
  (povučenih direktno sa uređaja preko `adb exec-out screencap`)** — tokom ovog poređenja
  otkrivena i ispravljena 2 stvarna bug-a (nezavisna od samog vizuelnog poklapanja grafika):
  1. **Duplirana očitavanja pri prvom connect-u**: `LibreLinkViewModel.connect()` je pokretao
     dva nezavisna sync-a odjednom — `syncScheduler.enqueue()` (WorkManager `PeriodicWorkRequest`
     po defaultu odmah izvrši prvi run) i direktan poziv `syncLatestReadings()` iz ViewModel-a.
     Oba su čitala isti (još null) `lastSyncedTimestamp` pre nego što ijedno stigne da ga upiše,
     pa su oba ubacila isti set očitavanja u Room (nema unique constraint-a na insert-u).
     Popravljeno dodavanjem `.setInitialDelay(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)` u
     `LibreLinkSyncScheduler.enqueue()` — periodika sad kreće tek posle prvog intervala, a
     direktan poziv iz `connect()` pokriva inicijalni sync.
  2. **Duplirana očitavanja pri svakom disconnect→reconnect ciklusu**: `clearSession()` (i
     Android i iOS actual) brisao je i `lastSyncedTimestamp` zajedno sa auth podacima.
     LibreLinkUp-ov `/graph` endpoint vraća istoriju (ne samo tačke posle poslednjeg sync-a), pa
     je reconnect posle svakog disconnect-a ponovo tretirao celu istoriju kao "novu" i duplirao
     već postojeća očitavanja u bazi. Popravljeno tako da `clearSession()` više ne briše
     `lastSyncedTimestamp` (samo auth polja i `lastSyncError`) — cursor sad preživljava
     disconnect/reconnect ka istom nalogu.
  - Oba bug-a potvrđena kao ispravljena kroz ponovljeni ručni test (disconnect → obriši
    duplikate → reconnect → nema novih duplikata).
- Grafik u Insulink-u ("Last 24 hours" filter) je u jednom trenutku prestajao pre poslednjeg
  sinhronizovanog očitavanja (do 17:45 dok je "latest reading" pokazivao 19:00) — **nije
  dijagnostikovano do kraja**, moguće nevezano za LibreLink (pre-postojeća logika filtriranja/
  grupisanja grafika), vredi proveriti ponovo ako se i dalje pojavljuje sad kad duplikati
  više ne remete podatke.

### Šta je ostalo
- Vizuelno poklapanje oblika krive sa zvaničnom LibreLinkUp aplikacijom nije do kraja
  potvrđeno broj-po-broj (samo vizuelno upoređeno preko screenshot-ova) — dovoljno za ovu
  sesiju, ali vredi dodati test koji poredi konkretne vrednosti ako se posumnja na dalje
  neslaganje.
- "Last 24 hours" grafik koji staje pre najnovijeg očitavanja (gore) — treba dodatno istražiti.
- LibreLinkUp iOS strana i dalje nije build-verifikovana (nema Mac pristupa, isto kao ostatak
  projekta).

---

## 2026-08-05 — FZ-14: pronađen i ispravljen timezone bug + chart poboljšanja

### Šta smo dodali/ispravili
- **Treći stvaran bug otkriven istim pristupom** (poređenje screenshot-ova Insulink vs.
  zvanična LibreLinkUp/FreeStyle LibreLink aplikacija, ovog puta uz sirov API odgovor ulogovan
  privremenim `println`-om u `KtorLibreLinkApiClient.fetchGlucoseReadings`): LibreLinkUp-ov
  `FactoryTimestamp` je UVEK UTC (API paralelno vraća i `Timestamp` polje — isto zidno vreme
  pomereno za lokalni UTC offset naloga). `LibreLinkTimestampParser.parseLibreLinkTimestamp`
  je te cifre tretirao kao da su već lokalno vreme uređaja i dodatno ih konvertovao kroz
  `TimeZone.currentSystemDefault()` — duplo primenjen offset, pa su sva sinhronizovana
  očitavanja bila prikazana ~2h ranije nego stvarno (za Beograd/CEST), ponekad i na pogrešan
  kalendarski dan blizu ponoći. Popravljeno korišćenjem `TimeZone.UTC` u parseru; test
  (`LibreLinkTimestampParserTest`) ažuriran da očekuje UTC epoch umesto lokalnog.
- Nakon popravke, potvrđeno tačno poklapanje vrednosti I vremena između Insulink-a i zvanične
  LibreLinkUp/FreeStyle LibreLink aplikacije na fizičkom uređaju (npr. "11.6 mmol/L @ 23:30" i
  "14.0 mmol/L @ 00:15" identično u obe aplikacije, ista realna očitavanja).
- Glucose grafik (`DynamicLineChart.kt`): dodato Vico `HorizontalBox` decoration koje senči
  ciljani opseg (70–126 mg/dL, isti pragovi kao postojeći "Below/In/Above target range"
  indikator) providnom zelenom bojom (`InsulinkTheme.colors.glucoseNormal`, alpha 0.15) iza
  linije. Visina grafika povećana sa Vico default 200dp na 340dp (+70%), pa naknadno smanjena
  za 15% na 289dp; oznake osa ("Glucose (mmol/L)", "Time"/"Date & Time") uklonjene po zahtevu
  korisnika — ostale samo brojčane vrednosti na osama.

### Odluke
- **Kontaminirani (pre-fix) podaci ne mogu se očistiti samim reinstall-om aplikacije**: glucose
  ekran pri svakom otvaranju povlači SVA očitavanja sa Firestore-a i potpuno zameni lokalnu
  Room bazu (`GlucoseWrapper` → `fetchAllGlucoseReadingsForUserAndUpdateDatabase`). Pošto
  `GlucoseReadingRepository.insert()`/`.delete()` uvek pišu i lokalno i na Firestore, stari
  bag-ovani LibreLinkUp unosi su preživljavali reinstalacije. Ručno swipe-brisanje kroz UI je
  tehnički ispravno (briše i lokalno i remote), ali je nepouzdano za veće količine redova —
  ručno swipe-ovanje je propustilo deo unosa (58 ukupno, 54 LibreLinkUp) pri prvom pokušaju.
  Za pouzdano masovno čišćenje napravljen jednokratan `@RunWith(AndroidJUnit4::class)`
  instrumented test koji poziva POSTOJEĆI `repository.delete()` za svaki LibreLinkUp red (isti
  kod koji app već koristi, samo automatizovano) — obrisan iz repo-a odmah nakon upotrebe,
  nije trajni deo koda.
- **Debug logovanje (sirov JSON odgovor) korišćeno privremeno pa uklonjeno**: dodato u
  `KtorLibreLinkApiClient.fetchGlucoseReadings` da bi se direktno videle vrednosti koje
  LibreLinkUp API vraća (umesto nagađanja sa piksela na screenshot-u), uklonjeno čim je uzrok
  bug-a potvrđen — nije ostalo u kodu.

### Verifikacija
- `:shared:testAndroidHostTest`, `:app:testDebugUnitTest` — BUILD SUCCESSFUL posle svake
  izmene (timestamp fix, chart izmene).
- Vizuelno poređenje screenshot-ova (povučenih preko `adb exec-out screencap`) sa zvaničnom
  LibreLinkUp/FreeStyle LibreLink aplikacijom na DVA fizička uređaja (Samsung A528B i S942B) —
  potvrđeno tačno poklapanje vrednosti i vremena nakon timezone fix-a i čišćenja kontaminiranih
  podataka.
- Chart izmene (senčenje opsega, visina, uklonjene oznake osa) ručno potvrđene screenshot-ovima
  posle svake pojedinačne izmene.
- Dva odvojena commit-a: timestamp/timezone fix i chart UI izmene (na zahtev korisnika, umesto
  jednog zajedničkog).

### Šta je ostalo
- **LibreLinkUp (FZ-14) integracija je sada u potpunosti verifikovana i komitovana** — build,
  testovi (25+), i vizuelna provera protiv zvanične aplikacije, uključujući 3 pronađena i
  ispravljena bug-a (dupli sync, gubljenje sync cursor-a, timezone).
- "Last 24 hours" grafik koji je ranije stao pre najnovijeg očitavanja — nije ponovo
  proveravano posle čišćenja podataka, vredi pogledati ako se opet pojavi.
- LibreLinkUp iOS strana i dalje nije build-verifikovana (nema Mac pristupa).

---

## 2026-08-05 — FZ-9: insulin doze + veza sa obrocima na glucose očitavanjima

### Šta smo dodali
- **Nov `insulin` feature (tipovi insulina koje korisnik koristi)**: potpuna vertikalna
  kriška po uzoru na `reminders` (domain `InsulinType(id, userId, name)` — bez kategorije,
  po odluci korisnika tokom planiranja; Room entitet/DAO/baza `insulin_types.db`; Firestore
  remote data source po istom "array polje na user doc-u" obrascu; repository; Koin modul;
  Hilt bridge u `SharedModule.kt`). App strana: `InsulinViewModel`/`InsulinScreen`/
  `InsulinWrapper` (lista + FAB + dijalog za dodavanje, samo naziv, swipe-to-delete) —
  strukturno skraćena verzija `RemindersScreen.kt` bez date/time picker-a i kategorije.
- **Nov sidebar tab "Insulini"** (EN: "Insulin Types") — dodat u `NavigationRoutes.kt`
  (`Screen.InsulinTypes`, sidebar-only isto kao Reminders/Friends/Report/Settings),
  `SideDrawer.kt` (nova stavka, reciklirana `ic_syringe` ikonica već korišćena za
  `ReminderType.INSULIN_REMINDER`), `AppNavigation.kt`.
- **`GlucoseReading` proširen** sa tri nullable polja: `insulinTypeId`, `insulinUnits`,
  `linkedMealId` — dodato kroz pravu Room `Migration` (v1→v2, tri `ALTER TABLE ADD COLUMN`)
  umesto `fallbackToDestructiveMigration()`, da se ne izgube postojeći podaci na uređaju
  (potvrđeno ručnim smoke testom da stari LibreLinkUp podaci prežive migraciju).
- **Novi `update()` put** kroz DAO (`@Update`) → repository → Firestore remote (kopija
  `FirebaseMealRemoteDataSource.updateMeal` obrasca — `pushReading`-ov `arrayUnion` NIJE
  upsert, pa je pravi update metod bio neophodan) — omogućava izmenu postojećeg očitavanja.
- **`AddGlucoseReadingDialog` sada radi i za dodavanje i za izmenu** (isti dijalog, ne
  duplirana verzija) — `GlucoseViewModel.editingReadingId` (null = add mode) određuje da li
  se na Save poziva `insert` ili `update`; dijalog dobija dodatne sekcije: dropdown za tip
  insulina (iz `InsulinTypeRepository`, sa "None" opcijom), tekstualno polje za jedinice,
  dropdown za povezivanje sa obrokom istog dana (iz `MealRepository.getMealsByDateForUser`,
  takođe sa "None" opcijom).
- **`GlucoseReadingItem` sada je dodirljiv** (pored postojećeg swipe-to-delete) — dodir
  otvara isti dijalog u edit modu, pred-popunjen. Kad su insulin/obrok postavljeni, prikazuje
  se mala bedž linija ispod komentara (npr. "5 IU · novorapid · Doručak").

### Odluke
- **Insulin doza NIJE posebna tabela/entitet** — direktno su dodata tri nullable polja na
  `GlucoseReading`, pošto korisnik uvek unosi dozu U KONTEKSTU jednog očitavanja (ne kao
  samostalan log), a kardinalnost je prirodno 0-ili-1 po očitavanju. Izbegnuto izmišljanje
  nove cross-table FK mašinerije — ionako ne postoji nijedan `@ForeignKey` primer nigde u
  kodu; `linkedMealId`/`insulinTypeId` prate isti obrazac kao `MealIngredientEntity.mealId`
  (obična `Long` referenca, ručno održavana, bez deklarativnog FK-a).
- **Bedž lookup mapa za listu koristi `allMealsForUser` (SVI obroci korisnika), ne
  `sameDayMealsForNewReading`** — ovo drugo je namenski ograničeno na dan koji je trenutno
  izabran u add/edit dijalogu (za dropdown), pa bi korišćenje istog izvora za listu tačno
  rešavalo samo imena obroka sa tog jednog dana. Uhvaćeno i ispravljeno tokom implementacije,
  pre ikakvog testiranja — vredi zapamtiti razliku između "za dropdown trenutnog dijaloga" i
  "za prikaz cele liste" flow-ova ubuduće.
- **Kategorija insulina (rapid/long-acting itd.) namerno izostavljena** — korisnik je tokom
  planiranja eksplicitno tražio samo naziv, bez kategorizacije, radi jednostavnosti.

### Verifikacija
- `:shared:testAndroidHostTest` (uključujući 9 novih testova za `insulin` modul: 4 mapper +
  5 repository), `:app:testDebugUnitTest` (uključujući 6 novih `InsulinViewModelTest` + 4
  nova `GlucoseViewModelTest` za edit-mode/insulin/meal polja — ukupno 16 u tom fajlu),
  `:app:assembleDebug` — svi BUILD SUCCESSFUL posle svakog od 4 commit-a (model+migracija,
  update() put, insulin feature+nav, glucose UI izmene).
- Ručni smoke test na telefonu (Samsung A528B), **korisnik je sam ručno testirao** (moji
  pokušaji preko `adb shell input tap` sa nagađanim koordinatama su bili nepouzdani — jedan
  tap na pogrešnu poziciju, i jedan slučaj gde je `keyevent 4` verovatno zatvorio ceo dijalog
  umesto tastature): dodavanje tipova insulina (dodato/obrisano "novorapid"/"tresiba", oba
  persistuju), dodavanje glucose očitavanja sa insulinom i povezanim obrokom preko FAB-a, i
  retroaktivna izmena postojećeg očitavanja dodirom iz liste — sve potvrđeno da radi ispravno
  od strane korisnika.
- **Nauka za sledeći put**: `adb shell uiautomator dump` + `grep bounds=` je mnogo pouzdaniji
  način da se nađu tačne koordinate UI elemenata nego procena piksela sa screenshot-a — moja
  prva dva pokušaja tapovanja FAB-a su promašila jer sam pogrešno procenio poziciju iz slike;
  tek uiautomator dump je dao tačne `bounds`.

### Šta je ostalo
- **FZ-9 (insulin doze + veza sa obrocima) je u potpunosti implementiran i verifikovan**,
  komitovan u 4 odvojena commit-a na `jovan/glucose-shared-migration` (nije pushovan).
- LibreLinkUp/insulin iOS strane i dalje nisu build-verifikovane (nema Mac pristupa).
- `specifikacija.md` i dalje nije pronađen nigde u repo-u — ako se pojavi, vredi uporediti
  FZ-9 implementaciju sa zvaničnim zahtevima (posebno da li je kategorija insulina ipak
  bila deo specifikacije).

---

## 2026-08-05 — Bug: LibreLinkUp sesija curela između Insulink naloga na istom uređaju

### Šta smo otkrili i popravili
- Korisnik je prijavio: kad se na istom telefonu prijavi sa DVA različita Google naloga u
  Insulink, oba vide/koriste ISTI LibreLinkUp login i povlače ista očitavanja — očigledno
  pogrešno, LibreLinkUp konekcija treba da bude vezana za konkretan Insulink nalog.
- **Uzrok**: `LibreLinkSessionStorage` (token, email, patientId, sync cursor, last error) je
  čuvan u JEDNOM globalnom SharedPreferences/NSUserDefaults slotu, potpuno bez svesti o tome
  koji je Firebase/Insulink korisnik trenutno prijavljen — `connect()`/`disconnect()`/
  `getSession()` u `LibreLinkRepository` nisu ni primali `userId` kao parametar.
- **Popravka**: svaki metod u `LibreLinkSessionStorage` sad prima `userId: String`, a Android/
  iOS implementacije prefiksuju svaki ključ sa `"${userId}_"` (na istoj SharedPreferences
  datoteci/NSUserDefaults instanci — nije bilo potrebno praviti posebne fajlove po korisniku).
  `LibreLinkRepository` i `LibreLinkViewModel` provlače `userId` kroz ceo lanac.
  `LibreLinkViewModel.session/lastSyncedTimestamp/lastSyncError` su sad plain `MutableStateFlow`
  koji se pune preko `refreshStatus()` (async, čita trenutnog korisnika preko
  `authRepository.getCurrentUserFlow().first()`); dodat je i `currentUserId: StateFlow<String?>`
  na ViewModel-u da `SettingsWrapper` može da veže `LaunchedEffect(currentUserId)` umesto
  `LaunchedEffect(Unit)` — refreshStatus() se sad ponovo poziva SVAKI PUT kad se promeni
  prijavljeni korisnik, ne samo pri prvom prikazu ekrana.

### Odluke
- Razmatrana i odbačena složenija varijanta: da `session`/`lastSyncedTimestamp`/`lastSyncError`
  budu potpuno reaktivni `StateFlow`-ovi izvedeni preko `combine(authRepository.getCurrentUserFlow(), refreshTrigger)` —
  tehnički rešava problem podjednako dobro, ali komplikuje testove (Room/Flow-bazirani
  `WhileSubscribed` zahteva aktivnog pretplatnika da bi se `.value` uopšte ažurirao, što bi
  polomilo postojeće testove koji čitaju `.value` odmah nakon `advanceUntilIdle()` bez
  eksplicitnog `.test {}` pretplaćivanja). Jednostavniji pristup (obično `MutableStateFlow` +
  eksplicitan `refreshStatus()` poziv iz `LaunchedEffect(currentUserId)`) je dovoljan i
  ostaje dosledan ostatku ViewModel-a u ovom fajlu.
- **Jednokratna posledica popravke**: postojeće konekcije sačuvane pod starim (neskopiranim)
  ključevima se neće naći pod novim per-user ključevima — nalog koji je bio povezan pre ove
  izmene mora ponovo da se poveže jednom. Potvrđeno od strane korisnika kao očekivano, ne
  gubitak podataka.

### Verifikacija
- `:shared:testAndroidHostTest` (10 testova za `LibreLinkRepositoryTest`, uključujući 2 nova
  koja eksplicitno proveravaju da connect/disconnect za jednog korisnika ne dira sesiju
  drugog), `:app:testDebugUnitTest` (6 testova za `LibreLinkViewModelTest`, uključujući 1 novi
  za scoping), `:app:assembleDebug` — svi BUILD SUCCESSFUL.
- Ručni test na telefonu (Samsung A528B) — **korisnik je sam testirao** tačan scenario koji je
  prijavio: prijava sa dva Google naloga, provera da drugi nalog ne vidi LibreLinkUp konekciju
  prvog — potvrđeno da radi ispravno.

### Šta je ostalo
- Popravka komitovana na `jovan/glucose-shared-migration`, nije pushovana.
- Isti princip (per-user scoping) vredi imati na umu za bilo koju BUDUĆU funkcionalnost koja
  čuva podatke lokalno mimo Room-a (Room upiti su već ispravno filtrirani po `userId` u WHERE
  klauzuli) — SharedPreferences/NSUserDefaults-bazirano čuvanje je lako prevideti kao "samo
  jedan korisnik po uređaju" podrazumevano, a ovaj bug pokazuje da ta pretpostavka ne važi.

---

## 2026-08-05 — Faza 6: Wear OS aplikacija (v1 — poslednje merenje, tile, brzi unos)

### Šta smo dodali
Novi Gradle modul `:wearApp` (preskočena faza 3 Compose Multiplatform UI i ostatak faze 5
FZ-10/FZ-12 po korisnikovoj odluci — ide se direktno na fazu 6). Obim namerno mali: prikaz
poslednjeg merenja glukoze na satu, tile sa istim podatkom, brzo dodavanje glukoze sa sata.

**Arhitektura**: sat NE radi samostalno sa internetom/Firebase-om — komunicira isključivo sa
uparenim telefonom preko Wearable Data Layer API-ja (`DataClient`/`MessageClient`). Telefon i
dalje radi sav auth/Firestore posao (već je ulogovan); sat samo prikazuje ono što mu telefon
pošalje i šalje "dodaj glukozu" zahteve nazad telefonu. Zato `:wearApp` NE zavisi od `:shared`
ni od Firebase-a/Room-a/Hilt-a/Koin-a — mnogo manji, jednostavniji modul (sopstveni
`applicationId = com.dj.insulink.wear`, `minSdk = 30`).

Telefon strana (`:app`, `core/wear/`):
- `WearSyncManager` — šalje poslednje merenje kao Data Layer `DataItem` (`/insulink/latest_reading`):
  formatirana vrednost (po korisnikovoj jedinici), LOW/NORMAL/HIGH status (isti pragovi kao
  `GlucoseLevelTag.kt`), timestamp. Pozvan iz tri mesta: `GlucoseViewModel.submitNewGlucoseReading()`
  (ručno dodavanje), `LibreLinkSyncWorker` (posle uspešne pozadinske CGM sinhronizacije — bitno
  da sat bude svež i kad app nije otvorena), `GlucoseWearListenerService` (echo potvrda nazad).
- `GlucoseWearListenerService` (`WearableListenerService`) — prima quick-add poruku sa sata
  (`/insulink/quick_add_glucose`, 12 bajtova: 8B timestamp + 4B mg/dL vrednost), upisuje kroz
  `GlucoseReadingRepository`. Nije Hilt-spravljan — čita zavisnosti direktno iz Koin
  `GlobalContext`, isti obrazac kao postojeći `LibreLinkSyncWorker`.

Sat strana (`:wearApp`):
- `data/` — `WearDataLayerContract` (putanje/ključevi, moraju ručno da se poklapaju sa
  telefonskom stranom pošto ne dele Kotlin kod), `WearDataListener` (uživo ažuriranja),
  `LatestReadingStore` (čita keširan `DataItem` sa satnog Data Layer-a — radi i bez telefona u
  blizini), `WearMessageSender` (šalje quick-add poruku).
- `ui/` — `LatestReadingScreen` (velika vrednost obojena po opsegu + "pre X min" + dugme
  "Dodaj"), `QuickAddScreen` (obična +/-5 stepper kontrola umesto Wear Compose `Picker`
  komponente — `Picker` API se menjao kroz verzije, `Text`/`Button`/`Chip` su stabilni godinama).
  `MainActivity` hostuje oba ekrana preko `SwipeDismissableNavHost`.
- `tile/` — `GlucoseTileService` (`androidx.wear.tiles.TileService` + `androidx.wear.protolayout`,
  premošćeno na coroutine preko `kotlinx-coroutines-guava`-inog `CoroutineScope.future{}` umesto
  Horologist-a, koji nikad nije izdao stabilnu verziju) prikazuje poslednju vrednost i otvara
  `MainActivity` na dodir. `GlucoseDataListenerService` traži osvežavanje tile-a čim telefon
  pošalje novu vrednost.

### Odluke
- **Wearable Data Layer API umesto samostalnog auth-a na satu** — korisnikova eksplicitna
  odluka posle poređenja trade-off-ova (manje posla, radi offline preko Bluetooth-a, ali
  zahteva da telefon bude u blizini za dodavanje/svež podatak).
- **Nema Horologist zavisnosti za tile** — Horologist (Google-ova pomoćna biblioteka za Tile
  boilerplate) nikad nije izdao stabilan release (najnovija `0.8.3-alpha`), pa je Tile pisan
  direktno preko `androidx.wear.tiles`/`androidx.wear.protolayout` Builder API-ja + malog
  `kotlinx-coroutines-guava` mosta — manje rizika za diplomski rad.
- **Nema Wear Compose `Picker` za quick-add** — API je nestabilan kroz verzije (menjao se broj/
  tip parametara), pa je izabrana najjednostavnija moguća kontrola (+/-5 dugmad) koja koristi
  samo davno stabilizovan `Chip`/`Button`/`Text` API.
- **Sve ProtoLayout/Tiles/Data-Layer pozive verifikovao direktno iz `.aar` fajlova** (raspakovan
  `classes.jar` iz Gradle keša, `javap -p` na tačnim klasama za povučenu verziju 1.6.2/1.4.2/
  20.0.1) umesto oslanjanja na dokumentaciju/kodlab primere — pretraga weba je više puta vratila
  međusobno nekonzistentne primere koda (stariji Builder-pattern API pomešan sa novijim
  Material3 DSL-om), pa je preciznost API poziva proverena na izvoru pre pisanja finalnog koda.

### Verifikacija
- `:wearApp:assembleDebug` i `:app:assembleDebug` — oba BUILD SUCCESSFUL.
- `:app:testDebugUnitTest` — 116/117 testova prolazi; jedini fail je već postojeća,
  nekomitovana, nepovezana WIP izmena (default timespan `LAST_DAY` umesto `ALL_READINGS`),
  ostavljena netaknuta u radnoj kopiji (nije deo Wear commit-a).
- **Testiranje na fizičkom satu NIJE urađeno** — korisnikov sat (Galaxy Watch, SM-R920) se
  praznio tokom sesije; korak 1 (prazan ekran) je bio instaliran i vizuelno potvrđen na satu
  pre nego što se ispraznio, ali koraci 2-6 (push podataka, quick-add, tile) čekaju sledeću
  sesiju kad se sat napuni.
- Usput otkriven i rešen problem sa punim C: diskom (0.14 GB slobodno) koji je rušio D8/dex
  build korak — obrisan Gradle keš (`C:\Users\jovan\.gradle\caches`, ~12GB, bezbedno/
  reverzibilno) uz korisnikovu potvrdu.

### Šta je ostalo
- **Testiranje na fizičkom satu** čim se napuni — instalirati `:wearApp` APK, proveriti sva 4
  preostala koraka (push sa telefona, quick-add sa sata, tile prikaz/osvežavanje, tap-to-open).
- Tile trenutno otvara samo `MainActivity` na dodir (ne postoji direktna akcija za dodavanje
  glukoze sa samog tile-a bez otvaranja app-a) — moguće poboljšanje kasnije, van trenutnog obima.
- Faza 3 (Compose Multiplatform UI na telefonu) i ostatak faze 5 (FZ-10 real-time sync, FZ-12
  statistika) i dalje nisu započeti — svesno preskočeni za fazu 6.

---

## 2026-08-05 (isto veče, kasnije) — Faza 6: sat se napunio, testiranje na uređaju + KRITIČNA popravka

### Šta smo otkrili i popravili
Sat se napunio, testirano uživo na oba fizička uređaja (telefon + sat) preko adb-a. Push sa
telefona je "uspevao" lokalno (`connectedNodes` neprazan, `putDataItem` vraćao uspeh), ali
DataItem NIKAD nije stizao na sat — ekran je stalno pokazivao "No reading yet".

**Uzrok (arhitekturna greška, ne bug u kodu)**: Wearable Data Layer API (`DataClient`/
`MessageClient`) sinhronizuje podatke IZMEĐU telefona i sata SAMO ako oba app-a imaju **isti
`applicationId`** (i isti signing certificate) — potvrđeno iz zvanične Android dokumentacije.
`:wearApp` je imao `applicationId = com.dj.insulink.wear`, različit od telefonskog
`com.dj.insulink` — Data Layer ih tretira kao potpuno nepovezane aplikacije, bez obzira na to
da li postoji Bluetooth konekcija između uređaja.

Dodatno je usput otkriveno i da **adb konekcija ≠ Bluetooth/Wear OS uparivanje** — prvi test je
pucao i zato što telefon (SM-A528B) korišćen za testiranje nije bio Bluetooth-uparen sa satom
(sat je bio uparen sa DRUGIM telefonom, SM-S942B). `dumpsys bluetooth_manager` na telefonu je
otkrio da sat nije u listi bondovanih uređaja — potvrda da je adb/wireless-debugging konekcija
potpuno odvojena od stvarnog OS-nivo uparivanja potrebnog za Data Layer.

**Popravka**: `wearApp/build.gradle.kts` — `applicationId` promenjen u `com.dj.insulink`
(identično telefonu). `namespace` OSTAJE `com.dj.insulink.wear` (Kotlin paketi, R klasa —
potpuno nezavisno od `applicationId`, nema promena u izvornom kodu). Pošto oba app-a sad imaju
isti `applicationId`, **ne mogu se instalirati preko `gradlew :app:installDebug :wearApp:installDebug`
kad su oba uređaja povezana** — Gradle gura oba APK-a na SVE povezane uređaje, što prepiše
pogrešnu app na pogrešnom uređaju (desilo se dvaput ove sesije). Od sada: `adb -s <serial>
install -r <apk>` ciljano, po uređaju.

Takođe dodato: `WearSyncManager` je pre ovoga bio potpuno nem (nijedan log) — dodato
`Log.d`/`Log.e` sa listom povezanih node-ova i rezultatom push-a. Bez ovoga bi dijagnostika
trajala mnogo duže.

### Nova funkcionalnost (na korisnikov zahtev tokom testiranja)
- **mmol/L na quick-add ekranu**: `WearSyncManager` sad šalje i `glucoseUnit.key` u DataItem-u;
  `QuickAddScreen`-ov +/- stepper radi u desetinkama mmol/L (celobrojno, isti princip kao
  `GlucoseUnit.formatOneDecimal` na telefonu — bez float drift-a) kad je korisnikova jedinica
  mmol/L, i konvertuje nazad u mg/dL samo pri potvrdi (pošto `GlucoseReading.value` uvek čuva
  mg/dL).
- **Push i na brisanje i na Firestore refresh**: `GlucoseViewModel.deleteGlucoseReading()` i
  `fetchAllGlucoseReadingsForUserAndUpdateDatabase()` (poziva se pri svakom otvaranju Glucose
  ekrana — Firestore rehydration) sad takođe pozivaju `pushLatestReadingToWear()`. Zajedno sa
  postojećim add/edit/LibreLinkUp/quick-add hook-ovima, SVAKA promena liste glukoza sad ažurira
  sat.

### Verifikacija
- **Uživo na fizičkim uređajima** (Samsung Galaxy telefon + Galaxy Watch5 Pro, SM-R920) posle
  popravke `applicationId`-a:
  - Push sa telefona → sat prikazuje vrednost (čak i keširanu vrednost od PRE popravke, koja je
    čekala u Data Layer redu dok nije postojao odgovarajući primalac).
  - **Uživo ažuriranje** dok je watch app u foreground-u (bez restart-a) — potvrđeno, boja se
    menja po opsegu (zeleno/narandžasto/crveno).
  - **Quick-add sa sata** — korisnik je ručno testirao, potvrdio da radi kraj-do-kraja.
- `:app:testDebugUnitTest` — 116/117 (isti poznati nepovezan fail).
- `:wearApp:assembleDebug`, `:app:compileDebugKotlin` — čisto.
- **Naknadno (isto veče, pošto se sat ponovo napunio i re-uparen preko Wi-Fi adb-a nakon što je
  ovaj računar bio uklonjen sa liste uparenih uređaja na satu)**: korisnik je sam ručno testirao
  delete-push i fetch-push hook-ove na uređaju — **potvrđeno, sve radi** (brisanje očitavanja i
  otvaranje Glucose ekrana oboje ažuriraju sat kako treba).

### Šta je ostalo
- Tile (korak 6) nije re-testiran posle `applicationId` popravke — trebalo bi da radi isto kao
  i pre (koristi isti `LatestReadingStore`), ali vredi potvrditi na uređaju.
- Opšta lekcija za bilo koji budući Wear OS rad: **`applicationId` telefonske i watch aplikacije
  MORA biti identičan** ako se koristi Wearable Data Layer API za komunikaciju između njih —
  ovo je različito od `namespace`/Kotlin paketa, koji mogu (i treba, radi jasnoće) da se
  razlikuju. Provera Bluetooth uparivanja (`adb shell dumpsys bluetooth_manager`) je koristan
  prvi korak kad Data Layer "tiho ne radi" bez ikakve greške u logu.

---

## 2026-09-02 — Nova funkcionalnost: prepoznavanje obroka sa slike (LogMeal API, van redosleda faza)

### Šta smo dodali
- Istraženo i ručno testirano preko curl-a (pre pisanja koda) dva kandidata za "slikaj obrok →
  dobij nutritivne vrednosti": **Spoonacular Image Analysis** (`food/images/analyze`, već koristimo
  isti API za meals pretragu) i **LogMeal API** (specijalizovan food-recognition servis). Test
  slika: indijski thali (više odvojenih jela na tacni) — realan slučaj korišćenja, ne izolovano
  jedno jelo.
  - Spoonacular: svu sliku svrstao pod JEDNU pogrešnu kategoriju ("chili"), samo 4 nutritivne
    vrednosti, procena preko sličnih recepata. Otkriven i usputan nalaz: API tiho odbija slike
    ispod određene rezolucije (`400` na 165×165, radi na 660×660) bez jasne poruke o uzroku.
  - LogMeal: prepoznao **10 odvojenih regiona hrane** na istoj slici (realno se poklapa sa curry/
    pirinač/sočivo/povrće sadržajem tacne), 35+ nutrijenata + Nutri-Score. Ubedljivo bolji za
    multi-dish slike, na cenu drugog vendora i dvostepenog poziva (segmentacija pa nutritivni
    podaci).
  - **Odluka: LogMeal**, na osnovu ovog uživo poređenja, ne apstraktnog poređenja specifikacija.
- LogMeal auth gotcha otkriven uživo: prvi ključ koji je korisnik dobio je vratio `401` — pripadao
  je account/company-level tipu naloga, ne **APIUser** tipu koji jedini sme da zove prepoznavanje.
  Trebalo je naći/kopirati specifično APIUser token sa LogMeal dashboard-a "Users" stranice.
  Drugi gotcha: LogMeal validira format slike po **ekstenziji fajla** u multipart upload-u, ne po
  stvarnom sadržaju — `.jfif` je odbijen (`400`) sve dok fajl nije preimenovan u `.jpg` sa istim
  bajtovima.
- Cena/limiti provereni uživo (WebFetch/WebSearch) pre integracije: **konkretne cene LogMeal-a
  nisu javno objavljene nigde** (stranica traži email kompanije da bi ih prikazala) — nije
  nagađano, korisniku prosleđeno transparentno. Poznato javno: trial 30 dana ili 200 poziva (šta
  pre istekne), do 5 APIUser naloga, standardni plan 20 prepoznavanja/dan po korisniku. Odlučeno
  da se ide u implementaciju sad, u okviru trial limita (dovoljno za diplomski/demo, ne za
  produkciju sa realnim korisnicima).
- **`:shared/commonMain`** (radi na Android i iOS, isti Ktor obrazac kao USDA/Spoonacular/
  LibreLinkUp): `FoodImageAnalysis` domen model (+ `FoodImageAnalysisException`),
  `FoodImageAnalysisRemoteDataSource` interfejs, `LogMealFoodImageAnalysisRemoteDataSource` —
  dvostepeni Ktor poziv (`POST /v2/image/segmentation/complete` multipart → uzima `imageId` →
  `POST /v2/nutrition/recipe/nutritionalInfo` sa tim `imageId`-jem), `LogMealApiModels.kt`
  (serializable response modeli, `totalNutrients` kao `Map<String, LogMealNutrientValue>` da ne
  mora svaki mogući nutrijent posebno da se deklariše). `MealRepository.analyzeFoodImage(...)`,
  `MealsModule` dobija novi `logMealApiKey` parametar.
- **`app` modul**: `BuildConfig.LOGMEAL_API_KEY` (isti obrazac kao USDA/Spoonacular, čita se iz
  `local.properties`, nije u git-u). Kamera dugme u `AddMealScreen` (pored polja za pretragu
  sastojaka) → `AddMealWrapper` pokreće `ActivityResultContracts.TakePicture()` preko VEĆ
  postojećeg `FileProvider`-a (isti authority kao za PDF deljenje u reports feature-u, ništa novo
  u manifestu) — snima punu rezoluciju u cache fajl, izbegava problem sa premalom slikom otkriven
  tokom Spoonacular testiranja. Rezultat analize prikazan u dijalogu (prepoznate namirnice +
  procenjene kalorije/UH/proteini + disclaimer da su procenjene vrednosti) sa "Dodaj u obrok" /
  "Odbaci".
- Testovi: 6 novih u `shared/commonTest`
  (`LogMealFoodImageAnalysisRemoteDataSourceTest`, preko `MockEngine`, isti obrazac kao
  `KtorFoodApiRemoteDataSourceTest`) + `FakeFoodImageAnalysisRemoteDataSource` +
  regresioni test u `MealRepositoryTest` za novu `analyzeFoodImage` delegaciju.

### Odluke
- **Cela fotografisana tacna kao JEDAN `Ingredient`, ne novi domen koncept**: LogMeal vraća
  nutritivne vrednosti za CELU sliku (ne po prepoznatom regionu), pa `estimatedIngredient.
  caloriesPer100g` polje zapravo nosi ukupnu procenu za CEO fotografisan obrok. UI ga dodaje sa
  podrazumevanom količinom 100g (isti trik kao postojeći "custom ingredient" flow — vrednosti
  prolaze kroz nepromenjene kad je količina 100g), a korisnik može da smanji/poveća količinu ako
  je pojeo samo deo tanjira. Nema potrebe za novom tabelom/entitetom — ponovna upotreba već
  migriranog `Ingredient` modela iz meals feature-a.
- **Nema `expect/actual` fasade za LogMeal**: za razliku od Firestore-a, LogMeal REST API nije
  platformski specifičan — ceo `LogMealFoodImageAnalysisRemoteDataSource` živi u `commonMain` i
  radiće i na iOS-u preko Darwin engine-a čim se iOS strana testira (isti presedan kao USDA/
  Spoonacular/LibreLinkUp Ktor pozivi).
- **Kamera preko `TakePicture()`, ne `GetContent()`/galerija**: bira se direktno snimanje pune
  rezolucije u FileProvider-om upravljan cache fajl, ne downscale-ovan bitmap preview niti
  postojeća slika iz galerije — direktna posledica ranije otkrivenog nalaza da LogMeal (i
  Spoonacular) tiho odbijaju slike ispod određene rezolucije.

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest` (uključujući 6 novih LogMeal
  testova, 0 fail), `:app:compileDebugKotlin`, `:app:assembleDebug`, `:app:testDebugUnitTest` —
  svi BUILD SUCCESSFUL.
- Ručno testirano preko curl-a PRE pisanja koda (segmentacija + nutritionalInfo poziv na realnoj
  test slici, uključujući oba auth/ekstenzija gotcha-a opisana gore) — kod je pisan sa već
  potvrđenim tačnim oblikom zahteva/odgovora, ne nagađanjem iz dokumentacije.

### Ručni test na uređaju — 2 stvarna buga otkrivena i ispravljena istog dana

Posle prvog install-a na fizičkom telefonu (Samsung, isti uređaj kao ranije sesije), ispravljena
su dva odvojena problema, oba pronađena ANALIZOM LOGCAT-a uživo (ne nagađanjem):

1. **Slika prevelika (prvi test)**: puna rezolucija kamere (nekoliko desetina MB) je premašivala
   LogMeal-ov upload limit. Popravljeno u `AddMealWrapper.kt`: fotografija se pre slanja
   dekodira preko `BitmapFactory` (sa `inSampleSize` da se izbegne OOM na punoj rezoluciji),
   skalira na max 1280px duže stranice, i JPEG-kompresuje sa iterativnim spuštanjem kvaliteta
   (90 → 30) dok ne stane pod 3MB. Rezultat se i dalje drži iznad ranije potvrđenog minimuma za
   LogMeal prepoznavanje (~660px).
2. **"Ništa se ne dešava" pri kliku OK (drugi test)**: dodato eksplicitno `Log.d`/`Log.e`
   logovanje na svaki korak toka (kamera rezultat → kompresija → LogMeal poziv), isti princip kao
   ranije za `WearSyncManager` — bez toga dijagnoza ne bi bila moguća. Log je pokazao
   `photoSaved=true` (kamera je prijavila uspeh) ali `context.contentResolver.openInputStream(uri)`
   je odmah zatim vratio `null` na TOJ ISTOJ `FileProvider` `content://` putanji. Provera preko
   `adb shell run-as com.dj.insulink ls -la cache/` je potvrdila da fajl STVARNO postoji na disku
   sa punim sadržajem (2.3MB) — dakle problem nije bio u kameri/pisanju, nego u nepouzdanom
   `ContentResolver` čitanju NAZAD kroz FileProvider u istom procesu, odmah posle
   `ActivityResult` callback-a (specifično na ovom Samsung uređaju/kamera app-u). **Popravka**:
   `FileProvider` `content://` URI se i dalje koristi SAMO da kamera aplikacija ima gde da piše
   (to joj je i dalje potrebno), ali čitanje NAZAD u našoj aplikaciji sad ide direktno preko
   `File`/`BitmapFactory.decodeFile(file.absolutePath, ...)` po apsolutnoj putanji koju smo mi
   sami generisali — potpuno zaobilazi `ContentResolver` za read stranu. Ovo je opštiji princip
   vredan pamćenja: **kad app piše I čita sopstveni fajl (ne deli ga sa drugom aplikacijom radi
   čitanja), FileProvider URI treba samo za pisanje od strane TREĆE strane; sopstveno čitanje
   nazad je pouzdanije direktno preko `File` puta, ne kroz `ContentResolver`**.
   - Usput ispravljeno i: `pendingPhotoUri`/`pendingPhotoPath` state prebačen sa `remember` na
     `rememberSaveable` (preživljava recreation/process death dok je kamera aplikacija u
     foreground-u — nije bio uzrok ovog konkretnog buga jer proces nije umro, ali je legitiman
     propust koji bi isti simptom izazvao u drugom scenariju).
   - `TakePicture()` `photoSaved=false` slučaj (korisnik otkaže/kamera ne uspe) sad eksplicitno
     prikazuje grešku korisniku (`reportMealPhotoReadError()`) umesto da tiho ne uradi ništa.

### Nova funkcionalnost (na korisnikov zahtev posle uspešnog testa)
- **Ispravka pogrešno prepoznatih namirnica pre dodavanja**: korisnik je primetio da LogMeal
  ponekad pogrešno prepozna namirnicu (npr. krompir → jaje). `MealPhotoAnalysisDialog` sad
  prikazuje svaku prepoznatu namirnicu kao editabilno `OutlinedTextField` (umesto statičnog
  spojenog teksta) sa X dugmetom za potpuno uklanjanje pogrešne stavke; potvrda dugme je
  onemogućeno ako korisnik obriše sve stavke. `MealsViewModel.acceptMealPhotoAnalysis(
  editedFoodNames: List<String>)` sastavlja finalni naziv sastojka od (mogućih) ispravki, sa
  fallback-om na originalni LogMeal naziv ako korisnik ostavi sve prazno. **Napomena**: ovo menja
  SAMO naziv sastojka — nutritivne vrednosti ostaju LogMeal-ova procena za CELU sliku (nema
  re-poziva ka nekom nutrition API-ju po ispravljenom nazivu), pošto LogMeal ionako ne vraća
  nutritivne podatke po pojedinačnoj stavci, samo agregatno za celu sliku.

### Verifikacija (kompletna)
- `:shared:testAndroidHostTest`, `:app:compileDebugKotlin`, `:app:assembleDebug`,
  `:app:testDebugUnitTest` — svi BUILD SUCCESSFUL posle svake izmene (uključujući finalnu, posle
  editable-ingredients dodatka).
- **Potvrđeno uživo na fizičkom telefonu, kraj-do-kraja**: dugme kamere → slikanje obroka →
  kompresija → LogMeal prepoznavanje → dijalog sa procenom i editabilnim namirnicama → ispravka
  pogrešno prepoznate stavke → dodavanje u obrok → čuvanje. Korisnik potvrdio da radi.

### Šta je ostalo
- iOS strana nije testirana (isto kao ostatak projekta — nema Mac pristupa).
- Ako se pređe sa trial-a na plaćeni LogMeal plan, korisnik treba sam da unese company email na
  `logmeal.com/api/pricing` da vidi stvarnu cenu — nije javno indeksirana, ne može se pribaviti
  automatizovano. Trial ima 200 ukupno poziva/30 dana — pratiti potrošnju ako se testira dalje.
- Nema testova za novi `acceptMealPhotoAnalysis(editedFoodNames)` UI-ovable-editing put niti za
  `downscaleAndCompressPhoto`/FileProvider fix (Android-specifičan kod u `app` modulu, van dosega
  postojećih `shared/commonTest` alata) — vredi razmisliti o instrumented/Robolectric testu ako se
  ovaj obrazac (kamera → FileProvider → direktan File read) ponovi za neki budući feature.
- Isto što i pre za faze 3–8 (Compose Multiplatform UI, iOS, preostale FZ nove funkcionalnosti,
  Web, testiranje/pisanje rada) — ovo je bila funkcionalnost dodata van redosleda, kao LibreLinkUp
  ranije.

---

## 2026-09-02 — Popravka: podsetnici sad rade pouzdano u pozadini (van redosleda faza)

### Kontekst
Korisnik je odustao od FZ-10 (real-time sync — nebitno za sada) i umesto toga prijavio da
`reminders` feature "šalje obaveštenja samo ako si u aplikaciji". Istraga koda (bez nagađanja)
otkrila je DVA odvojena, nezavisna problema.

### Šta smo dodali/ispravili
1. **Podsetnik se gasio posle PRVOG okidanja, zauvek** (pravi bug, nezavisan od optimizacije
   baterije): `ReminderScheduler.scheduleDaily()` zakazuje `AlarmManager.setExactAndAllowWhileIdle`
   — ovo je JEDNOKRATAN alarm. `ReminderReceiver.onReceive()` nikad nije re-zakazivao sledeći dan,
   niti je bilo koji drugi kod to radio (otvaranje Reminders ekrana samo fetch-uje sa Firestore-a,
   ne re-armira postojeće podsetnike). Efektivno: svaki "dnevni" podsetnik je zapravo radio SAMO
   JEDNOM u životu. Popravljeno:
   - `ReminderReceiver` konvertovan u Hilt `@AndroidEntryPoint` (`@Inject lateinit var
     reminderScheduler: ReminderScheduler`) — isti obrazac kao ostali manifest-registrovani
     komponenti u projektu.
   - `ReminderScheduler.scheduleDaily()` sad prosleđuje `hour`/`minute` kao Intent extra-e (uz
     već postojeće `title`/`message`/`notificationId`), da bi `ReminderReceiver` mogao da
     re-zakaže SUTRAŠNJE okidanje odmah posle prikazivanja notifikacije, bez DB round-trip-a.
   - Usput ispravljen i sitan pre-postojeći bug u istoj liniji: `putExtra("notificationId",
     reminderId)` je čuvao `Long` dok je `ReminderReceiver` čitao `getIntExtra` (`Int`) — Android
     tiho vraća default `0` na type mismatch (ne baca izuzetak), pa su SVE notifikacije efektivno
     delile ID `0` (bezopasno za sam alarm mehanizam, ali bi se notifikacije međusobno
     prepisivale u notification tray-u). Popravljeno sa `reminderId.toInt()` pri upisu.
2. **Alarmi ne preživljavaju restart telefona ni update aplikacije** — `AlarmManager` briše sve
   zakazane alarme na reboot; `RECEIVE_BOOT_COMPLETED` dozvola je već postojala u manifestu, ali
   NIJE postojao nijedan receiver koji je sluša. Dodato:
   - `ReminderDao.getAllReminders()` — nova `suspend` (ne `Flow`) metoda, SVI lokalni podsetnici
     preko svih korisnika (bez `userId` filtera — boot receiver nema kontekst "trenutnog
     korisnika" pre nego što se bilo koja Activity/ViewModel pokrene).
   - `ReminderRepository.getAllReminders()` — tanka delegacija ka DAO-u.
   - Nov `BootReminderReceiver` (Hilt `@AndroidEntryPoint`, `goAsync()` + korutina na
     `Dispatchers.IO` pošto je DB čitanje suspend), registrovan u manifestu za
     `ACTION_BOOT_COMPLETED` **i** `ACTION_MY_PACKAGE_REPLACED` (i update aplikacije briše
     alarme na nekim OEM-ovima, jeftino dodati istu zaštitu).
3. **Optimizacija baterije** (korisnikova sopstvena dijagnoza, potvrđena ručno na uređaju,
   PRE nego što sam stigao do koda) — OS je ubijao pozadinski proces/odlagao alarm dok app nije
   bio izuzet od Doze/battery optimizacije. Rešeno RUČNO od strane korisnika (Settings → Battery
   → izuzeti aplikaciju); **nije dodat proaktivni in-app prompt** za ovo (npr.
   `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — ostaje kao mogući budući follow-up
   ako se pokaže da drugi korisnici imaju isti problem bez znanja da ga ručno reše.

### Odluke
- **`ReminderReceiver` ponovo zakazuje SAM SEBE, ne čita iz baze**: brže (nema DB round-trip u
  vremenski osetljivom `BroadcastReceiver.onReceive()` koji ima ~10s limit), i hour/minute su već
  poznati pošiljaocu (istom kodu koji je originalno zakazao alarm) — nema potrebe za dodatnim
  lookup-om. Boot receiver JESTE DB-baziran jer tu nema drugog izvora (proces je taman pokrenut).
- **Boot receiver ne filtrira po korisniku**: umesto da pokušava da odredi "trenutnog korisnika"
  (FirebaseAuth stanje bi verovatno bilo dostupno i pre bilo koje Activity, ali nepotrebno
  komplikuje kod), jednostavnije i robusnije je re-armirati SVE lokalno keširane podsetnike bez
  obzira na `userId` — AlarmManager alarm sam po sebi ne "pripada" ni jednom Firebase nalogu,
  samo lokalnoj bazi.

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest` (uključujući nov
  `getAllReminders_mapsEveryLocalReminderAcrossUsers` test), `:app:compileDebugKotlin`,
  `:app:assembleDebug`, `:app:testDebugUnitTest` — svi BUILD SUCCESSFUL.
- Korisnik potvrdio da posle popravke podsetnici rade pouzdano — testirano sa zatvorenom
  aplikacijom na fizičkom uređaju.

### Šta je ostalo
- Reboot-persistence (`BootReminderReceiver`) nije eksplicitno testiran uživo (zahteva stvarni
  restart telefona) — logika je ista kao već potvrđeno radeći `scheduleDaily` poziv, nizak rizik,
  ali vredi potvrditi ako se ukaže prilika.
- Mogući budući follow-up: proaktivan in-app prompt za battery optimization izuzeće (trenutno
  korisnik mora sam da zna da to uradi kroz Android Settings).
- Isto što i pre za preostale faze/FZ (FZ-10 svesno preskočen po korisnikovoj odluci, FZ-12,
  faza 3 Compose Multiplatform UI, faza 4 iOS, faza 7 Web, faza 8 testiranje/pisanje rada).

---

## 2026-09-03 — Popravka: LibreLinkUp je povlačio tuđe podatke, ne korisnikove

### Kontekst
Korisnik je prijavio da se LibreLinkUp vrednosti u Insulink-u ne poklapaju sa zvaničnom
aplikacijom za isti trenutak (van ranije rešenog timezone buga — vidi 2026-08-05 stavku). Uz
prijavu je nalepio Claude-ov predlog "alternativa" (neslužbeni LibreLinkUp API preko biblioteka
kao `pylibrelinkup`) — ispostavilo se da je to TAČNO ono što aplikacija već koristi, pa je pravi
uzrok tražen dalje u postojećem kodu, ne u zameni API-ja.

### Dijagnostika (dokazima, ne nagađanjem)
- Umesto da se traže LibreLinkUp email/lozinka (osetljiv medicinski nalog, ne treba da prolazi
  kroz chat), dodato je PRIVREMENO logovanje sirovog JSON odgovora direktno u
  `KtorLibreLinkApiClient.fetchGlucoseReadings` (uređaj već ima aktivnu sesiju, ne treba ponovna
  prijava). Novo "Sync Now" dugme dodato u LibreLink sekciju Settings ekrana (nije postojao
  nijedan način da se sync ručno okine pre ovoga — samo pri `connect()` ili čekanjem ~15 min
  periodičnog posla).
- Sirov odgovor je otkrio: `connection.firstName/lastName` = **"Kristina Milicic"**, ne korisnik
  (JWT token prijave potvrđuje da je LibreLinkUp nalog prijavljen kao "Jovan Pavlovic"). Takođe
  `activeSensors: []` i `graphData: []` za tu vezu — očitavanje od 114 mg/dL bilo je zastarelo
  ~3 nedelje (senzor te osobe trenutno nije aktivan).
- **Pravi uzrok**: `LibreLinkRepository.connect()` je radio `connections.firstOrNull()` — slepo
  uzimao PRVU vezu koju LibreLinkUp API vrati, bez ikakve provere da li je to korisnikov
  sopstveni senzor ili neko drugi koga prati preko istog naloga. Korisnik je objasnio da koristi
  DVA odvojena Abbott app-a: **LibreLink** (uparen direktno sa njegovim senzorom, nema API) i
  **LibreLinkUp** (app za pratioce, JEDINI kanal sa API-jem, ali vidi samo osobe koje su
  EKSPLICITNO podelile podatke sa njim). Njegov sopstveni senzor prvobitno NIJE bio podeljen sa
  LibreLinkUp nalogom — jedina veza je bila Kristina (koju prati kao pratilac). Ovo nije bug u
  smislu "loš kod čita loše podatke" nego "kod je čitao JEDINU dostupnu vezu, koja slučajno nije
  bila korisnikova". Korisnik je sam, van aplikacije, podelio svoj LibreLink senzor sa istim
  LibreLinkUp nalogom (standardni Abbott "invite a follower" tok) — posle toga je nalog imao DVE
  veze, i `firstOrNull()` je postao stvaran problem (nedeterministički/nepouzdan izbor između dve
  validne veze).
- Privremeni debug log uklonjen odmah posle dijagnoze (nije ostao u kodu).

### Šta smo dodali
- **`LibreLinkRepository.connect()` razdvojen u dva koraka**:
  - `login(email, password): Result<LibreLinkLoginResult>` — autentifikuje i vraća SVE
    konekcije koje nalog vidi, NE upisuje sesiju (nova `LibreLinkLoginResult(email, auth,
    connections)` domen klasa u `:shared`).
  - `connect(userId, email, auth, connection): Result<LibreLinkSession>` — upisuje sesiju za
    IZABRANU konekciju (auth se prenosi iz prvog koraka, nema drugog login poziva).
- **`LibreLinkViewModel`**: novo `LibreLinkConnectState.ChoosingConnection(connections)` stanje.
  `connect()` sad poziva `login()`; ako ima tačno 1 konekcija, odmah finalizuje (isto ponašanje
  kao pre za uobičajen slučaj); ako ima više, prelazi u `ChoosingConnection` i čeka
  `selectConnection(connection)` poziv iz UI-ja. `pendingLogin` (auth + email) drži se SAMO u
  memoriji ViewModel-a (nikad ne persistuje) između koraka. Novo `cancelSelectingConnection()`.
- **UI**: `LibreLinkSection.kt` dobija `LibreLinkChooseConnectionContent` — lista svih konekcija
  kao klikabilne kartice (ime + radio dugme), sa Cancel dugmetom da se vrati na formu za unos.
- **"Sync Now" dugme** (trajno zadržano, ne samo za debug): korisno samo po sebi — ranije nije
  postojao način da se ručno okine sync bez disconnect/reconnect ciklusa.
- Testovi: `LibreLinkRepositoryTest` prepisan za `login()`/`connect()` dvostepeni API (uključujući
  test da `login()` sam po sebi ne upisuje sesiju), `LibreLinkViewModelTest` prepisan sa novim
  scenarijima (jedna konekcija → auto-finalizuj, više konekcija → picker, `selectConnection`,
  `cancelSelectingConnection`).

### Odluke
- **Auth se ne traži ponovo pri biranju konekcije**: `LibreLinkAuth` (token) dobijen u `login()`
  koraku se prenosi kroz `pendingLogin` i ponovo koristi u `connect()` — nema potrebe za drugim
  network pozivom ka Abbott-u samo zato što korisnik bira IZMEĐU već poznatih konekcija.
- **Auto-finalizacija za tačno 1 konekciju**: većina korisnika (koji ne prate nikog drugog) neće
  ni primetiti promenu — ponašanje ostaje identično kao pre (odmah connect, bez ekstra klika).
  Picker se pojavljuje SAMO kad je stvarno dvosmisleno (2+ konekcije).
- **Nema automatskog "pogodi koja si ti" pokušaja** (npr. poklapanje imena sa Insulink nalogom)
  — eksplicitan korisnički izbor je pouzdaniji i jednostavniji od heuristike koja bi mogla
  pogrešno da pogodi (ime u LibreLinkUp profilu ne mora da se poklapa sa Insulink nalogom).

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:compileDebugKotlin`,
  `:app:testDebugUnitTest`, `:app:assembleDebug` — svi BUILD SUCCESSFUL.
- Korisnik je uživo podelio svoj LibreLink senzor sa LibreLinkUp nalogom (van aplikacije,
  standardni Abbott tok), zatim disconnect→reconnect u Insulink-u — pojavio se picker sa dve
  konekcije, izabrao sebe, **potvrdio da se vrednosti sada tačno poklapaju sa zvaničnom
  aplikacijom**.

### Šta je ostalo
- Ako korisnik ikad prestane da prati Kristinu ili doda još neku vezu, picker će se ponovo
  pojaviti pri sledećem punom re-connect-u (očekivano ponašanje, ne bug).
- Isto što i pre za preostale faze/FZ.

---

## 2026-09-03 — FZ-12: statistika + Glucose ekran prerađen u dnevni prikaz (van redosleda faza)

### Kontekst i tok
Korisnik je tražio FZ-12 (statistika): dnevni prosek, prosek za 7/15/30/90 dana, min/max po
izabranom opsegu, i korelaciju insulin/obrok ↔ šećer (koristeći polja koja `GlucoseReading` već
ima: `insulinTypeId`/`insulinUnits`/`linkedMealId`, popunjavaju se opciono pri unosu očitavanja).
Implementacija je prošla kroz nekoliko iteracija na osnovu korisnikovog fidbeka:
1. Prva verzija: range chip-ovi (Today/7/15/30/90) + statistika + korelacija (Pearson r, scatter
   grafik preko `Canvas`, pošto Vico 2.2.0 nema prirodnu podršku za scatter prikaz).
2. Korisnik: ukloni korelaciju sa insulinom/obrocima, i umesto range chip-ova neka GLAVNI EKRAN
   (ne Statistics — pravi **Glucose** ekran) prikazuje samo TRENUTNI DAN sa swipe navigacijom.
   Implementirano tako (Statistics privremeno postao dnevni prikaz).
3. Korisnik: pogrešno sam protumačio — Statistics treba da OSTANE range-based (chip-ovi), samo
   bez korelacije; dnevni prikaz sa swipe-om ide na pravi Glucose ekran. Vraćeno.

### Šta smo dodali (finalno stanje)
- **`:shared/commonMain`**: `StatisticsCalculator` (čiste funkcije — prosek/min/max/std.
  devijacija/broj očitavanja/Time-in-Range, reuse postojećih pragova 70–126 mg/dL),
  `StatisticsRange` enum (`TODAY`/`LAST_7_DAYS`/`LAST_15_DAYS`/`LAST_30_DAYS`/`LAST_90_DAYS`) sa
  `startMillis()` ekstenzijom. `LocalTimeOfDay.kt` dobija `startOfDayMillis(epochMillis)`,
  `shiftedDayStartMillis(epochMillis, days)` (DST-bezbedna aritmetika preko kotlinx-datetime
  `LocalDate`, ne sirovi millis) i `daysAgoMillis(days)` — koriste ih i Statistics i Glucose.
- **Statistics ekran** (nov, `side_drawer` unos, sopstvena ikonica `ic_statistics.xml`): chip-ovi
  perioda, kartice (prosek/min/max/std.dev./broj očitavanja), Time-in-Range traka. Bez ijedne
  zavisnosti od meals/insulin feature-a — insulin/meal korelacija koda je u potpunosti obrisana
  (uključujući `CorrelationResult`/`CorrelationPoint` domen modele i scatter chart, koji su
  postojali kratko pre nego što je korisnik tražio da se uklone).
- **Glucose ekran** (prerađen): `GlucoseReadingTimespan` enum (All/Last day/3 days/week/month —
  rolling prozor od "sada") potpuno uklonjen, zamenjen jednim kalendarskim danom (podrazumevano
  danas). Navigacija: ◀/▶ dugmad + horizontalni swipe gest, ograničen da ne ide u budućnost.
  `GlucoseViewModel.glucoseReadingsForSelectedDay` sad koristi
  `getGlucoseReadingsByDateRange(userId, dayStart, dayEnd)` umesto filtriranja cele istorije u
  memoriji. `latestGlucoseReading` (statusna kartica na vrhu) namerno OSTAJE nezavisna od dana
  koji se pregleda — uvek prikazuje pravi najnoviji unos (preko `getAllGlucoseReadingsForUser`),
  da korisnik uvek vidi trenutno stanje čak i dok gleda unazad u istoriju. `DynamicLineChart`-ov
  x-osa format je sad uvek `HH:mm` (uvek prikazuje tačno jedan dan, `timespan` parametar uklonjen).
- **Otkriven i izbegnut gest konflikt**: lista očitavanja (`GlucoseReadingItem`) već koristi
  horizontalni `SwipeToDismissBox` za brisanje. Swipe-za-promenu-dana je namerno OGRANIČEN samo na
  gornji deo ekrana (statusna kartica + day header + grafik), NE na celu skrolabilnu kolonu — u
  suprotnom bi dva horizontalna gesta na istom dodiru konkurisala jedno drugom.
- Testovi: `StatisticsCalculatorTest` (14), `LocalTimeOfDayTest` (5, novo — pokriva DST-bezbednu
  aritmetiku dana), `GlucoseViewModelTest` prepisan za date-range upit i navigaciju po danu
  (`goToPreviousDay`/`goToNextDay`/`canGoToNextDay`), ukupno 20 testova u tom fajlu.

### Odluke
- **Scatter grafik odbačen zajedno sa korelacijom** — nije bilo vredno zadržati mrtav kod
  (`Canvas`-baziran scatter chart, Pearson koeficijent) kad ga korisnik nije tražio; obrisano u
  potpunosti umesto ostavljeno "za svaki slučaj", isti princip kao ranije (`NetworkModule.kt`,
  `InsulinkDatabase.kt` presedani).
- **`latestGlucoseReading` odvojen od `glucoseReadingsForSelectedDay`**: statusna kartica na vrhu
  Glucose ekrana namerno ne prati izabrani dan — ovo je svesna UX odluka (prikazuje TRENUTNO
  stanje korisnika nezavisno od toga koji dan istorije pregleda), ne previd.
- **Dan-aritmetika preko `kotlinx.datetime.LocalDate`, ne sirovi millis**: `shiftedDayStartMillis`
  ide kroz `LocalDate.plus(DatePeriod(days = ...))` da ostane tačna preko DST prelaza, gde dan
  nije uvek tačno 24h. Pokriveno testovima (`LocalTimeOfDayTest`).

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:compileDebugKotlin`,
  `:app:testDebugUnitTest` (uključujući 20/20 u `GlucoseViewModelTest`), `:app:assembleDebug` —
  svi BUILD SUCCESSFUL, posle svake od tri iteracije.
- Korisnik potvrdio uživo na fizičkom uređaju: Statistics chip-ovi + statistika rade, Glucose
  dnevni prikaz + swipe navigacija rade, bez konflikta sa swipe-to-delete na listi.

### Šta je ostalo
- Isto što i pre za preostale faze/FZ (faza 3 Compose Multiplatform UI, faza 4 iOS, faza 7 Web,
  faza 8 testiranje/pisanje rada; Health Connect integracija za Samsung Health istražena ali
  odložena po korisnikovoj odluci — vidi prethodnu sesiju).

---

## 2026-09-04 — Početak faze 4: prvi Compose Multiplatform ekran + iOS build bez Mac-a

### Kontekst i rok
Korisnik nema Mac do sutra, a rok za snimak aplikacije na Android-u I iOS-u je ponedeljak. Cilj
danas: napraviti najmanji realan, ali PRAVI (ne toy demo) Compose Multiplatform vertikalni presek
kroz Glucose feature koji radi na oba OS-a, i proveriti koliko se od iOS build lanca može
potvrditi BEZ Mac-a, da se sutra ne gubi vreme na iznenađenja.

### Odluka o obimu (dogovoreno sa korisnikom)
Puna migracija SVIH ekrana na Compose Multiplatform (faza 3 u celosti) nije realna do ponedeljka.
Umesto toga: jedan feature (Glucose, već najkompletniji u `:shared`) dobija nov, namerno manji
MVP Compose Multiplatform ekran u `shared/commonMain` — bez insulin/meal povezivanja, bez Wear OS
push-a, bez ručne izmene datuma/vremena (novi unos dobija trenutno vreme, izmena čuva original) —
da prvi iOS build/test ciklus, rađen na slepo, ostane što manjeg rizika. Android-ov postojeći,
potpuno funkcionalan Hilt Glucose ekran NIJE dirat (nulti rizik regresije na već-verifikovanu
funkcionalnost); umesto toga isti novi deljeni ekran je DODATNO dostupan na Android strani preko
novog side drawer unosa "Glucose (shared UI)", da se u snimku vidi da isti kod stvarno radi na
oba OS-a, ne dva odvojena UI-ja koja liče jedan na drugi.

### Šta smo dodali
- **`UserSession`** (`shared/commonMain/core/session`) — objekat sa `MutableStateFlow<String?>`,
  zamenjuje direktnu zavisnost od Firebase Auth-a (koji i dalje postoji SAMO na Android strani).
  Android: `SharedViewModel.getCurrentUser()` upisuje pravi Firebase uid. iOS: `initKoinIOS()`
  upisuje fiksni lokalni demo id (`"ios-demo-user"`) — nema još prijave na iOS-u, podaci ostaju
  samo lokalni (Room/SQLite bundled, bez cloud sync-a — isto kao i za sve ostale feature-e na
  iOS-u za sada, vidi `NotImplemented*RemoteDataSource`).
- **`shared/commonMain/feature/glucose/ui/viewmodel/GlucoseViewModel.kt`** — nov Koin `single`
  (ne Hilt), koristi `GlucoseReadingRepository` + `SettingsPreferences` + `UserSession`. Dnevni
  prikaz + prev/next navigacija (isti obrazac kao Android-ov, iz prošle sesije), add/edit/delete.
- **`shared/commonMain/feature/glucose/ui/GlucoseScreen.kt`** — status kartica, dan-header sa
  ‹/› (obična `Text`, ne `Icon` — vidi "Problemi" niže), prost `Canvas`-baziran linijski grafik
  (Vico, korišćen u Android ekranu, nije Compose Multiplatform kompatibilan), lista očitavanja,
  add/edit dijalog. Nema zavisnosti od Android string resursa/teme — brend boje su lokalne
  `Color(0x...)` konstante, tekstovi su hardkodovani (privremeno, dok se ne doda compose-resources
  i18n — obeleženo kao ostatak posla).
- **`shared/commonMain/core/dispatcher/IoDispatcher.kt`** (`expect val ioDispatcher`) + android/ios
  actual — zamenjuje SVAKI direktan poziv `Dispatchers.IO` u `:shared/commonMain` (13 fajlova:
  svi repozitorijumi i `buildXDatabase()` funkcije). Razlog u sekciji "Problemi" niže.
- **`shared/commonMain/core/time/LocalTimeOfDay.kt`** — dodato `timeOfDayLabel`/`dateTimeLabel`/
  `shortWeekdayDateLabel` (ručno građeni iz `LocalDateTime` polja, BEZ `SimpleDateFormat` — taj
  je JVM-only, ne postoji van Android/JVM strane).
- **`shared/iosMain/core/di/KoinInit.ios.kt`** — `initKoinIOS()`, poziva se jednom iz
  `MainViewController.kt` pre prvog Compose ekrana (Android već ima svoj `startKoin` poziv u
  `InsulinkApplication`, iOS ga do sada nije imao uopšte).
- **`org.example.project.App()`** (`shared/commonMain`) — prepravljen iz KMP wizard placeholder-a
  (dugme "Click me!") u pravi root ekran: `MaterialTheme` + `GlucoseScreen`. Koristi ga i iOS
  (`MainViewController`) i Android (novi `SharedGlucoseDemo` route).
- **Android strana**: nov `Screen.SharedGlucoseDemo` route, side drawer unos ("Glucose (shared
  UI)", `ic_devices.xml`), poziva `org.example.project.App()` direktno (bez Hilt-a za taj ekran).
- **`iosApp/Configuration/Config.xcconfig`**: `PRODUCT_NAME`/`PRODUCT_BUNDLE_IDENTIFIER` sa KMP
  wizard default-a ("proba kmp" / `org.example.project.probakmp$(TEAM_ID)`) na `Insulink` /
  `com.dj.insulink.ios`. `TEAM_ID` ostaje prazan do sutra (postavlja se u Xcode-u iz korisnikovog
  Apple ID naloga).

### Problemi otkriveni BEZ Mac-a (najvažniji deo današnjeg rada)
Umesto da se čeka Mac da bi se build uopšte probao, korišćeno je da Kotlin/Native ume da
kompajlira klib-ove (metadata i stvarni `iosArm64`/`iosSimulatorArm64` target kod) i na Windows-u
— samo link/codesign/Xcode/simulator zahtevaju Mac. Ovim putem otkrivena su tri prava, ozbiljna
problema koja bi sutra na Mac-u izgledala kao nepoznata, teško-dijagnostikovana greška:

1. **`Dispatchers.IO` ne postoji u `commonMain` API površini koju Kotlin/Native vidi.** Svi
   repozitorijumi u `:shared` (glucose, meals, insulin, friends, reminders, fitness, librelink)
   su ga koristili direktno u `commonMain` kodu — radilo je na Android/JVM strani (zato niko nije
   primetio), ali bi potpuno blokiralo BILO KAKAV iOS build, ne samo Glucose. Otkriveno preko
   `:shared:compileIosMainKotlinMetadata` ("Unresolved reference 'IO'" na ~30 mesta). Popravljeno
   uvođenjem `ioDispatcher` (gore) — Android actual i dalje koristi pravi `Dispatchers.IO`
   (identično ponašanje, nulti rizik za Android), iOS actual koristi `Dispatchers.Default`.
2. **`GlobalContext` (Koin) nije deo commonMain API površine** — samo JVM/Android varijanta Koin-a
   ga ima (potvrđeno raspakivanjem `koin-core-jvm-4.1.1.jar` nasuprot `koin-core-metadata-4.1.1.jar`
   — `GlobalContext`/`KoinPlatformTools` klase postoje samo u JVM jar-u). Popravljeno korišćenjem
   `org.koin.mp.KoinPlatform.getKoin()` (multiplatform-bezbedan Koin API) u `App.kt`.
3. **Kotlin/Native ABI verzija ne odgovara** — `composeMultiplatform` 1.11.1 (i uz njega vezan
   `material3` 1.11.0-alpha07), `androidxLifecycleMultiplatform` 2.11.0-beta01 i `ktor` 3.4.0 su
   svi objavljeni sa native klib ABI 2.3.0 (Kotlin 2.3.20/2.3.0 kompajlerom), a projekat je
   pinovan na Kotlin **2.2.20**, čiji Kotlin/Native kompajler ume da učita samo ABI <= 2.2.0.
   Android/JVM strana ovo ne vidi (JVM classfile nema takvo ograničenje) — `:app:assembleDebug`
   je i dalje prolazio, pa bi ovo sutra na Mac-u ispalo kao potpuno iznenađenje tek pri
   `:shared:embedAndSignAppleFrameworkForXcode`/Xcode build-u. CMP-ov zvaničan changelog (GitHub
   release 1.11.0) eksplicitno kaže "Kotlin 2.3 is required for native and web platforms" — ovaj
   projekat namerno NE podiže Kotlin na 2.3 ovako blizu roka (veliki, rizičan zahvat — vidi gotcha
   #5 u CLAUDE.md). Umesto toga vraćeno na poslednje verzije u svakoj liniji potvrđene da rade sa
   Kotlin 2.2.20 (proveravano jedno po jedno preko zvaničnih JetBrains/Ktor release beleški, isti
   princip kao ranija material3 gotcha): `composeMultiplatform` → 1.10.0, `composeMaterial3` →
   1.10.0-alpha05, `androidxLifecycleMultiplatform` → 2.10.0-alpha06, `ktor` → 3.3.3. Detaljan
   komentar ostavljen u `gradle/libs.versions.toml` i CLAUDE.md (gotcha #5) da se ne ponovi.

### Manje odluke
- **Bez ikonica** (`Icons.Filled.*`) u novom deljenom ekranu — `androidx.compose.material:material-
  icons-extended` je Android-only artefakt (ne radi za iOS target), a JetBrains-ov CMP
  `material-icons-core` artefakt (`org.jetbrains.compose.material:material-icons-core`) se
  pokazao nedostupan za tačno `composeMultiplatform` verziju koju smo prvo probali (1.11.1) —
  umesto dodatnog kopanja po još jednoj nezavisnoj verzionoj liniji, dan-navigacija i dugmad
  koriste obične `Text("‹")`/`Text("+")`/`Text("✕")` glifove. Sasvim dovoljno za MVP, nula
  dodatnih zavisnosti.
- **`compileIosMainKotlinMetadata`/`compileKotlinIosArm64`/`compileKotlinIosSimulatorArm64` kao
  redovan deo provere** — ovi Gradle taskovi rade na Windows-u (samo Kotlin/Native kompajler,
  bez Apple linker-a/Xcode-a) i otkrivaju gotovo sve greške vezane za iOS pre nego što se uopšte
  stigne do Mac-a. Vredi ih pokretati posle svake promene u `:shared` do kraja iOS rada.

### Verifikacija
- `:shared:compileAndroidMain`, `:shared:compileIosMainKotlinMetadata`,
  `:shared:compileKotlinIosArm64`, `:shared:compileKotlinIosSimulatorArm64`,
  `:shared:testAndroidHostTest`, `:app:compileDebugKotlin`, `:app:testDebugUnitTest`,
  `:app:assembleDebug` — svi BUILD SUCCESSFUL.
- Vizuelna provera novog "Glucose (shared UI)" ekrana na fizičkom Android uređaju NIJE urađena u
  ovoj sesiji (nijedan uređaj nije bio povezan) — prvo sledeće na listi kad korisnik proba.
- Xcode build / simulator / uređaj pokretanje ostaju potpuno neverifikovani do sutra (Mac).

### Šta je ostalo
- Sutra (kad stigne Mac): otvoriti `iosApp.xcodeproj`, postaviti `TEAM_ID` (Signing & Capabilities
  → Team, besplatan Apple ID nalog je dovoljan za simulator/sopstveni uređaj), pokrenuti build —
  očekivano da preostanu SAMO Xcode/link-specifične greške (ako ih uopšte bude), pošto je sav
  Kotlin/Native kod već potvrđeno kompajlira.
- Vizuelno potvrditi na Android uređaju da novi deljeni ekran radi kako treba pre nego što se
  osloni na njega kao referencu za iOS izgled.
- i18n (compose-resources) za deljeni ekran — tekstovi su za sada hardkodovani na srpskom.
- Ako ostane vremena: isti obrazac (Koin ViewModel + deljeni Compose ekran) ponoviti za još
  jedan-dva ekrana pre snimka, npr. Statistics — trenutno je iOS ograničen na samo Glucose.
