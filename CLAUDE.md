# Insulink KMP — kontekst za Claude Code

## O projektu

Diplomski rad: migracija postojeće Android aplikacije **Insulink** (praćenje dijabetesa — glukoza, obroci, insulin, fizička aktivnost, podsetnici, prijatelji, izveštaji) na **Kotlin Multiplatform** arhitekturu sa podrškom za Android, iOS, Wear OS i (opciono) web.

Puna specifikacija rada nalazi se u `specifikacija.md` u korenu repoa — **pre bilo kakvih većih arhitekturnih odluka, proveri taj fajl.** Sadrži: analizu postojećeg rešenja, izbor KMP biblioteka, predloženu Gradle strukturu modula, sve funkcionalne i nefunkcionalne zahteve (FZ-1 do FZ-14), model podataka i plan realizacije po fazama.

## Trenutno stanje (ažurirano 2026-09-07)

- Repo: `insulin_kmp` na GitHub-u (prebačen iz originalnog Insulink repoa uz zadržanu git istoriju, ne fork)
- Postojeći `androidApp` (ranije `app`) modul i dalje radi nepromenjen — Hilt, Room, Retrofit/Gson, Firebase
- `:shared` Gradle modul (Kotlin Multiplatform + Compose Multiplatform + Android KMP library plugin) sada builduje i radi na oba OS-a.
- **Mac/Xcode je sada dostupan i aktivno se koristi** (developer je ranije radio na Windows-u
  "na slepo"). Android SDK je na ovom Mac-u instaliran preko Homebrew-a
  (`brew install --cask android-commandlinetools`, pa `sdkmanager` za platform-tools/platform
  36/build-tools), sa `local.properties` → `sdk.dir=/opt/homebrew/share/android-commandlinetools`.
  Prvi uspešan Xcode/simulator build (iPhone 16 Pro, iOS 18.6) je potvrđen istog dana.
- **iOS strana je feature-parity sa Android-om** za sve glavne tokove — korisnik je eksplicitno
  tražio da SVAKI feature koji radi na Android-u radi identično na iOS-u, i to je (osim Meals
  kamere, namerno preskočene) urađeno kroz 6 faza, sve potvrđeno uživo od strane korisnika u
  iOS simulatoru. Vidi `dnevnik.md` (unosi 2026-09-06/2026-09-07) za pun dnevni tok; sažetak
  po fazi ispod.
