package com.dj.insulink.core

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.dj.insulink.core.navigation.AppNavigation
import com.dj.insulink.core.ui.theme.InsulinkTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        setContent {
            InsulinkTheme {
                AppNavigation()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("insulink_settings", Context.MODE_PRIVATE)
        val languageKey = prefs.getString("app_language", "en") ?: "en"
        val locale = Locale.forLanguageTag(languageKey)
        Locale.setDefault(locale)

        val config = newBase.resources.configuration
        config.setLocale(locale)

        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}