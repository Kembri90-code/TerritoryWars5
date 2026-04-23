plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.territorywars"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.territorywars"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API-ключ Яндекс Карт (и в манифест, и в BuildConfig)
        val yandexKey = "aa464463-dc33-4ba3-8537-d4c089f4cc05"
        manifestPlaceholders["YANDEX_MAPKIT_API_KEY"] = yandexKey
        buildConfigField("String", "YANDEX_MAPKIT_API_KEY", "\"$yandexKey\"")

        // URL бэкенда
        buildConfigField("String", "BASE_URL", "\"http://93.183.74.141/api/\"")
        buildConfigField("String", "WS_URL", "\"ws://93.183.74.141\"")

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Yandex MapKit
    implementation(libs.yandex.mapkit)

    // Google Location (Fused Location Provider)
    implementation(libs.play.services.location)

    // Socket.IO
    implementation(libs.socket.io)

    // Coil
    implementation(libs.coil.compose)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Google Fonts (Plus Jakarta Sans, DM Mono)
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Timber
    implementation(libs.timber)


    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
}
