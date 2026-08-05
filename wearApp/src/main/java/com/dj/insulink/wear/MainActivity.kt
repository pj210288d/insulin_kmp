package com.dj.insulink.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

// Step 1 of the Wear OS companion app: bare scaffolding, no data yet.
// Confirms the module builds, installs, and renders on a Wear OS device/emulator.
// Steps 2-6 (see the "Wear OS companion app" plan) add the Data Layer sync,
// the real "latest reading" screen, quick add, and the tile.
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Insulink")
        }
    }
}
