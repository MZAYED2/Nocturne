import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val generatedBridgeAssets = layout.buildDirectory.dir("generated/watch-assets")
val stageBridgeAssets = tasks.register<Sync>("stageBridgeAssets") {
    from(rootProject.layout.projectDirectory.dir("distribution")) {
        include("Nocturne-Emerald-Push.apk", "catalog.json")
    }
    into(generatedBridgeAssets)
}
tasks.named("preBuild").configure { dependsOn(stageBridgeAssets) }

android {
    sourceSets.named("debug") { assets.directories.add(generatedBridgeAssets.get().asFile.path) }
    sourceSets.named("release") { assets.directories.add(generatedBridgeAssets.get().asFile.path) }
    namespace = "com.mohamedzayed.nocturne.wear"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mohamedzayed.nocturne"
        minSdk = 33
        targetSdk = 36
        versionCode = providers.gradleProperty("releaseVersionCode").orElse("1006").get().toInt()
        versionName = providers.gradleProperty("releaseVersionName").orElse("1.0.6").get()
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

    val hasReleaseSigning = resolvedStoreFile.exists() && !keystorePasswordEnv.isNullOrEmpty() &&
        !keyAliasEnv.isNullOrEmpty() && !keyPasswordEnv.isNullOrEmpty()

    val requireReleaseSigning = tasks.register("requireReleaseSigning") {
        inputs.property("releaseSigningConfigured", hasReleaseSigning)
        doLast {
            check(inputs.properties["releaseSigningConfigured"] == true) {
                "Configure the permanent Nocturne signing key before building a release APK."
            }
        }
    }
    tasks.matching { it.name == "preReleaseBuild" }.configureEach {
        dependsOn(requireReleaseSigning)
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
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
