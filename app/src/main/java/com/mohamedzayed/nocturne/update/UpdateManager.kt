package com.mohamedzayed.nocturne.update

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.mohamedzayed.nocturne.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val phoneApkUrl: String?,
    val phoneApkName: String?,
    val phoneApkSize: Long,
    val bridgeApkUrl: String?,
    val bridgeApkName: String?,
    val bridgeApkSize: Long,
    val catalogUrl: String?
) {
    val numericVersion: Int
        get() = parseVersionToNumber(tagName)

    companion object {
        fun parseVersionToNumber(tag: String): Int {
            val clean = tag.removePrefix("v").trim()
            val parts = clean.split(".")
            return try {
                val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
                val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
                major * 1000 + minor * 100 + patch
            } catch (_: Exception) {
                0
            }
        }
    }
}

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpToDate(val currentVersion: String) : UpdateStatus()
    data class UpdateAvailable(
        val release: ReleaseInfo,
        val isPhoneUpdate: Boolean,
        val isBridgeUpdate: Boolean
    ) : UpdateStatus()
    data class Downloading(
        val progress: Float,
        val currentBytes: Long,
        val totalBytes: Long,
        val target: String
    ) : UpdateStatus()
    data class Downloaded(
        val release: ReleaseInfo,
        val apkFile: File,
        val target: String
    ) : UpdateStatus()
    data class Installing(val target: String) : UpdateStatus()
    data class Error(val message: String, val hint: String = "") : UpdateStatus()
}

class UpdatePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nocturne_updates", Context.MODE_PRIVATE)

    var autoCheck: Boolean
        get() = prefs.getBoolean("auto_check", true)
        set(value) = prefs.edit().putBoolean("auto_check", value).apply()

    var autoDownload: Boolean
        get() = prefs.getBoolean("auto_download", false)
        set(value) = prefs.edit().putBoolean("auto_download", value).apply()

    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifi_only", true)
        set(value) = prefs.edit().putBoolean("wifi_only", value).apply()

    var lastCheckTimestamp: Long
        get() = prefs.getLong("last_check_ts", 0L)
        set(value) = prefs.edit().putLong("last_check_ts", value).apply()
}

object UpdateManager {
    private const val TAG = "NocturneUpdate"
    private const val REPO_RELEASES_URL = "https://api.github.com/repos/MZAYED2/Nocturne/releases/latest"

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    suspend fun checkLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(REPO_RELEASES_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Nocturne-Android-App/${currentVersionName}")
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 12000
            conn.readTimeout = 15000

            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(Exception("استجابة غير متوقعة من GitHub: HTTP $code"))
            }

            val bodyText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(bodyText)

            val tagName = json.optString("tag_name", "")
            val name = json.optString("name", tagName)
            val body = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")

            var phoneApkUrl: String? = null
            var phoneApkName: String? = null
            var phoneApkSize: Long = 0L

            var bridgeApkUrl: String? = null
            var bridgeApkName: String? = null
            var bridgeApkSize: Long = 0L

            var catalogUrl: String? = null

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    val size = asset.optLong("size", 0L)

