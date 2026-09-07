package com.dj.insulink.shared.feature.friends.di

import com.dj.insulink.shared.feature.friends.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.friends.data.local.FriendsDatabase
import com.dj.insulink.shared.feature.friends.data.local.buildFriendsDatabase
import com.dj.insulink.shared.feature.friends.data.repository.FriendRepository
import com.dj.insulink.shared.feature.friends.ui.viewmodel.FriendsViewModel
import com.dj.insulink.shared.feature.settings.di.settingsModule
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformFriendsModule(): Module

val friendsModule = module {
    includes(platformFriendsModule())
    // settingsModule se ovde eksplicitno uključuje jer FriendsViewModel (deljeni Compose
    // Multiplatform MVP ekran) zavisi od SettingsPreferences - isti obrazac kao glucoseModule.
    includes(settingsModule)
    single<FriendsDatabase> { buildFriendsDatabase(get<DatabaseFactory>().create()) }
    single { get<FriendsDatabase>().friendDao() }
    single { FriendRepository(get(), get()) }
    single { FriendsViewModel(get(), get()) }
}
