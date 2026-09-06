package com.dj.insulink.shared.core.ui

import androidx.compose.ui.Modifier

// Android-ov "Shared UI" side-drawer ekran (SharedGlucoseDemo ruta u AppNavigation.kt) prikazuje
// App() unutar Scaffold-a čiji se innerPadding već prosleđuje NavHost-u preko
// Modifier.padding(innerPadding) - status bar je već konzumiran gore, pre nego što stigne do
// ovog composable-a. No-op ovde, da se ne udvostruči taj padding.
actual fun Modifier.sharedRootTopInset(): Modifier = this
