package com.dj.insulink.core.navigation

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dj.insulink.R
import com.dj.insulink.core.ui.viewmodel.SharedViewModel
import com.dj.insulink.core.utils.navigateTo
import com.dj.insulink.feature.librelink.ui.LibreLinkHelpScreen
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
import com.dj.insulink.shared.core.ui.SideDrawer
import com.dj.insulink.shared.core.ui.SideDrawerDestination
import com.dj.insulink.shared.core.ui.SideDrawerParams
import com.dj.insulink.shared.feature.auth.ui.ForgotPasswordScreen
import com.dj.insulink.shared.feature.auth.ui.LoginScreen
import com.dj.insulink.shared.feature.auth.ui.RegistrationScreen
import com.dj.insulink.shared.feature.auth.ui.viewmodel.AuthViewModel
import com.dj.insulink.shared.feature.meals.ui.MealsScreen
import com.dj.insulink.shared.feature.meals.ui.viewmodel.MealsViewModel
import com.dj.insulink.shared.feature.reminders.ui.RemindersScreen
import com.dj.insulink.shared.feature.reminders.ui.viewmodel.RemindersViewModel
import com.dj.insulink.shared.feature.reports.ui.ReportsScreen
import com.dj.insulink.shared.feature.reports.ui.viewmodel.ReportsViewModel
import com.dj.insulink.shared.feature.settings.ui.SettingsScreen
import com.dj.insulink.shared.feature.settings.ui.viewmodel.SettingsViewModel
import com.dj.insulink.shared.feature.statistics.ui.StatisticsScreen
import com.dj.insulink.shared.feature.statistics.ui.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current

    val sharedViewModel: SharedViewModel = hiltViewModel()

    val currentUser = sharedViewModel.currentUser.collectAsStateWithLifecycle()

    val authViewModel = remember { KoinPlatform.getKoin().get<AuthViewModel>() }
    val authIsLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val authErrorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val authInfoMessage by authViewModel.infoMessage.collectAsStateWithLifecycle()
    val authCurrentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestinationRoute = currentBackStackEntry?.destination?.route
    val currentDestination = Screen.findDestinationByRoute(currentDestinationRoute)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sharedViewModel.getCurrentUser()
    }

    var hasHandledInitialAuthState by remember { mutableStateOf(false) }
    LaunchedEffect(authCurrentUser) {
        if (!hasHandledInitialAuthState) {
            hasHandledInitialAuthState = true
            return@LaunchedEffect
        }
        if (authCurrentUser != null) {
            sharedViewModel.getCurrentUser()
            navController.navigateTo(Screen.Glucose.route)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentDestinationRoute in Screen.topBarAndSideDrawerDestinations.map { it.route },
        drawerContent = {
            ModalDrawerSheet {
                SideDrawer(
                    params = SideDrawerParams(
                        userDisplayName = "${currentUser.value?.firstName.orEmpty()} ${currentUser.value?.lastName.orEmpty()}",
                        userEmail = currentUser.value?.email.orEmpty(),
                        items = SideDrawerDestination.entries.toList(),
                        selected = currentDestinationRoute.toSideDrawerDestinationOrNull(),
                        onNavigate = { destination ->
                            navController.navigateTo(destination.toScreenRoute())
                            coroutineScope.launch { drawerState.close() }
                        },
                        onSignOutClick = {
                            sharedViewModel.signOut(context)
                            navController.navigateTo(Screen.Login.route)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentDestinationRoute in Screen.topBarAndSideDrawerDestinations.map { it.route }) {
                    CenterAlignedTopAppBar(
                        title = {
                            currentDestination?.titleRes?.takeIf { it != 0 }?.let {
                                Text(
                                    text = stringResource(it),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_menu),
                                    tint = Color(0xFFB2B2B2),
                                    contentDescription = ""
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (currentDestinationRoute in Screen.topBarAndSideDrawerDestinations.map { it.route }) {
                    NavigationBar {
                        Screen.bottomBarDestinations.forEach { destination ->
                            destination.icon?.let {
                                NavigationBarItem(
                                    selected = currentDestinationRoute == destination.route,
                                    label = {
                                        Text(text = if (destination.titleRes != 0) stringResource(destination.titleRes) else "")
                                    },
                                    icon = {
                                        Icon(imageVector = it, contentDescription = "")
                                    },
                                    onClick = {
                                        navController.navigateTo(destination.route)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (sharedViewModel.isUserLoggedIn()) Screen.Glucose.route else Screen.Login.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Registration.route) {
                    RegistrationScreen(
                        viewModel = authViewModel,
                        isLoading = authIsLoading,
                        errorMessage = authErrorMessage,
                        onNavigateToLogin = { navController.navigateTo(Screen.Login.route) }
                    )
                }
                composable(Screen.Login.route) {
                    LoginScreen(
                        viewModel = authViewModel,
                        isLoading = authIsLoading,
                        errorMessage = authErrorMessage,
                        onNavigateToRegister = { navController.navigateTo(Screen.Registration.route) },
                        onNavigateToForgotPassword = { navController.navigateTo(Screen.ForgotPassword.route) }
                    )
                }
                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        viewModel = authViewModel,
                        isLoading = authIsLoading,
                        errorMessage = authErrorMessage,
                        infoMessage = authInfoMessage,
                        onNavigateBack = { navController.navigateTo(Screen.Login.route) }
                    )
                }
                composable(Screen.Glucose.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<GlucoseViewModel>() }
                    GlucoseScreen(viewModel = viewModel)
                }
                composable(Screen.Meals.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<MealsViewModel>() }
                    MealsScreen(viewModel = viewModel)
                }
                composable(Screen.Fitness.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<FitnessViewModel>() }
                    FitnessScreen(viewModel = viewModel)
                }
                composable(Screen.Reminders.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<RemindersViewModel>() }
                    RemindersScreen(viewModel = viewModel)
                }
                composable(Screen.Friends.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<FriendsViewModel>() }
                    FriendsScreen(viewModel = viewModel)
                }
                composable(Screen.Report.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<ReportsViewModel>() }
                    ReportsScreen(viewModel = viewModel)
                }
                composable(Screen.Settings.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<SettingsViewModel>() }
                    val activity = context as Activity
                    SettingsScreen(
                        viewModel = viewModel,
                        onLanguageChanged = { activity.recreate() }
                    )
                }
                composable(Screen.InsulinTypes.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<InsulinViewModel>() }
                    InsulinScreen(viewModel = viewModel)
                }
                composable(Screen.Statistics.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<StatisticsViewModel>() }
                    StatisticsScreen(viewModel = viewModel)
                }
                composable(Screen.LibreLink.route) {
                    val viewModel = remember { KoinPlatform.getKoin().get<LibreLinkViewModel>() }
                    LibreLinkScreen(
                        viewModel = viewModel,
                        onHelpClick = { navController.navigateTo(Screen.LibreLinkHelp.route) }
                    )
                }
                composable(Screen.LibreLinkHelp.route) {
                    LibreLinkHelpScreen(onClose = { navController.popBackStack() })
                }
            }
        }
    }
}

private fun SideDrawerDestination.toScreenRoute(): String = when (this) {
    SideDrawerDestination.REMINDERS -> Screen.Reminders.route
    SideDrawerDestination.REPORTS -> Screen.Report.route
    SideDrawerDestination.FRIENDS -> Screen.Friends.route
    SideDrawerDestination.STATISTICS -> Screen.Statistics.route
    SideDrawerDestination.INSULIN_TYPES -> Screen.InsulinTypes.route
    SideDrawerDestination.LIBRELINK -> Screen.LibreLink.route
    SideDrawerDestination.SETTINGS -> Screen.Settings.route
}

private fun String?.toSideDrawerDestinationOrNull(): SideDrawerDestination? = when (this) {
    Screen.Reminders.route -> SideDrawerDestination.REMINDERS
    Screen.Report.route -> SideDrawerDestination.REPORTS
    Screen.Friends.route -> SideDrawerDestination.FRIENDS
    Screen.Statistics.route -> SideDrawerDestination.STATISTICS
    Screen.InsulinTypes.route -> SideDrawerDestination.INSULIN_TYPES
    Screen.LibreLink.route -> SideDrawerDestination.LIBRELINK
    Screen.Settings.route -> SideDrawerDestination.SETTINGS
    else -> null
}
