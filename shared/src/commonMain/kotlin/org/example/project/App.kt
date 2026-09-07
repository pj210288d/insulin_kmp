package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.session.SettingsSession
import com.dj.insulink.shared.core.ui.sharedRootTopInset
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.ui.ForgotPasswordScreen
import com.dj.insulink.shared.feature.auth.ui.LoginScreen
import com.dj.insulink.shared.feature.auth.ui.RegistrationScreen
import com.dj.insulink.shared.feature.auth.ui.viewmodel.AuthViewModel
import com.dj.insulink.shared.feature.fitness.ui.FitnessScreen
import com.dj.insulink.shared.feature.fitness.ui.viewmodel.FitnessViewModel
import com.dj.insulink.shared.feature.friends.ui.FriendsScreen
import com.dj.insulink.shared.feature.friends.ui.viewmodel.FriendsViewModel
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
import com.dj.insulink.shared.feature.reports.ui.ReportsScreen
import com.dj.insulink.shared.feature.reports.ui.viewmodel.ReportsViewModel
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.ui.SettingsScreen
import com.dj.insulink.shared.feature.settings.ui.viewmodel.SettingsViewModel
import com.dj.insulink.shared.feature.statistics.ui.StatisticsScreen
import com.dj.insulink.shared.feature.statistics.ui.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

