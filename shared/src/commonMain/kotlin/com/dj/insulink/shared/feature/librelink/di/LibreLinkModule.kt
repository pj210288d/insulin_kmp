package com.dj.insulink.shared.feature.librelink.di

import com.dj.insulink.shared.feature.librelink.data.remote.KtorLibreLinkApiClient
import com.dj.insulink.shared.feature.librelink.data.remote.LibreLinkApiClient
import com.dj.insulink.shared.feature.librelink.data.remote.createHttpClient
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.librelink.ui.viewmodel.LibreLinkViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformLibreLinkModule(): Module

val librelinkModule = module {
    includes(platformLibreLinkModule())
    single { createHttpClient() }
    single<LibreLinkApiClient> { KtorLibreLinkApiClient(get()) }
    single { LibreLinkRepository(get(), get(), get()) }
    single { LibreLinkViewModel(get()) }
}
