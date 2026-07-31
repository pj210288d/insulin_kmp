package com.dj.insulink.core

import android.app.Application
import com.dj.insulink.shared.feature.glucose.di.glucoseModule
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

@HiltAndroidApp
class InsulinkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@InsulinkApplication)
            modules(glucoseModule)
        }
    }
}