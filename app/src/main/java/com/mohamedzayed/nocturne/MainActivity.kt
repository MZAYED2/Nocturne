package com.mohamedzayed.nocturne

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import com.mohamedzayed.nocturne.update.ReleaseInfo
import com.mohamedzayed.nocturne.update.UpdateCenterDialog
import com.mohamedzayed.nocturne.update.UpdateCheckWorker
import com.mohamedzayed.nocturne.update.UpdateManager
import com.mohamedzayed.nocturne.update.UpdatePreferences
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Date
import kotlin.coroutines.resume
import kotlin.math.*

enum class DisplayMode {
    ACTIVE,
    AOD
}

data class WatchFaceItem(
    val id: String,
    val name: String,
    val nameAr: String,
    val description: String,
    val descriptionAr: String,
    val packageName: String,
    val validationToken: String,
    val apkFilename: String,
    val downloadUrl: String,
    val preview: String,
    val versionCode: Int,
    val versionName: String
)

sealed class PushState {
    object Idle : PushState()
    data class Downloading(val progress: Float, val detail: String) : PushState()
    data class Validating(val detail: String) : PushState()
    data class Connecting(val detail: String) : PushState()
    data class Transferring(val progress: Float, val detail: String) : PushState()
    data class Installing(val detail: String) : PushState()
    data class Success(val message: String, val slotId: String?) : PushState()
    data class Error(val stage: String, val message: String, val hint: String) : PushState()
}

data class ColorOption(val id: String, val nameAr: String, val color: Color)
data class AccentOption(val id: String, val nameAr: String, val color: Color)

val backgroundOptions = listOf(
    ColorOption("black", "أسود خالص", Color(0xFF000000)),
    ColorOption("navy", "أزرق ليلي", Color(0xFF070F19)),
    ColorOption("teal", "تركواز داكن", Color(0xFF061713)),
    ColorOption("warm", "أسود دافئ", Color(0xFF17110B)),
    ColorOption("graphite", "جرافيت", Color(0xFF15171B))
)

val accentOptions = listOf(
    AccentOption("sage", "أخضر سيج (الأصلي)", Color(0xFF8FA89B)),
    AccentOption("blue", "أزرق نيلي", Color(0xFF5B84A6)),
    AccentOption("gold", "ذهبي عتيق", Color(0xFFD4AF37)),
    AccentOption("emerald", "زمردي", Color(0xFF388E3C)),
    AccentOption("silver", "فضي قمري", Color(0xFFCFD8DC))
)

val ornamentOptions = listOf(
    "balanced" to "متوازن",
    "minimal" to "بسيط",
    "ornate" to "مزخرف",
    "none" to "بدون"
)

val dialOptions = listOf(
    "full" to "جميع الأرقام",
    "cardinal" to "الأرباع فقط",
    "markers" to "علامات فقط"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val shouldOpenUpdates = intent?.getBooleanExtra("open_updates_center", false) ?: false
        setContent {
            NocturneTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    NocturneScreen(
                        modifier = Modifier.padding(innerPadding),
                        initialOpenUpdates = shouldOpenUpdates
                    )
                }
            }
        }
    }
}

