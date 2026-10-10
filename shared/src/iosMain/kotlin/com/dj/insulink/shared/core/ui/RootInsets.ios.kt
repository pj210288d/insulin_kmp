package com.dj.insulink.shared.core.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

actual fun Modifier.sharedRootTopInset(): Modifier = this.statusBarsPadding()
