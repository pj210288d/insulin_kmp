package com.dj.insulink.shared.feature.meals.photo

import androidx.compose.runtime.Composable

// Slikanje/biranje fotografije obroka za LogMeal prepoznavanje - platform-specifičan UI (Android
// koristi Activity Result API + FileProvider, iOS koristi UIImagePickerController), pa je ovo
// expect/actual composable factory po istom obrascu kao ostatak deljenog UI-ja koji mora da
// premosti platform API-je (npr. ReminderNotificationScheduler). Rezultat je uvek JPEG bajtovi,
// već downskalovani/kompresovani (isto ograničenje kao Android-ov postojeći
// AddMealWrapper.downscaleAndCompressPhoto - LogMeal odbija prevelike slike) - MealsViewModel
// samo prosleđuje bajtove u MealRepository.analyzeFoodImage, ne zna ništa o poreklu slike.
//
// Dodato 2026-09-07 na zahtev korisnika: pošto je za testiranje na iOS-u trenutno dostupan samo
// Simulator (bez fizičke kamere), MORA postojati opcija iz galerije - kamera se testira tek
// sutra na fizičkom uređaju. `isCameraAvailable` postoji baš zbog toga (Android: uvek true, uređaj
// uvek ima kameru u praksi; iOS: UIImagePickerController.isSourceTypeAvailable(.camera) - false na
// simulatoru, true na fizičkom uređaju) - UI sakriva dugme za kameru kad je false umesto da pokuša
// i tiho ne uradi ništa.
interface MealPhotoPickerLauncher {
    val isCameraAvailable: Boolean
    fun pickFromGallery()
    fun takePhoto()
}

@Composable
expect fun rememberMealPhotoPickerLauncher(
    onPhotoPicked: (ByteArray) -> Unit,
    onError: (String) -> Unit
): MealPhotoPickerLauncher
