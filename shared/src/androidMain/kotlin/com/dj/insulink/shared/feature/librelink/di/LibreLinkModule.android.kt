package com.dj.insulink.shared.feature.librelink.di

import com.dj.insulink.shared.feature.librelink.data.local.AndroidLibreLinkSessionStorage
import com.dj.insulink.shared.feature.librelink.data.local.LibreLinkSessionStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformLibreLinkModule(): Module = module {
    single<LibreLinkSessionStorage> { AndroidLibreLinkSessionStorage(androidContext()) }
}
