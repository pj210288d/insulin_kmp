package org.example.project

import androidx.compose.ui.window.ComposeUIViewController
import com.dj.insulink.shared.core.di.initKoinIOS
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoinIOS()
    return ComposeUIViewController { App() }
}
