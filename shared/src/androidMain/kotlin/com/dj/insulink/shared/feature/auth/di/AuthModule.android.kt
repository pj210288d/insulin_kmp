package com.dj.insulink.shared.feature.auth.di

import com.dj.insulink.shared.feature.auth.data.FirebaseAuthRepository
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformAuthModule(): Module = module {
    single<AuthRepository> { FirebaseAuthRepository(get(), get()) }
}
