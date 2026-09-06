package com.dj.insulink.shared.feature.auth.di

import com.dj.insulink.shared.core.firestore.FirestoreRestClient
import com.dj.insulink.shared.feature.auth.data.RestAuthRepository
import com.dj.insulink.shared.feature.auth.data.local.AuthTokenStorage
import com.dj.insulink.shared.feature.auth.data.remote.FirebaseAuthRestClient
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformAuthModule(): Module = module {
    single { FirebaseAuthRestClient() }
    single { FirestoreRestClient() }
    single { AuthTokenStorage() }
    single<AuthRepository> { RestAuthRepository(get(), get(), get()) }
}
