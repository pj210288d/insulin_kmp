package com.dj.insulink.shared.feature.friends.di

import com.dj.insulink.shared.feature.friends.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.friends.data.remote.FriendRemoteDataSource
import com.dj.insulink.shared.feature.friends.data.remote.NotImplementedFriendRemoteDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformFriendsModule(): Module = module {
    single { DatabaseFactory() }
    single<FriendRemoteDataSource> { NotImplementedFriendRemoteDataSource() }
}
