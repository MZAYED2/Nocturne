package com.mohamedzayed.nocturne.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mohamedzayed.nocturne.MainActivity
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = UpdatePreferences(context)
        if (!prefs.autoCheck) {
            Log.i(TAG, "Auto-check is disabled in preferences. Skipping.")
            return Result.success()
        }

        val result = UpdateManager.checkLatestRelease()
        if (result.isFailure) {
            Log.w(TAG, "Update check failed in worker: ${result.exceptionOrNull()?.message}")
            return Result.retry()
        }

        val release = result.getOrNull() ?: return Result.success()
        prefs.lastCheckTimestamp = System.currentTimeMillis()

        if (!UpdateManager.isNewerThanCurrent(release)) {
            Log.i(TAG, "Current version is up to date: ${UpdateManager.currentVersionName}")
            return Result.success()
        }

        Log.i(TAG, "Newer version detected: ${release.tagName} (current: ${UpdateManager.currentVersionName})")

        var isDownloaded = false
        var apkFile: File? = null

        if (prefs.autoDownload && release.phoneApkUrl != null && release.phoneApkName != null) {
            if (!prefs.wifiOnly || UpdateManager.isWifiConnected(context)) {
                val destFile = File(context.cacheDir, "Nocturne-Phone-${release.numericVersion}.apk")
                val dlResult = UpdateManager.downloadApk(
                    context = context,
                    urlString = release.phoneApkUrl,
                    destinationFile = destFile,
                    expectedSize = release.phoneApkSize,
                    expectedSha256 = release.phoneApkSha256,
                    wifiOnly = prefs.wifiOnly,
                    onProgress = { _, _, _ -> }
                )
                if (dlResult.isSuccess) {
                    val downloadedFile = dlResult.getOrThrow()
                    if (UpdateManager.verifyApkSignerMatchesCurrentApp(context, downloadedFile, release.numericVersion)) {
                        isDownloaded = true
                        apkFile = downloadedFile
                    }
                }
            }
        }

        showUpdateNotification(release, isDownloaded)
        return Result.success()
    }

    private fun showUpdateNotification(release: ReleaseInfo, isDownloaded: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحديثات Nocturne",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات توفر وتنزيل إصدارات Nocturne الجديدة"
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_updates_center", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (isDownloaded) {
            "تحديث Nocturne ${release.tagName} جاهز للتثبيت!"
        } else {
            "يتوفر إصدار جديد من Nocturne: ${release.tagName}"
        }

        val content = if (isDownloaded) {
            "تم تنزيل حزمة التحديث تلقائياً والتحقق من سلامتها. اضغط لتثبيت التحديث الآن."
        } else {
            "اضغط لمعاينة وتنزيل التحديث وتحديث واجهات ساعتك."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
        private const val CHANNEL_ID = "nocturne_updates_channel"
        private const val NOTIFICATION_ID = 2005
        private const val WORK_NAME = "nocturne_periodic_update_check"

        fun schedule(context: Context, wifiOnly: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
