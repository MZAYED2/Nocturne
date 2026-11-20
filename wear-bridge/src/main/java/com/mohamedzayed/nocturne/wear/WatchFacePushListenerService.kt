package com.mohamedzayed.nocturne.wear

import android.content.Context
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
import java.io.FileOutputStream

class WatchFacePushListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "NocturneBridge"
        const val PATH_PING = "/nocturne/ping"
        const val PATH_PONG = "/nocturne/pong"
        const val PATH_PUSH_DIRECT = "/nocturne/push_direct"
        const val PATH_PUSH_CHANNEL = "/nocturne/push_channel"
        const val PATH_PUSH_RESULT = "/nocturne/push_result"
        const val PATH_LIST = "/nocturne/list_installed"
        const val PATH_SET_ACTIVE = "/nocturne/set_active"
    }

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
                            put("version", "1.0")
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

            PATH_PUSH_DIRECT -> {
                scope.launch {
                    handleDirectPush(messageEvent.data, senderNodeId)
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
        Log.i(TAG, "Channel opened on path: $path")

        if (path.startsWith(PATH_PUSH_CHANNEL)) {
            scope.launch {
                handleChannelPush(channel)
            }
        }
    }

    private suspend fun handleChannelPush(channel: ChannelClient.Channel) {
        val tempApk = File(cacheDir, "incoming_pushed_wf.apk")
        try {
            val channelClient = Wearable.getChannelClient(applicationContext)
            val inputStream = channelClient.getInputStream(channel).await()

            // 1. First 4 bytes indicate length of JSON header
            val headerLenBytes = ByteArray(4)
            var read = 0
            while (read < 4) {
                val r = inputStream.read(headerLenBytes, read, 4 - read)
                if (r == -1) break
                read += r
            }

            val headerLen = ((headerLenBytes[0].toInt() and 0xFF) shl 24) or
                    ((headerLenBytes[1].toInt() and 0xFF) shl 16) or
                    ((headerLenBytes[2].toInt() and 0xFF) shl 8) or
                    (headerLenBytes[3].toInt() and 0xFF)

            val headerBytes = ByteArray(headerLen)
            read = 0
            while (read < headerLen) {
                val r = inputStream.read(headerBytes, read, headerLen - read)
                if (r == -1) break
                read += r
            }

            val header = JSONObject(String(headerBytes))
            val packageName = header.getString("packageName")
            val token = header.getString("token")
            val setActive = header.optBoolean("setActive", true)

            // 2. Remaining bytes are the APK file
            FileOutputStream(tempApk).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }

            // 3. Install via WatchFacePushManager
            installWatchFaceApk(tempApk, packageName, token, setActive, channel.nodeId)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling channel push", e)
            sendPushResult(channel.nodeId, false, e.message ?: "Failed in channel transfer", null, false)
        } finally {
            if (tempApk.exists()) {
                tempApk.delete()
            }
        }
    }

    private suspend fun handleDirectPush(data: ByteArray, senderNodeId: String) {
        val tempApk = File(cacheDir, "direct_pushed_wf.apk")
        try {
            // First 4 bytes: header length
            val headerLen = ((data[0].toInt() and 0xFF) shl 24) or
                    ((data[1].toInt() and 0xFF) shl 16) or
                    ((data[2].toInt() and 0xFF) shl 8) or
                    (data[3].toInt() and 0xFF)

            val headerStr = String(data, 4, headerLen)
            val header = JSONObject(headerStr)
            val packageName = header.getString("packageName")
            val token = header.getString("token")
            val setActive = header.optBoolean("setActive", true)

            val apkOffset = 4 + headerLen
            val apkLength = data.size - apkOffset

            FileOutputStream(tempApk).use { output ->
                output.write(data, apkOffset, apkLength)
                output.flush()
            }

            installWatchFaceApk(tempApk, packageName, token, setActive, senderNodeId)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling direct push", e)
            sendPushResult(senderNodeId, false, e.message ?: "Failed in direct push", null, false)
        } finally {
            if (tempApk.exists()) {
                tempApk.delete()
            }
        }
    }

    private suspend fun installWatchFaceApk(
        apkFile: File,
        packageName: String,
        token: String,
        setActive: Boolean,
        senderNodeId: String
    ) {
        val pushManager = WatchFacePushManagerFactory.createWatchFacePushManager(applicationContext)
        val pfd = ParcelFileDescriptor.open(apkFile, ParcelFileDescriptor.MODE_READ_ONLY)

        var finalSlotId: String? = null
        var isNowActive = false

        try {
            val list = pushManager.listWatchFaces()
            val existing = list.installedWatchFaceDetails.firstOrNull { it.packageName == packageName }

            if (existing != null) {
                Log.i(TAG, "Updating existing watch face in slot ${existing.slotId}")
                pushManager.updateWatchFace(existing.slotId, pfd, token)
                finalSlotId = existing.slotId
            } else if (list.remainingSlotCount > 0) {
                Log.i(TAG, "Adding new watch face")
                val slot = pushManager.addWatchFace(pfd, token)
                finalSlotId = slot.slotId
            } else {
                val replaceSlot = list.installedWatchFaceDetails.firstOrNull()?.slotId ?: "0"
                Log.i(TAG, "No slots remaining, replacing slot $replaceSlot")
                pushManager.updateWatchFace(replaceSlot, pfd, token)
                finalSlotId = replaceSlot
            }

            if (setActive && finalSlotId != null) {
                try {
                    pushManager.setWatchFaceAsActive(finalSlotId)
                    isNowActive = true
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set watch face as active (might require manual select or permission)", e)
                    isNowActive = false
                }
            }

            sendPushResult(senderNodeId, true, null, finalSlotId, isNowActive)

        } catch (e: Exception) {
            Log.e(TAG, "WatchFacePushManager failed", e)
            sendPushResult(senderNodeId, false, e.message ?: "Push installation failed", null, false)
        } finally {
            pfd.close()
        }
    }

    private suspend fun sendPushResult(
        targetNodeId: String,
        success: Boolean,
        errorMessage: String?,
        slotId: String?,
        isActive: Boolean
    ) {
        try {
            val reply = JSONObject().apply {
                put("success", success)
                if (errorMessage != null) put("error", errorMessage)
                if (slotId != null) put("slotId", slotId)
                put("isActive", isActive)
            }
            Wearable.getMessageClient(applicationContext)
                .sendMessage(targetNodeId, PATH_PUSH_RESULT, reply.toString().toByteArray())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending push result back to phone", e)
        }
    }
}
