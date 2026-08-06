plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Standalone Wear OS companion app. Deliberately does NOT depend on :shared or Firebase —
// it talks only to the phone app over the Wearable Data Layer API (DataClient/MessageClient),
// which already has an authenticated session and does all Room/Firestore work on its behalf.
// See the "Wear OS companion app" plan for the full rationale.
//
// applicationId MUST match :app's (com.dj.insulink), NOT just be similarly-named - the Data
// Layer API (DataClient/MessageClient) only syncs between a phone app and watch app that share
// the same applicationId and signing certificate. This is unrelated to `namespace` below, which
// only controls the Kotlin/R-class package and stays com.dj.insulink.wear so all of this
// module's source files keep their own distinct package. Confirmed against Android's own
// "Data Layer API" docs after this exact mismatch caused pushed DataItems to silently never
// reach a real device in testing (connected node present, push "succeeded" locally, nothing
// ever arrived on the watch).
android {
    namespace = "com.dj.insulink.wear"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dj.insulink"
        minSdk = libs.versions.android.minSdkWear.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)

    // Wearable Data Layer API (DataClient/MessageClient)
    implementation(libs.play.services.wearable)

    // Tile (step 6)
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material)
    implementation(libs.kotlinx.coroutines.guava)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
