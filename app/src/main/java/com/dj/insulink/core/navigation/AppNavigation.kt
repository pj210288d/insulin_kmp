package com.dj.insulink.core.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.dj.insulink.auth.ui.wrapper.ForgotPasswordWrapper
import com.dj.insulink.auth.ui.wrapper.LoginWrapper
import com.dj.insulink.auth.ui.wrapper.RegistrationWrapper
import com.dj.insulink.core.ui.SideDrawer
import com.dj.insulink.core.ui.SideDrawerParams
import com.dj.insulink.core.ui.viewmodel.SharedViewModel
import com.dj.insulink.core.utils.navigateTo
import com.dj.insulink.feature.meals.ui.wrapper.AddMealWrapper
import com.dj.insulink.feature.meals.ui.wrapper.MealsWrapper
import com.dj.insulink.feature.fitness.ui.wrapper.FitnessWrapper
import com.dj.insulink.feature.friends.ui.wrapper.FriendsWrapper
import com.dj.insulink.feature.glucose.ui.wrapper.GlucoseWrapper
import com.dj.insulink.feature.insulin.ui.wrapper.InsulinWrapper
import com.dj.insulink.feature.reminders.ui.wrapper.RemindersWrapper
import com.dj.insulink.feature.reports.ui.wrapper.ReportsWrapper
import com.dj.insulink.feature.settings.ui.wrapper.SettingsWrapper
import com.dj.insulink.feature.statistics.ui.wrapper.StatisticsWrapper
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current

    val sharedViewModel: SharedViewModel = hiltViewModel()

    val currentUser = sharedViewModel.currentUser.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestinationRoute = currentBackStackEntry?.destination?.route
    val currentDestination = Screen.findDestinationByRoute(currentDestinationRoute)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sharedViewModel.getCurrentUser()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentDestinationRoute in Screen.topBarAndSideDrawerDestinations.map { it.route },
        drawerContent = {
            SideDrawer(
                params = SideDrawerParams(
                    currentUser = currentUser.value,
                    navigateToReminders = {
                        navController.navigateTo(Screen.Reminders.route)
                        coroutineScope.launch { drawerState.close() }
                    },
                    navigateToFriends = {
                        navController.navigateTo(Screen.Friends.route)
                        coroutineScope.launch { drawerState.close() }
                    },
                    navigateToReports = {
                        navController.navigateTo(Screen.Report.route)
                        coroutineScope.launch { drawerState.close() }
                    },
                    navigateToSettings = {
                        navController.navigateTo(Screen.Settings.route)
                        coroutineScope.launch { drawerState.close() }
                    },
                    navigateToInsulinTypes = {
                        navController.navigateTo(Screen.InsulinTypes.route)
                        coroutineScope.launch { drawerState.close() }
                    },
                    navigateToStatistics = {
                        navController.navigateTo(Screen.Statistics.route)
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
                    RegistrationWrapper(
                        fetchUser = {
                            sharedViewModel.getCurrentUser()
                        },
                        navigateToHome = {
                            navController.navigateTo(Screen.Glucose.route)
                        },
                        navigateToLogin = {
                            navController.navigateTo(Screen.Login.route)
                        }
                    )
                }
                composable(Screen.Login.route) {
                    LoginWrapper(
                        fetchUser = {
                            sharedViewModel.getCurrentUser()
                        },
                        navigateToHome = {
                            navController.navigateTo(Screen.Glucose.route)
                        },
                        navigateToRegistration = {
                            navController.navigateTo(Screen.Registration.route)
                        },
                        navigateToForgotPassword = {
                            navController.navigateTo(Screen.ForgotPassword.route)
                        }
                    )
                }
                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordWrapper()
                }
                composable(Screen.Glucose.route) {
                    GlucoseWrapper(currentUser = currentUser.value)
                }
                composable(Screen.Meals.route) {
                    MealsWrapper(
                        currentUser = currentUser.value,
                        navigateToAddMeal = {
                            navController.navigate(Screen.AddMeal.route)
                        }
                    )
                }
                composable(Screen.AddMeal.route) {
                    AddMealWrapper(
                        currentUser = currentUser.value,
                        navigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Fitness.route) {
                    FitnessWrapper(currentUser = currentUser.value)
                }
                composable(Screen.Reminders.route) {
                    RemindersWrapper(currentUser = currentUser.value)
                }
                composable(Screen.Friends.route) {
                    FriendsWrapper(currentUser = currentUser.value)
                }
                composable(Screen.Report.route) {
                    ReportsWrapper(currentUser = currentUser.value)
                }
                composable(Screen.Settings.route) {
                    SettingsWrapper()
                }
                composable(Screen.InsulinTypes.route) {
                    InsulinWrapper(currentUser = currentUser.value)
                }
                composable(Screen.Statistics.route) {
                    StatisticsWrapper()
                }
            }
        }
    }
}
