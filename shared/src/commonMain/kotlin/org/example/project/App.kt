package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.dj.insulink.shared.feature.glucose.ui.GlucoseScreen
import com.dj.insulink.shared.feature.glucose.ui.viewmodel.GlucoseViewModel
import com.dj.insulink.shared.feature.statistics.ui.StatisticsScreen
import com.dj.insulink.shared.feature.statistics.ui.viewmodel.StatisticsViewModel
import org.koin.mp.KoinPlatform

// Root ekran deljen preko Compose Multiplatform-a - koristi ga i iOS (MainViewController.ios.kt
// poziva initKoinIOS() pa ComposeUIViewController { App() }) i Android (SharedGlucoseDemo route
// u :app poziva ovaj isti App()) - vidi CLAUDE.md, faza 4 MVP. Prikazuje dva deljena MVP ekrana
// (Glucose, Statistics - vidi njihove ViewModel-e za obim) iza proste tab-trake, bez prijave
// (vidi UserSession) i bez prave navigacione biblioteke - samo lokalni Compose state, dovoljno
// za dva ekrana. GlucoseViewModel/StatisticsViewModel su Koin single-ovi (vidi
// glucoseModule/statisticsModule), zato se ovde uzimaju direktno preko KoinPlatform-a
// (multiplatform-bezbedan način da se dođe do trenutne Koin instance - obično GlobalContext,
// dostupan samo na JVM/Android strani) umesto Compose-Koin integracije - isti obrazac kao
// postojeći SharedModule.kt most (Hilt -> Koin) na Android strani, samo u suprotnom smeru.
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
                }
            }
        }
    }
}

private enum class SharedTab { GLUCOSE, STATISTICS }

@Composable
private fun SharedTabBar(selectedTab: SharedTab, onSelect: (SharedTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        SharedTabItem(
            label = "Glukoza",
            selected = selectedTab == SharedTab.GLUCOSE,
            onClick = { onSelect(SharedTab.GLUCOSE) },
            modifier = Modifier.weight(1f)
        )
        SharedTabItem(
            label = "Statistika",
            selected = selectedTab == SharedTab.STATISTICS,
            onClick = { onSelect(SharedTab.STATISTICS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SharedTabItem(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
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
