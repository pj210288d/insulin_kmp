package com.dj.insulink.shared.core.ui

import androidx.compose.ui.Modifier

// Top inset za shared App() root - vidi actual-e. Android i iOS se razlikuju u tome ko već
// konzumira status bar inset iznad ovog composable-a, pa je ovo namerno expect/actual, ne
// zajednički kod (isti obrazac kao ioDispatcher u core/dispatcher).
expect fun Modifier.sharedRootTopInset(): Modifier
