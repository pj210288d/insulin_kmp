package com.dj.insulink.shared.feature.friends.di

import com.dj.insulink.shared.feature.friends.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.friends.data.remote.FirebaseFriendRemoteDataSource
import com.dj.insulink.shared.feature.friends.data.remote.FriendRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformFriendsModule(): Module = module {
    single { DatabaseFactory(androidContext()) }
    single<FriendRemoteDataSource> { FirebaseFriendRemoteDataSource(get()) }
}
