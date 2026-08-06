package com.dj.insulink.wear.ui

import androidx.compose.ui.graphics.Color

// Plain color constants, independent of the phone app's InsulinkTheme (wearApp deliberately
// doesn't depend on :app or :shared UI code - see the "Wear OS companion app" plan).
object WearColors {
    val Low = Color(0xFFEF5350)
    val Normal = Color(0xFF66BB6A)
    val High = Color(0xFFFFA726)
}
