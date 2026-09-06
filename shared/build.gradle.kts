import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    androidLibrary {
       namespace = "org.example.project.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.encoding)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("androidMainImplementation", platform(libs.firebase.bom))
}

// Faza 1 (Auth REST): shared/commonMain treba Firebase project_id + Web API key da bi Ktor
// klijent mogao da pozove Identity Toolkit/Firestore REST API i sa iOS-a (gde google-services
// Gradle plugin, koji ovo generiše samo za androidMain kao Android string resurse, ne postoji).
// Umesto da se ključ hardkoduje u commit-ovan .kt fajl, generiše se ovde iz already-gitignored
// app/google-services.json u build/ (takođe gitignore-ovan) - ključ nikad ne ulazi u git.
// Web API key nije tajna po Firebase-ovoj bezbednosnoj postavci (pristup se ograničava preko
// Firebase Security Rules, ne skrivanjem ključa), ali fajl ostaje gitignore-ovan jer je developer
// tako već odlučio za google-services.json - ovaj task samo poštuje tu odluku umesto da je
// zaobiđe.
val generatedFirebaseConfigDir =
    layout.buildDirectory.dir("generated/source/firebaseConfig/commonMain")

val generateFirebaseConfig by tasks.registering {
    val googleServicesFile = rootProject.file("app/google-services.json")
    val outputDir = generatedFirebaseConfigDir

    inputs.file(googleServicesFile).optional()
    outputs.dir(outputDir)

    doLast {
        val packageDir = outputDir.get().asFile
            .resolve("com/dj/insulink/shared/core/config")
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("FirebaseConfig.kt")

        if (!googleServicesFile.exists()) {
            // Isti preduslov koji Android build već ima (:app:processDebugGoogleServices puca
            // bez ovog fajla) - samo generiši placeholder da commonMain kompajlira, sa jasnom
            // porukom ako se ikad stvarno pozove.
            outputFile.writeText(
                """
                |package com.dj.insulink.shared.core.config
                |
                |// app/google-services.json nije pronađen u trenutku build-a - vidi
                |// :shared:generateFirebaseConfig. Auth REST pozivi će pući dok se fajl ne doda.
                |internal const val FIREBASE_PROJECT_ID = ""
                |internal const val FIREBASE_WEB_API_KEY = ""
                |
                """.trimMargin()
            )
            return@doLast
        }

        val json = groovy.json.JsonSlurper().parse(googleServicesFile) as Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val projectId = (json["project_info"] as Map<String, *>)["project_id"] as String
        @Suppress("UNCHECKED_CAST")
        val firstClient = (json["client"] as List<Map<String, *>>).first()
        @Suppress("UNCHECKED_CAST")
        val apiKey = (firstClient["api_key"] as List<Map<String, *>>).first()["current_key"] as String

        outputFile.writeText(
            """
            |package com.dj.insulink.shared.core.config
            |
            |// Generisano iz app/google-services.json - vidi :shared:generateFirebaseConfig u
            |// shared/build.gradle.kts. NE dirati ručno, NE commit-ovati (leži u build/).
            |internal const val FIREBASE_PROJECT_ID = "$projectId"
            |internal const val FIREBASE_WEB_API_KEY = "$apiKey"
            |
            """.trimMargin()
        )
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateFirebaseConfig.map { generatedFirebaseConfigDir.get() })
}