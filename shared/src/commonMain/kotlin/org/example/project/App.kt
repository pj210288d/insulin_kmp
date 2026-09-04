package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.dj.insulink.shared.feature.glucose.ui.GlucoseScreen
import com.dj.insulink.shared.feature.glucose.ui.viewmodel.GlucoseViewModel
import org.koin.mp.KoinPlatform

// Root ekran deljen preko Compose Multiplatform-a - koristi ga i iOS (MainViewController.ios.kt
// poziva initKoinIOS() pa ComposeUIViewController { App() }) i Android (SharedGlucoseWrapper u
// :app poziva ovaj isti App()) - vidi CLAUDE.md, faza 4 MVP. Trenutno prikazuje samo deljeni
// Glucose ekran (bez navigacije/prijave - vidi GlucoseViewModel za obim ove MVP verzije).
// GlucoseViewModel je Koin single (vidi glucoseModule), zato se ovde uzima direktno preko
// KoinPlatform-a (multiplatform-bezbedan način da se dođe do trenutne Koin instance - obično
// GlobalContext, dostupan samo na JVM/Android strani) umesto Compose-Koin integracije - isti
// obrazac kao postojeći SharedModule.kt most (Hilt -> Koin) na Android strani, samo u
// suprotnom smeru.
@Composable
fun App() {
    MaterialTheme(colorScheme = insulinkColorScheme()) {
        val viewModel = remember { KoinPlatform.getKoin().get<GlucoseViewModel>() }
        GlucoseScreen(viewModel = viewModel)
    }
}

@Composable
private fun insulinkColorScheme() = lightColorScheme(
    primary = Color(0xFF4A7BF6),
    secondary = Color(0xFF8A5CF5),
    background = Color(0xFFF7F8FC),
    surface = Color.White
)
