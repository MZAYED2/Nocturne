import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mohamedzayed.nocturne"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mohamedzayed.nocturne"
        minSdk = 24
        targetSdk = 37
        versionCode = providers.gradleProperty("releaseVersionCode").orElse("1006").get().toInt()
        versionName = providers.gradleProperty("releaseVersionName").orElse("1.0.6").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val localProps = Properties()
    val localPropFile = rootProject.file("local.properties")
    if (localPropFile.exists()) {
        localPropFile.inputStream().use { localProps.load(it) }
    }

    val keystoreEnvPath = providers.environmentVariable("NOCTURNE_KEYSTORE_PATH")
        .orElse(providers.environmentVariable("ANDROID_KEYSTORE_PATH")).orNull
        ?: localProps.getProperty("nocturne.keystore.path")
    val keystorePasswordEnv = providers.environmentVariable("NOCTURNE_KEYSTORE_PASSWORD")
        .orElse(providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")).orNull
        ?: localProps.getProperty("nocturne.keystore.password")
    val keyAliasEnv = providers.environmentVariable("NOCTURNE_KEY_ALIAS")
        .orElse(providers.environmentVariable("ANDROID_KEY_ALIAS")).orNull
        ?: localProps.getProperty("nocturne.key.alias")
    val keyPasswordEnv = providers.environmentVariable("NOCTURNE_KEY_PASSWORD")
        .orElse(providers.environmentVariable("ANDROID_KEY_PASSWORD")).orNull
        ?: localProps.getProperty("nocturne.key.password")

    val resolvedStoreFile = if (keystoreEnvPath != null) rootProject.file(keystoreEnvPath) else rootProject.file(".signing/nocturne-release.jks")

    signingConfigs {
        create("release") {
            if (resolvedStoreFile.exists() && !keystorePasswordEnv.isNullOrEmpty() && !keyAliasEnv.isNullOrEmpty() && !keyPasswordEnv.isNullOrEmpty()) {
                storeFile = resolvedStoreFile
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            } else {
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
