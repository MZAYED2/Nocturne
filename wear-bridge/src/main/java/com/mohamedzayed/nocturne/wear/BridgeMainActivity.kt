package com.mohamedzayed.nocturne.wear

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.watchfacepush.WatchFacePushManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BridgeMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "NocturneBridge"
        const val ACTION_TEST_PUSH = "com.mohamedzayed.nocturne.wear.TEST_PUSH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            BridgeScreen(
                onCheckPermission = {
                    ContextCompat.checkSelfPermission(
                        this,
                        "com.google.wear.permission.SET_PUSHED_WATCH_FACE_AS_ACTIVE"
                    ) == PackageManager.PERMISSION_GRANTED
                },
                onTestPushEmerald = { callback ->
                    testPushEmerald(callback)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_TEST_PUSH) {
            val apkPath = intent.getStringExtra("apkPath") ?: "/sdcard/Download/Nocturne-Emerald-Push.apk"
            val token = intent.getStringExtra("token") ?: "EmGVHYSZjOXqn2847E1derHhu/D+eHyn1ZlJLggCpr0=:MS4wLjA="
            Log.i(TAG, "Received ACTION_TEST_PUSH for $apkPath")
            pushApk(File(apkPath), token) { success, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun testPushEmerald(callback: (Boolean, String) -> Unit) {
        val emeraldFile = File(cacheDir, "Nocturne-Emerald-Push.apk")
        val token = "W07v05D8pJ0R3om235ZHsB8E8HqvE0yIlR+KqKBc540=:MS4wLjA="
        pushApk(emeraldFile, token, callback)
    }

    private fun pushApk(file: File, token: String, callback: (Boolean, String) -> Unit) {
        if (!file.exists()) {
            callback(false, "File not found: ${file.name}")
            return
        }

        val pushManager = WatchFacePushManagerFactory.createWatchFacePushManager(applicationContext)

        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val list = pushManager.listWatchFaces()
                val existing = list.installedWatchFaceDetails.firstOrNull()

                val slotId = if (existing != null) {
                    Log.i(TAG, "Updating slot ${existing.slotId}")
                    pushManager.updateWatchFace(existing.slotId, pfd, token)
                    existing.slotId
                } else {
                    Log.i(TAG, "Adding new watch face")
                    val slot = pushManager.addWatchFace(pfd, token)
                    slot.slotId
                }
                pfd.close()

                var activeSet = false
                try {
                    pushManager.setWatchFaceAsActive(slotId)
                    activeSet = true
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set watch face active", e)
                }

                withContext(Dispatchers.Main) {
                    callback(true, "✓ تم تثبيت الواجهة في خانة $slotId (نشطة: $activeSet)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Push failed", e)
                withContext(Dispatchers.Main) {
                    callback(false, "فشل التثبيت: ${e.message}")
                }
            }
        }
    }
}

@Composable
fun BridgeScreen(
    onCheckPermission: () -> Boolean,
    onTestPushEmerald: ((Boolean, String) -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(onCheckPermission()) }
    var statusText by remember { mutableStateOf("جاهز لمزامنة الوشوش من الموبايل") }
    var installedInfo by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    fun refreshInstalled() {
        // Will refresh when needed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B0E))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NOCTURNE BRIDGE",
            color = Color(0xFF86A789),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusText,
            color = Color(0xFFD4E2D4),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!hasPermission) {
            Button(
                onClick = {
                    launcher.launch("com.google.wear.permission.SET_PUSHED_WATCH_FACE_AS_ACTIVE")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C3E30)
                ),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(
                    text = "منح إذن التفعيل التلقائي",
                    color = Color(0xFFFFF0D8),
                    fontSize = 10.sp
                )
            }
        } else {
            Text(
                text = "✓ إذن التفعيل التلقائي مفعّل",
                color = Color(0xFF86A789),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Test Emerald button
        Button(
            onClick = {
                statusText = "جاري تثبيت Emerald..."
                onTestPushEmerald { success, msg ->
                    statusText = msg
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1B4332)
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Text(
                text = "اختبار تثبيت Emerald (تعديل الوش)",
                color = Color(0xFFD8F3DC),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
