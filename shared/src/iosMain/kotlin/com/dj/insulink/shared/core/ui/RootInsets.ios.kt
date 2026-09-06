package com.dj.insulink.shared.core.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

// iOS root (MainViewController.kt) nema nikakav app bar/Scaffold iznad App() - bez ovoga tab
// traka sedi direktno ispod sistemskog status bara i preklapa se sa satom/baterijom (viđeno na
// prvom Xcode/simulator screenshot-u, 2026-09-06). Android ne treba ovo - vidi actual u
// androidMain, već ima Scaffold innerPadding iznad ovog mesta.
actual fun Modifier.sharedRootTopInset(): Modifier = this.statusBarsPadding()
