# Insulink — plan objave na App Store i Play Store

Napisano 2026-10-11. Cilj: dovesti aplikaciju do objave u obe prodavnice, pokrenuti naplatu i
dovesti prve prave korisnike.

Polazna tačka: aplikacija samo **beleži** insulin i ne računa dozu. To treba da ostane tako, jer je
kalkulator doze najveća regulatorna zamka (Apple pravilo 1.4.1, EU MDR).

## Stanje u kodu na dan pisanja

| Nalaz | Zašto je bitno |
|---|---|
| 273 necommitovane izmene na `jovan/glucose-shared-migration` | Prvo commit i merge u `master`; sve ostalo ide preko toga. |
| Nema brisanja naloga | Apple i Google ga obavezno traže u aplikaciji, a Google i preko web linka. |
| Nema politike privatnosti ni uslova korišćenja | Obavezno za obe prodavnice, a zdravstveni podaci su posebna kategorija po GDPR-u. |
| Google Sign-In na iOS-u bez Sign in with Apple | Apple po pravilu 4.8 odbija takvu aplikaciju. |
| `isMinifyEnabled = false`, nema release potpisivanja, `versionCode = 1` | Nema release builda za Play. |
| LogMeal/Spoonacular/USDA ključevi ugrađeni u aplikaciju | Mogu se izvući iz APK-a, pa neko može da troši kvotu; besplatni planovi često ne dozvoljavaju komercijalnu upotrebu. |
| LibreLinkUp klijent se predstavlja kao `llu.android` 4.16.0 | Nezvanični API: Abbott ga može ugasiti u svakom trenutku, a krši njihove uslove. |
| Nema Crashlytics-a, analitike ni naplate | Bez toga se ne vide padovi, zadržavanje korisnika ni prihod. |
| `allowBackup="true"`, shared namespace `org.example.project.shared` | Sitnice za sređivanje pre objave. |

## Faza 1: Odluke koje blokiraju sve ostalo (1–2 nedelje)

- [ ] **Pravni oblik.** Proveriti u Play Console da li zdravstvene/medicinske aplikacije traže
      organizacioni nalog (firma ili preduzetnik sa D-U-N-S brojem) umesto ličnog — pre plaćanja
      25 $. Ako važi, otvoriti preduzetničku radnju, koja je svakako potrebna za naplatu.
- [ ] **Apple Developer nalog** (99 $ godišnje); može kao fizičko lice.
- [ ] **Pozicioniranje kao dnevnik, ne medicinsko sredstvo.** Odricanje od odgovornosti u
      onboarding-u i opisu u prodavnici, bez ikakvih preporuka terapije.
- [ ] **LibreLinkUp.** Predlog: ostaje kao opciona, jasno označena „eksperimentalna” funkcija na
      kojoj ne počiva ni marketing ni naplata. Rizik od odbijanja ili gašenja je realan.
- [ ] Proveriti da li su isplate iz obe prodavnice podržane za Srbiju i šta je potrebno od banke.

## Faza 2: Tehnička spremnost (3–5 nedelja)

Prva tri reda blokiraju sve ostalo i rade se tim redom.

- [ ] Commit i merge trenutnih izmena u `master`.
- [ ] Brisanje naloga: Auth, Firestore i lokalna baza, plus izvoz podataka.
- [ ] Sign in with Apple preko `ASAuthorizationController` (bez CocoaPods-a, kao ostatak iOS strane).
- [ ] Politika privatnosti i uslovi korišćenja (srpski i engleski), dostupni iz aplikacije i sa weba.
- [ ] Pregled Firestore pravila: korisnik čita samo svoje podatke, a prijatelji samo ono što je
      izričito podeljeno.
- [ ] API ključevi iza malog proxy-ja (Cloud Functions) sa ograničenjem po korisniku; proveriti
      uslove komercijalne upotrebe za LogMeal, Spoonacular i USDA.
- [ ] R8/minify, release keystore, Play App Signing, isti potpis za `wearApp`.
- [ ] Crashlytics i minimalna analitika uz pristanak korisnika.
- [ ] Naplata: pretplata preko Play Billing-a i StoreKit-a.
- [ ] Otkomentarisati fix za duplikate prijatelja u `FriendsViewModel` kad se beta završi.
- [ ] Meals kamera na iOS-u.
- [ ] Testiranje na fizičkom iPhone-u i Android telefonu.
- [ ] Onboarding, prazna stanja, engleski prevod.
- [ ] `allowBackup`, preimenovanje shared namespace-a.
- [ ] Materijal za prodavnice: ikonica, snimci ekrana, opis, Data Safety i App Privacy obrasci,
      Health apps deklaracija.

## Faza 3: Beta (2–4 nedelje)

- [ ] TestFlight i Play closed testing sa 20–50 pravih dijabetičara.
- [ ] Koristiti `uputstvo_za_testere.md` i SUS upitnik.
- [ ] Meriti koliko se korisnika vrati posle 7 dana; to je važnije od broja instalacija.

Ciljni datum za javnu betu: **14. novembar 2026, Svetski dan dijabetesa**. To je rok za betu, ne
za punu objavu.

## Faza 4: Objava

- [ ] Predaja na pregled u obe prodavnice; računati na bar jedno odbijanje i ispravku.
- [ ] Početna stranica sa linkovima ka prodavnicama.
- [ ] Praćenje padova i recenzija u prvim nedeljama.

## Zarada

Tržište je teško: mySugr je praktično besplatan, a iza konkurencije stoje farmaceutske firme. Prvih
godinu dana realno je računati na džeparac, ne platu. Šansa je u niši.

- **Freemium pretplata**, oko 2–4 € mesečno ili 20–30 € godišnje. Osnovno beleženje ostaje zauvek
  besplatno.
- **Premium**: AI prepoznavanje obroka sa slike (ionako košta po pozivu), PDF izveštaji za lekara,
  napredna statistika, Wear OS.
- **Bez reklama**: zdravstveni podaci i oglasne mreže donose pravne probleme i gubitak poverenja.
- **Prednosti**: srpski jezik i region bivše Jugoslavije (nema ozbiljne lokalne konkurencije),
  praćenje člana porodice (roditelji dece sa tipom 1), glukoza na satu.

## Promocija

- [ ] Javna beta za Svetski dan dijabetesa (14. novembar).
- [ ] Udruženja dijabetičara i Facebook grupe u regionu; nastup kao autor koji traži povratne
      informacije, ne kao prodavac.
- [ ] Endokrinolozi i savetovališta: PDF izveštaj je razlog da lekar preporuči aplikaciju.
- [ ] Početna stranica sa listom čekanja (potrebna i za politiku privatnosti).
- [ ] Optimizacija opisa u prodavnici na srpskom, hrvatskom, bosanskom i engleskom.
- [ ] Kratki video snimci tipa „napravio sam aplikaciju za dijabetes kao diplomski”.
- [ ] Reddit (r/diabetes_t1, r/androidapps), uz poštovanje pravila o samopromociji.

Objavljena aplikacija sa pravim korisnicima je i jak dokaz u CV-ju za KMP i Android pozicije, pa
ovaj plan ne isključuje traženje posla.
