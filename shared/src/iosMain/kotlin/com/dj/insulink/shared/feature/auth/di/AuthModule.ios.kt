package com.dj.insulink.shared.feature.auth.di

import com.dj.insulink.shared.core.auth.IosAuthTokenProvider
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
    // IosAuthTokenProvider je javan (ne private single unutar auth modula) - koriste ga i
    // Faza 2 FirestoreRestXRemoteDataSource actual-i van feature/auth paketa.
    single { IosAuthTokenProvider(get(), get()) }
    single<AuthRepository> { RestAuthRepository(get(), get(), get(), get()) }
}
