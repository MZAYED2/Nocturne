package com.mohamedzayed.nocturne.update

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.mohamedzayed.nocturne.ConnectedBridge
import com.mohamedzayed.nocturne.findWatchBridge
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateCenterDialog(
    initialRelease: ReleaseInfo?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UpdatePreferences(context) }

    var autoCheck by remember { mutableStateOf(prefs.autoCheck) }
    var autoDownload by remember { mutableStateOf(prefs.autoDownload) }
    var wifiOnly by remember { mutableStateOf(prefs.wifiOnly) }

    var latestRelease by remember { mutableStateOf(initialRelease) }
    var isChecking by remember { mutableStateOf(false) }
    var checkMessage by remember { mutableStateOf<String?>(null) }

    var isDownloadingPhone by remember { mutableStateOf(false) }
    var phoneDownloadProgress by remember { mutableFloatStateOf(0f) }
    var phoneDownloadStatus by remember { mutableStateOf("") }
    var phoneDownloadedFile by remember { mutableStateOf<File?>(null) }

    var isTransferringBridge by remember { mutableStateOf(false) }
    var bridgeTransferStatus by remember { mutableStateOf("") }

    var connectedBridge by remember { mutableStateOf<ConnectedBridge?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            connectedBridge = try {
                findWatchBridge(context)
            } catch (_: com.google.android.gms.common.api.ApiException) {
                null
            }
            delay(8000)
        }
    }

    val hasNewVersion = latestRelease?.let(UpdateManager::isNewerThanCurrent) == true

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "مركز تحديثات Nocturne",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1E6D2),
                        fontSize = 17.sp
                    )
                    Text(
                        text = "تحديث مباشر فوق النسخة القديمة من GitHub",
                        color = Color(0xFF8A99A8),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // 1. Version Status Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141A22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232D3B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("إصدار تطبيق الموبايل الحالي:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    "v${UpdateManager.currentVersionName} (Build ${UpdateManager.currentVersionCode})",
                                    color = Color(0xFFF1E6D2),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            if (hasNewVersion) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Text(
                                        "يتوفر ${latestRelease?.tagName}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        if (latestRelease == null) "لم يتم الفحص" else "مُحدّث ✓",
                                        color = Color(0xFF81D4FA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (latestRelease != null && hasNewVersion) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "أحدث إصدار متوفر على GitHub: ${latestRelease?.tagName}",
                                color = Color(0xFFD4AF37),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (checkMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(checkMessage!!, color = Color(0xFF81D4FA), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Check Button
                OutlinedButton(
                    onClick = {
                        isChecking = true
                        checkMessage = "جاري الاتصال بـ GitHub للتحقق من الإصدارات..."
                        scope.launch {
                            val res = UpdateManager.checkLatestRelease()
                            isChecking = false
                            if (res.isSuccess) {
                                val rel = res.getOrThrow()
                                latestRelease = rel
                                prefs.lastCheckTimestamp = System.currentTimeMillis()
                                if (UpdateManager.isNewerThanCurrent(rel)) {
                                    checkMessage = "تم العثور على إصدار جديد: ${rel.tagName}"
                                } else {
                                    checkMessage = "أنت تستخدم أحدث إصدار متوفر (${rel.tagName}) ✓"
                                }
                            } else {
                                checkMessage = "تعذر الفحص: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF81D4FA))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري الفحص...", fontSize = 12.sp, color = Color(0xFF81D4FA))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF81D4FA))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص التحديثات الآن", fontSize = 12.sp, color = Color(0xFF81D4FA))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Actions: Update Phone App
                if (hasNewVersion && latestRelease?.phoneApkUrl != null) {
                    val rel = latestRelease!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1B2A1E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحديث تطبيق الموبايل (${rel.tagName})", color = Color(0xFFE8F5E9), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            val sizeMb = if (rel.phoneApkSize > 0) String.format("%.1f MB", rel.phoneApkSize / (1024f * 1024f)) else "APK"
                            Text("حجم الحزمة: $sizeMb — التثبيت المباشر فوق النسخة الحالية بدون فقد البيانات.", color = Color(0xFFC8E6C9), fontSize = 11.sp)

                            if (isDownloadingPhone) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { phoneDownloadProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF81C784)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(phoneDownloadStatus, color = Color(0xFFE2E8F0), fontSize = 11.sp)
                            }

                            if (!isDownloadingPhone && phoneDownloadStatus.isNotEmpty()) {
                                Text(phoneDownloadStatus, color = Color(0xFFE2E8F0), fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (phoneDownloadedFile != null && phoneDownloadedFile!!.exists()) {
                                Button(
                                    onClick = {
                                        val installRes = UpdateManager.installPhoneApk(context, phoneDownloadedFile!!)
                                        if (installRes.isFailure) {
                                            Toast.makeText(context, installRes.exceptionOrNull()?.message ?: "خطأ في التثبيت", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تثبيت التحديث الآن (Install Update)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isDownloadingPhone = true
                                        phoneDownloadStatus = "جاري الاتصال بالسيرفر..."
                                        scope.launch {
                                            val destFile = File(context.cacheDir, "Nocturne-Phone-${rel.numericVersion}.apk")
                                            val dlRes = UpdateManager.downloadApk(
                                                context = context,
                                                urlString = rel.phoneApkUrl!!,
                                                destinationFile = destFile,
                                                expectedSize = rel.phoneApkSize,
                                                expectedSha256 = rel.phoneApkSha256,
                                                wifiOnly = wifiOnly,
                                                onProgress = { fraction, current, total ->
                                                    phoneDownloadProgress = fraction
                                                    val curMb = current / (1024 * 1024)
                                                    val totMb = if (total > 0) total / (1024 * 1024) else 0L
                                                    phoneDownloadStatus = "جاري التنزيل: ${curMb}MB / ${totMb}MB (${(fraction * 100).toInt()}%)"
                                                }
                                            )
                                            if (dlRes.isSuccess) {
                                                val file = dlRes.getOrThrow()
                                                phoneDownloadStatus = "فحص سلامة ومطابقة التوقيع الرقمي..."
                                                val isSignatureValid = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    UpdateManager.verifyApkSignerMatchesCurrentApp(context, file, rel.numericVersion)
                                                }
                                                if (isSignatureValid) {
                                                    phoneDownloadedFile = file
                                                    phoneDownloadStatus = "اكتمل التنزيل والتحقق بنجاح! جاهز للتثبيت."
                                                    UpdateManager.installPhoneApk(context, file).onFailure {
                                                        phoneDownloadStatus = it.message ?: "تعذر فتح المثبّت"
                                                    }
                                                } else {
                                                    phoneDownloadStatus = "تم رفض التحديث: الحزمة أو إصدارها أو توقيعها غير مطابق."
                                                    phoneDownloadedFile = null
                                                    file.delete()
                                                }
                                            } else {
                                                phoneDownloadStatus = "فشل التنزيل: ${dlRes.exceptionOrNull()?.message}"
                                            }
                                            isDownloadingPhone = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isDownloadingPhone
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تنزيل وتثبيت تحديث الموبايل", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val bridge = connectedBridge
                Text(
                    when {
                        bridge == null -> "WearBridge: لا توجد ساعة تستجيب"
                        bridge.protocol != 2 -> "WearBridge ${bridge.version} على ${bridge.node.displayName}: حدّثه على الساعة من مصدره الرسمي"
                        else -> "WearBridge ${bridge.version} (${bridge.versionCode}) على ${bridge.node.displayName}"
                    },
                    color = Color(0xFFE2E8F0), fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 3. Actions: Update WearBridge on Watch
                if (latestRelease?.bridgeApkUrl != null && bridge?.protocol == 2 &&
                    latestRelease!!.numericVersion > bridge.versionCode) {
                    val rel = latestRelease!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E2633),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334E68)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Watch, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحديث برنامج الساعة (WearBridge)", color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "تنزيل ونقل حزمة WearBridge (${rel.tagName}) عبر البلوتوث للساعة لتحديثها مباشرة فوق القديمة.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )

                            if (bridgeTransferStatus.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(bridgeTransferStatus, color = Color(0xFF81D4FA), fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isTransferringBridge = true
                                    bridgeTransferStatus = "جاري تنزيل حزمة WearBridge من GitHub..."
                                    scope.launch {
                                        val destFile = File(context.cacheDir, "Nocturne-WearBridge-${rel.numericVersion}.apk")
                                        val dlRes = UpdateManager.downloadApk(
                                            context = context,
                                            urlString = rel.bridgeApkUrl!!,
                                            destinationFile = destFile,
                                            expectedSize = rel.bridgeApkSize,
                                            expectedSha256 = rel.bridgeApkSha256,
                                            wifiOnly = wifiOnly,
                                            onProgress = { fraction, current, total ->
                                                val curMb = current / (1024 * 1024)
                                                val totMb = if (total > 0) total / (1024 * 1024) else 0L
                                                bridgeTransferStatus = "تنزيل الحزمة: ${curMb}MB / ${totMb}MB (${(fraction * 100).toInt()}%)"
                                            }
                                        )

                                        if (dlRes.isSuccess) {
                                            val apk = dlRes.getOrThrow()
                                            bridgeTransferStatus = "جاري نقل الحزمة للساعة عبر البلوتوث..."
                                            val transferRes = UpdateManager.transferBridgeApkToWatch(context, apk, rel.numericVersion) { progress ->
                                                bridgeTransferStatus = progress
                                            }
                                            isTransferringBridge = false
                                            if (transferRes.isSuccess) {
                                                bridgeTransferStatus = "الساعة أكدت استلام الحزمة والتحقق منها. افتح WearBridge ووافق على التثبيت."
                                            } else {
                                                bridgeTransferStatus = "فشل النقل للساعة: ${transferRes.exceptionOrNull()?.message}"
                                            }
                                        } else {
                                            isTransferringBridge = false
                                            bridgeTransferStatus = "فشل تنزيل حزمة الساعة: ${dlRes.exceptionOrNull()?.message}"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isTransferringBridge
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نقل تحديث WearBridge إلى الساعة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 4. Update Preferences
                Text("إعدادات التحديث التلقائي", color = Color(0xFFBAC7D5), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF141A22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232D3B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("فحص التحديثات تلقائياً (Auto-check)", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("فحص دوري عند فتح التطبيق وكل 24 ساعة", color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                            Switch(
                                checked = autoCheck,
                                onCheckedChange = {
                                    autoCheck = it
                                    prefs.autoCheck = it
                                    if (it) UpdateCheckWorker.schedule(context, wifiOnly)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37), checkedTrackColor = Color(0xFF5A481B))
                            )
                        }

                        Divider(color = Color(0xFF232D3B), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تنزيل التحديثات تلقائياً (Auto-download)", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("تنزيل ملفات التحديث في الخلفية وإشعارك عند الجاهزية", color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                            Switch(
                                checked = autoDownload,
                                onCheckedChange = {
                                    autoDownload = it
                                    prefs.autoDownload = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37), checkedTrackColor = Color(0xFF5A481B))
                            )
                        }

                        Divider(color = Color(0xFF232D3B), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("التنزيل عبر Wi-Fi فقط (Wi-Fi only)", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("منع استهلاك باقة بيانات الهاتف أثناء التنزيل", color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                            Switch(
                                checked = wifiOnly,
                                onCheckedChange = {
                                    wifiOnly = it
                                    prefs.wifiOnly = it
                                    if (autoCheck) UpdateCheckWorker.schedule(context, it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37), checkedTrackColor = Color(0xFF5A481B))
                            )
                        }
                    }
                }

                // 5. Release Notes / Changelog
                if (latestRelease != null && latestRelease!!.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("سجل التغييرات وملاحظات الإصدار (${latestRelease!!.tagName})", color = Color(0xFFBAC7D5), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F141C),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2633)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = latestRelease!!.body,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("إغلاق", color = Color(0xFF81D4FA), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1A212B),
        shape = RoundedCornerShape(16.dp)
    )
}
