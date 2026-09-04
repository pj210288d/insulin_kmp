package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.feature.fitness.ui.FitnessScreen
import com.dj.insulink.shared.feature.fitness.ui.viewmodel.FitnessViewModel
import com.dj.insulink.shared.feature.glucose.ui.GlucoseScreen
import com.dj.insulink.shared.feature.glucose.ui.viewmodel.GlucoseViewModel
import com.dj.insulink.shared.feature.insulin.ui.InsulinScreen
import com.dj.insulink.shared.feature.insulin.ui.viewmodel.InsulinViewModel
import com.dj.insulink.shared.feature.librelink.ui.LibreLinkScreen
import com.dj.insulink.shared.feature.librelink.ui.viewmodel.LibreLinkViewModel
import com.dj.insulink.shared.feature.meals.ui.MealsScreen
import com.dj.insulink.shared.feature.meals.ui.viewmodel.MealsViewModel
import com.dj.insulink.shared.feature.reminders.ui.RemindersScreen
import com.dj.insulink.shared.feature.reminders.ui.viewmodel.RemindersViewModel
import com.dj.insulink.shared.feature.settings.ui.SettingsScreen
import com.dj.insulink.shared.feature.settings.ui.viewmodel.SettingsViewModel
import com.dj.insulink.shared.feature.statistics.ui.StatisticsScreen
import com.dj.insulink.shared.feature.statistics.ui.viewmodel.StatisticsViewModel
import org.koin.mp.KoinPlatform

// Root ekran deljen preko Compose Multiplatform-a - koristi ga i iOS (MainViewController.ios.kt
// poziva initKoinIOS() pa ComposeUIViewController { App() }) i Android (SharedGlucoseDemo route
// u :app poziva ovaj isti App()) - vidi CLAUDE.md, faza 4 MVP. Prikazuje osam deljenih MVP
// ekrana (Glucose, Statistics, Insulin, Settings, Reminders, Fitness, LibreLinkUp, Meals - vidi
// njihove ViewModel-e za obim) iza proste horizontalno-skrolabilne tab-trake, bez prijave (vidi
// UserSession) i bez prave navigacione biblioteke - samo lokalni Compose state, dovoljno za
// par ekrana. Svaki ViewModel je Koin single (vidi glucoseModule/statisticsModule/
// insulinModule/settingsModule/remindersModule/fitnessModule/librelinkModule), zato se ovde
// uzimaju direktno preko KoinPlatform-a (multiplatform-bezbedan način da se dođe do trenutne
// Koin instance - obično GlobalContext, dostupan samo na JVM/Android strani) umesto
// Compose-Koin integracije - isti obrazac kao postojeći SharedModule.kt most (Hilt -> Koin) na
// Android strani, samo u suprotnom smeru.
@Composable
fun App() {
    MaterialTheme(colorScheme = insulinkColorScheme()) {
        var selectedTab by remember { mutableStateOf(SharedTab.GLUCOSE) }

        Column(modifier = Modifier.fillMaxSize()) {
            SharedTabBar(selectedTab = selectedTab, onSelect = { selectedTab = it })
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    SharedTab.GLUCOSE -> {
                        val viewModel = remember { KoinPlatform.getKoin().get<GlucoseViewModel>() }
                        GlucoseScreen(viewModel = viewModel)
                    }
                    SharedTab.STATISTICS -> {
                        val viewModel = remember { KoinPlatform.getKoin().get<StatisticsViewModel>() }
                        StatisticsScreen(viewModel = viewModel)
                    }
                    SharedTab.INSULIN -> {
                        val viewModel = remember { KoinPlatform.getKoin().get<InsulinViewModel>() }
                        InsulinScreen(viewModel = viewModel)
                    }
                    SharedTab.SETTINGS -> {
                        val viewModel = remember { KoinPlatform.getKoin().get<SettingsViewModel>() }
                        SettingsScreen(viewModel = viewModel)
                    }
                    SharedTab.REMINDERS -> {
                        val viewModel = remember { KoinPlatform.getKoin().get<RemindersViewModel>() }
                        RemindersScreen(viewModel = viewModel)
                    }
                    SharedTab.FITNESS -> {
                        val viewModel = remember { KoinPlatform.getKoin().get<FitnessViewModel>() }
                        FitnessScreen(viewModel = viewModel)
                    }
                    SharedTab.LIBRELINK -> {
                        val viewModel = remember { KoinPlatform.getKoin().get<LibreLinkViewModel>() }
                        LibreLinkScreen(viewModel = viewModel)
                    }
                    SharedTab.MEALS -> {
                        val viewModel = remember { KoinPlatform.getKoin().get<MealsViewModel>() }
                        MealsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

private enum class SharedTab(val label: String) {
    GLUCOSE("Glukoza"),
    STATISTICS("Statistika"),
    INSULIN("Insulin"),
    SETTINGS("Podešavanja"),
    REMINDERS("Podsetnici"),
    FITNESS("Fitnes"),
    LIBRELINK("LibreLinkUp"),
    MEALS("Obroci")
}

@Composable
private fun SharedTabBar(selectedTab: SharedTab, onSelect: (SharedTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .horizontalScroll(rememberScrollState())
    ) {
        SharedTab.entries.forEach { tab ->
            SharedTabItem(
                label = tab.label,
                selected = tab == selectedTab,
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun SharedTabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFF4A7BF6) else Color(0xFF9AA0A6)
        )
    }
}

@Composable
private fun insulinkColorScheme() = lightColorScheme(
    primary = Color(0xFF4A7BF6),
    secondary = Color(0xFF8A5CF5),
    background = Color(0xFFF7F8FC),
    surface = Color.White
)