- **Arhitektonska odluka**: Firebase Auth + Firestore na iOS-u NIJE preko GitLive Firebase KMP
  SDK-a, nego preko ručno pisanog Ktor REST klijenta (Identity Toolkit REST API za Auth,
  Firestore REST API za dokumente/query). Razlog: izbegavanje CocoaPods-a i rizika od
  Kotlin/Native ABI sudara sa pinovanim Kotlin 2.2.20 (isti tip problema kao gotcha #5 ispod).
  Isti princip važi za Google Sign-In (ručni OAuth 2.0 Authorization Code + PKCE preko
  `ASWebAuthenticationSession`, ne GoogleSignIn SDK), Reminders-notifikacije
  (`platform.UserNotifications.*`) i Reports-PDF (`platform.UIKit`/`CoreGraphics` preko
  `UIGraphicsPDFRenderer`) — sve to su "besplatni" ObjC/C sistemski frameworkovi, bez ijedne
  nove binarne zavisnosti/CocoaPod-a.

### Šta je urađeno po fazi (plan iz `~/.claude/plans/dynamic-cooking-noodle.md`)

- **Faza 0 (status-bar overlap)** ✅ — `expect fun Modifier.sharedRootTopInset()` (no-op na
  Android, `statusBarsPadding()` na iOS) u `shared/commonMain/core/ui/RootInsets.kt`.
- **Faza 1 (Auth)** ✅ — Prava prijava/registracija/reset lozinke/verifikacija emaila preko
  `FirebaseAuthRestClient` (commonMain/Ktor), token storage preko `TokenStorage` expect/actual,
  `UserSession`/`AuthSession` prošireni na pravo auth stanje. **Google Sign-In je takođe dodat**
  (nije bio u originalnom planu, dodat na zahtev korisnika) preko
  `GoogleSignInCoordinator` (PKCE + `ASWebAuthenticationSession`) i
  `accounts:signInWithIdp`. Nove shared `LoginScreen`/`RegistrationScreen`/`ForgotPasswordScreen`
  u `feature/auth/ui`. `App()` root sada ima pravi gate (`AuthFlow` / `LoadingScreen` /
  `MainTabs`) umesto fiksnog demo-id-a.
- **Faza 2 (cloud sync)** ✅ — `FirestoreRestClient` + `FirestoreValue` (commonMain) za
  get/PATCH `users/{uid}` dokumenta u Firestore typed-value JSON formatu. Svih 7 relevantnih
  ViewModel-a (Glucose, Insulin, Fitness, Reminders, Meals, Friends, Reports) ima `init` blok
  koji na promenu `UserSession.currentUserId` pokreće `fetchXAndUpdateDatabase` - ovo je bio
  potreban naknadni fix (vidi "Rešeni problemi" #6 ispod), bez njega se podaci nisu pojavljivali
  na novom uređaju/instalaciji iako je nalog isti.
- **Glucose dijalog dovden do pune paritetnosti** ✅ (dodatni zahtev korisnika, van originalnog
  plana) — dodavanje/izmena očitavanja sada ima datum/vreme (Material3 `DatePicker`/
  `TimePicker`, potvrđeno radi na iOS-u uprkos pinovanoj starijoj CMP verziji), tip/dozu
  insulina, povezivanje sa obrokom istog dana — isto kao Android.
- **Faza 3 (Friends)** ✅ — nov shared ekran, `FirestoreRestFriendRemoteDataSource` (uz
  `:runQuery` structured query podršku u `FirestoreRestClient.queryEqual`). **Poznat bug
  namerno ostavljen aktivan** na eksplicitan zahtev korisnika (2026-09-07): dodavanje istog
  prijatelja dva puta pravi duplikat u lokalnoj bazi, i ne postoji dugme za uklanjanje
  prijatelja. Fix za oboje je **napisan ali zakomentarisan u kodu** (ne u posebnim fajlovima) —
  `FriendsViewModel.addFriend()` dedup provera i `removeFriend()` funkcija, i
  `FriendsScreen.kt` `FriendRow` delete `IconButton`. Namera: ovo treba da ostane kao bug koji
  beta korisnici sami pronađu, tek posle beta perioda otkomentarisati. **Ne "popravljaj" ovo bez
  eksplicitnog naloga korisnika** — ovo je namerno stanje, ne previd.
- **Faza 4 (Reminders notifikacije)** ✅ — `ReminderNotificationScheduler` expect/actual;
  iOS actual koristi `UNUserNotificationCenter` + `UNCalendarNotificationTrigger(repeats=true)`;
  Android actual dokumentovano no-op (wrapuje postojeći `AlarmManager` tok nepromenjen).
- **Faza 5 (Meals kamera)** ❌ **namerno preskočeno** — iOS simulator nema pravu kameru, pa je
  ova stavka niskog prioriteta za snimak i eksplicitno je odložena/preskočena po dogovoru sa
  korisnikom. `MealRepository.analyzeFoodImage` ostaje platform-agnostičan i spreman za kasniju
  `UIImagePickerController` implementaciju ako zatreba.
- **Faza 6 (Reports PDF)** ✅ — `IosPdfReportGenerator` preko `UIGraphicsPDFRenderer` +
  starije Core Graphics C API (`CGContextShowTextAtPoint`/`CGContextSelectFont`) jer moderne
  `NSString.drawAtPoint` nije uspevalo da se resolve-uje u cinterop-u iz nepoznatog razloga
  (vidi "Rešeni problemi" #7). `PdfShareCoordinator` preko `UIActivityViewController`.
  `isPdfReportSupported` expect/actual (`false` na Android — Android ima svoj postojeći PDF
  tok van shared UI-ja — `true` na iOS).
- **Pet+ deljenih Compose Multiplatform ekrana** postoji u `shared/commonMain`, svaki kao
  Koin `single` ViewModel + Compose ekran u odgovarajućem `feature/*/ui` paketu: Glucose,
  Statistics, Insulin, Settings, Reminders, Fitness, LibreLinkUp, Meals, Friends, Reports.
  Svi se prikazuju iza horizontalno-skrolabilne tab-trake (`SharedTabBar`/`SharedTab` enum) u
  `org.example.project.App()` - to je iOS-ov root ekran (`MainViewController.kt` poziva
  `initKoinIOS()` pa `ComposeUIViewController { App() }`) i ISTOVREMENO Android-ov "Shared UI
  (also on iOS)" side-drawer ekran (`AppNavigation.kt` poziva `org.example.project.App()`
  direktno) - dokazano isti kod na oba OS-a, ne dva odvojena UI-ja.
- `iosApp/iosApp.xcodeproj`: `IPHONEOS_DEPLOYMENT_TARGET` namerno spušten na 15.0 (bio je
  18.2, KMP wizard default). `TEAM_ID`/Signing je postavljen u Xcode-u (Signing & Capabilities
  → Team, besplatan Apple ID nalog). Napomena: postoji "stray"/nepovezana `firebase-ios-sdk` SPM
  paket referenca u `project.pbxproj` iz ranijeg eksperimentisanja — bezopasna (ništa je ne
  koristi), korisnik može da je ukloni preko Xcode-a (File → Add Packages / Package Dependencies)
  ako želi čistiji projekat, nije blokirajuće.

### Otvoreno / poznato ograničeno

- **Nijedan fizički uređaj (ni Android ni iOS) još nije korišćen za testiranje ove sesije** —
  sve je rađeno na Android emulatoru (ranije) i iOS Simulator-u (ova sesija). Pre finalnog
  snimka razmisliti da li je fizički uređaj potreban/dostupan (korisnik JESTE potvrdio cross-device
  sync uživo preko Google Sign-In-a sa istog naloga korišćenog na pravom Android telefonu ranije
  u razvoju, samo ne u istoj sesiji/na istom uređaju paralelno).
- Wear OS i web (opciono) nisu deo ovog kruga rada — van obima trenutnog "Android = iOS"
  zahteva.

## Rešeni problemi pri postavljanju (da se ne ponavljaju)

1. **`android {}` vs `androidLibrary {}` blok** — za AGP ispod 8.12.0 koristi se `androidLibrary {}`, ne `android {}` (real projekat ima AGP 8.11.1). Ne diraj ovo bez razloga da ne izazoveš regresiju.
2. **Version catalog (`gradle/libs.versions.toml`)** — kad se dodaju novi KMP/Compose alias-i, moraju se dodati i odgovarajući `[libraries]` unosi, ne samo `[versions]`/`[plugins]`. Crtice u ključu (`compose-components-resources`) postaju ugnježdeni pristup u kodu (`libs.compose.components.resources`).
3. **Material3 ima poseban verzioni ciklus** — u Compose Multiplatform 1.11.1, `material3` artefakt je ostao na `1.11.0-alpha07` (nije stigao do 1.11.1 kao runtime/ui/foundation). Ima svoj `composeMaterial3` version key u tomlu, odvojen od `composeMultiplatform`. Ako se compose verzija ikad podigne, proveri zvanične JetBrains release notes za tačnu material3 verziju pre nego što je uskladiš sa ostatkom.
4. Ne diraj AGP verziju na 9.0+ dok se ne planira namerna migracija — AGP 9.x zahteva potpuno razdvajanje Android app modula od KMP shared modula i menja ceo pristup (built-in Kotlin, `com.android.kotlin.multiplatform.library` obavezan). Trenutno radimo sa AGP 8.11.1 i `androidLibrary {}` sintaksom namerno.
5. **Kotlin/Native ABI verzija blokira iOS build ako se compose/lifecycle/ktor podignu na najnovije** — Kotlin je pinovan na `2.2.20` (`kotlin` u tomlu). Kotlin/Native kompajler koji ide uz tu verziju ume da učita samo klib-ove sa ABI <= 2.2.0. `composeMultiplatform` 1.11.x, `composeMaterial3` 1.11.0-alpha07, `androidxLifecycleMultiplatform` 2.11.0-beta01 i `ktor` 3.4.0 su svi objavljeni sa ABI 2.3.0 (Kotlin 2.3.20/2.3.0 kompajlerom) - Android/JVM strana ih normalno može da koristi (JVM classfile nema ovo ograničenje), pa se problem NE vidi na `:app:compileDebugKotlin`/`:app:assembleDebug`, samo na `:shared:compileKotlinIosArm64`/`compileKotlinIosSimulatorArm64` ("KLIB resolver: ... incompatible ABI version"). Vraćeno na poslednje potvrđene kompatibilne verzije (composeMultiplatform 1.10.0, material3 1.10.0-alpha05, lifecycle 2.10.0-alpha06, ktor 3.3.3) — vidi opširan komentar u `gradle/libs.versions.toml` iznad tih ključeva. Da bi se koristile novije verzije, prvo treba podići Kotlin na 2.3+ (veći, rizičniji zahvat — utiče na KSP/Room/Compose compiler plugin/AGP kompatibilnost), pa istom logikom kao gotcha #3 proveriti zvanične JetBrains release notes za tačno uparene verzije PRE podizanja bilo koje od te četiri.
6. **`Json { encodeDefaults = true }` je OBAVEZNO za sve Ktor/Firebase REST pozive** — bez ovoga, polja sa default vrednostima (npr. `EmailPasswordRequest.returnSecureToken: Boolean = true`) se tiho izostavljaju iz JSON tela zahteva. Ovo je uzrokovalo pravi login crash ("Fields [refreshToken, expiresIn] are required") jer `accounts:signInWithPassword` zahteva to polje dok `accounts:signUp` ne zahteva — registracija je radila, login nije. Podešeno globalno u `HttpClientFactory.createCoreHttpClient()`. Response parsing na svim REST klijentima (`FirebaseAuthRestClient`, `FirestoreRestClient`) je namerno ručan preko `JsonObject`/`requireField` helpera (ne strogi `@Serializable`) da bi greške jasno prijavile koja polja SU prisutna kad neko nedostaje — korisno za debug protiv pravog Firebase backend-a.
7. **Novi shared ViewModel MORA sam da pokrene cloud fetch u `init` bloku** — postojanje `fetchXAndUpdateDatabase(userId)` metode u repozitorijumu ne znači da se ona ikad poziva. Bez eksplicitnog `init { viewModelScope.launch { UserSession.currentUserId.collect { ... fetchXAndUpdateDatabase(it) } } }` u SVAKOM feature ViewModel-u, lokalna baza na novom uređaju/instalaciji ostaje prazna iako je nalog isti i podaci postoje na serveru (uhvaćeno tek kad je korisnik uživo testirao cross-device sync preko Google Sign-In-a). Ovaj obrazac je sada u svih 7 feature ViewModel-a (Glucose, Insulin, Fitness, Reminders, Meals, Friends, Reports) — kopiraj ga za svaki budući novi feature.
8. **iOS Core Graphics tekst u PDF-u: `NSString.drawAtPoint` se ne resolve-uje u ovom cinterop-u** (nepoznat tačan uzrok, probano sa više varijanti importa/cast-ova, i na metadata i na real-target kompajliranju) — radno rešenje je stariji C API (`CGContextShowTextAtPoint`/`CGContextSelectFont`, sa `CGTextEncoding.kCGEncodingMacRoman` — ugnježden enum, ne top-level). Iako Apple dokumentacija ovo označava "No longer supported", to je samo deprecation napomena — header-i (proveri sa `grep` u pravom iOS SDK-u) i dalje deklarišu ove funkcije i one rade. Upisivanje fajla ide preko POSIX `fopen`/`fwrite` (ne `NSData.writeToFile`, isti tip cinterop problema).
9. **`UIGraphicsPDFRenderer` ima top-down CTM, ali `CGContextShowTextAtPoint` ne prati je automatski** — klasična Core Graphics zamka: renderer kontekst već ima flip-ovan koordinatni sistem (Y raste na dole), pa NE treba ručno raditi `PAGE_HEIGHT - y` na pozicijama (dovodi do obrnutog redosleda linija), ALI treba jednom po strani pozvati `CGContextSetTextMatrix(cgContext, CGAffineTransformMakeScale(1.0, -1.0))` da se sam tekst/glifovi ne iscrtavaju naopako. Oba efekta izgledaju slično na prvi pogled ("tekst je čudan") ali su odvojeni bugovi sa odvojenim fix-evima — ne pokušavaj da ih rešiš jednim zajedničkim flip-om.
10. **Prvi put na novom Mac-u**: Android SDK se ne podrazumeva — instaliraj preko `brew install --cask android-commandlinetools`, pa `sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;<verzija>"`, pa napravi `local.properties` sa `sdk.dir=<put do android-commandlinetools>`. Takođe proveri `git config --global user.name/user.email` — prvi commit na novom nalogu/mašini ume da se auto-potpiše pogrešnim identitetom (macOS lokalno korisničko ime), popravi sa `git commit --amend --reset-author` ako se desi.

## Plan migracije (iz specifikacije, poglavlje 9)

1. ✅ Priprema — čišćenje `dataREMOVE` referenci, KMP Gradle struktura
2. ✅ Migracija modela i poslovne logike u `commonMain` (Room Multiplatform, `BundledSQLiteDriver` na iOS)
3. ✅ Migracija Android UI-ja na Compose Multiplatform (deljeni ekrani, vidi "Trenutno stanje" gore)
4. ✅ iOS aplikacija — svih 10 shared ekrana radi na iOS-u, pun Auth+cloud-sync tok, Xcode/simulator build potvrđen
5. **U TOKU / delimično** — Nove funkcionalnosti: FZ-9 (insulin doze) ✅, FZ-10 (real-time/cloud sync) ✅ za oba OS-a, FZ-12 (statistika) ✅, FZ-14 (LibreLinkUp) ✅ (ručna/foreground sinhronizacija; pozadinska periodična sinhronizacija - `WorkManager`/`BGTaskScheduler` - namerno nije urađena, poliranje van roka)
6. Wear OS aplikacija — nije započeto, van obima trenutnog "Android = iOS" kruga rada
7. Web aplikacija (opciono) — nije započeto
8. Testiranje, evaluacija, pisanje rada — u toku (`dnevnik.md` se ažurira posle svake faze/dana)

**Napomena o pristupu (istorijska, i dalje validna kao princip)**: svaki feature je prebačen pojedinačno kroz ceo lanac — entitet → repozitorijum → Koin modul → Ktor/REST poziv — u `shared/commonMain`, uz potvrdu da `androidApp` i dalje radi identično pre prelaska na sledeći. Isti obrazac (jedan feature/slice po commit-u + pun verifikacioni lanac + ručna provera) korišćen je i za ceo Auth/cloud-sync/Friends/Reminders/Reports rad opisan gore.

## Napomene o razvojnom okruženju

- **Mac/Xcode je sada dostupan** (ranije developer nije imao pristup, radio je "na slepo" sa
  Windows-a — vidi istoriju u `dnevnik.md` ako je bitan kontekst tog perioda). Android SDK na
  ovom Mac-u je instaliran preko Homebrew-a (vidi gotcha #10 gore).
- Rok: snimak (screen recording) aplikacije kako radi i na Android-u i na iOS-u, do ponedeljka
  (2026-09-08). Sav rad opisan u "Trenutno stanje" iznad je odrađen i uživo potvrđen od
  korisnika u iOS simulatoru pre tog roka.
- Git remote koristi Personal Access Token (ne SSH) za push na GitHub
