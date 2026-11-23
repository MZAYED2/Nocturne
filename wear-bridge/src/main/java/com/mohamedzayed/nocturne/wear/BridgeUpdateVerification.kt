package com.mohamedzayed.nocturne.wear

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.io.File

internal fun isValidBridgeUpdate(context: Context, apk: File): Boolean = runCatching {
    val flags = PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_CONFIGURATIONS
    val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
        ?: return false
    val installed = context.packageManager.getPackageInfo(context.packageName, flags)
    val installedSigners = installed.signerSet()
    val requiresWatch = archive.reqFeatures.orEmpty().any {
        it.name == PackageManager.FEATURE_WATCH && it.flags and android.content.pm.FeatureInfo.FLAG_REQUIRED != 0
    }
    archive.packageName == installed.packageName && requiresWatch &&
        (archive.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE) <= android.os.Build.VERSION.SDK_INT &&
        archive.longVersionCode > installed.longVersionCode &&
        installedSigners.isNotEmpty() && archive.signerSet() == installedSigners
}.getOrDefault(false)

private fun PackageInfo.signerSet(): Set<String> =
    signingInfo?.apkContentsSigners.orEmpty().map { it.toCharsString() }.toSet()
