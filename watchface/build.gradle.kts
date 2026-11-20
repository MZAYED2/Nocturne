plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mohamedzayed.nocturne.wear.watchfacepush.nocturne"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.mohamedzayed.nocturne.wear.watchfacepush.nocturne"
        minSdk = 34
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
            optimization {
                enable = false
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/refinement/res").get().asFile)
    enableKotlin = false
}

apply(from = "refinement-assets.gradle.kts")

dependencies {
}
