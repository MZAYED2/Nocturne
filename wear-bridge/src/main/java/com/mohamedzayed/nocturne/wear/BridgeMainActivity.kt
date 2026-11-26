package com.mohamedzayed.nocturne.wear

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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

data class InstalledSlotInfo(
    val slotId: String,
    val packageName: String,
    val isActive: Boolean
)

class BridgeMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "NocturneBridge"
    }

    private var bridgeInstallInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BridgeScreen(
                onCheckPermission = {
                    ContextCompat.checkSelfPermission(
                        this,
                        "com.google.wear.permission.SET_PUSHED_WATCH_FACE_AS_ACTIVE"
                    ) == PackageManager.PERMISSION_GRANTED
                },
                onQueryInstalled = {
                    queryInstalledFaces()
                },
                onInstallFromAssets = { callback ->
                    installSageFromAssets(callback)
                },
                onCheckBridgeUpdate = {
                    val file = File(filesDir, "Nocturne-WearBridge-update.apk")
                    file.takeIf { it.isFile && isValidBridgeUpdate(this, it) }
                },
                onInstallBridgeUpdate = { file ->
                    installBridgeApk(file)
                }
            )
        }
    }

    private fun installBridgeApk(apkFile: File) {
        if (bridgeInstallInProgress) return
        if (!packageManager.canRequestPackageInstalls()) {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                Toast.makeText(this, "امنح إذن التثبيت ثم اضغط تثبيت مرة أخرى", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.w(TAG, "Wear OS can't open unknown app installation settings", e)
                Toast.makeText(this, "ثبّت WearBridge من مصدره الرسمي على الساعة", Toast.LENGTH_LONG).show()
            }
            return
        }
        bridgeInstallInProgress = true
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { commitBridgeUpdate(this@BridgeMainActivity, apkFile) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not start WearBridge update", e)
                Toast.makeText(this@BridgeMainActivity, e.localizedMessage, Toast.LENGTH_LONG).show()
            } finally {
                bridgeInstallInProgress = false
            }
        }
    }

    private suspend fun queryInstalledFaces(): Pair<Int, List<InstalledSlotInfo>> = withContext(Dispatchers.IO) {
        try {
            val pushManager = WatchFacePushManagerFactory.createWatchFacePushManager(applicationContext)
            val list = pushManager.listWatchFaces()
            val details = list.installedWatchFaceDetails.map { item ->
                val isActive = try {
                    pushManager.isWatchFaceActive(item.packageName)
                } catch (_: Exception) {
                    false
                }
                InstalledSlotInfo(item.slotId, item.packageName, isActive)
            }
            Pair(list.remainingSlotCount, details)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query installed watch faces", e)
            Pair(0, emptyList())
        }
    }

    private fun installSageFromAssets(callback: (Boolean, String) -> Unit) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val apk = File.createTempFile("sage-", ".apk", cacheDir)
                try {
                    val catalog = assets.open("catalog.json").bufferedReader().use { org.json.JSONObject(it.readText()) }
                    val faces = catalog.getJSONArray("watchfaces")
                    val entries = (0 until faces.length()).map(faces::getJSONObject)
                    val face = entries.first { it.getString("id") == "sage_nocturne" }
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                    assets.open(face.getString("apk_filename")).use { input ->
                        java.security.DigestInputStream(input, digest).use { verified ->
                            apk.outputStream().use { verified.copyTo(it) }
                        }
                    }
                    val hash = digest.digest().joinToString("") { "%02x".format(it) }
                    check(apk.length() == face.getLong("size_bytes") && hash == face.getString("sha256")) {
                        "حزمة Sage المدمجة لا تطابق بيانات التحقق"
                    }
                    val result = installWatchFace(
                        this@BridgeMainActivity, apk, face.getString("package_name"),
                        face.getString("validation_token"), "local-sage"
                    ) {}
                    true to "✓ تم التحقق من تفعيل Sage في الخانة ${result.slotId}"
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Local Sage installation failed", e)
                    false to ("تعذر تفعيل Sage: " + e.localizedMessage)
                } finally {
                    apk.delete()
                }
            }
            callback(result.first, result.second)
        }
    }
}

@Composable
fun BridgeScreen(
    onCheckPermission: () -> Boolean,
    onQueryInstalled: suspend () -> Pair<Int, List<InstalledSlotInfo>>,
    onInstallFromAssets: ((Boolean, String) -> Unit) -> Unit,
    onCheckBridgeUpdate: () -> File?,
    onInstallBridgeUpdate: (File) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(onCheckPermission()) }
    var statusText by remember { mutableStateOf("خدمة الربط: نشطة وتستمع للموبايل ✓") }
    var remainingSlots by remember { mutableStateOf(1) }
    var installedFaces by remember { mutableStateOf<List<InstalledSlotInfo>>(emptyList()) }
    var isOperating by remember { mutableStateOf(false) }
    var pendingUpdateFile by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    fun refreshState() {
        pendingUpdateFile = onCheckBridgeUpdate()
        coroutineScope.launch {
            val (slots, list) = onQueryInstalled()
            remainingSlots = slots
            installedFaces = list
        }
    }

    LaunchedEffect(Unit) {
        refreshState()
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

        // Installed info
        Text(
            text = "الخانات المتبقية: $remainingSlots | المثبتة: ${installedFaces.size}",
            color = Color(0xFFBAC7D5),
            fontSize = 10.sp
        )

        installedFaces.forEach { item ->
            Text(
                text = "• خانة ${item.slotId}: ${if (item.isActive) "نشطة الآن ✓" else "مثبتة وغير نشطة"}",
                color = if (item.isActive) Color(0xFF4CAF50) else Color(0xFFFFA000),
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (pendingUpdateFile != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .background(Color(0xFF1E2836))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تحديث جديد متوفر لـ WearBridge!",
                    color = Color(0xFFFFD54F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        pendingUpdateFile?.let { onInstallBridgeUpdate(it) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF57F17)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تثبيت التحديث الآن",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Refresh Button
        Button(
            onClick = {
                refreshState()
                statusText = "تم تحديث حالة الواجهات المثبتة بنجاح"
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1B2A38)
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Text(
                text = "تحديث وفحص الواجهات",
                color = Color(0xFF81D4FA),
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Local real test button
        Button(
            onClick = {
                isOperating = true
                statusText = "جاري تثبيت وتفعيل Sage فعلياً..."
                onInstallFromAssets { success, msg ->
                    isOperating = false
                    statusText = msg
                    refreshState()
                }
            },
            enabled = !isOperating,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1B4332)
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (isOperating) {
                CircularProgressIndicator(
                    modifier = Modifier.height(14.dp).width(14.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "تثبيت Sage من الذاكرة (اختبار فعلي)",
                    color = Color(0xFFD8F3DC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
