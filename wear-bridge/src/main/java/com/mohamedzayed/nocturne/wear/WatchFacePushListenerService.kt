package com.mohamedzayed.nocturne.wear

import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File

class WatchFacePushListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "NocturneBridge"
        const val PATH_PING = "/nocturne/ping"
        const val PATH_PONG = "/nocturne/pong"
        const val PATH_PUSH_CHANNEL = "/nocturne/push_v2/"
        private const val MAX_APK_BYTES = 150L * 1024 * 1024
        const val PATH_PUSH_RESULT = "/nocturne/push_result"
     }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val senderNodeId = messageEvent.sourceNodeId

        Log.i(TAG, "Message received on path: $path from: $senderNodeId")

        when (path) {
            PATH_PING -> {
                scope.launch {
                    try {
                        val responseJson = JSONObject().apply {
                            put("bridgeInstalled", true)
                            put("version", BuildConfig.VERSION_NAME)
                            put("versionCode", BuildConfig.VERSION_CODE)
                            put("protocol", 2)
                            put("requestId", String(messageEvent.data, Charsets.UTF_8))
                        }
                        val request = String(messageEvent.data, Charsets.UTF_8).split('|', limit = 3)
                        if (request.size > 1) {
                            val management = try {
                                watchFaceInstallLock.withLock {
                                    val manager = androidx.wear.watchfacepush.WatchFacePushManagerFactory.createWatchFacePushManager(applicationContext)
                                    if (request[1] == "remove") {
                                        val target = manager.listWatchFaces().installedWatchFaceDetails
                                            .firstOrNull { it.packageName == request.getOrNull(2) }
                                            ?: error("Watch face is not installed in a Nocturne slot")
                                        check(!manager.isWatchFaceActive(target.packageName)) {
                                            "Switch to another watch face on the watch before removing the active face."
                                        }
                                        manager.removeWatchFace(target.slotId)
                                        check(manager.listWatchFaces().installedWatchFaceDetails.none { it.slotId == target.slotId }) {
                                            "Wear OS did not confirm removal."
                                        }
                                    } else if (request[1] == "activate") {
                                        val target = manager.listWatchFaces().installedWatchFaceDetails
                                            .firstOrNull { it.packageName == request.getOrNull(2) }
                                            ?: error("Watch face is not installed in a Nocturne slot")
                                        manager.setWatchFaceAsActive(target.slotId)
                                        check(manager.isWatchFaceActive(target.packageName)) {
                                            "Wear OS did not confirm activation."
                                        }
                                    }
                                    val slotsInfo = manager.listWatchFaces()
                                    val slots = org.json.JSONArray()
                                    slotsInfo.installedWatchFaceDetails.forEach { face ->
                                        slots.put(JSONObject().put("package", face.packageName)
                                            .put("active", manager.isWatchFaceActive(face.packageName)))
                                    }
                                    JSONObject().put("slots", slots)
                                        .put("remainingSlotCount", slotsInfo.remainingSlotCount)
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                JSONObject().put("error", e.localizedMessage ?: "Watch operation failed")
                            }
                            responseJson.put("management", management)
                        }
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(senderNodeId, PATH_PONG, responseJson.toString().toByteArray())
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error responding to ping", e)
                    }
                }
            }

        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        val path = channel.path
        Log.i(TAG, "Channel opened on path: $path from node: ${channel.nodeId}")

        if (path.startsWith(PATH_PUSH_CHANNEL)) {
            val requestId = path.removePrefix(PATH_PUSH_CHANNEL)
            if (runCatching { java.util.UUID.fromString(requestId).toString() }.getOrNull() != requestId) {
                Wearable.getChannelClient(this).close(channel)
                return
            }
            scope.launch { receiveWatchFace(channel, requestId) }
            return
        }

    }

    private fun notifyBridgeUpdateReady() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bridge_updates",
                "تحديثات WearBridge",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, BridgeMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("has_bridge_update", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(applicationContext, "bridge_updates")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("تحديث WearBridge جاهز")
            .setContentText("تم استلام تحديث جديد من الهاتف. افتح التطبيق للتثبيت.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(3001, notification)
    }

    private suspend fun receiveWatchFace(channel: ChannelClient.Channel, requestId: String) {
        val channels = Wearable.getChannelClient(this)
        var stage = "RECEIVE"
        val reply = JSONObject().put("success", false).put("isActive", false)
        var apk: File? = null
        try {
            val tempApk = File.createTempFile("watchface-", ".apk", cacheDir)
            apk = tempApk
            val stream = channels.getInputStream(channel).await()
            val metadata = kotlinx.coroutines.runInterruptible {
                java.io.DataInputStream(stream.buffered()).use { input ->
                    val request = JSONObject(input.readUTF())
                    val size = request.getLong("fileSize")
                    require(size in 1..MAX_APK_BYTES) { "حجم الحزمة غير مدعوم: $size" }
                    val expectedHash = request.getString("sha256")
                    require(Regex("[0-9a-f]{64}").matches(expectedHash)) { "بصمة SHA-256 غير صالحة" }
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                    tempApk.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var remaining = size
                        while (remaining > 0) {
                            val count = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
                            if (count < 0) throw java.io.EOFException("وصل ${size - remaining} من $size بايت")
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            remaining -= count
                        }
                    }
                    val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
                    check(tempApk.length() == size && actualHash == expectedHash) { "حجم الحزمة أو بصمة SHA-256 لا تتطابق" }
                    check(input.read() == -1) { "استقبلت الساعة بيانات إضافية بعد نهاية الحزمة" }
                    Log.i(TAG, "Verified $requestId bytes=$size sha256=$actualHash")
                    reply.put("fileSize", size).put("sha256", actualHash)
                    request
                }
            }
            stage = "VALIDATE"
            val packageName = metadata.getString("packageName")
            reply.put("packageName", packageName)
            if (metadata.optString("kind") == "bridge") {
                check(packageName == applicationContext.packageName && isValidBridgeUpdate(this, tempApk)) {
                    "تحديث WearBridge لا يطابق الحزمة أو التوقيع أو رقم الإصدار"
                }
                val update = File(filesDir, "Nocturne-WearBridge-update.apk")
                watchFaceInstallLock.withLock {
                    check(tempApk.renameTo(update)) { "تعذر حفظ حزمة تحديث الساعة" }
                }
                stage = "UPDATE_READY"
                reply.put("success", true)
                runCatching { notifyBridgeUpdateReady() }
            } else {
                installWatchFace(this, tempApk, metadata.getString("packageName"), metadata.getString("token"), requestId) { stage = it }
                    .let { slotId -> reply.put("slotId", slotId).put("success", true).put("isActive", true) }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Push $requestId failed at $stage", e)
            reply.put("error", e.localizedMessage ?: e.javaClass.simpleName)
        } finally {
            apk?.delete()
        }
        try {
            reply.put("stage", stage)
            Wearable.getMessageClient(this)
                .sendMessage(channel.nodeId, "$PATH_PUSH_RESULT/$requestId", reply.toString().toByteArray())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Could not deliver result for $requestId", e)
        } finally {
            channels.close(channel)
        }
    }
}
