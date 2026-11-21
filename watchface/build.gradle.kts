import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mohamedzayed.nocturne.watchfacepush.nocturne"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = providers.gradleProperty("wffPackageName").orElse("com.mohamedzayed.nocturne.watchfacepush.nocturne").get()
        minSdk = 34
        targetSdk = 36
        versionCode = providers.gradleProperty("wffVersionCode").orElse(providers.gradleProperty("releaseVersionCode").orElse("1007")).get().toInt()
        versionName = providers.gradleProperty("wffVersionName").orElse(providers.gradleProperty("releaseVersionName").orElse("1.0.7")).get()
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
