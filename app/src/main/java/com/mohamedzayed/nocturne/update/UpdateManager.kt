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
import com.mohamedzayed.nocturne.findWatchBridge
import com.mohamedzayed.nocturne.transferVerifiedWatchApk
import com.mohamedzayed.nocturne.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
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
    val catalogUrl: String?,
    val phoneApkSha256: String?,
    val bridgeApkSha256: String?
) {
    val numericVersion: Int
        get() = parseVersionToNumber(tagName)

    companion object {
        fun parseVersionToNumber(tag: String): Int {
            val match = Regex("v?(\\d{1,4})\\.(\\d)\\.(\\d{1,2})").matchEntire(tag.trim()) ?: return 0
            val (major, minor, patch) = match.destructured
            return major.toInt() * 1000 + minor.toInt() * 100 + patch.toInt()
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
            val feed = BuildConfig.UPDATE_FEED_URL.ifBlank { REPO_RELEASES_URL }
            val conn = openDownload(feed)
            val bodyText = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
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
            var phoneApkSha256: String? = null
            var bridgeApkSha256: String? = null

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    val size = asset.optLong("size", 0L)
                    val digest = asset.optString("digest").takeIf { it.startsWith("sha256:") }
                        ?.removePrefix("sha256:")?.lowercase()
                        ?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }

                    when {
                        assetName.startsWith("Nocturne-Phone-", ignoreCase = true) && assetName.endsWith(".apk", ignoreCase = true) -> {
                            if (digest == null || size <= 0) continue
                            phoneApkSha256 = digest
                            phoneApkUrl = downloadUrl
                            phoneApkName = assetName
                            phoneApkSize = size
                        }
                        assetName.startsWith("Nocturne-WearBridge-", ignoreCase = true) && assetName.endsWith(".apk", ignoreCase = true) -> {
                            if (digest == null || size <= 0) continue
                            bridgeApkSha256 = digest
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
                catalogUrl = catalogUrl,
                phoneApkSha256 = phoneApkSha256,
                bridgeApkSha256 = bridgeApkSha256
            )
            require(release.numericVersion > 0 && release.phoneApkUrl != null) {
                "بيانات الإصدار أو بصمة حزمة الهاتف غير متوفرة"
            }
            Result.success(release)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error checking releases", e)
            Result.failure(e)
        }
    }

    fun isNewerThanCurrent(release: ReleaseInfo): Boolean = release.numericVersion > currentVersionCode

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun openDownload(initialUrl: String): HttpURLConnection {
        var url = URL(initialUrl)
        repeat(8) {
            val localTest = BuildConfig.DEBUG && BuildConfig.UPDATE_FEED_URL.isNotBlank() &&
                url.protocol == "http" && url.host == "127.0.0.1"
            require(url.protocol == "https" || localTest) { "رابط التحديث غير آمن" }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "Nocturne-Android-App/$currentVersionName")
                if (url.host == "api.github.com") setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                val code = connection.responseCode
                if (code == 200) return connection
                check(code in 300..399) { "استجابة غير متوقعة أثناء التحديث: HTTP $code" }
                val target = connection.getHeaderField("Location") ?: error("مسار إعادة التوجيه مفقود")
                url = URL(url, target)
            } catch (e: Exception) {
                connection.disconnect()
                throw e
            }
            connection.disconnect()
        }
        error("عدد عمليات إعادة التوجيه أكبر من المتوقع")
    }

    suspend fun downloadApk(
        context: Context,
        urlString: String,
        destinationFile: File,
        expectedSize: Long,
        expectedSha256: String?,
        wifiOnly: Boolean = false,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var temp: File? = null
        try {
            check(!wifiOnly || isWifiConnected(context)) { "التنزيل معطل خارج شبكة Wi-Fi حسب إعدادات التحديث" }
            require(expectedSize in 1..(512L * 1024 * 1024) &&
                expectedSha256?.matches(Regex("[0-9a-f]{64}")) == true) { "بيانات تحقق التحديث غير صالحة" }
            val digest = MessageDigest.getInstance("SHA-256")
            val working = File.createTempFile("update-", ".part", destinationFile.parentFile)
            temp = working
            val connection = openDownload(urlString)
            val size = try {
                connection.inputStream.use { input ->
                    working.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            check(total <= expectedSize) { "حجم الحزمة أكبر من المعلن" }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            onProgress(total.toFloat() / expectedSize, total, expectedSize)
                        }
                        total
                    }
                }
            } finally {
                connection.disconnect()
            }
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            check(size == expectedSize && sha256 == expectedSha256) { "حجم الحزمة أو بصمة SHA-256 لا تتطابق" }
            check(working.renameTo(destinationFile)) { "تعذر حفظ حزمة التحديث" }
            Log.i(TAG, "Verified downloaded APK bytes=$size sha256=$sha256")
            Result.success(destinationFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        } finally {
            temp?.delete()
        }
    }

    @Suppress("DEPRECATION")
    fun verifyApkSignerMatchesCurrentApp(context: Context, apkFile: File, expectedCode: Int? = null): Boolean = runCatching {
        val pm = context.packageManager
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val signingFlags = if (modern) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val flags = signingFlags or PackageManager.GET_CONFIGURATIONS
        val archive = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
        val installed = pm.getPackageInfo(context.packageName, flags)
        val version = if (modern) archive.longVersionCode else archive.versionCode.toLong()
        val installedVersion = if (modern) installed.longVersionCode else installed.versionCode.toLong()
        fun signers(info: android.content.pm.PackageInfo): Set<String> =
            (if (modern) info.signingInfo?.apkContentsSigners else info.signatures).orEmpty()
                .map { it.toCharsString() }.toSet()
        val currentSigners = signers(installed)
        val requiresWatch = archive.reqFeatures.orEmpty().any {
            it.name == PackageManager.FEATURE_WATCH && it.flags and android.content.pm.FeatureInfo.FLAG_REQUIRED != 0
        }
        archive.packageName == context.packageName && !requiresWatch && version > installedVersion &&
            (expectedCode == null || version == expectedCode.toLong()) &&
            currentSigners.isNotEmpty() && currentSigners == signers(archive) &&
            (archive.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE) <= Build.VERSION.SDK_INT
    }.getOrDefault(false)

    fun installPhoneApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            check(verifyApkSignerMatchesCurrentApp(context, apkFile)) {
                "تعذر التثبيت: الحزمة أو التوقيع غير مطابق، أو الإصدار ليس أحدث"
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
        requiredVersion: Int,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bridge = findWatchBridge(context) ?: error("لم يرد WearBridge من أي ساعة متصلة")
            check(bridge.protocol == 2) { "ثبّت إصدار WearBridge المتوافق على الساعة من مصدره الرسمي أولاً" }
            check(requiredVersion > bridge.versionCode) { "WearBridge المتصل يستخدم إصداراً مساوياً أو أحدث" }
            val response = transferVerifiedWatchApk(context, bridge, apkFile, context.packageName, "bridge") { sent ->
                onProgress(if (sent) "انتظار تأكيد الساعة لحفظ التحديث..." else "نقل التحديث إلى ${bridge.node.displayName}...")
            }
            check(response.optBoolean("success") && response.optString("stage") == "UPDATE_READY") {
                response.optString("error", "لم تؤكد الساعة أن حزمة التحديث جاهزة")
            }
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(IllegalStateException("لم تؤكد الساعة استلام تحديث WearBridge"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage Bridge update", e)
            Result.failure(e)
        }
    }
}
