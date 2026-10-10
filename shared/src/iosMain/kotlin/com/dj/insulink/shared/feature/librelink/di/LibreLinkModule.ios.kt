package com.dj.insulink.shared.feature.librelink.di

import com.dj.insulink.shared.feature.librelink.data.local.IosLibreLinkSessionStorage
import com.dj.insulink.shared.feature.librelink.data.local.LibreLinkSessionStorage
import com.dj.insulink.shared.feature.librelink.data.sync.IosLibreLinkSyncCoordinator
import com.dj.insulink.shared.feature.librelink.data.sync.LibreLinkSyncCoordinator
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformLibreLinkModule(): Module = module {
    single<LibreLinkSessionStorage> { IosLibreLinkSessionStorage() }
    single<LibreLinkSyncCoordinator> { IosLibreLinkSyncCoordinator(get()) }
}
