package com.mohamedzayed.nocturne.wear

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "NocturneWearBridge"
private const val MAX_WATCH_APK_BYTES = 50_000_000L

data class ConnectedBridge(
    val node: Node,
    val version: String,
    val protocol: Int,
    val versionCode: Int,
    val management: JSONObject?
)

data class WatchSlot(
    val packageName: String,
    val active: Boolean
)

object WearBridgeManager {

    suspend fun findWatchBridge(
        context: Context,
        managementRequest: String? = null,
        targetNodeId: String? = null
    ): ConnectedBridge? = withContext(Dispatchers.IO) {
        val nodes = try {
            Wearable.getNodeClient(context).connectedNodes.await()
                .filter { targetNodeId == null || it.id == targetNodeId }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get connected nodes", e)
            return@withContext null
        }
        if (nodes.isEmpty()) return@withContext null

        val messages = Wearable.getMessageClient(context)
        for (node in nodes.sortedWith(compareByDescending<Node> { it.isNearby }.thenBy { it.id })) {
            val nonce = UUID.randomUUID().toString() + managementRequest?.let { "|$it" }.orEmpty()
            val response = CompletableDeferred<JSONObject>()
            val listener = MessageClient.OnMessageReceivedListener { event ->
                if (event.path == "/nocturne/pong" && event.sourceNodeId == node.id) {
                    val json = runCatching { JSONObject(String(event.data, Charsets.UTF_8)) }.getOrNull()
                    if (json?.optString("requestId") == nonce || (json?.optInt("protocol") == 0 && json.optBoolean("bridgeInstalled"))) {
                        response.complete(json)
                    }
                }
            }
            val reply = try {
                withTimeoutOrNull(4000) {
                    messages.addListener(listener).await()
                    messages.sendMessage(node.id, "/nocturne/ping", nonce.toByteArray()).await()
                    response.await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Bridge discovery failed on ${node.displayName}", e)
                null
            } finally {
                messages.removeListener(listener)
            }
            if (reply != null) {
                return@withContext ConnectedBridge(
                    node = node,
                    version = reply.optString("version", "1.0.0"),
                    protocol = reply.optInt("protocol", 2),
                    versionCode = reply.optInt("versionCode", 1),
                    management = reply.optJSONObject("management")
                )
            }
        }
        null
    }

    suspend fun querySlots(context: Context, targetNodeId: String? = null): List<WatchSlot> {
        val bridge = findWatchBridge(context, managementRequest = "slots", targetNodeId = targetNodeId)
        val slotsJson = bridge?.management?.optJSONArray("slots") ?: return emptyList()
        val list = mutableListOf<WatchSlot>()
        for (i in 0 until slotsJson.length()) {
            val item = slotsJson.optJSONObject(i) ?: continue
            list.add(
                WatchSlot(
                    packageName = item.optString("package", ""),
                    active = item.optBoolean("active", false)
                )
            )
        }
        return list
    }

    suspend fun removeSlot(context: Context, bridge: ConnectedBridge, packageName: String): Result<Unit> {
        val response = findWatchBridge(
            context = context,
            managementRequest = "remove|$packageName",
            targetNodeId = bridge.node.id
        ) ?: return Result.failure(Exception("Watch bridge disconnected"))

        val mgmt = response.management
        if (mgmt != null && mgmt.has("error")) {
            return Result.failure(Exception(mgmt.optString("error", "Removal failed")))
        }
        return Result.success(Unit)
    }

    suspend fun activateSlot(context: Context, bridge: ConnectedBridge, packageName: String): Result<Unit> {
        val response = findWatchBridge(
            context = context,
            managementRequest = "activate|$packageName",
            targetNodeId = bridge.node.id
        ) ?: return Result.failure(Exception("Watch bridge disconnected"))

        val mgmt = response.management
        if (mgmt != null && mgmt.has("error")) {
            return Result.failure(Exception(mgmt.optString("error", "Activation failed")))
        }
        return Result.success(Unit)
    }

    fun getRemainingSlotCount(bridge: ConnectedBridge?): Int? {
        val count = bridge?.management?.optInt("remainingSlotCount", -1) ?: -1
        return if (count >= 0) count else null
    }

    suspend fun transferVerifiedWatchApk(
        context: Context,
        bridge: ConnectedBridge,
        apkFile: File,
        packageName: String,
        kind: String,
        token: String = "",
        onProgress: (sent: Boolean) -> Unit
    ): JSONObject = pushWatchFace(context, bridge, apkFile, packageName, token, kind, onProgress = { sent -> onProgress(sent) })

    suspend fun pushWatchFace(
        context: Context,
        bridge: ConnectedBridge,
        apkFile: File,
        packageName: String,
        token: String = "",
        kind: String = "wff",
        onProgress: (Boolean) -> Unit
    ): JSONObject = pushWatchFace(
        context = context,
        bridge = bridge,
        apkFile = apkFile,
        packageName = packageName,
        token = token,
        kind = kind,
        onProgress = { transferred, total -> onProgress(transferred >= total) },
        onStage = { _, _ -> }
    )

    suspend fun pushWatchFace(
        context: Context,
        bridge: ConnectedBridge,
        apkFile: File,
        packageName: String,
        token: String = "",
        kind: String = "wff",
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onStage: (stage: String, detail: String) -> Unit = { _, _ -> }
    ): JSONObject = withContext(Dispatchers.IO) {
        val size = apkFile.length()
        require(size in 1..MAX_WATCH_APK_BYTES) { "APK size unsupported: $size" }
        val sha256 = watchApkSha256(apkFile)
        val requestId = UUID.randomUUID().toString()
        val metadata = JSONObject()
            .put("packageName", packageName)
            .put("token", token)
            .put("kind", kind)
            .put("fileSize", size)
            .put("sha256", sha256)

        val messageClient = Wearable.getMessageClient(context)
        val result = CompletableDeferred<JSONObject>()
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.sourceNodeId == bridge.node.id) {
                if (event.path == "/nocturne/push_result/$requestId") {
                    runCatching { JSONObject(String(event.data, Charsets.UTF_8)) }
                        .onSuccess { result.complete(it) }
                        .onFailure { result.completeExceptionally(it) }
                } else if (event.path == "/nocturne/push_stage/$requestId") {
                    val json = runCatching { JSONObject(String(event.data, Charsets.UTF_8)) }.getOrNull()
                    if (json != null) {
                        onStage(json.optString("stage", ""), json.optString("detail", ""))
                    }
                }
            }
        }
        val channels = Wearable.getChannelClient(context)
        val reply = try {
            messageClient.addListener(listener).await()
            onStage("CONNECT", "Connecting to watch channel...")
            val channel = withTimeout(20000) {
                channels.openChannel(bridge.node.id, "/nocturne/push_v2/$requestId").await()
            }
            try {
                withTimeout(300000) {
                    onStage("TRANSFER", "Initiating wireless streaming...")
                    Log.i(TAG, "Sending $requestId $packageName bytes=$size sha256=$sha256")
                    val rawStream = channels.getOutputStream(channel).await()
                    val bufferedStream = rawStream.buffered(64 * 1024)
                    val dataOut = DataOutputStream(bufferedStream)

                    // Write metadata header
                    dataOut.writeUTF(metadata.toString())
                    dataOut.flush()

                    // Stream APK payload in 64 KB chunks with byte-level progress reporting
                    runInterruptible {
                        apkFile.inputStream().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var totalSent = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                dataOut.write(buffer, 0, count)
                                totalSent += count
                                onProgress(totalSent, size)
                            }
                        }
                        dataOut.flush()
                        bufferedStream.flush()
                        rawStream.flush()
                    }

                    // CRITICAL: DO NOT close the stream yet!
                    // Closing the stream prematurely sends a channel close frame to the watch
                    // before the watch finishes processing the stream.
                    // Keep stream open while waiting for the watch's acknowledgment and installation result.
                    onStage("WAIT_RESULT", "Awaiting confirmation from watch...")
                    val watchReply = result.await()

                    try {
                        dataOut.close()
                    } catch (_: Exception) {}

                    watchReply
                }
            } finally {
                try {
                    channels.close(channel).await()
                } catch (_: Exception) {}
            }
        } finally {
            messageClient.removeListener(listener)
        }
        Log.i(TAG, "Watch result $requestId: $reply")
        if (reply.optBoolean("success")) {
            check(reply.optLong("fileSize") == size && reply.optString("sha256").equals(sha256, ignoreCase = true) &&
                    reply.optString("packageName") == packageName) { "Watch response does not match sent package" }
        }
        reply
    }

    fun watchApkSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun isApkFileValid(file: File): Boolean {
        if (!file.exists() || file.length() < 50_000L) return false
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

    suspend fun downloadApkWithValidation(
        urlString: String,
        destinationFile: File,
        expectedMinBytes: Long = 50_000L,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            var currentUrl = urlString
            var redirectCount = 0
            while (redirectCount < 5) {
                val url = URL(currentUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: return@withContext Result.failure(Exception("Redirect without Location header"))
                    currentUrl = location
                    redirectCount++
                    connection.disconnect()
                    continue
                }
                if (code !in 200..299) {
                    connection.disconnect()
                    return@withContext Result.failure(Exception("HTTP error $code while downloading APK"))
                }
                val contentLength = connection.contentLengthLong
                tempFile = File.createTempFile("nocturne_dl_", ".tmp", destinationFile.parentFile)
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var totalRead = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            totalRead += read
                            if (contentLength > 0) {
                                onProgress(totalRead.toFloat() / contentLength, totalRead, contentLength)
                            }
                        }
                    }
                }
                connection.disconnect()
                break
            }
            val downloaded = tempFile ?: return@withContext Result.failure(Exception("Failed to download file"))
            if (!isApkFileValid(downloaded) || downloaded.length() < expectedMinBytes) {
                downloaded.delete()
                return@withContext Result.failure(Exception("Downloaded APK file is invalid or too small"))
            }
            if (destinationFile.exists()) destinationFile.delete()
            if (!downloaded.renameTo(destinationFile)) {
                downloaded.copyTo(destinationFile, overwrite = true)
                downloaded.delete()
            }
            Result.success(destinationFile)
        } catch (e: Exception) {
            tempFile?.delete()
            Result.failure(e)
        }
    }

    fun openGalaxyWearable(context: Context) {
        try {
            val samsung = context.packageManager.getLaunchIntentForPackage("com.samsung.android.app.watchmanager")
            if (samsung != null) {
                context.startActivity(samsung)
                return
            }
        } catch (_: Exception) {}

        try {
            val wear = context.packageManager.getLaunchIntentForPackage("com.google.android.wearable.app")
            if (wear != null) {
                context.startActivity(wear)
                return
            }
        } catch (_: Exception) {}

        Toast.makeText(context, "Galaxy Wearable / Wear OS app not installed", Toast.LENGTH_LONG).show()
    }
}
