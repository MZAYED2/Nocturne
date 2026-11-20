plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mohamedzayed.nocturne.wear"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mohamedzayed.nocturne.wear"
        minSdk = 33
        targetSdk = 36
        versionCode = providers.gradleProperty("releaseVersionCode").orElse("1005").get().toInt()
        versionName = providers.gradleProperty("releaseVersionName").orElse("1.0.5").get()
    }

    val keystoreEnvPath = providers.environmentVariable("NOCTURNE_KEYSTORE_PATH")
        .orElse(providers.environmentVariable("ANDROID_KEYSTORE_PATH")).orNull
    val keystorePasswordEnv = providers.environmentVariable("NOCTURNE_KEYSTORE_PASSWORD")
        .orElse(providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")).orNull
    val keyAliasEnv = providers.environmentVariable("NOCTURNE_KEY_ALIAS")
        .orElse(providers.environmentVariable("ANDROID_KEY_ALIAS")).orNull
    val keyPasswordEnv = providers.environmentVariable("NOCTURNE_KEY_PASSWORD")
        .orElse(providers.environmentVariable("ANDROID_KEY_PASSWORD")).orNull
    val localKeystoreFile = rootProject.file(".signing/nocturne-release.jks")

    signingConfigs {
        create("release") {
            if (keystoreEnvPath != null && keystorePasswordEnv != null && keyAliasEnv != null && keyPasswordEnv != null) {
                storeFile = file(keystoreEnvPath)
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            } else if (localKeystoreFile.exists()) {
                storeFile = localKeystoreFile
                storePassword = "NocturneSecretKey2026!"
                keyAlias = "nocturne"
                keyPassword = "NocturneSecretKey2026!"
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
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.play.services.wearable)
    implementation(libs.wear.watchfacepush)
    implementation(libs.kotlinx.coroutines.play.services)
}
