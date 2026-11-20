package com.mohamedzayed.nocturne.wear

import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.watchfacepush.WatchFacePushManager
import androidx.wear.watchfacepush.WatchFacePushManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class WatchFacePushListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "NocturneBridge"
        const val PATH_PING = "/nocturne/ping"
        const val PATH_PONG = "/nocturne/pong"
        const val PATH_PREPARE_PUSH = "/nocturne/prepare_push"
        const val PATH_READY_FOR_PUSH = "/nocturne/ready_for_push"
        const val PATH_PUSH_CHANNEL = "/nocturne/push_channel"
        const val PATH_PUSH_RESULT = "/nocturne/push_result"
        const val PATH_LIST = "/nocturne/list_installed"
        const val PATH_SET_ACTIVE = "/nocturne/set_active"
    }

    private var pendingPackageName: String = "com.mohamedzayed.nocturne.wear.watchfacepush.nocturne"
    private var pendingToken: String = ""
    private var pendingFileSize: Long = 0L
    private var pendingSetActive: Boolean = true
    private var pendingSenderNodeId: String = ""

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val senderNodeId = messageEvent.sourceNodeId

        Log.i(TAG, "Message received on path: $path from: $senderNodeId")

        when (path) {
            PATH_PING -> {
                scope.launch {
                    try {
                        val pushManager = WatchFacePushManagerFactory.createWatchFacePushManager(applicationContext)
                        val listResponse = pushManager.listWatchFaces()
                        val responseJson = JSONObject().apply {
                            put("bridgeInstalled", true)
                            put("version", "1.0.4")
                            put("remainingSlots", listResponse.remainingSlotCount)
                            put("installedCount", listResponse.installedWatchFaceDetails.size)
                        }
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(senderNodeId, PATH_PONG, responseJson.toString().toByteArray())
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error responding to ping", e)
                    }
                }
            }

            PATH_PREPARE_PUSH -> {
                scope.launch {
                    try {
                        val req = JSONObject(String(messageEvent.data))
                        pendingPackageName = req.optString("packageName", "com.mohamedzayed.nocturne.wear.watchfacepush.nocturne")
                        pendingToken = req.getString("token")
                        pendingFileSize = req.optLong("fileSize", 0L)
                        pendingSetActive = req.optBoolean("setActive", true)
                        pendingSenderNodeId = senderNodeId

                        Log.i(TAG, "Prepared push metadata: pkg=$pendingPackageName, size=$pendingFileSize, setActive=$pendingSetActive")

                        val tempApk = File(cacheDir, "incoming_pushed_wf.apk")
                        if (tempApk.exists()) {
                            tempApk.delete()
                        }

                        val reply = JSONObject().apply {
                            put("ready", true)
                            put("packageName", pendingPackageName)
                        }
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(senderNodeId, PATH_READY_FOR_PUSH, reply.toString().toByteArray())
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in prepare push", e)
                        sendPushResult(senderNodeId, "PREPARE", false, "فشل تجهيز استقبال الواجهة: ${e.message}", null, false)
                    }
                }
            }

            PATH_LIST -> {
                scope.launch {
                    try {
                        val pushManager = WatchFacePushManagerFactory.createWatchFacePushManager(applicationContext)
                        val listResponse = pushManager.listWatchFaces()
                        val array = org.json.JSONArray()
                        for (item in listResponse.installedWatchFaceDetails) {
                            val isActive = try {
                                pushManager.isWatchFaceActive(item.packageName)
                            } catch (e: Exception) {
                                false
                            }
                            array.put(JSONObject().apply {
                                put("slotId", item.slotId)
                                put("packageName", item.packageName)
                                put("isActive", isActive)
                            })
                        }
                        val result = JSONObject().apply {
                            put("slots", array)
                            put("remainingSlots", listResponse.remainingSlotCount)
                        }
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(senderNodeId, PATH_LIST + "_reply", result.toString().toByteArray())
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error listing watch faces", e)
                    }
                }
            }

            PATH_SET_ACTIVE -> {
                scope.launch {
                    try {
                        val req = JSONObject(String(messageEvent.data))
                        val slotId = req.getString("slotId")
                        val pushManager = WatchFacePushManagerFactory.createWatchFacePushManager(applicationContext)
                        pushManager.setWatchFaceAsActive(slotId)
                        val reply = JSONObject().apply {
                            put("success", true)
                            put("slotId", slotId)
                            put("isActive", true)
                        }
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(senderNodeId, PATH_SET_ACTIVE + "_reply", reply.toString().toByteArray())
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting watch face active", e)
                        val reply = JSONObject().apply {
                            put("success", false)
                            put("error", e.message ?: "Unknown error")
                        }
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(senderNodeId, PATH_SET_ACTIVE + "_reply", reply.toString().toByteArray())
                            .await()
                    }
                }
            }
        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        val path = channel.path
        Log.i(TAG, "Channel opened on path: $path from node: ${channel.nodeId}")

        if (path.startsWith(PATH_PUSH_CHANNEL)) {
            val tempApk = File(cacheDir, "incoming_pushed_wf.apk")
            if (tempApk.exists()) {
                tempApk.delete()
            }

            val channelClient = Wearable.getChannelClient(applicationContext)
            val fileUri = Uri.fromFile(tempApk)

            Log.i(TAG, "Starting ChannelClient.receiveFile for $fileUri")
            channelClient.receiveFile(channel, fileUri, false)
                .addOnSuccessListener {
                    Log.i(TAG, "receiveFile completed successfully! File size: ${tempApk.length()} bytes")
                    scope.launch {
                        processReceivedWatchFaceApk(tempApk, channel.nodeId)
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "ChannelClient.receiveFile failed", error)
                    scope.launch {
                        sendPushResult(
                            targetNodeId = channel.nodeId,
                            stage = "RECEIVE",
                            success = false,
                            errorMessage = "انقطعت قناة الاستقبال على الساعة: ${error.localizedMessage}",
                            slotId = null,
                            isActive = false
                        )
                    }
                }
            return
        }

        if (path == "/nocturne/update_bridge_channel") {
            val updateApk = File(filesDir, "Nocturne-WearBridge-update.apk")
            if (updateApk.exists()) updateApk.delete()

            val channelClient = Wearable.getChannelClient(applicationContext)
            val fileUri = Uri.fromFile(updateApk)

            Log.i(TAG, "Starting ChannelClient.receiveFile for Bridge update: $fileUri")
            channelClient.receiveFile(channel, fileUri, false)
                .addOnSuccessListener {
                    Log.i(TAG, "Bridge update received successfully! File size: ${updateApk.length()} bytes")
                    notifyBridgeUpdateReady(updateApk)
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Failed to receive bridge update", error)
                }
            return
        }
    }

    private fun notifyBridgeUpdateReady(apkFile: File) {
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

    private suspend fun processReceivedWatchFaceApk(apkFile: File, senderNodeId: String) {
        // 1. Verify file integrity
        if (!apkFile.exists() || apkFile.length() < 1_000_000L) {
            val size = if (apkFile.exists()) apkFile.length() else 0L
            Log.e(TAG, "Received file is invalid or too small: $size bytes")
            sendPushResult(
                targetNodeId = senderNodeId,
                stage = "RECEIVE",
                success = false,
                errorMessage = "الحزمة المستلمة ناقصة أو تالفة ($size بايت)",
                slotId = null,
                isActive = false
            )
            return
        }

        // Verify ZIP magic header
        val isZipValid = try {
            FileInputStream(apkFile).use { fis ->
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

        if (!isZipValid) {
            Log.e(TAG, "Received file does not have valid APK/ZIP header")
            sendPushResult(
                targetNodeId = senderNodeId,
                stage = "VALIDATE",
                success = false,
                errorMessage = "الملف المستلم ليس حزمة APK صالحة",
                slotId = null,
                isActive = false
            )
            return
        }

        // 2. Install using WatchFacePushManager
        Log.i(TAG, "Installing verified APK via WatchFacePushManager with token: $pendingToken")
        val pushManager = WatchFacePushManagerFactory.createWatchFacePushManager(applicationContext)

        val pfd = try {
            ParcelFileDescriptor.open(apkFile, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open ParcelFileDescriptor", e)
            sendPushResult(
                targetNodeId = senderNodeId,
                stage = "INSTALL",
                success = false,
                errorMessage = "فشل فتح ملف الواجهة: ${e.localizedMessage}",
                slotId = null,
                isActive = false
            )
            return
        }

        var finalSlotId: String? = null
        var isNowActive = false

        try {
            val list = pushManager.listWatchFaces()
            Log.i(TAG, "Current installed watch faces: ${list.installedWatchFaceDetails.size}, remainingSlots: ${list.remainingSlotCount}")

            val existing = list.installedWatchFaceDetails.firstOrNull { it.packageName == pendingPackageName }

            if (existing != null) {
                Log.i(TAG, "Updating existing watch face in slot ${existing.slotId}")
                pushManager.updateWatchFace(existing.slotId, pfd, pendingToken)
                finalSlotId = existing.slotId
            } else if (list.remainingSlotCount > 0) {
                Log.i(TAG, "Adding new watch face")
                val slot = pushManager.addWatchFace(pfd, pendingToken)
                finalSlotId = slot.slotId
            } else {
                val replaceSlot = list.installedWatchFaceDetails.firstOrNull()?.slotId ?: "0"
                Log.i(TAG, "No slots remaining, updating slot $replaceSlot")
                pushManager.updateWatchFace(replaceSlot, pfd, pendingToken)
                finalSlotId = replaceSlot
            }

            pfd.close()

            // 3. Set Active if requested
            if (pendingSetActive && finalSlotId != null) {
                try {
                    pushManager.setWatchFaceAsActive(finalSlotId)
                    isNowActive = true
                    Log.i(TAG, "Watch face set as active successfully in slot $finalSlotId")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set watch face as active (permission might be missing)", e)
                    isNowActive = false
                }
            }

            sendPushResult(
                targetNodeId = senderNodeId,
                stage = "COMPLETE",
                success = true,
                errorMessage = null,
                slotId = finalSlotId,
                isActive = isNowActive
            )

        } catch (e: Exception) {
            Log.e(TAG, "WatchFacePushManager install failed", e)
            pfd.close()
            sendPushResult(
                targetNodeId = senderNodeId,
                stage = "INSTALL",
                success = false,
                errorMessage = "فشل تثبيت نظام Wear OS للواجهة: ${e.localizedMessage}",
                slotId = null,
                isActive = false
            )
        } finally {
            if (apkFile.exists()) {
                apkFile.delete()
            }
        }
    }

    private suspend fun sendPushResult(
        targetNodeId: String,
        stage: String,
        success: Boolean,
        errorMessage: String?,
        slotId: String?,
        isActive: Boolean
    ) {
        val target = if (targetNodeId.isNotEmpty()) targetNodeId else pendingSenderNodeId
        if (target.isEmpty()) {
            Log.w(TAG, "Cannot send push result, targetNodeId is empty")
            return
        }

        try {
            val reply = JSONObject().apply {
                put("stage", stage)
                put("success", success)
                if (errorMessage != null) put("error", errorMessage)
                if (slotId != null) put("slotId", slotId)
                put("isActive", isActive)
                put("packageName", pendingPackageName)
            }
            Log.i(TAG, "Sending push result to $target: $reply")
            Wearable.getMessageClient(applicationContext)
                .sendMessage(target, PATH_PUSH_RESULT, reply.toString().toByteArray())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending push result back to phone", e)
        }
    }
}
