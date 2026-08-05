package com.dj.insulink.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.dj.insulink.wear.data.WearMessageSender
import com.dj.insulink.wear.ui.LatestReadingScreen
import com.dj.insulink.wear.ui.QuickAddScreen
import com.dj.insulink.wear.ui.rememberLatestReadingState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InsulinkWearApp()
        }
    }
}

@Composable
fun InsulinkWearApp() {
    MaterialTheme {
        val navController: NavHostController = rememberSwipeDismissableNavController()
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val latestReading by rememberLatestReadingState()

        SwipeDismissableNavHost(navController = navController, startDestination = Routes.LATEST_READING) {
            composable(Routes.LATEST_READING) {
                LatestReadingScreen(
                    latestReading = latestReading,
                    onAddClick = { navController.navigate(Routes.QUICK_ADD) }
                )
            }
            composable(Routes.QUICK_ADD) {
                QuickAddScreen(
                    initialValueMgDl = latestReading?.value ?: DEFAULT_QUICK_ADD_VALUE,
                    onConfirm = { value ->
                        coroutineScope.launch {
                            WearMessageSender(context).sendQuickAddGlucose(value)
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}

private object Routes {
    const val LATEST_READING = "latest_reading"
    const val QUICK_ADD = "quick_add"
}

private const val DEFAULT_QUICK_ADD_VALUE = 100