                    when {
                        assetName.startsWith("Nocturne-Phone-", ignoreCase = true) && assetName.endsWith(".apk", ignoreCase = true) -> {
                            phoneApkUrl = downloadUrl
                            phoneApkName = assetName
                            phoneApkSize = size
                        }
                        assetName.startsWith("Nocturne-WearBridge-", ignoreCase = true) && assetName.endsWith(".apk", ignoreCase = true) -> {
                            bridgeApkUrl = downloadUrl
                            bridgeApkName = assetName
                            bridgeApkSize = size
                        }
                        assetName.equals("catalog.json", ignoreCase = true) -> {
                            catalogUrl = downloadUrl
                        }
                    }
                }
            }

            val release = ReleaseInfo(
                tagName = tagName,
                name = name,
                body = body,
                publishedAt = publishedAt,
                phoneApkUrl = phoneApkUrl,
                phoneApkName = phoneApkName,
                phoneApkSize = phoneApkSize,
                bridgeApkUrl = bridgeApkUrl,
                bridgeApkName = bridgeApkName,
                bridgeApkSize = bridgeApkSize,
                catalogUrl = catalogUrl
            )
            Result.success(release)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking releases", e)
            Result.failure(e)
        }
    }

    fun isNewerThanCurrent(release: ReleaseInfo): Boolean {
        val currentNum = ReleaseInfo.parseVersionToNumber(currentVersionName)
        val releaseNum = release.numericVersion
        return if (currentNum > 0 && releaseNum > 0) {
            releaseNum > currentNum
        } else {
            !release.tagName.equals("v$currentVersionName", ignoreCase = true) &&
                    !release.tagName.equals(currentVersionName, ignoreCase = true)
        }
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    suspend fun downloadApk(
        context: Context,
        urlString: String,
        destinationFile: File,
        wifiOnly: Boolean = false,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (wifiOnly && !isWifiConnected(context)) {
            return@withContext Result.failure(Exception("التنزيل معطل خارج شبكة Wi-Fi حسب إعدادات التحديث"))
        }

        try {
            var currentUrl = urlString
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 8

            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 20000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Nocturne-Android-App/$currentVersionName")
                    setRequestProperty("Accept", "*/*")
                }

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: return@withContext Result.failure(Exception("توجيه HTTP بدون مسار Location"))
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(url, location).toString()
                    }
                    connection.disconnect()
                    redirects++
                } else if (code == 200) {
                    break
                } else {
                    return@withContext Result.failure(Exception("خطأ في الخادم أثناء التنزيل: HTTP $code"))
                }
            }

            val finalConn = connection ?: return@withContext Result.failure(Exception("فشل فتح الاتصال"))
            val totalBytes = finalConn.contentLengthLong
            val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.downloading")
            if (tempFile.exists()) tempFile.delete()

            finalConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val fraction = if (totalBytes > 0) totalRead.toFloat() / totalBytes.toFloat() else 0f
                        onProgress(fraction, totalRead, totalBytes)
                    }
                    output.flush()
                }
            }

            // Verify basic file integrity
            if (tempFile.length() < 1_000_000L) {
                tempFile.delete()
                return@withContext Result.failure(Exception("الملف المُنزّل تالف أو حجمه أقل من المتوقع (${tempFile.length()} بايت)"))
            }

            if (!isZipMagicValid(tempFile)) {
                tempFile.delete()
                return@withContext Result.failure(Exception("الملف المُنزّل ليس حزمة APK صالحة (فشل فحص توقيع ZIP)"))
            }

            if (destinationFile.exists()) destinationFile.delete()
            if (!tempFile.renameTo(destinationFile)) {
                return@withContext Result.failure(Exception("فشل نقل الملف المؤقت إلى المسار النهائي"))
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    private fun isZipMagicValid(file: File): Boolean {
        return try {
            FileInputStream(file).use { fis ->
                val magic = ByteArray(4)
                fis.read(magic) == 4 &&
                        magic[0] == 0x50.toByte() &&
                        magic[1] == 0x4B.toByte() &&
                        magic[2] == 0x03.toByte() &&
                        magic[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun verifyApkSignerMatchesCurrentApp(context: Context, apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            val currentPkgInfo = pm.getPackageInfo(context.packageName, flags)

            val currentSha = getSignatureSha256(currentPkgInfo)
            val archiveSha = getSignatureSha256(archiveInfo)

            Log.i(TAG, "Signature check -> Current: $currentSha, Archive: $archiveSha")
            currentSha != null && currentSha == archiveSha
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification error", e)
            false
        }
    }

    private fun getSignatureSha256(pkgInfo: android.content.pm.PackageInfo): String? {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners ?: pkgInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return null
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(cert)
            digest.joinToString("") { "%02X".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun installPhoneApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(Exception("ملف التحديث غير موجود"))
            }

            // Check Unknown Sources permission on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return Result.failure(Exception("يرجى منح إذن تثبيت التطبيقات غير المعروفة لـ Nocturne للمتابعة"))
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            Result.failure(e)
        }
    }

    suspend fun transferBridgeApkToWatch(
        context: Context,
        apkFile: File,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!apkFile.exists()) {
                return@withContext Result.failure(Exception("ملف تحديث WearBridge غير موجود"))
            }

            onProgress("جاري الاتصال بالساعة عبر البلوتوث...")
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            val watchNode = nodes.firstOrNull()
                ?: return@withContext Result.failure(Exception("لم يتم العثور على ساعة متصلة حالياً"))

            onProgress("فتح قناة نقل التحديث إلى ${watchNode.displayName}...")
            val channelClient = Wearable.getChannelClient(context)
            val channel = channelClient.openChannel(watchNode.id, "/nocturne/update_bridge_channel").await()

            onProgress("جاري إرسال حزمة WearBridge إلى الساعة...")
            val fileUri = Uri.fromFile(apkFile)
            channelClient.sendFile(channel, fileUri).await()

            onProgress("اكتمل نقل التحديث إلى الساعة بنجاح! افتح شاشة الساعة لتأكيد التثبيت.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transfer bridge apk", e)
            Result.failure(e)
        }
    }
}
