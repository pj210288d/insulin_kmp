package com.dj.insulink.shared.feature.auth.di

import com.dj.insulink.shared.feature.auth.ui.viewmodel.AuthViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformAuthModule(): Module

val authModule = module {
    includes(platformAuthModule())
    single { AuthViewModel(get()) }
}
