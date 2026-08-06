package com.dj.insulink.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.dj.insulink.R

sealed class Screen(
    val route: String,
    val icon: ImageVector? = null,
    @StringRes val titleRes: Int = 0
) {
    object Registration : Screen("registration")
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")
    object Glucose : Screen("glucose", icon = Icons.Filled.WaterDrop, titleRes = R.string.nav_title_glucose)
    object Meals : Screen("meals", icon = Icons.Filled.Restaurant, titleRes = R.string.nav_title_meals)
    object AddMeal : Screen("add_meal")
    object Fitness : Screen("fitness", icon = Icons.AutoMirrored.Filled.DirectionsRun, titleRes = R.string.nav_title_fitness)
    object Reminders : Screen("reminders", titleRes = R.string.nav_title_reminders)
    object Friends : Screen("friends", titleRes = R.string.nav_title_friends)
    object Report : Screen("report", titleRes = R.string.nav_title_report)
    object Settings : Screen("settings", titleRes = R.string.nav_title_settings)
    object InsulinTypes : Screen("insulin_types", titleRes = R.string.nav_title_insulin_types)

    companion object {
        val allDestinations: List<Screen> = listOf(
            Registration,
            Login,
            ForgotPassword,
            Glucose,
            Meals,
            AddMeal,
            Fitness,
            Reminders,
            Friends,
            Report,
            Settings,
            InsulinTypes
        )

        val bottomBarDestinations: List<Screen> = listOf(
            Meals,
            Glucose,
            Fitness
        )

        val topBarAndSideDrawerDestinations: List<Screen> = bottomBarDestinations + listOf(
            Reminders,
            Friends,
            Report,
            Settings,
            InsulinTypes
        )

        fun findDestinationByRoute(route: String?): Screen? {
            return allDestinations.find { it.route == route }
        }
    }
}