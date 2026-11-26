package com.mohamedzayed.nocturne.wear

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.wear.watchfacepush.WatchFacePushManagerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

internal val watchFaceInstallLock = Mutex()

data class InstallResult(
    val slotId: String,
    val isActive: Boolean
)

internal suspend fun installWatchFace(
    context: Context,
    apk: File,
    packageName: String,
    token: String,
    requestId: String,
    onStage: (String) -> Unit
): InstallResult {
    require(packageName.startsWith("${context.packageName}.watchfacepush.")) { "اسم حزمة الواجهة غير مدعوم" }
    val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
    require(archive?.packageName == packageName) { "هوية APK لا تطابق الواجهة المطلوبة" }
    require(token.isNotBlank()) { "رمز تحقق Watch Face Push مفقود" }
    onStage("INSTALL")

    return watchFaceInstallLock.withLock {
        val manager = WatchFacePushManagerFactory.createWatchFacePushManager(context.applicationContext)
        val slots = manager.listWatchFaces()
        val installed = slots.installedWatchFaceDetails
        val existing = installed.firstOrNull { it.packageName == packageName }
        val activeSlot = installed.firstOrNull {
            try { manager.isWatchFaceActive(it.packageName) } catch (_: Exception) { false }
        }
        val replacement = existing ?: activeSlot ?: installed.firstOrNull().takeIf { slots.remainingSlotCount == 0 }

        val slotId = ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            if (replacement != null) {
                manager.updateWatchFace(replacement.slotId, descriptor, token)
                replacement.slotId
            } else {
                check(slots.remainingSlotCount > 0) { "لا توجد خانة متاحة لواجهة Nocturne" }
                manager.addWatchFace(descriptor, token).slotId
            }
        }
        check(manager.listWatchFaces().installedWatchFaceDetails.any {
            it.slotId == slotId && it.packageName == packageName
        }) { "لم يؤكد النظام تثبيت الواجهة في الخانة المطلوبة" }
        Log.i("NocturneBridge", "Installed $requestId $packageName slot=$slotId")

        onStage("ACTIVATE")
        var isActive = try {
            manager.isWatchFaceActive(packageName)
        } catch (_: Exception) { false }

        if (!isActive) {
            try {
                manager.setWatchFaceAsActive(slotId)
            } catch (e: Exception) {
                Log.w("NocturneBridge", "setWatchFaceAsActive note: ${e.message}")
            }
            isActive = withTimeoutOrNull(4000) {
                while (!manager.isWatchFaceActive(packageName)) delay(250)
                true
            } == true
        }

        Log.i("NocturneBridge", "Active $requestId $packageName slot=$slotId isActive=$isActive")
        onStage("COMPLETE")
        InstallResult(slotId, isActive)
    }
}
