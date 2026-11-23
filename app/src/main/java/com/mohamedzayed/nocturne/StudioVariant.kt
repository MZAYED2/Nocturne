package com.mohamedzayed.nocturne

import android.content.Context
import org.json.JSONObject

private const val SAGE_NAVY_PACKAGE = "com.mohamedzayed.nocturne.watchfacepush.sagenavy"
private const val SAGE_NAVY_APK = "Nocturne-Sage-Navy-Push.apk"

internal fun resolveStudioVariant(context: Context, master: WatchFaceItem, preset: ThemePreset): WatchFaceItem {
    require(master.id == preset.parent && master.id == "sage_nocturne") {
        "This master has no validated Studio package."
    }
    require(preset.settings.copy(aodPreview = false) == DialSettings(background = "navy")) {
        "Choose Navy background and retain the original dial, ornaments and palette."
    }
    val manifest = try {
        context.assets.open("watchfaces/Nocturne-Sage-Navy.json").bufferedReader().use {
            JSONObject(it.readText())
        }
    } catch (e: java.io.FileNotFoundException) {
        throw IllegalStateException("This build does not include the validated Sage Navy package.", e)
    }
    require(manifest.getString("packageName") == SAGE_NAVY_PACKAGE && manifest.getString("apk") == SAGE_NAVY_APK)
    val checksum = manifest.getString("sha256")
    val bytes = manifest.getLong("bytes")
    val token = manifest.getString("token")
    require(checksum.matches(Regex("[0-9a-f]{64}")) && bytes > 0 && token.isNotBlank())
    return master.copy(
        name = preset.name,
        nameAr = preset.name,
        packageName = SAGE_NAVY_PACKAGE,
        apkFilename = SAGE_NAVY_APK,
        validationToken = token,
        downloadUrl = "",
        sha256 = checksum,
        sizeBytes = bytes
    )
}