@Composable
fun NocturneScreen(modifier: Modifier = Modifier, initialOpenUpdates: Boolean = false) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val updatePrefs = remember { UpdatePreferences(context) }
    var latestReleaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var hasUpdateAvailable by remember { mutableStateOf(false) }
    var showUpdateCenterDialog by remember { mutableStateOf(initialOpenUpdates) }

    LaunchedEffect(Unit) {
        if (updatePrefs.autoCheck) {
            UpdateCheckWorker.schedule(context, updatePrefs.wifiOnly)
            try {
                val relRes = UpdateManager.checkLatestRelease()
                if (relRes.isSuccess) {
                    val rel = relRes.getOrThrow()
                    latestReleaseInfo = rel
                    hasUpdateAvailable = UpdateManager.isNewerThanCurrent(rel)
                }
            } catch (_: Exception) {}
        }
    }

    val defaultCatalog = remember {
        listOf(
            WatchFaceItem(
                id = "sage_nocturne",
                name = "Sage Nocturne",
                nameAr = "Sage Nocturne (العثماني الأخضر)",
                description = "Ottoman Sage Green heritage dial with engraved calligraphy, small moon phase, and active/AOD modes.",
                descriptionAr = "التصميم العثماني الأخضر الأصلي مع الزخرفة العثمانية، القمر الصغير، الذكر، والتعقيدات الثلاث.",
                packageName = "com.mohamedzayed.nocturne.watchfacepush.nocturne",
                validationToken = "LEyUVVVxzrdjahyMZuKs1kuDJfvjhYbAsDZANJ8y8Gw=:MS4wLjA=",
                apkFilename = "Nocturne-Sage-Push.apk",
                downloadUrl = "https://github.com/MZAYED2/Nocturne/releases/latest/download/Nocturne-Sage-Push.apk",
                preview = "preview_sage.png",
                versionCode = 1,
                versionName = "1.0.0"
            ),
            WatchFaceItem(
                id = "lunar_phase",
                name = "Lunar Phase",
                nameAr = "Lunar Phase (القمر السينمائي)",
                description = "Large poetic astronomical moon disc filling the watch display, with luminous dual-contrast hands and minimal AOD dial.",
                descriptionAr = "قمر سينمائي كبير يملأ 92% من الشاشة بإضاءة ثلاثية الأبعاد دقيقة فلكياً عبر 29 منزلة، مع عقارب بتباين مزدوج، وشاشة AOD سوداء موفرة للطاقة.",
                packageName = "com.mohamedzayed.nocturne.watchfacepush.nocturne",
                validationToken = "gdlDw+2eKHp7UIsh884nEwZRDfaSnQa46Ve2sjQzFK0=:MS4wLjA=",
                apkFilename = "Nocturne-Lunar-Push.apk",
                downloadUrl = "https://github.com/MZAYED2/Nocturne/releases/latest/download/Nocturne-Lunar-Push.apk",
                preview = "preview_lunar.png",
                versionCode = 1,
                versionName = "1.0.0"
            ),
            WatchFaceItem(
                id = "obsidian_atelier",
                name = "Obsidian Atelier",
                nameAr = "Obsidian Atelier (الميكانيكي الفاخر)",
                description = "Masterpiece Haute Horlogerie mechanical dial in Brushed Platinum and Smoked Graphite with Dauphine hands, Small Seconds subdial, Power Reserve indicator, and date window.",
                descriptionAr = "تصميم ميكانيكي كلاسيكي فاخر بلون البلاتين المصقول والجرافيت الدخاني، مع عقارب دوفين المركزية، عقرب الثواني الصغير عند 6، مؤشر احتياطي الطاقة عند 9، ونافذة التاريخ المشطوفة.",
                packageName = "com.mohamedzayed.nocturne.watchfacepush.nocturne",
                validationToken = "/W6ruN8V/lOBkN9ZRlohS2vnErEUlhcSy8BYpzExTY8=:MS4wLjA=",
                apkFilename = "Nocturne-Obsidian-Push.apk",
                downloadUrl = "https://github.com/MZAYED2/Nocturne/releases/latest/download/Nocturne-Obsidian-Push.apk",
                preview = "preview_obsidian.png",
                versionCode = 1,
                versionName = "1.0.0"
            ),
            WatchFaceItem(
                id = "emerald_nocturne",
                name = "Emerald Nocturne",
                nameAr = "Emerald Nocturne (الزمردي الإمبراطوري)",
                description = "Imperial Emerald and Gold edition with ornate Ottoman pattern, golden hands, and emerald accents.",
                descriptionAr = "الإصدار الزمردي والذهبي الإمبراطوري بتعديل بصري واضح وعقارب ذهبية لاختبار وتأكيد التحديث اللاسلكي.",
                packageName = "com.mohamedzayed.nocturne.watchfacepush.nocturne",
                validationToken = "+PMlrhh0DSVpFbZYvKOea3AYQ6d89EvZbq5xRhRhz5U=:MS4wLjA=",
                apkFilename = "Nocturne-Emerald-Push.apk",
                downloadUrl = "https://github.com/MZAYED2/Nocturne/releases/latest/download/Nocturne-Emerald-Push.apk",
                preview = "preview_emerald.png",
                versionCode = 1,
                versionName = "1.0.0"
            )
        )
    }

    var catalog by remember { mutableStateOf(defaultCatalog) }
    var selectedItem by remember { mutableStateOf(defaultCatalog[0]) }

    var displayMode by remember { mutableStateOf(DisplayMode.ACTIVE) }
    var selectedBackground by remember { mutableStateOf(backgroundOptions[0]) }
    var selectedAccent by remember { mutableStateOf(accentOptions[0]) }
    var selectedOrnament by remember { mutableStateOf(ornamentOptions[0].first) }
    var selectedDial by remember { mutableStateOf(dialOptions[0].first) }

    val todayMoonPhase = remember { calculateCurrentMoonPhase() }
    var previewMoonPhase by remember { mutableFloatStateOf(todayMoonPhase.toFloat()) }

    var isConnectedToWatch by remember { mutableStateOf<Boolean?>(null) }
    var connectedWatchName by remember { mutableStateOf("") }
    var isBridgeActive by remember { mutableStateOf(false) }

    var pushState by remember { mutableStateOf<PushState>(PushState.Idle) }
    var showInstructionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loadedLocal = loadCatalogFromAssets(context)
        if (loadedLocal.isNotEmpty()) {
            catalog = loadedLocal
            selectedItem = loadedLocal[0]
        }
        val remoteCatalog = fetchRemoteCatalog()
        if (remoteCatalog.isNotEmpty()) {
            catalog = remoteCatalog
            val currentSelected = remoteCatalog.firstOrNull { it.id == selectedItem.id }
            if (currentSelected != null) {
                selectedItem = currentSelected
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isNotEmpty()) {
                val node = nodes[0]
                isConnectedToWatch = true
                connectedWatchName = node.displayName

                val messageClient = Wearable.getMessageClient(context)
                val pingListener = object : MessageClient.OnMessageReceivedListener {
                    override fun onMessageReceived(event: MessageEvent) {
                        if (event.path == "/nocturne/pong") {
                            isBridgeActive = true
                            messageClient.removeListener(this)
                        }
                    }
                }
                messageClient.addListener(pingListener)
                try {
                    messageClient.sendMessage(node.id, "/nocturne/ping", ByteArray(0)).await()
                } catch (_: Exception) {}
            } else {
                isConnectedToWatch = false
            }
        } catch (_: Exception) {
            isConnectedToWatch = false
        }
    }

    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = Calendar.getInstance()
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F141A)
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOCTURNE",
                        color = Color(0xFFF1E6D2),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "v${UpdateManager.currentVersionName} • Galaxy Watch 5",
                        color = Color(0xFF8A99A8),
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Update Center Button
                    Surface(
                        onClick = { showUpdateCenterDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (hasUpdateAvailable) Color(0xFF1B3828) else Color(0xFF1E2630),
                        border = BorderStroke(
                            1.dp,
                            if (hasUpdateAvailable) Color(0xFF4CAF50) else Color(0xFF334E68)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = if (hasUpdateAvailable) Color(0xFF81C784) else Color(0xFFD4AF37),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasUpdateAvailable) "تحديث متوفر!" else "التحديثات",
                                color = if (hasUpdateAvailable) Color(0xFFA5D6A7) else Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isConnectedToWatch == true) {
                            if (isBridgeActive) Color(0xFF1B3828) else Color(0xFF2E3321)
                        } else Color(0xFF262C36),
                        border = BorderStroke(
                            1.dp,
                            if (isConnectedToWatch == true) {
                                if (isBridgeActive) Color(0xFF2E7D32) else Color(0xFF8D7B1C)
                            } else Color(0xFF455A64)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isConnectedToWatch == true) {
                                            if (isBridgeActive) Color(0xFF4CAF50) else Color(0xFFFFC107)
                                        } else Color(0xFF9E9E9E)
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isConnectedToWatch == true) {
                                    if (connectedWatchName.isNotEmpty()) {
                                        if (isBridgeActive) "$connectedWatchName" else "$connectedWatchName"
                                    } else "الساعة متصلة"
                                } else {
                                    "في انتظار الساعة"
                                },
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mode Selector (Active vs AOD)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF171E26),
                border = BorderStroke(1.dp, Color(0xFF25303D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { displayMode = DisplayMode.ACTIVE },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (displayMode == DisplayMode.ACTIVE) Color(0xFF2E3E34) else Color.Transparent,
                            contentColor = if (displayMode == DisplayMode.ACTIVE) Color(0xFFF1E6D2) else Color(0xFF8FA1B2)
                        )
                    ) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active (النشط)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { displayMode = DisplayMode.AOD },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (displayMode == DisplayMode.AOD) Color(0xFF1E2833) else Color.Transparent,
                            contentColor = if (displayMode == DisplayMode.AOD) Color(0xFFF1E6D2) else Color(0xFF8FA1B2)
                        )
                    ) {
                        Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AOD (الاستعداد)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Realistic Watch Canvas Preview
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(310.dp)
                    .shadow(16.dp, CircleShape)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color(0xFF1C2229),
                    border = BorderStroke(5.dp, Brush.radialGradient(
                        listOf(Color(0xFF3A4754), Color(0xFF1A2027))
                    ))
                ) {
                    WatchFaceDialCanvas(
                        faceId = selectedItem.id,
                        displayMode = displayMode,
                        bgColor = if (selectedItem.id == "lunar_phase" && displayMode == DisplayMode.ACTIVE) Color(0xFF000000) else selectedBackground.color,
                        accentColor = selectedAccent.color,
                        ornamentType = selectedOrnament,
                        dialType = selectedDial,
                        moonPhaseIndex = previewMoonPhase.roundToInt().coerceIn(0, 28),
                        currentTime = currentTime
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedItem.id == "obsidian_atelier") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161C24),
                    border = BorderStroke(1.dp, Color(0xFF263342))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Watch,
                            contentDescription = null,
                            tint = Color(0xFFCFD8DC),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تعقيدات ميكانيكية: ثوانٍ مستقلة عند 6 • احتياطي طاقة عند 9 • نافذة تاريخ 4:30",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                val phaseInfo = getMoonPhaseName(previewMoonPhase.roundToInt().coerceIn(0, 28))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161C24),
                    border = BorderStroke(1.dp, Color(0xFF263342))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Brightness3,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "منزلة القمر: ${phaseInfo.first} (يوم ${previewMoonPhase.roundToInt()}/28)",
                            color = Color(0xFFF1E6D2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مكتبة واجهات Nocturne (${catalog.size})",
                    color = Color(0xFFBAC7D5),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val refreshed = fetchRemoteCatalog()
                            if (refreshed.isNotEmpty()) {
                                catalog = refreshed
                                snackbarHostState.showSnackbar("✓ تم تحديث قائمة الواجهات من GitHub بنجاح!")
                            } else {
                                snackbarHostState.showSnackbar("تعذر الاتصال بـ GitHub، استخدام القائمة المحلية.")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF81D4FA))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحديث من GitHub", fontSize = 11.sp, color = Color(0xFF81D4FA))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            catalog.forEach { item ->
                val isSelected = selectedItem.id == item.id
                Surface(
                    onClick = { selectedItem = item },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Color(0xFF222B35) else Color(0xFF141A22),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFD4AF37) else Color(0xFF263342))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (item.id) {
                                            "lunar_phase" -> Color(0xFF1A232E)
                                            "obsidian_atelier" -> Color(0xFF22262B)
                                            "emerald_nocturne" -> Color(0xFF0F3B25)
                                            else -> Color(0xFF1E3A2D)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.id) {
                                        "lunar_phase" -> Icons.Default.Brightness2
                                        "obsidian_atelier" -> Icons.Default.Watch
                                        "emerald_nocturne" -> Icons.Default.Diamond
                                        else -> Icons.Default.FilterVintage
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFD4AF37) else Color(0xFF8A99A8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.nameAr,
                                    color = if (isSelected) Color(0xFFF1E6D2) else Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.descriptionAr,
                                    color = Color(0xFF8A99A8),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD4AF37)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedItem.id == "sage_nocturne") {
                Text(
                    text = "لون خلفية الميناء",
                    color = Color(0xFFBAC7D5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    backgroundOptions.forEach { opt ->
                        FilterChip(
                            selected = selectedBackground == opt,
                            onClick = { selectedBackground = opt },
                            label = { Text(opt.nameAr, fontSize = 12.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(opt.color)
                                        .border(1.dp, Color(0xFF8A99A8), CircleShape)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2C3947),
                                selectedLabelColor = Color(0xFFF1E6D2),
                                containerColor = Color(0xFF161C24),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "لون الزخرفة والعقارب",
                    color = Color(0xFFBAC7D5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accentOptions.forEach { opt ->
                        FilterChip(
                            selected = selectedAccent == opt,
                            onClick = { selectedAccent = opt },
                            label = { Text(opt.nameAr, fontSize = 12.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(opt.color)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2C3947),
                                selectedLabelColor = Color(0xFFF1E6D2),
                                containerColor = Color(0xFF161C24),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }

            if (selectedItem.id != "obsidian_atelier") {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "معاينة منازل القمر الـ 29 (من المحاق إلى البدر):",
                    color = Color(0xFFBAC7D5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Slider(
                    value = previewMoonPhase,
                    onValueChange = { previewMoonPhase = it },
                    valueRange = 0f..28f,
                    steps = 27,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFF1E6D2),
                        activeTrackColor = Color(0xFFD4AF37),
                        inactiveTrackColor = Color(0xFF2C3947)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("محاق (0)", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("تربيع أول (7)", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("بدر كامل (14)", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("تربيع أخير (21)", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("محاق (28)", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress and Error State Rendering
            when (val st = pushState) {
                is PushState.Downloading -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF15222E),
                        border = BorderStroke(1.dp, Color(0xFF1E88E5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF81D4FA), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(st.detail, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { st.progress }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF81D4FA))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                is PushState.Validating -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A2633),
                        border = BorderStroke(1.dp, Color(0xFF0288D1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF29B6F6), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(st.detail, color = Color(0xFFE1F5FE), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                is PushState.Connecting -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1C2D24),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF66BB6A), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(st.detail, color = Color(0xFFE8F5E9), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                is PushState.Transferring -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF172922),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF4CAF50), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(st.detail, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { st.progress }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF4CAF50))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                is PushState.Installing -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF262315),
                        border = BorderStroke(1.dp, Color(0xFFFFA000)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFFFC107), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(st.detail, color = Color(0xFFFFF8E1), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                is PushState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF132A1C),
                        border = BorderStroke(1.dp, Color(0xFF388E3C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(st.message, color = Color(0xFFE8F5E9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (st.slotId != null) {
                                    Text("تم التحقق وتفعيل الخانة: ${st.slotId}", color = Color(0xFFA5D6A7), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                is PushState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2A1515),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("خطأ في مرحلة [${st.stage}]: ${st.message}", color = Color(0xFFFFEBEE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (st.hint.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("نصيحة الحل: ${st.hint}", color = Color(0xFFFFCDD2), fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                else -> {}
            }

            val isBusy = pushState is PushState.Downloading ||
                    pushState is PushState.Validating ||
                    pushState is PushState.Connecting ||
                    pushState is PushState.Transferring ||
                    pushState is PushState.Installing

            Button(
                onClick = {
                    coroutineScope.launch {
                        applyWatchFaceDirectly(
                            context = context,
                            item = selectedItem,
                            onStateChange = { pushState = it },
                            onSuccess = { msg, slotId ->
                                pushState = PushState.Success(msg, slotId)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onError = { stage, err, hint ->
                                pushState = PushState.Error(stage, err, hint)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("فشل في مرحلة [$stage]: $err")
                                }
                            }
                        )
                    }
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E6B4F),
                    contentColor = Color(0xFFFFFFFF),
                    disabledContainerColor = Color(0xFF1C3A2B),
                    disabledContentColor = Color(0xFF75877D)
                )
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("جاري التنفيذ والتطبيق على الساعة...", fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.Watch, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "تطبيق الواجهة على الساعة (Apply to Watch)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    openGalaxyWearable(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF384A5D)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF81D4FA)
                )
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فتح في تطبيق Galaxy Wearable",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = { showInstructionsDialog = true }
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("دليل التثبيت بدون Wireless Debugging عبر GeminiMan", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = {
                Text("دليل الربط والتحديث اللاسلكي التلقائي", fontWeight = FontWeight.Bold, color = Color(0xFFF1E6D2))
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. تثبيت برنامج الربط لأول مرة فقط (GeminiMan Wear OS Manager):\n" +
                                "• اسم الملف: Nocturne-WearBridge-v1.0.4.apk\n" +
                                "• ثبّت هذا الملف مرة واحدة فقط على الساعة بواسطة تطبيق GeminiMan على هاتفك أو عبر أي مدير Wear OS.\n" +
                                "• بعد التثبيت، افتح تطبيق Nocturne Bridge على الساعة ووافق على إذن التفعيل التلقائي إذا طُلب.\n\n" +
                                "2. التثبيت والتحديث التلقائي بدون تصحيح أخطاء (Wireless Debugging OFF):\n" +
                                "• يمكنك الآن إغلاق وتأمين تصحيح الأخطاء اللاسلكي نهائياً على الساعة.\n" +
                                "• من تطبيق Nocturne على الموبايل، اختر أي واجهة (Sage Nocturne أو Lunar Phase أو Emerald Nocturne) واضغط 'Apply to Watch'.\n" +
                                "• سيقوم التطبيق بنقل الواجهة وتثبيتها وتفعيلها فوراً على ساعتك في الخلفية!\n\n" +
                                "3. التحديثات والوشوش الجديدة من GitHub:\n" +
                                "• أي واجهة تُرفع على GitHub Releases ستظهر تلقائياً في مكتبة التطبيق دون الحاجة لتحديث برنامج الربط أو تطبيق الموبايل.",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstructionsDialog = false }) {
                    Text("فهمت ذلك", color = Color(0xFF81D4FA))
                }
            },
            containerColor = Color(0xFF1E2630),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showUpdateCenterDialog) {
        UpdateCenterDialog(
            initialRelease = latestReleaseInfo,
            onDismissRequest = { showUpdateCenterDialog = false }
        )
    }
}

// ---------------------------------------------------------
// Direct Push to Watch Implementation (Multi-Stage Robust Pipeline)
// ---------------------------------------------------------

suspend fun applyWatchFaceDirectly(
    context: Context,
    item: WatchFaceItem,
    onStateChange: (PushState) -> Unit,
    onSuccess: (String, String?) -> Unit,
    onError: (String, String, String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        // ---------------------------------------------------------
        // STAGE 1 & 2: Local Cache & GitHub Download with Validation
        // ---------------------------------------------------------
        onStateChange(PushState.Validating("فحص توفر حزمة ${item.name} محلياً..."))
        val apkFile = File(context.cacheDir, item.apkFilename)

        var isFileReady = isApkFileValid(apkFile)

        if (!isFileReady) {
            // Check bundled assets
            try {
                val assetList = context.assets.list("watchfaces") ?: emptyArray()
                if (assetList.contains(item.apkFilename)) {
                    onStateChange(PushState.Validating("نسخ الحزمة من أصول التطبيق المدمجة..."))
                    context.assets.open("watchfaces/${item.apkFilename}").use { input ->
                        FileOutputStream(apkFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    isFileReady = isApkFileValid(apkFile)
                }
            } catch (_: Exception) {}
        }

        if (!isFileReady) {
            // Download from GitHub Release with multi-redirect loop
            onStateChange(PushState.Downloading(0.02f, "1/5: جاري تنزيل ${item.name} من GitHub..."))
            val dlResult = downloadApkWithValidation(
                urlString = item.downloadUrl,
                destinationFile = apkFile,
                expectedMinBytes = 10_000_000L,
                onProgress = { fraction, current, total ->
                    val mbCurrent = current / (1024 * 1024)
                    val mbTotal = if (total > 0) total / (1024 * 1024) else 0L
                    val detail = if (mbTotal > 0) {
                        "1/5: جاري تنزيل الواجهة من GitHub ($mbCurrent / $mbTotal MB - ${(fraction * 100).toInt()}%)"
                    } else {
                        "1/5: جاري تنزيل الواجهة من GitHub ($mbCurrent MB)"
                    }
                    onStateChange(PushState.Downloading(fraction, detail))
                }
            )

            if (dlResult.isFailure) {
                val ex = dlResult.exceptionOrNull()
                onError(
                    "DOWNLOAD",
                    ex?.localizedMessage ?: "فشل تنزيل ملف الواجهة من GitHub",
                    "تأكد من اتصال الهاتف بالإنترنت، أو تحقق من وجود ملفات الإصدار على GitHub"
                )
                return@withContext
            }
        }

        // Final verification of APK integrity
        onStateChange(PushState.Validating("2/5: التحقق من سلامة الحزمة والترويسة..."))
        if (!isApkFileValid(apkFile)) {
            apkFile.delete()
            onError(
                "VALIDATE",
                "ملف الواجهة تالف أو غير صالح (الحجم: ${apkFile.length()} بايت)",
                "تم حذف الملف التالف، يرجى إعادة المحاولة لتحميله مجدداً"
            )
            return@withContext
        }

        // ---------------------------------------------------------
        // STAGE 3: Connected Watch Discovery & Bridge Handshake
        // ---------------------------------------------------------
        onStateChange(PushState.Connecting("3/5: البحث عن ساعة Galaxy Watch 5..."))
        val nodeClient = Wearable.getNodeClient(context)
        val nodes = nodeClient.connectedNodes.await()
        if (nodes.isEmpty()) {
            onError(
                "CONNECT",
                "لم يتم العثور على ساعة متصلة بالبلوتوث",
                "تأكد من اقتران Galaxy Watch 5 عبر تطبيق Galaxy Wearable وتشغيل البلوتوث"
            )
            return@withContext
        }
        val targetNode = nodes[0]

        // Handshake: Ping Bridge
        val messageClient = Wearable.getMessageClient(context)
        var bridgeResponding = false
        val pingListener = object : MessageClient.OnMessageReceivedListener {
            override fun onMessageReceived(event: MessageEvent) {
                if (event.path == "/nocturne/pong") {
                    bridgeResponding = true
                }
            }
        }
        messageClient.addListener(pingListener)
        try {
            messageClient.sendMessage(targetNode.id, "/nocturne/ping", ByteArray(0)).await()
            // Wait up to 3.5s for bridge pong
            var waited = 0
            while (!bridgeResponding && waited < 35) {
                delay(100)
                waited++
            }
        } catch (_: Exception) {}
        messageClient.removeListener(pingListener)

        if (!bridgeResponding) {
            onError(
                "CONNECT",
                "برنامج الربط (Nocturne-WearBridge) غير نشط على الساعة",
                "تأكد من تثبيت Nocturne-WearBridge-v1.0.4.apk على الساعة عبر GeminiMan وافتحه مرة واحدة"
            )
            return@withContext
        }

        // Send metadata prepare message
        onStateChange(PushState.Connecting("3/5: مصافحة تجهيز النقل مع الساعة..."))
        val preparePayload = JSONObject().apply {
            put("packageName", item.packageName)
            put("token", item.validationToken)
            put("apkFilename", item.apkFilename)
            put("fileSize", apkFile.length())
            put("setActive", true)
        }

        var readyForPush = false
        val readyListener = object : MessageClient.OnMessageReceivedListener {
            override fun onMessageReceived(event: MessageEvent) {
                if (event.path == "/nocturne/ready_for_push") {
                    readyForPush = true
                }
            }
        }
        messageClient.addListener(readyListener)
        try {
            messageClient.sendMessage(targetNode.id, "/nocturne/prepare_push", preparePayload.toString().toByteArray()).await()
            var waited = 0
            while (!readyForPush && waited < 40) {
                delay(100)
                waited++
            }
        } catch (_: Exception) {}
        messageClient.removeListener(readyListener)

        // ---------------------------------------------------------
        // STAGE 4: Channel File Transfer via ChannelClient
        // ---------------------------------------------------------
        val mbSize = apkFile.length() / (1024 * 1024)
        onStateChange(PushState.Transferring(0.1f, "4/5: جاري نقل حزمة الواجهة ($mbSize MB) للساعة عبر البلوتوث..."))

        val channelClient = Wearable.getChannelClient(context)
        val channel = channelClient.openChannel(targetNode.id, "/nocturne/push_channel").await()

        try {
            val fileUri = Uri.fromFile(apkFile)
            Log.i("NocturnePhone", "Sending file via channelClient.sendFile: $fileUri")
            channelClient.sendFile(channel, fileUri).await()
            Log.i("NocturnePhone", "sendFile completed successfully!")
            onStateChange(PushState.Transferring(1.0f, "4/5: اكتمل نقل الملف، جاري استلام وتثبيت الواجهة..."))
        } catch (e: Exception) {
            Log.e("NocturnePhone", "sendFile failed", e)
            try { channelClient.close(channel).await() } catch (_: Exception) {}
            onError(
                "TRANSFER",
                "انقطعت قناة الاتصال أثناء نقل الملف: ${e.localizedMessage}",
                "تأكد من بقاء شاشة الساعة مضاءة وقربها من الهاتف ثم أعد المحاولة"
            )
            return@withContext
        }

        // ---------------------------------------------------------
        // STAGE 5: Await Installation Result from Watch Bridge
        // ---------------------------------------------------------
        onStateChange(PushState.Installing("5/5: جاري تثبيت وتفعيل الواجهة على الساعة عبر نظام Wear OS..."))

        val resultJson = withTimeoutOrNull(90000L) {
            suspendCancellableCoroutine<JSONObject> { cont ->
                val resultListener = object : MessageClient.OnMessageReceivedListener {
                    override fun onMessageReceived(event: MessageEvent) {
                        if (event.path == "/nocturne/push_result") {
                            messageClient.removeListener(this)
                            try {
                                val json = JSONObject(String(event.data))
                                cont.resume(json)
                            } catch (e: Exception) {
                                cont.resume(JSONObject().put("success", false).put("error", e.message))
                            }
                        }
                    }
                }
                messageClient.addListener(resultListener)
                cont.invokeOnCancellation {
                    messageClient.removeListener(resultListener)
                }
            }
        }

        if (resultJson != null) {
            val success = resultJson.optBoolean("success", false)
            val slotId = resultJson.optString("slotId", null)
            val isActive = resultJson.optBoolean("isActive", false)

            if (success) {
                onSuccess("✓ تم تثبيت وتفعيل ${item.nameAr} على الساعة بنجاح!", slotId)
            } else {
                val stage = resultJson.optString("stage", "INSTALL")
                val errorMsg = resultJson.optString("error", "فشل التثبيت على الساعة")
                onError(stage, errorMsg, "تحقق من منح إذن التفعيل التلقائي في تطبيق Nocturne Bridge على الساعة")
            }
        } else {
            // Timeout awaiting confirmation
            onError(
                "TIMEOUT",
                "استغرق التثبيت وقتاً أطول من المتوقع على الساعة",
                "تحقق من شاشة الساعة، فقد تكون الواجهة قد تم تثبيتها وتنتظر الاختيار"
            )
        }

        // Backup config sync for backward compatibility
        try {
            val req = PutDataMapRequest.create("/nocturne/config").apply {
                dataMap.putString("watch_theme", item.id)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(req)
        } catch (_: Exception) {}

    } catch (e: Exception) {
        Log.e("NocturnePhone", "Push failed unexpected error", e)
        val msg = e.localizedMessage ?: e.message ?: "خطأ غير معروف"
        if (msg.contains("API_UNAVAILABLE", ignoreCase = true) || msg.contains("17: API", ignoreCase = true)) {
            onError(
                "CONNECT",
                "خدمة Wear OS غير متاحة أو لا توجد ساعة مقترنة بهذا الهاتف حالياً",
                "تأكد من إقران الساعة وتوصيلها بالهاتف عبر تطبيق Galaxy Wearable ثم أعد المحاولة"
            )
        } else {
            onError("SYSTEM", "حدث خطأ غير متوقع: $msg", "يرجى إعادة المحاولة")
        }
    }
}

// ---------------------------------------------------------
// Helpers: Robust Download & APK Validation
// ---------------------------------------------------------

fun isApkFileValid(file: File): Boolean {
    if (!file.exists() || file.length() < 10_000_000L) return false
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
    expectedMinBytes: Long = 10_000_000L,
    onProgress: (Float, Long, Long) -> Unit
): Result<File> = withContext(Dispatchers.IO) {
    var tempFile: File? = null
    try {
        var currentUrl = urlString
        var redirectCount = 0
        var connection: HttpURLConnection? = null
        var totalBytes = -1L

        // Follow redirects explicitly across domains (e.g. GitHub to release-assets.githubusercontent.com)
        while (redirectCount < 8) {
            val url = URL(currentUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 20000
            connection.readTimeout = 45000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Nocturne-Companion/1.0")
            connection.connect()

            val code = connection.responseCode
            if (code in 300..399) {
                val loc = connection.getHeaderField("Location")
                    ?: return@withContext Result.failure(IOException("HTTP $code redirect missing Location header"))
                connection.disconnect()
                currentUrl = if (loc.startsWith("http://") || loc.startsWith("https://")) {
                    loc
                } else {
                    URL(url, loc).toString()
                }
                redirectCount++
                continue
            }

            if (code != 200) {
                connection.disconnect()
                return@withContext Result.failure(IOException("HTTP $code from $currentUrl"))
            }

            totalBytes = connection.contentLengthLong
            break
        }

        val conn = connection ?: return@withContext Result.failure(IOException("No connection established"))
        tempFile = File(destinationFile.parentFile, "${destinationFile.name}.part")
        if (tempFile.exists()) tempFile.delete()

        var downloadedBytes = 0L
        conn.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(32768)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (totalBytes > 0) {
                        val fraction = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        onProgress(fraction, downloadedBytes, totalBytes)
                    } else {
                        onProgress(0.5f, downloadedBytes, totalBytes)
                    }
                }
                output.flush()
            }
        }
        conn.disconnect()

        if (tempFile.length() < expectedMinBytes) {
            val size = tempFile.length()
            tempFile.delete()
            return@withContext Result.failure(IOException("الملف المحمل غير مكتمل: حجمه $size بايت فقط"))
        }

        if (!isApkFileValid(tempFile)) {
            tempFile.delete()
            return@withContext Result.failure(IOException("الملف المحمل ليس حزمة APK صالحة"))
        }

        if (destinationFile.exists()) destinationFile.delete()
        if (!tempFile.renameTo(destinationFile)) {
            tempFile.copyTo(destinationFile, overwrite = true)
            tempFile.delete()
        }

        Result.success(destinationFile)
    } catch (e: Exception) {
        tempFile?.delete()
        Result.failure(e)
    }
}

// ---------------------------------------------------------
// Catalog Loading Helpers
// ---------------------------------------------------------

fun loadCatalogFromAssets(context: Context): List<WatchFaceItem> {
    return try {
        val jsonStr = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        parseCatalogJson(jsonStr)
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun fetchRemoteCatalog(): List<WatchFaceItem> = withContext(Dispatchers.IO) {
    val urls = listOf(
        "https://raw.githubusercontent.com/MZAYED2/Nocturne/main/distribution/catalog.json",
        "https://github.com/MZAYED2/Nocturne/releases/latest/download/catalog.json"
    )
    for (urlString in urls) {
        try {
            var currentUrl = urlString
            var redirectCount = 0
            var connection: HttpURLConnection? = null
            while (redirectCount < 5) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 10000
                connection.instanceFollowRedirects = false
                connection.connect()
                val code = connection.responseCode
                if (code in 300..399) {
                    val loc = connection.getHeaderField("Location") ?: break
                    connection.disconnect()
                    currentUrl = if (loc.startsWith("http")) loc else URL(url, loc).toString()
                    redirectCount++
                    continue
                }
                if (code == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    val parsed = parseCatalogJson(jsonStr)
                    if (parsed.isNotEmpty()) return@withContext parsed
                }
                break
            }
            connection?.disconnect()
        } catch (_: Exception) {}
    }
    emptyList()
}

fun parseCatalogJson(jsonString: String): List<WatchFaceItem> {
    val list = mutableListOf<WatchFaceItem>()
    try {
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("watchfaces")
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                WatchFaceItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    nameAr = obj.optString("name_ar", obj.getString("name")),
                    description = obj.optString("description", ""),
                    descriptionAr = obj.optString("description_ar", obj.optString("description", "")),
                    packageName = obj.optString("package_name", "com.mohamedzayed.nocturne.wear.watchfacepush.nocturne"),
                    validationToken = obj.getString("validation_token"),
                    apkFilename = obj.getString("apk_filename"),
                    downloadUrl = obj.optString("download_url", "https://github.com/MZAYED2/Nocturne/releases/latest/download/${obj.getString("apk_filename")}"),
                    preview = obj.optString("preview", ""),
                    versionCode = obj.optInt("version_code", 1),
                    versionName = obj.optString("version_name", "1.0.0")
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

// ---------------------------------------------------------
// Watch Dial Canvas Drawing - Photorealistic Authentic Renderer
// ---------------------------------------------------------

val lunarMoonResIds = intArrayOf(
    R.drawable.lunar_moon_00, R.drawable.lunar_moon_01, R.drawable.lunar_moon_02, R.drawable.lunar_moon_03,
    R.drawable.lunar_moon_04, R.drawable.lunar_moon_05, R.drawable.lunar_moon_06, R.drawable.lunar_moon_07,
    R.drawable.lunar_moon_08, R.drawable.lunar_moon_09, R.drawable.lunar_moon_10, R.drawable.lunar_moon_11,
    R.drawable.lunar_moon_12, R.drawable.lunar_moon_13, R.drawable.lunar_moon_14, R.drawable.lunar_moon_15,
    R.drawable.lunar_moon_16, R.drawable.lunar_moon_17, R.drawable.lunar_moon_18, R.drawable.lunar_moon_19,
    R.drawable.lunar_moon_20, R.drawable.lunar_moon_21, R.drawable.lunar_moon_22, R.drawable.lunar_moon_23,
    R.drawable.lunar_moon_24, R.drawable.lunar_moon_25, R.drawable.lunar_moon_26, R.drawable.lunar_moon_27,
    R.drawable.lunar_moon_28
)

val refinedMoonResIds = intArrayOf(
    R.drawable.refined_moon_00, R.drawable.refined_moon_01, R.drawable.refined_moon_02, R.drawable.refined_moon_03,
    R.drawable.refined_moon_04, R.drawable.refined_moon_05, R.drawable.refined_moon_06, R.drawable.refined_moon_07,
    R.drawable.refined_moon_08, R.drawable.refined_moon_09, R.drawable.refined_moon_10, R.drawable.refined_moon_11,
    R.drawable.refined_moon_12, R.drawable.refined_moon_13, R.drawable.refined_moon_14, R.drawable.refined_moon_15,
    R.drawable.refined_moon_16, R.drawable.refined_moon_17, R.drawable.refined_moon_18, R.drawable.refined_moon_19,
    R.drawable.refined_moon_20, R.drawable.refined_moon_21, R.drawable.refined_moon_22, R.drawable.refined_moon_23,
    R.drawable.refined_moon_24, R.drawable.refined_moon_25, R.drawable.refined_moon_26, R.drawable.refined_moon_27,
    R.drawable.refined_moon_28
)

val dhikrResIds = intArrayOf(
    R.drawable.uc_dhikr_0, R.drawable.uc_dhikr_1, R.drawable.uc_dhikr_2, R.drawable.uc_dhikr_3
)

@Composable
fun WatchFaceDialCanvas(
    faceId: String,
    displayMode: DisplayMode,
    bgColor: Color,
    accentColor: Color,
    ornamentType: String,
    dialType: String,
    moonPhaseIndex: Int,
    currentTime: Calendar
) {
    val context = LocalContext.current
    val bitmapCache = remember { mutableMapOf<Int, ImageBitmap>() }

    fun getBmp(resId: Int): ImageBitmap {
        return bitmapCache.getOrPut(resId) {
            BitmapFactory.decodeResource(context.resources, resId).asImageBitmap()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = size.width / 1000f

        when (faceId) {
            "lunar_phase" -> {
                drawLunarPhaseFace(
                    displayMode = displayMode,
                    scale = scale,
                    moonPhaseIndex = moonPhaseIndex,
                    currentTime = currentTime,
                    getBmp = ::getBmp
                )
            }
            "obsidian_atelier" -> {
                drawObsidianAtelierFace(
                    displayMode = displayMode,
                    scale = scale,
                    currentTime = currentTime,
                    getBmp = ::getBmp
                )
            }
            "emerald_nocturne" -> {
                drawEmeraldNocturneFace(
                    displayMode = displayMode,
                    scale = scale,
                    moonPhaseIndex = moonPhaseIndex,
                    currentTime = currentTime,
                    getBmp = ::getBmp
                )
            }
            else -> {
                drawSageNocturneFace(
                    displayMode = displayMode,
                    scale = scale,
                    bgColor = bgColor,
                    accentColor = accentColor,
                    ornamentType = ornamentType,
                    dialType = dialType,
                    moonPhaseIndex = moonPhaseIndex,
                    currentTime = currentTime,
                    getBmp = ::getBmp
                )
            }
        }
    }
}

fun DrawScope.drawSageNocturneFace(
    displayMode: DisplayMode,
    scale: Float,
    bgColor: Color,
    accentColor: Color,
    ornamentType: String,
    dialType: String,
    moonPhaseIndex: Int,
    currentTime: Calendar,
    getBmp: (Int) -> ImageBitmap
) {
    val isAod = displayMode == DisplayMode.AOD

    // 1. Background
    val dialBg = if (isAod) Color(0xFF000000) else bgColor
    drawCircle(color = dialBg, radius = 500f * scale, center = Offset(500f * scale, 500f * scale))

    // 2. Ottoman Arabesque on Left Half (Active mode only)
    if (!isAod && ornamentType != "none") {
        val ornRes = when (ornamentType) {
            "minimal" -> R.drawable.visible_0_orn_0
            "ornate" -> R.drawable.visible_0_orn_2
            else -> R.drawable.visible_0_orn_1
        }
        val ornBmp = getBmp(ornRes)
        drawImage(
            image = ornBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )
    }

    // 3. Dial Numerals and Minute Track
    val dialRes = if (isAod) {
        R.drawable.refined_0_dial_0_aod
    } else {
        when (dialType) {
            "cardinal" -> R.drawable.refined_0_dial_1
            "markers" -> R.drawable.refined_0_dial_2
            else -> R.drawable.refined_0_dial_0
        }
    }
    val dialBmp = getBmp(dialRes)
    drawImage(
        image = dialBmp,
        dstOffset = IntOffset(0, 0),
        dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
    )

    // 4. Subdial Rings
    val ringBmp = getBmp(R.drawable.uc_0_ring)
    val ringAlpha = if (isAod) 0.5f else 1.0f

    // Left Ring (Battery)
    drawImage(
        image = ringBmp,
        dstOffset = IntOffset((171 * scale).toInt(), (391 * scale).toInt()),
        dstSize = IntSize((228 * scale).toInt(), (228 * scale).toInt()),
        alpha = ringAlpha
    )

    // Right Ring (Calendar)
    drawImage(
        image = ringBmp,
        dstOffset = IntOffset((601 * scale).toInt(), (391 * scale).toInt()),
        dstSize = IntSize((228 * scale).toInt(), (228 * scale).toInt()),
        alpha = ringAlpha
    )

    // Subdial Complications Content
    if (!isAod) {
        // Battery Icon inside left subdial
        val batBmp = getBmp(R.drawable.refined_battery)
        val batW = (65 * scale).toInt()
        val batH = (45 * scale).toInt()
        val batX = (285 * scale - batW / 2).toInt()
        val batY = (420 * scale).toInt()
        drawImage(
            image = batBmp,
            dstOffset = IntOffset(batX, batY),
            dstSize = IntSize(batW, batH)
        )

        drawContext.canvas.nativeCanvas.apply {
            val numPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#FFF0D8")
                textAlign = Paint.Align.CENTER
                textSize = 34f * scale
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            val labelPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#A8B5A2")
                textAlign = Paint.Align.CENTER
                textSize = 20f * scale
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.08f
            }

            // Battery Percentage & Label
            drawText("82%", 285f * scale, 502f * scale, numPaint)
            drawText("BATTERY", 285f * scale, 538f * scale, labelPaint)

            // Date & Month
            val day = currentTime.get(Calendar.DAY_OF_MONTH).toString()
            val monthNames = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
            val month = monthNames[currentTime.get(Calendar.MONTH)]

            drawText(month, 715f * scale, 455f * scale, labelPaint)
            drawText(day, 715f * scale, 505f * scale, numPaint)

            // Steps Indicator at Bottom
            drawText("7,420", 500f * scale, 710f * scale, numPaint)
            drawText("STEPS", 500f * scale, 742f * scale, labelPaint)
        }

        // Steps Arc
        val arcCenter = Offset(500f * scale, 700f * scale)
        val arcRadius = 145f * scale
        drawArc(
            color = Color(0x33A8B5A2),
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = 4f * scale, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF8FA89B),
            startAngle = 140f,
            sweepAngle = 190f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = 4f * scale, cap = StrokeCap.Round)
        )
    }

    // 5. Dhikr Calligraphy (Active and subtle in AOD)
    val dhikrIndex = (currentTime.get(Calendar.MINUTE) / 15) % 4
    val dhikrBmp = getBmp(dhikrResIds[dhikrIndex])
    drawImage(
        image = dhikrBmp,
        dstOffset = IntOffset((60 * scale).toInt(), (320 * scale).toInt()),
        dstSize = IntSize((880 * scale).toInt(), (144 * scale).toInt()),
        alpha = if (isAod) 0.65f else 0.95f
    )

    // 6. Photorealistic Small Moon at Top (Active mode only)
    if (!isAod) {
        val safeMoonIndex = moonPhaseIndex.coerceIn(0, 28)
        val moonBmp = getBmp(refinedMoonResIds[safeMoonIndex])
        drawImage(
            image = moonBmp,
            dstOffset = IntOffset((392 * scale).toInt(), (122 * scale).toInt()),
            dstSize = IntSize((216 * scale).toInt(), (216 * scale).toInt())
        )
    }

    // 7. Authentic Skeleton Hands
    val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
    val minutes = currentTime.get(Calendar.MINUTE)
    val seconds = currentTime.get(Calendar.SECOND)

    val hourDeg = hours * 30f + minutes * 0.5f
    val minDeg = minutes * 6f + seconds * 0.1f
    val secDeg = seconds * 6f

    val centerDial = Offset(500f * scale, 500f * scale)

    // Hour Hand (pivot at 500, 500)
    val hourRes = if (isAod) R.drawable.refined_0_hour_aod else R.drawable.refined_0_hour
    val hourBmp = getBmp(hourRes)
    withTransform({
        rotate(degrees = hourDeg, pivot = centerDial)
    }) {
        drawImage(
            image = hourBmp,
            dstOffset = IntOffset((480 * scale).toInt(), (250 * scale).toInt()),
            dstSize = IntSize((40 * scale).toInt(), (280 * scale).toInt())
        )
    }

    // Minute Hand (pivot at 500, 500)
    val minRes = if (isAod) R.drawable.refined_0_minute_aod else R.drawable.refined_0_minute
    val minBmp = getBmp(minRes)
    withTransform({
        rotate(degrees = minDeg, pivot = centerDial)
    }) {
        drawImage(
            image = minBmp,
            dstOffset = IntOffset((483 * scale).toInt(), (150 * scale).toInt()),
            dstSize = IntSize((34 * scale).toInt(), (380 * scale).toInt())
        )
    }

    // Seconds Hand (Active only)
    if (!isAod) {
        val secBmp = getBmp(R.drawable.uc_0_second)
        withTransform({
            rotate(degrees = secDeg, pivot = centerDial)
        }) {
            drawImage(
                image = secBmp,
                dstOffset = IntOffset((492 * scale).toInt(), (140 * scale).toInt()),
                dstSize = IntSize((16 * scale).toInt(), (395 * scale).toInt())
            )
        }
    }

    // Center Hub
    val hubBmp = getBmp(R.drawable.uc_0_hub)
    drawImage(
        image = hubBmp,
        dstOffset = IntOffset((477 * scale).toInt(), (477 * scale).toInt()),
        dstSize = IntSize((46 * scale).toInt(), (46 * scale).toInt())
    )
}

fun DrawScope.drawLunarPhaseFace(
    displayMode: DisplayMode,
    scale: Float,
    moonPhaseIndex: Int,
    currentTime: Calendar,
    getBmp: (Int) -> ImageBitmap
) {
    val isAod = displayMode == DisplayMode.AOD
    val centerDial = Offset(500f * scale, 500f * scale)

    // 1. Pure AMOLED Black Background
    drawCircle(color = Color(0xFF000000), radius = 500f * scale, center = centerDial)

    if (!isAod) {
        // Active Mode: 92% Screen Diameter Massive Photorealistic Moon
        val safeMoonIndex = moonPhaseIndex.coerceIn(0, 28)
        val moonBmp = getBmp(lunarMoonResIds[safeMoonIndex])
        drawImage(
            image = moonBmp,
            dstOffset = IntOffset((40 * scale).toInt(), (40 * scale).toInt()),
            dstSize = IntSize((920 * scale).toInt(), (920 * scale).toInt())
        )

        // NO Hour numbers in Active mode! Only Luminous Dual-Contrast Hands:
        val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
        val minutes = currentTime.get(Calendar.MINUTE)
        val seconds = currentTime.get(Calendar.SECOND)

        val hourDeg = hours * 30f + minutes * 0.5f
        val minDeg = minutes * 6f + seconds * 0.1f
        val secDeg = seconds * 6f

        // Hour Hand
        val hourBmp = getBmp(R.drawable.lunar_hour)
        withTransform({
            rotate(degrees = hourDeg, pivot = centerDial)
        }) {
            drawImage(
                image = hourBmp,
                dstOffset = IntOffset((480 * scale).toInt(), (250 * scale).toInt()),
                dstSize = IntSize((40 * scale).toInt(), (280 * scale).toInt())
            )
        }

        // Minute Hand
        val minBmp = getBmp(R.drawable.lunar_minute)
        withTransform({
            rotate(degrees = minDeg, pivot = centerDial)
        }) {
            drawImage(
                image = minBmp,
                dstOffset = IntOffset((483 * scale).toInt(), (150 * scale).toInt()),
                dstSize = IntSize((34 * scale).toInt(), (380 * scale).toInt())
            )
        }

        // Second Hand (with red accented tip)
        val secBmp = getBmp(R.drawable.lunar_second)
        withTransform({
            rotate(degrees = secDeg, pivot = centerDial)
        }) {
            drawImage(
                image = secBmp,
                dstOffset = IntOffset((492 * scale).toInt(), (140 * scale).toInt()),
                dstSize = IntSize((16 * scale).toInt(), (395 * scale).toInt())
            )
        }

        // Center Hub
        val hubBmp = getBmp(R.drawable.lunar_hub)
        drawImage(
            image = hubBmp,
            dstOffset = IntOffset((483 * scale).toInt(), (483 * scale).toInt()),
            dstSize = IntSize((34 * scale).toInt(), (34 * scale).toInt())
        )
    } else {
        // AOD Mode: Pure AMOLED Black, Moon Completely Gone!
        // Elegant Moonlight Dial Markers & Cormorant Garamond Numerals:
        val dialBmp = getBmp(R.drawable.lunar_dial_aod)
        drawImage(
            image = dialBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )

        val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
        val minutes = currentTime.get(Calendar.MINUTE)

        val hourDeg = hours * 30f + minutes * 0.5f
        val minDeg = minutes * 6f

        // Hour Hand AOD
        val hourBmp = getBmp(R.drawable.lunar_hour_aod)
        withTransform({
            rotate(degrees = hourDeg, pivot = centerDial)
        }) {
            drawImage(
                image = hourBmp,
                dstOffset = IntOffset((480 * scale).toInt(), (250 * scale).toInt()),
                dstSize = IntSize((40 * scale).toInt(), (280 * scale).toInt())
            )
        }

        // Minute Hand AOD
        val minBmp = getBmp(R.drawable.lunar_minute_aod)
        withTransform({
            rotate(degrees = minDeg, pivot = centerDial)
        }) {
            drawImage(
                image = minBmp,
                dstOffset = IntOffset((483 * scale).toInt(), (150 * scale).toInt()),
                dstSize = IntSize((34 * scale).toInt(), (380 * scale).toInt())
            )
        }

        // Second Hand is HIDDEN in AOD!

        // Center Hub
        val hubBmp = getBmp(R.drawable.lunar_hub)
        drawImage(
            image = hubBmp,
            dstOffset = IntOffset((483 * scale).toInt(), (483 * scale).toInt()),
            dstSize = IntSize((34 * scale).toInt(), (34 * scale).toInt())
        )
    }
}

fun DrawScope.drawEmeraldNocturneFace(
    displayMode: DisplayMode,
    scale: Float,
    moonPhaseIndex: Int,
    currentTime: Calendar,
    getBmp: (Int) -> ImageBitmap
) {
    val isAod = displayMode == DisplayMode.AOD
    val centerDial = Offset(500f * scale, 500f * scale)

    // Background
    val dialBg = if (isAod) Color(0xFF000000) else Color(0xFF0A1D13)
    drawCircle(color = dialBg, radius = 500f * scale, center = centerDial)

    // Ottoman Arabesque (Emerald Golden)
    if (!isAod) {
        val ornBmp = getBmp(R.drawable.visible_3_orn_1)
        drawImage(
            image = ornBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )
    }

    // Dial Numerals & Markers (Emerald Gold)
    val dialRes = if (isAod) R.drawable.refined_3_dial_0_aod else R.drawable.refined_3_dial_0
    val dialBmp = getBmp(dialRes)
    drawImage(
        image = dialBmp,
        dstOffset = IntOffset(0, 0),
        dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
    )

    // Subdial Rings
    val ringBmp = getBmp(R.drawable.uc_3_ring)
    val ringAlpha = if (isAod) 0.5f else 1.0f
    drawImage(
        image = ringBmp,
        dstOffset = IntOffset((171 * scale).toInt(), (391 * scale).toInt()),
        dstSize = IntSize((228 * scale).toInt(), (228 * scale).toInt()),
        alpha = ringAlpha
    )
    drawImage(
        image = ringBmp,
        dstOffset = IntOffset((601 * scale).toInt(), (391 * scale).toInt()),
        dstSize = IntSize((228 * scale).toInt(), (228 * scale).toInt()),
        alpha = ringAlpha
    )

    if (!isAod) {
        drawContext.canvas.nativeCanvas.apply {
            val numPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#F7E5A9")
                textAlign = Paint.Align.CENTER
                textSize = 34f * scale
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            val labelPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#D4AF37")
                textAlign = Paint.Align.CENTER
                textSize = 20f * scale
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.08f
            }

            drawText("85%", 285f * scale, 502f * scale, numPaint)
            drawText("BATTERY", 285f * scale, 538f * scale, labelPaint)

            val day = currentTime.get(Calendar.DAY_OF_MONTH).toString()
            drawText("EMERALD", 715f * scale, 455f * scale, labelPaint)
            drawText(day, 715f * scale, 505f * scale, numPaint)
        }

        // Small Moon
        val safeMoonIndex = moonPhaseIndex.coerceIn(0, 28)
        val moonBmp = getBmp(refinedMoonResIds[safeMoonIndex])
        drawImage(
            image = moonBmp,
            dstOffset = IntOffset((392 * scale).toInt(), (122 * scale).toInt()),
            dstSize = IntSize((216 * scale).toInt(), (216 * scale).toInt())
        )
    }

    // Hands
    val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
    val minutes = currentTime.get(Calendar.MINUTE)
    val seconds = currentTime.get(Calendar.SECOND)

    val hourDeg = hours * 30f + minutes * 0.5f
    val minDeg = minutes * 6f + seconds * 0.1f
    val secDeg = seconds * 6f

    val hourRes = if (isAod) R.drawable.refined_3_hour_aod else R.drawable.refined_3_hour
    val hourBmp = getBmp(hourRes)
    withTransform({
        rotate(degrees = hourDeg, pivot = centerDial)
    }) {
        drawImage(
            image = hourBmp,
            dstOffset = IntOffset((480 * scale).toInt(), (250 * scale).toInt()),
            dstSize = IntSize((40 * scale).toInt(), (280 * scale).toInt())
        )
    }

    val minRes = if (isAod) R.drawable.refined_3_minute_aod else R.drawable.refined_3_minute
    val minBmp = getBmp(minRes)
    withTransform({
        rotate(degrees = minDeg, pivot = centerDial)
    }) {
        drawImage(
            image = minBmp,
            dstOffset = IntOffset((483 * scale).toInt(), (150 * scale).toInt()),
            dstSize = IntSize((34 * scale).toInt(), (380 * scale).toInt())
        )
    }

    if (!isAod) {
        val secBmp = getBmp(R.drawable.uc_3_second)
        withTransform({
            rotate(degrees = secDeg, pivot = centerDial)
        }) {
            drawImage(
                image = secBmp,
                dstOffset = IntOffset((492 * scale).toInt(), (140 * scale).toInt()),
                dstSize = IntSize((16 * scale).toInt(), (395 * scale).toInt())
            )
        }
    }

    val hubBmp = getBmp(R.drawable.uc_3_hub)
    drawImage(
        image = hubBmp,
        dstOffset = IntOffset((477 * scale).toInt(), (477 * scale).toInt()),
        dstSize = IntSize((46 * scale).toInt(), (46 * scale).toInt())
    )
}

fun DrawScope.drawObsidianAtelierFace(
    displayMode: DisplayMode,
    scale: Float,
    currentTime: Calendar,
    getBmp: (Int) -> ImageBitmap
) {
    val isAod = displayMode == DisplayMode.AOD
    val centerDial = Offset(500f * scale, 500f * scale)

    // 1. Pure AMOLED Black Background
    drawCircle(color = Color(0xFF000000), radius = 500f * scale, center = centerDial)

    if (!isAod) {
        // Active Mode:
        // 1. Brushed Metal Dial with Subdials & Applied Markers
        val dialBmp = getBmp(R.drawable.obsidian_dial)
        drawImage(
            image = dialBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )

        // 2. Beveled Date Window at 4:30 (Day of month)
        drawContext.canvas.nativeCanvas.apply {
            val datePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#E7E5DE")
                textAlign = Paint.Align.CENTER
                textSize = 34f * scale
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            val day = currentTime.get(Calendar.DAY_OF_MONTH).toString()
            drawText(day, 690f * scale, 672f * scale, datePaint)
        }

        // 3. Power Reserve Needle at 9 o'clock (285, 490)
        val powerBmp = getBmp(R.drawable.obsidian_power_hand)
        val powerPivot = Offset(285f * scale, 520f * scale)
        val batteryPct = 84f
        val powerAngle = (batteryPct - 50f) * 1.2f
        withTransform({
            rotate(degrees = powerAngle, pivot = powerPivot)
        }) {
            drawImage(
                image = powerBmp,
                dstOffset = IntOffset((278 * scale).toInt(), (450 * scale).toInt()),
                dstSize = IntSize((14 * scale).toInt(), (100 * scale).toInt())
            )
        }

        // 4. Small Seconds at 6 o'clock (Pivot at 500, 710)
        val seconds = currentTime.get(Calendar.SECOND)
        val secDeg = seconds * 6f
        val secPivot = Offset(500f * scale, 710f * scale)
        val smallSecBmp = getBmp(R.drawable.obsidian_small_sec)
        withTransform({
            rotate(degrees = secDeg, pivot = secPivot)
        }) {
            drawImage(
                image = smallSecBmp,
                dstOffset = IntOffset((493 * scale).toInt(), (606 * scale).toInt()),
                dstSize = IntSize((14 * scale).toInt(), (130 * scale).toInt())
            )
        }

        // 5. Center Dauphine Hands (Hour & Minute - NO center second hand)
        val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
        val minutes = currentTime.get(Calendar.MINUTE)
        val hourDeg = hours * 30f + minutes * 0.5f
        val minDeg = minutes * 6f + seconds * 0.1f

        val hourBmp = getBmp(R.drawable.obsidian_hour)
        withTransform({
            rotate(degrees = hourDeg, pivot = centerDial)
        }) {
            drawImage(
                image = hourBmp,
                dstOffset = IntOffset((477 * scale).toInt(), (245 * scale).toInt()),
                dstSize = IntSize((46 * scale).toInt(), (290 * scale).toInt())
            )
        }

        val minBmp = getBmp(R.drawable.obsidian_minute)
        withTransform({
            rotate(degrees = minDeg, pivot = centerDial)
        }) {
            drawImage(
                image = minBmp,
                dstOffset = IntOffset((481 * scale).toInt(), (145 * scale).toInt()),
                dstSize = IntSize((38 * scale).toInt(), (390 * scale).toInt())
            )
        }

        // 6. Central Hub Cap
        val hubBmp = getBmp(R.drawable.obsidian_hub)
        drawImage(
            image = hubBmp,
            dstOffset = IntOffset((474 * scale).toInt(), (474 * scale).toInt()),
            dstSize = IntSize((52 * scale).toInt(), (52 * scale).toInt())
        )
    } else {
        // AOD Mode:
        // 1. AOD Dial (AMOLED Black with applied titanium markers & subtle rings)
        val dialBmp = getBmp(R.drawable.obsidian_dial_aod)
        drawImage(
            image = dialBmp,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize((1000 * scale).toInt(), (1000 * scale).toInt())
        )

        // 2. AOD Date Window
        drawContext.canvas.nativeCanvas.apply {
            val datePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#AAB6BB")
                textAlign = Paint.Align.CENTER
                textSize = 32f * scale
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            val day = currentTime.get(Calendar.DAY_OF_MONTH).toString()
            drawText(day, 690f * scale, 672f * scale, datePaint)
        }

        // Center Dauphine Hands AOD (Hour & Minute with Platinum Luminous Contour)
        val hours = currentTime.get(Calendar.HOUR_OF_DAY) % 12
        val minutes = currentTime.get(Calendar.MINUTE)
        val hourDeg = hours * 30f + minutes * 0.5f
        val minDeg = minutes * 6f

        val hourBmp = getBmp(R.drawable.obsidian_hour_aod)
        withTransform({
            rotate(degrees = hourDeg, pivot = centerDial)
        }) {
            drawImage(
                image = hourBmp,
                dstOffset = IntOffset((477 * scale).toInt(), (245 * scale).toInt()),
                dstSize = IntSize((46 * scale).toInt(), (290 * scale).toInt())
            )
        }

        val minBmp = getBmp(R.drawable.obsidian_minute_aod)
        withTransform({
            rotate(degrees = minDeg, pivot = centerDial)
        }) {
            drawImage(
                image = minBmp,
                dstOffset = IntOffset((481 * scale).toInt(), (145 * scale).toInt()),
                dstSize = IntSize((38 * scale).toInt(), (390 * scale).toInt())
            )
        }

        // Central Hub Cap
        val hubBmp = getBmp(R.drawable.obsidian_hub)
        drawImage(
            image = hubBmp,
            dstOffset = IntOffset((474 * scale).toInt(), (474 * scale).toInt()),
            dstSize = IntSize((52 * scale).toInt(), (52 * scale).toInt())
        )
    }
}


// ---------------------------------------------------------
// Moon Phase Calculation Helpers
// ---------------------------------------------------------

fun calculateCurrentMoonPhase(): Int {
    val now = Date().time
    val knownNewMoon = 1704974220000L
    val synodicPeriodMillis = 29.530588853 * 24.0 * 60.0 * 60.0 * 1000.0
    val diff = (now - knownNewMoon) % synodicPeriodMillis
    val positiveDiff = if (diff < 0) diff + synodicPeriodMillis else diff
    val cycleFraction = positiveDiff / synodicPeriodMillis
    return (cycleFraction * 28.0).roundToInt().coerceIn(0, 28)
}

fun getMoonPhaseName(phaseIndex: Int): Pair<String, String> {
    return when (phaseIndex) {
        0, 28 -> "محاق (New Moon)" to "New Moon"
        in 1..6 -> "هلال متزايد (Waxing Crescent)" to "Waxing Crescent"
        7 -> "تربيع أول (First Quarter)" to "First Quarter"
        in 8..13 -> "أحدب متزايد (Waxing Gibbous)" to "Waxing Gibbous"
        14 -> "بدر كامل (Full Moon)" to "Full Moon"
        in 15..20 -> "أحدب متناقص (Waning Gibbous)" to "Waning Gibbous"
        21 -> "تربيع أخير (Last Quarter)" to "Last Quarter"
        else -> "هلال متناقص (Waning Crescent)" to "Waning Crescent"
    }
}

fun openGalaxyWearable(context: Context) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage("com.samsung.android.app.watchmanager")
        if (intent != null) {
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {}

    try {
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.wearable.app")
        if (intent != null) {
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {}

    Toast.makeText(context, "يرجى فتح تطبيق Galaxy Wearable واختيار Nocturne لتخصيص الواجهة", Toast.LENGTH_LONG).show()
}
