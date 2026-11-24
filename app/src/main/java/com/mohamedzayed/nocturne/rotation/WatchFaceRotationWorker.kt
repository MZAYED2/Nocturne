package com.mohamedzayed.nocturne.rotation

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mohamedzayed.nocturne.data.CatalogRepository
import com.mohamedzayed.nocturne.data.RotationPreferences
import com.mohamedzayed.nocturne.wear.WearBridgeManager
import java.io.File
import java.util.concurrent.TimeUnit

class WatchFaceRotationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val rotPrefs = RotationPreferences(context)
        if (!rotPrefs.enabled) {
            Log.i(TAG, "Automatic rotation is disabled. Skipping.")
            return Result.success()
        }

        // Connectivity Gate: Do not initiate rotation if watch is disconnected
        val bridge = WearBridgeManager.findWatchBridge(context)
        if (bridge == null) {
            Log.w(TAG, "No compatible watch connected. Skipping scheduled rotation safely.")
            return Result.success()
        }

        val catalog = CatalogRepository.loadLocalCatalog(context)
        val eligibleFaces = catalog.filter { rotPrefs.participatingFaces.contains(it.id) }
        if (eligibleFaces.size < 2) {
            Log.w(TAG, "Fewer than 2 participating faces selected. Skipping rotation.")
            return Result.success()
        }

        val slots = WearBridgeManager.querySlots(context, bridge.node.id)
        val activePackage = slots.firstOrNull { it.active }?.packageName
        val currentFace = eligibleFaces.firstOrNull { it.packageName == activePackage }

        val nextFace = if (rotPrefs.order == "random") {
            val candidates = eligibleFaces.filterNot { it.id == currentFace?.id }
            if (candidates.isNotEmpty()) candidates.random() else eligibleFaces.random()
        } else {
            val currentIndex = if (currentFace != null) eligibleFaces.indexOf(currentFace) else -1
            val nextIndex = (currentIndex + 1) % eligibleFaces.size
            eligibleFaces[nextIndex]
        }

        Log.i(TAG, "Selected next face for rotation: ${nextFace.name} (${nextFace.id})")

        // 1. Try fast slot activation if already installed
        val isAlreadyInstalled = slots.any { it.packageName == nextFace.packageName }
        if (isAlreadyInstalled) {
            val actResult = WearBridgeManager.activateSlot(context, bridge, nextFace.packageName)
            if (actResult.isSuccess) {
                rotPrefs.recordSuccessfulRotation(nextFace.id)
                Log.i(TAG, "Rotation successful via fast slot activation: ${nextFace.name}")
                return Result.success()
            }
            Log.w(TAG, "Fast slot activation failed, attempting package push fallback...")
        }

        // 2. Fallback: Push package
        val apkFile = File(context.cacheDir, nextFace.apkFilename)
        if (!WearBridgeManager.isApkFileValid(apkFile)) {
            val dlRes = WearBridgeManager.downloadApkWithValidation(
                urlString = nextFace.downloadUrl,
                destinationFile = apkFile,
                onProgress = { _, _, _ -> }
            )
            if (dlRes.isFailure) {
                Log.e(TAG, "Could not acquire APK for ${nextFace.name}: ${dlRes.exceptionOrNull()?.message}")
                return Result.success() // Return success to avoid WorkManager backoff explosion
            }
        }

        val pushRes = WearBridgeManager.pushWatchFace(
            context = context,
            bridge = bridge,
            apkFile = apkFile,
            packageName = nextFace.packageName,
            token = nextFace.validationToken,
            kind = "wff",
            onProgress = {}
        )

        if (pushRes.optBoolean("success")) {
            rotPrefs.recordSuccessfulRotation(nextFace.id)
            Log.i(TAG, "Rotation successful via package push: ${nextFace.name}")
        } else {
            Log.e(TAG, "Rotation failed during push: ${pushRes.optString("error")}")
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "NocturneRotationWorker"
        private const val WORK_NAME = "nocturne_watchface_rotation"

        fun schedule(context: Context, intervalHours: Int) {
            val safeHours = when (intervalHours) {
                1, 3, 6, 12, 24 -> intervalHours
                else -> 3
            }

            val constraints = Constraints.Builder().build()

            val workRequest = PeriodicWorkRequestBuilder<WatchFaceRotationWorker>(
                safeHours.toLong(),
                TimeUnit.HOURS
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.i(TAG, "Scheduled automatic rotation every $safeHours hours")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled automatic rotation work")
        }
    }
}
