package com.territorywars

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TerritoryWarsApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализация Timber (логи только в debug-сборке)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Инициализация Яндекс MapKit
        MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
    }
}