// Root ekran deljen preko Compose Multiplatform-a - koristi ga i iOS (MainViewController.ios.kt
// poziva initKoinIOS() pa ComposeUIViewController { App() }) i Android (SharedGlucoseDemo route
// u :app poziva ovaj isti App()) - vidi CLAUDE.md, faza 4 MVP.
//
// 2026-09-07: navigaciona ljuska prepravljena da vizuelno/strukturno prati PRAVI Android
// AppNavigation.kt 1:1 (na eksplicitan zahtev korisnika) - `ModalNavigationDrawer` +
// `CenterAlignedTopAppBar` (hamburger levo, naslov po sredini) + `NavigationBar` na dnu, umesto
// prethodne proste horizontalno-skrolabilne tab-trake. Bottom bar ima tačno Android-ov
// `Screen.bottomBarDestinations`: Obroci, Glukoza, Fitnes. Sidebar ima na vrhu ime/prezime +
// email (isto kao Android-ov SideDrawer.kt), pa Android-ov `SideDrawer.kt` redosled: Podsetnici,
// Prijatelji, Izveštaji, Podešavanja - plus Insulin/Statistika/LibreLinkUp koji na Android-u ISTO
// žive u sidebaru (SideDrawer.kt ima i njih, `navigateToInsulinTypes`/`navigateToStatistics`) i
// koji već postoje kao deljeni ekrani - namerno NISU izbačeni iz navigacije da se ne izgubi
// postojeća funkcionalnost, samo su na sidebaru posle prve četiri (korisnik ih nije pomenuo, ali
// "ništa što je radilo ne sme da prestane da radi" iz Faze 1 plana i dalje važi). Bez ikonica
// (Icons.Filled.*, isti razlog kao ostatak deljenog UI-ja) - "icon" slot u NavigationBarItem/
// NavigationDrawerItem je prost emoji Text, ne vector ikonica.
//
// Faza 1 (Auth, vidi plan): pre svega ovoga se nalazi pravi login gate - AuthViewModel.
// restoreSession() se zove jednom pri prvoj kompoziciji; dok traje, prikazuje se spinner; ako
// nema sesije, prikazuju se shared Login/Registration/ForgotPassword ekrani (AuthScreen enum
// ispod); tek kad AuthSession.currentUser nije null prikazuje se navigaciona ljuska iznad.
@Composable
fun App() {
    MaterialTheme(colorScheme = insulinkColorScheme()) {
        val authViewModel = remember { KoinPlatform.getKoin().get<AuthViewModel>() }
        val isRestoringSession by authViewModel.isRestoringSession.collectAsState()
        val currentUser by authViewModel.currentUser.collectAsState()

        LaunchedEffect(Unit) { authViewModel.restoreSession() }
        // Vidi LocalizationSession/SettingsSession - nav labele (bottom bar/sidebar) i jedinica
        // za glukozu treba odmah da odražavaju već perzistiran izbor, i pre nego što korisnik
        // ikad otvori Settings tab ove sesije.
        LaunchedEffect(Unit) {
            val settingsPreferences = KoinPlatform.getKoin().get<SettingsPreferences>()
            LocalizationSession.restoreFrom(settingsPreferences)
            SettingsSession.restoreFrom(settingsPreferences)
        }

        val user = currentUser
        when {
            isRestoringSession -> LoadingScreen()
            user == null -> AuthFlow(authViewModel)
            else -> MainTabs(authViewModel, user)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize().sharedRootTopInset(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private enum class AuthScreen { LOGIN, REGISTER, FORGOT_PASSWORD }

@Composable
private fun AuthFlow(authViewModel: AuthViewModel) {
    var screen by remember { mutableStateOf(AuthScreen.LOGIN) }
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val infoMessage by authViewModel.infoMessage.collectAsState()

    fun navigate(target: AuthScreen) {
        authViewModel.clearMessages()
        screen = target
    }

    Box(modifier = Modifier.fillMaxSize().sharedRootTopInset()) {
        when (screen) {
            AuthScreen.LOGIN -> LoginScreen(
                viewModel = authViewModel,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onNavigateToRegister = { navigate(AuthScreen.REGISTER) },
                onNavigateToForgotPassword = { navigate(AuthScreen.FORGOT_PASSWORD) }
            )
            AuthScreen.REGISTER -> RegistrationScreen(
                viewModel = authViewModel,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onNavigateToLogin = { navigate(AuthScreen.LOGIN) }
            )
            AuthScreen.FORGOT_PASSWORD -> ForgotPasswordScreen(
                viewModel = authViewModel,
                isLoading = isLoading,
                errorMessage = errorMessage,
                infoMessage = infoMessage,
                onNavigateBack = { navigate(AuthScreen.LOGIN) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabs(authViewModel: AuthViewModel, currentUser: AuthUser) {
    var currentDestination by remember { mutableStateOf(AppDestination.GLUCOSE) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val language by LocalizationSession.currentLanguage.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                SideDrawerContent(
                    currentUser = currentUser,
                    selected = currentDestination,
                    language = language,
                    onNavigate = { destination ->
                        currentDestination = destination
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSignOut = {
                        coroutineScope.launch { drawerState.close() }
                        authViewModel.signOut()
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize().sharedRootTopInset()
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(currentDestination.label(language)) },
                    navigationIcon = {
                        TextButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Text("☰", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(
                    current = currentDestination,
                    language = language,
                    onSelect = { currentDestination = it }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                ScreenContent(currentDestination)
            }
        }
    }
}

@Composable
private fun ScreenContent(destination: AppDestination) {
    when (destination) {
        AppDestination.MEALS -> {
            val viewModel = remember { KoinPlatform.getKoin().get<MealsViewModel>() }
            MealsScreen(viewModel = viewModel)
        }
        AppDestination.GLUCOSE -> {
            val viewModel = remember { KoinPlatform.getKoin().get<GlucoseViewModel>() }
            GlucoseScreen(viewModel = viewModel)
        }
        AppDestination.FITNESS -> {
            val viewModel = remember { KoinPlatform.getKoin().get<FitnessViewModel>() }
            FitnessScreen(viewModel = viewModel)
        }
        AppDestination.REMINDERS -> {
            val viewModel = remember { KoinPlatform.getKoin().get<RemindersViewModel>() }
            RemindersScreen(viewModel = viewModel)
        }
        AppDestination.FRIENDS -> {
            val viewModel = remember { KoinPlatform.getKoin().get<FriendsViewModel>() }
            FriendsScreen(viewModel = viewModel)
        }
        AppDestination.REPORTS -> {
            val viewModel = remember { KoinPlatform.getKoin().get<ReportsViewModel>() }
            ReportsScreen(viewModel = viewModel)
        }
        AppDestination.SETTINGS -> {
            val viewModel = remember { KoinPlatform.getKoin().get<SettingsViewModel>() }
            SettingsScreen(viewModel = viewModel)
        }
        AppDestination.INSULIN_TYPES -> {
            val viewModel = remember { KoinPlatform.getKoin().get<InsulinViewModel>() }
            InsulinScreen(viewModel = viewModel)
        }
        AppDestination.STATISTICS -> {
            val viewModel = remember { KoinPlatform.getKoin().get<StatisticsViewModel>() }
            StatisticsScreen(viewModel = viewModel)
        }
        AppDestination.LIBRELINK -> {
            val viewModel = remember { KoinPlatform.getKoin().get<LibreLinkViewModel>() }
            LibreLinkScreen(viewModel = viewModel)
        }
    }
}

@Composable
private fun BottomNavBar(current: AppDestination, language: AppLanguage, onSelect: (AppDestination) -> Unit) {
    NavigationBar {
        BOTTOM_BAR_DESTINATIONS.forEach { destination ->
            NavigationBarItem(
                selected = destination == current,
                onClick = { onSelect(destination) },
                icon = { Text(destination.emoji) },
                label = { Text(destination.label(language)) }
            )
        }
    }
}

@Composable
private fun SideDrawerContent(
    currentUser: AuthUser,
    selected: AppDestination,
    language: AppLanguage,
    onNavigate: (AppDestination) -> Unit,
    onSignOut: () -> Unit
) {
    val isEnglish = language == AppLanguage.ENGLISH
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(
                text = "${currentUser.firstName} ${currentUser.lastName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = currentUser.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        DRAWER_DESTINATIONS.forEach { destination ->
            NavigationDrawerItem(
                label = { Text(destination.label(language)) },
                icon = { Text(destination.emoji) },
                selected = destination == selected,
                onClick = { onNavigate(destination) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            Text(if (isEnglish) "Sign out" else "Odjava", color = MaterialTheme.colorScheme.error)
        }
    }
}

// Isti spisak i redosled kao Android-ov Screen.kt/SideDrawer.kt - vidi komentar iznad App().
// `label(language)` - vidi LocalizationSession za obim promene jezika (samo navigacija +
// Settings ekran, odluka korisnika 2026-09-07).
private enum class AppDestination(val emoji: String) {
    MEALS("🍽"),
    GLUCOSE("💧"),
    FITNESS("🏃"),
    REMINDERS("⏰"),
    FRIENDS("👥"),
    REPORTS("📄"),
    SETTINGS("⚙️"),
    INSULIN_TYPES("💉"),
    STATISTICS("📊"),
    LIBRELINK("📡");

    fun label(language: AppLanguage): String {
        val english = language == AppLanguage.ENGLISH
        return when (this) {
            MEALS -> if (english) "Meals" else "Obroci"
            GLUCOSE -> if (english) "Glucose" else "Glukoza"
            FITNESS -> if (english) "Fitness" else "Fitnes"
            REMINDERS -> if (english) "Reminders" else "Podsetnici"
            FRIENDS -> if (english) "Friends" else "Prijatelji"
            REPORTS -> if (english) "Reports" else "Izveštaji"
            SETTINGS -> if (english) "Settings" else "Podešavanja"
            INSULIN_TYPES -> "Insulin"
            STATISTICS -> if (english) "Statistics" else "Statistika"
            LIBRELINK -> "LibreLinkUp"
        }
    }
}

private val BOTTOM_BAR_DESTINATIONS = listOf(
    AppDestination.MEALS,
    AppDestination.GLUCOSE,
    AppDestination.FITNESS
)

private val DRAWER_DESTINATIONS = listOf(
    AppDestination.REMINDERS,
    AppDestination.FRIENDS,
    AppDestination.REPORTS,
    AppDestination.SETTINGS,
    AppDestination.INSULIN_TYPES,
    AppDestination.STATISTICS,
    AppDestination.LIBRELINK
)

@Composable
private fun insulinkColorScheme() = lightColorScheme(
    primary = Color(0xFF4A7BF6),
    secondary = Color(0xFF8A5CF5),
    background = Color(0xFFF7F8FC),
    surface = Color.White
)
