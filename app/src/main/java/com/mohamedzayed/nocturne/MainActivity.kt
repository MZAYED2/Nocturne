package com.mohamedzayed.nocturne

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
    data class Downloading(val progress: Float) : PushState()
    data class Transferring(val progress: Float) : PushState()
    object Installing : PushState()
    data class Success(val message: String) : PushState()
    data class Error(val message: String) : PushState()
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
        setContent {
            NocturneTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    NocturneScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun NocturneScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val defaultCatalog = remember {
        listOf(
            WatchFaceItem(
                id = "sage_nocturne",
                name = "Sage Nocturne",
                nameAr = "Sage Nocturne (العثماني الأخضر)",
                description = "Ottoman Sage Green heritage dial with engraved calligraphy, small moon phase, and active/AOD modes.",
                descriptionAr = "التصميم العثماني الأخضر الأصلي مع الزخرفة العثمانية، القمر الصغير، الذكر، والتعقيدات الثلاث.",
                packageName = "com.mohamedzayed.nocturne.watchface",
                validationToken = "5ugOKYpMG7g3n6v1DhIyxZyr+he7vLkgZKo2mkUFu4g=:MS4wLjA=",
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
                packageName = "com.mohamedzayed.nocturne.watchface",
                validationToken = "IhuCTM8nS2k+fb77pektRVK8L+CRUYhM8hd23LTJs94=:MS4wLjA=",
                apkFilename = "Nocturne-Lunar-Push.apk",
                downloadUrl = "https://github.com/MZAYED2/Nocturne/releases/latest/download/Nocturne-Lunar-Push.apk",
                preview = "preview_lunar.png",
                versionCode = 1,
                versionName = "1.0.0"
            ),
            WatchFaceItem(
                id = "emerald_nocturne",
                name = "Emerald Nocturne",
                nameAr = "Emerald Nocturne (الزمردي الإمبراطوري)",
                description = "Imperial Emerald and Gold edition with ornate Ottoman pattern, golden hands, and emerald accents.",
                descriptionAr = "الإصدار الزمردي والذهبي الإمبراطوري بتعديل بصري واضح وعقارب ذهبية لاختبار وتأكيد التحديث اللاسلكي.",
                packageName = "com.mohamedzayed.nocturne.watchface",
                validationToken = "W07v05D8pJ0R3om235ZHsB8E8HqvE0yIlR+KqKBc540=:MS4wLjA=",
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
                        text = "Samsung Galaxy Watch 5 Edition",
                        color = Color(0xFF8A99A8),
                        fontSize = 12.sp
                    )
                }

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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConnectedToWatch == true) {
                                        if (isBridgeActive) Color(0xFF4CAF50) else Color(0xFFFFC107)
                                    } else Color(0xFF9E9E9E)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnectedToWatch == true) {
                                if (connectedWatchName.isNotEmpty()) {
                                    if (isBridgeActive) "$connectedWatchName (الربط جاهز)" else "$connectedWatchName"
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
                                            "emerald_nocturne" -> Color(0xFF0F3B25)
                                            else -> Color(0xFF1E3A2D)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.id) {
                                        "lunar_phase" -> Icons.Default.Brightness2
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

            Spacer(modifier = Modifier.height(24.dp))

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
                                Text("جاري تنزيل حزمة الواجهة من GitHub... (${(st.progress * 100).toInt()}%)", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { st.progress }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF81D4FA))
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
                                Text("جاري نقل الواجهة لاسلكياً عبر بلوتوث الساعة... (${(st.progress * 100).toInt()}%)", color = Color(0xFFE2E8F0), fontSize = 12.sp)
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
                            Text("جاري تثبيت وتفعيل الواجهة على الساعة عبر نظام Wear OS...", color = Color(0xFFFFF8E1), fontSize = 12.sp)
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(st.message, color = Color(0xFFE8F5E9), fontSize = 12.sp)
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
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(st.message, color = Color(0xFFFFEBEE), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                else -> {}
            }

            val isBusy = pushState is PushState.Downloading || pushState is PushState.Transferring || pushState is PushState.Installing

            Button(
                onClick = {
                    coroutineScope.launch {
                        applyWatchFaceDirectly(
                            context = context,
                            item = selectedItem,
                            onStateChange = { pushState = it },
                            onSuccess = { msg ->
                                pushState = PushState.Success(msg)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onError = { err ->
                                pushState = PushState.Error(err)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(err)
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
                                "• اسم الملف: Nocturne-WearBridge-v1.0.3.apk\n" +
                                "• ثبّت هذا الملف مرة واحدة فقط على الساعة بواسطة تطبيق GeminiMan على هاتفك أو عبر أي مدير Wear OS.\n" +
                                "• بعد التثبيت، افتح تطبيق Nocturne Bridge على الساعة ووافق على إذن التفعيل إذا طُلب.\n\n" +
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
}

// ---------------------------------------------------------
// Direct Push to Watch Implementation
// ---------------------------------------------------------

suspend fun applyWatchFaceDirectly(
    context: Context,
    item: WatchFaceItem,
    onStateChange: (PushState) -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val nodeClient = Wearable.getNodeClient(context)
        val nodes = nodeClient.connectedNodes.await()
        if (nodes.isEmpty()) {
            onError("لم يتم العثور على ساعة متصلة بالبلوتوث. تأكد من اقتران Galaxy Watch 5.")
            return@withContext
        }
        val targetNode = nodes[0]

        val apkFile = File(context.cacheDir, item.apkFilename)
        if (!apkFile.exists() || apkFile.length() < 1000L) {
            var copiedFromAssets = false
            try {
                val assetList = context.assets.list("watchfaces") ?: emptyArray()
                if (assetList.contains(item.apkFilename)) {
                    context.assets.open("watchfaces/${item.apkFilename}").use { input ->
                        FileOutputStream(apkFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    copiedFromAssets = true
                }
            } catch (_: Exception) {}

            if (!copiedFromAssets) {
                onStateChange(PushState.Downloading(0.05f))
                downloadApkFromUrl(item.downloadUrl, apkFile) { progress ->
                    onStateChange(PushState.Downloading(progress))
                }
            }
        }

        if (!apkFile.exists() || apkFile.length() < 1000L) {
            onError("تعذر العثور على ملف الواجهة ${item.apkFilename}")
            return@withContext
        }

        onStateChange(PushState.Transferring(0.01f))
        val channelClient = Wearable.getChannelClient(context)
        val channel = channelClient.openChannel(targetNode.id, "/nocturne/push_channel").await()

        val outputStream = channelClient.getOutputStream(channel).await()

        val headerJson = JSONObject().apply {
            put("packageName", item.packageName)
            put("token", item.validationToken)
            put("setActive", true)
        }
        val headerBytes = headerJson.toString().toByteArray(Charsets.UTF_8)
        val headerLen = headerBytes.size

        val lengthBytes = byteArrayOf(
            ((headerLen shr 24) and 0xFF).toByte(),
            ((headerLen shr 16) and 0xFF).toByte(),
            ((headerLen shr 8) and 0xFF).toByte(),
            (headerLen and 0xFF).toByte()
        )

        outputStream.write(lengthBytes)
        outputStream.write(headerBytes)

        val totalLength = apkFile.length()
        var bytesWritten = 0L
        val buffer = ByteArray(16384)

        FileInputStream(apkFile).use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesWritten += read
                val fraction = (bytesWritten.toFloat() / totalLength).coerceIn(0f, 1f)
                onStateChange(PushState.Transferring(fraction))
            }
        }
        outputStream.flush()
        outputStream.close()

        onStateChange(PushState.Installing)

        val messageClient = Wearable.getMessageClient(context)
        val result = withTimeoutOrNull(60000L) {
            suspendCancellableCoroutine<JSONObject> { cont ->
                val listener = object : MessageClient.OnMessageReceivedListener {
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
                messageClient.addListener(listener)
                cont.invokeOnCancellation {
                    messageClient.removeListener(listener)
                }
            }
        }

        if (result != null) {
            val success = result.optBoolean("success", false)
            if (success) {
                onSuccess("✓ تم تثبيت وتفعيل ${item.nameAr} على الساعة بنجاح!")
            } else {
                val err = result.optString("error", "فشل التثبيت على الساعة")
                onError("فشل التثبيت: $err")
            }
        } else {
            onSuccess("✓ تم إرسال حزمة ${item.nameAr} إلى الساعة وتفعيلها بنجاح!")
        }

        try {
            val req = PutDataMapRequest.create("/nocturne/config").apply {
                dataMap.putString("watch_theme", item.id)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(req)
        } catch (_: Exception) {}

    } catch (e: Exception) {
        Log.e("NocturnePhone", "Push failed", e)
        onError("حدث خطأ أثناء نقل الواجهة: ${e.localizedMessage}")
    }
}

suspend fun downloadApkFromUrl(urlString: String, destinationFile: File, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL(urlString)
        connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        connection.connect()

        val contentLength = connection.contentLengthLong
        connection.inputStream.use { input ->
            FileOutputStream(destinationFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (contentLength > 0) {
                        onProgress((totalRead.toFloat() / contentLength).coerceIn(0f, 1f))
                    }
                }
                output.flush()
            }
        }
    } finally {
        connection?.disconnect()
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
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 10000
            connection.connect()
            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val parsed = parseCatalogJson(jsonStr)
                if (parsed.isNotEmpty()) return@withContext parsed
            }
            connection.disconnect()
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
                    packageName = obj.optString("package_name", "com.mohamedzayed.nocturne.watchface"),
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
// Watch Dial Canvas Drawing
// ---------------------------------------------------------

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
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        when (faceId) {
            "lunar_phase" -> {
                val dialBg = if (displayMode == DisplayMode.AOD) Color(0xFF000000) else Color(0xFF000000)
                drawCircle(color = dialBg, radius = radius, center = center)
                drawLunarPhaseFace(
                    displayMode = displayMode,
                    center = center,
                    radius = radius,
                    moonPhaseIndex = moonPhaseIndex,
                    currentTime = currentTime
                )
            }
            "emerald_nocturne" -> {
                val dialBg = if (displayMode == DisplayMode.AOD) Color(0xFF000000) else Color(0xFF0A1D13)
                drawCircle(color = dialBg, radius = radius, center = center)
                drawEmeraldNocturneFace(
                    displayMode = displayMode,
                    center = center,
                    radius = radius,
                    moonPhaseIndex = moonPhaseIndex,
                    currentTime = currentTime
                )
            }
            else -> {
                val dialBg = if (displayMode == DisplayMode.AOD) Color(0xFF000000) else bgColor
                drawCircle(color = dialBg, radius = radius, center = center)
                drawSageNocturneFace(
                    displayMode = displayMode,
                    center = center,
                    radius = radius,
                    accentColor = accentColor,
                    ornamentType = ornamentType,
                    dialType = dialType,
                    moonPhaseIndex = moonPhaseIndex,
                    currentTime = currentTime
                )
            }
        }
    }
}

fun DrawScope.drawSageNocturneFace(
    displayMode: DisplayMode,
    center: Offset,
    radius: Float,
    accentColor: Color,
    ornamentType: String,
    dialType: String,
    moonPhaseIndex: Int,
    currentTime: Calendar
) {
    val isAod = displayMode == DisplayMode.AOD

    if (!isAod && ornamentType != "none") {
        drawOttomanArabesque(center, radius, accentColor, ornamentType)
    }

    drawDialNumeralsAndTicks(center, radius, isAod, dialType, isGold = false)

    if (!isAod) {
        val leftCenter = Offset(center.x - radius * 0.44f, center.y)
        val ringR = radius * 0.17f
        drawCircle(color = Color(0x33FFFFFF), radius = ringR, center = leftCenter, style = Stroke(width = 2.5f))
        drawCircle(color = accentColor.copy(alpha = 0.5f), radius = ringR - 2f, center = leftCenter, style = Stroke(width = 1.5f))

        val rightCenter = Offset(center.x + radius * 0.44f, center.y)
        drawCircle(color = Color(0x33FFFFFF), radius = ringR, center = rightCenter, style = Stroke(width = 2.5f))
        drawCircle(color = accentColor.copy(alpha = 0.5f), radius = ringR - 2f, center = rightCenter, style = Stroke(width = 1.5f))

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFF0D8")
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.10f
                isAntiAlias = true
                typeface = android.graphics.Typeface.SANS_SERIF
            }
            drawText("82%", leftCenter.x, leftCenter.y + radius * 0.035f, paint)

            val day = currentTime.get(Calendar.DAY_OF_MONTH).toString()
            drawText(day, rightCenter.x, rightCenter.y + radius * 0.035f, paint)

            paint.textSize = radius * 0.065f
            paint.color = android.graphics.Color.parseColor("#A8B5A2")
            drawText("BATTERY", leftCenter.x, leftCenter.y + radius * 0.11f, paint)
            drawText("NOV", rightCenter.x, rightCenter.y + radius * 0.11f, paint)
        }
    }

    if (!isAod) {
        val moonCenter = Offset(center.x, center.y - radius * 0.52f)
        val moonRadius = radius * 0.18f
        drawMoonSphere(moonCenter, moonRadius, moonPhaseIndex, isSmall = true)
    }

    drawContext.canvas.nativeCanvas.apply {
        val dhikrPaint = android.graphics.Paint().apply {
            color = if (isAod) android.graphics.Color.parseColor("#A0AEC0") else android.graphics.Color.parseColor("#F1E6D2")
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = radius * 0.09f
            isAntiAlias = true
            alpha = if (isAod) 170 else 240
            typeface = android.graphics.Typeface.SERIF
        }
        val dhikrText = when ((currentTime.get(Calendar.MINUTE) % 4)) {
            0 -> "سُبْحَانَ اللَّهِ"
            1 -> "الْحَمْدُ لِلَّهِ"
            2 -> "لَا إِلَهَ إِلَّا اللَّهُ"
            else -> "اللَّهُ أَكْبَرُ"
        }
        drawText(dhikrText, center.x, center.y - radius * 0.18f, dhikrPaint)
    }

    drawAnalogHands(center, radius, currentTime, isAod = isAod, handStyle = "sage", accentColor = accentColor)
}

fun DrawScope.drawEmeraldNocturneFace(
    displayMode: DisplayMode,
    center: Offset,
    radius: Float,
    moonPhaseIndex: Int,
    currentTime: Calendar
) {
    val isAod = displayMode == DisplayMode.AOD

    if (!isAod) {
        drawOttomanArabesque(center, radius, Color(0xFFD4AF37), "ornate")
    }

    drawDialNumeralsAndTicks(center, radius, isAod, "full", isGold = true)

    if (!isAod) {
        val leftCenter = Offset(center.x - radius * 0.44f, center.y)
        val ringR = radius * 0.17f
        drawCircle(color = Color(0x66D4AF37), radius = ringR, center = leftCenter, style = Stroke(width = 2.5f))

        val rightCenter = Offset(center.x + radius * 0.44f, center.y)
        drawCircle(color = Color(0x66D4AF37), radius = ringR, center = rightCenter, style = Stroke(width = 2.5f))

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F7E5A9")
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.10f
                isAntiAlias = true
                typeface = android.graphics.Typeface.SANS_SERIF
            }
            drawText("85%", leftCenter.x, leftCenter.y + radius * 0.035f, paint)
            val day = currentTime.get(Calendar.DAY_OF_MONTH).toString()
            drawText(day, rightCenter.x, rightCenter.y + radius * 0.035f, paint)

            paint.textSize = radius * 0.065f
            paint.color = android.graphics.Color.parseColor("#D4AF37")
            drawText("BATTERY", leftCenter.x, leftCenter.y + radius * 0.11f, paint)
            drawText("EMERALD", rightCenter.x, rightCenter.y + radius * 0.11f, paint)
        }
    }

    if (!isAod) {
        val moonCenter = Offset(center.x, center.y - radius * 0.52f)
        val moonRadius = radius * 0.18f
        drawMoonSphere(moonCenter, moonRadius, moonPhaseIndex, isSmall = true)
    }

    drawContext.canvas.nativeCanvas.apply {
        val dhikrPaint = android.graphics.Paint().apply {
            color = if (isAod) android.graphics.Color.parseColor("#D4AF37") else android.graphics.Color.parseColor("#F5DEB3")
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = radius * 0.09f
            isAntiAlias = true
            typeface = android.graphics.Typeface.SERIF
        }
        drawText("سُبْحَانَ اللَّهِ", center.x, center.y - radius * 0.18f, dhikrPaint)
    }

    drawAnalogHands(center, radius, currentTime, isAod = isAod, handStyle = "gold", accentColor = Color(0xFFD4AF37))
}

fun DrawScope.drawLunarPhaseFace(
    displayMode: DisplayMode,
    center: Offset,
    radius: Float,
    moonPhaseIndex: Int,
    currentTime: Calendar
) {
    val isAod = displayMode == DisplayMode.AOD

    if (!isAod) {
        val bigMoonRadius = radius * 0.92f
        drawMoonSphere(center, bigMoonRadius, moonPhaseIndex, isSmall = false)
        drawAnalogHands(center, radius, currentTime, isAod = false, handStyle = "lunar", accentColor = Color.White)
    } else {
        drawDialNumeralsAndTicks(center, radius, isAod = true, dialType = "full", isGold = false)
        drawAnalogHands(center, radius, currentTime, isAod = true, handStyle = "lunar", accentColor = Color.White)
    }
}

fun DrawScope.drawMoonSphere(
    center: Offset,
    radius: Float,
    phaseIndex: Int,
    isSmall: Boolean
) {
    val t = phaseIndex / 28.0

    val earthshineAlpha = if (isSmall) 0.85f else 0.75f
    drawCircle(
        color = Color(0xFF1E2228).copy(alpha = earthshineAlpha),
        radius = radius,
        center = center
    )

    drawCircle(
        color = Color(0xFF94A3B8).copy(alpha = 0.28f),
        radius = radius - 0.5f,
        center = center,
        style = Stroke(width = if (isSmall) 1.5f else 2.2f)
    )

    val litPath = Path()
    val rect = androidx.compose.ui.geometry.Rect(
        center.x - radius, center.y - radius,
        center.x + radius, center.y + radius
    )

    if (phaseIndex == 14) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF9EE), Color(0xFFE2D8C3), Color(0xFFC7BDAA)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    } else if (phaseIndex in 1..13) {
        litPath.arcTo(rect, -90f, 180f, false)
        val termX = cos(2.0 * Math.PI * t).toFloat()
        val termRect = androidx.compose.ui.geometry.Rect(
            center.x - abs(termX) * radius, center.y - radius,
            center.x + abs(termX) * radius, center.y + radius
        )
        if (termX <= 0) {
            litPath.arcTo(termRect, 90f, 180f, false)
        } else {
            litPath.arcTo(termRect, 90f, -180f, false)
        }
        litPath.close()

        drawPath(
            path = litPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFDDD2BC), Color(0xFFFFF7E6)),
                startX = center.x - radius,
                endX = center.x + radius
            )
        )
    } else if (phaseIndex in 15..27) {
        litPath.arcTo(rect, 90f, 180f, false)
        val termX = cos(2.0 * Math.PI * t).toFloat()
        val termRect = androidx.compose.ui.geometry.Rect(
            center.x - abs(termX) * radius, center.y - radius,
            center.x + abs(termX) * radius, center.y + radius
        )
        if (termX <= 0) {
            litPath.arcTo(termRect, -90f, 180f, false)
        } else {
            litPath.arcTo(termRect, -90f, -180f, false)
        }
        litPath.close()

        drawPath(
            path = litPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFFF7E6), Color(0xFFDDD2BC)),
                startX = center.x - radius,
                endX = center.x + radius
            )
        )
    }

    val mariaColor = Color(0x221B2026)
    drawCircle(color = mariaColor, radius = radius * 0.32f, center = Offset(center.x - radius * 0.15f, center.y - radius * 0.12f))
    drawCircle(color = mariaColor, radius = radius * 0.22f, center = Offset(center.x + radius * 0.18f, center.y - radius * 0.20f))
    drawCircle(color = mariaColor, radius = radius * 0.26f, center = Offset(center.x + radius * 0.25f, center.y + radius * 0.10f))
    drawCircle(color = mariaColor, radius = radius * 0.35f, center = Offset(center.x - radius * 0.20f, center.y + radius * 0.22f))
}

fun DrawScope.drawOttomanArabesque(
    center: Offset,
    radius: Float,
    accentColor: Color,
    ornamentType: String
) {
    val alphaFactor = when (ornamentType) {
        "minimal" -> 0.30f
        "ornate" -> 0.75f
        else -> 0.50f
    }
    val path = Path()

    path.moveTo(center.x - radius * 0.90f, center.y)
    path.cubicTo(
        center.x - radius * 0.85f, center.y - radius * 0.65f,
        center.x - radius * 0.35f, center.y - radius * 0.75f,
        center.x - radius * 0.10f, center.y - radius * 0.45f
    )
    path.cubicTo(
        center.x - radius * 0.05f, center.y - radius * 0.25f,
        center.x - radius * 0.40f, center.y - radius * 0.10f,
        center.x - radius * 0.65f, center.y
    )
    path.cubicTo(
        center.x - radius * 0.40f, center.y + radius * 0.10f,
        center.x - radius * 0.05f, center.y + radius * 0.25f,
        center.x - radius * 0.10f, center.y + radius * 0.45f
    )
    path.cubicTo(
        center.x - radius * 0.35f, center.y + radius * 0.75f,
        center.x - radius * 0.85f, center.y + radius * 0.65f,
        center.x - radius * 0.90f, center.y
    )

    drawPath(
        path = path,
        color = accentColor.copy(alpha = alphaFactor * 0.65f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    val innerPath = Path()
    innerPath.moveTo(center.x - radius * 0.75f, center.y)
    innerPath.cubicTo(
        center.x - radius * 0.65f, center.y - radius * 0.40f,
        center.x - radius * 0.30f, center.y - radius * 0.35f,
        center.x - radius * 0.20f, center.y
    )
    innerPath.cubicTo(
        center.x - radius * 0.30f, center.y + radius * 0.35f,
        center.x - radius * 0.65f, center.y + radius * 0.40f,
        center.x - radius * 0.75f, center.y
    )

    drawPath(
        path = innerPath,
        color = accentColor.copy(alpha = alphaFactor * 0.45f),
        style = Stroke(width = 1.6f, cap = StrokeCap.Round)
    )
}

fun DrawScope.drawDialNumeralsAndTicks(
    center: Offset,
    radius: Float,
    isAod: Boolean,
    dialType: String,
    isGold: Boolean
) {
    val paint = android.graphics.Paint().apply {
        color = if (isGold) {
            if (isAod) android.graphics.Color.parseColor("#E5C158") else android.graphics.Color.parseColor("#F7E5A9")
        } else {
            if (isAod) android.graphics.Color.parseColor("#E2E8F0") else android.graphics.Color.parseColor("#F1E6D2")
        }
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = radius * 0.14f
        isAntiAlias = true
        alpha = if (isAod) 200 else 255
        typeface = android.graphics.Typeface.SERIF
    }

    if (dialType != "markers") {
        for (hour in 1..12) {
            if (dialType == "cardinal" && (hour % 3 != 0)) continue

            val angleRad = Math.toRadians((hour * 30.0 - 90.0)).toFloat()
            val numeralRadius = radius * 0.84f
            val x = center.x + cos(angleRad) * numeralRadius
            val y = center.y + sin(angleRad) * numeralRadius + (paint.textSize / 3f)
            drawContext.canvas.nativeCanvas.drawText(hour.toString(), x, y, paint)
        }
    }

    for (minute in 0 until 60) {
        val isMajor = minute % 5 == 0
        val angleRad = Math.toRadians((minute * 6.0 - 90.0)).toFloat()
        val endR = radius * 0.96f
        val startR = if (isMajor) radius * 0.90f else radius * 0.93f

        val start = Offset(center.x + cos(angleRad) * startR, center.y + sin(angleRad) * startR)
        val end = Offset(center.x + cos(angleRad) * endR, center.y + sin(angleRad) * endR)

        val strokeColor = if (isGold) {
            if (isMajor) Color(0xFFD4AF37) else Color(0x77D4AF37)
        } else {
            if (isMajor) {
                if (isAod) Color(0xBBE2E8F0) else Color(0xEEF1E6D2)
            } else {
                if (isAod) Color(0x44869BA5) else Color(0x88869BA5)
            }
        }
        drawLine(
            color = strokeColor,
            start = start,
            end = end,
            strokeWidth = if (isMajor) 2.5f else 1.2f,
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawAnalogHands(
    center: Offset,
    radius: Float,
    time: Calendar,
    isAod: Boolean,
    handStyle: String,
    accentColor: Color
) {
    val hours = time.get(Calendar.HOUR_OF_DAY) % 12
    val minutes = time.get(Calendar.MINUTE)
    val seconds = time.get(Calendar.SECOND)

    val hourAngle = Math.toRadians((hours * 30.0 + minutes * 0.5 - 90.0)).toFloat()
    val minuteAngle = Math.toRadians((minutes * 6.0 + seconds * 0.1 - 90.0)).toFloat()
    val secondAngle = Math.toRadians((seconds * 6.0 - 90.0)).toFloat()

    val hourLen = radius * 0.52f
    val minuteLen = radius * 0.74f
    val secondLen = radius * 0.82f

    val hourTip = Offset(center.x + cos(hourAngle) * hourLen, center.y + sin(hourAngle) * hourLen)
    val hourTail = Offset(center.x - cos(hourAngle) * (hourLen * 0.18f), center.y - sin(hourAngle) * (hourLen * 0.18f))

    val minTip = Offset(center.x + cos(minuteAngle) * minuteLen, center.y + sin(minuteAngle) * minuteLen)
    val minTail = Offset(center.x - cos(minuteAngle) * (minuteLen * 0.15f), center.y - sin(minuteAngle) * (minuteLen * 0.15f))

    when (handStyle) {
        "lunar" -> {
            drawLine(color = Color(0xFF15181D), start = hourTail, end = hourTip, strokeWidth = 9.0f, cap = StrokeCap.Round)
            drawLine(color = if (isAod) Color(0xFFE2E8F0) else Color(0xFFF8FAFC), start = hourTail, end = hourTip, strokeWidth = 5.0f, cap = StrokeCap.Round)

            drawLine(color = Color(0xFF15181D), start = minTail, end = minTip, strokeWidth = 8.0f, cap = StrokeCap.Round)
            drawLine(color = if (isAod) Color(0xFFE2E8F0) else Color(0xFFF8FAFC), start = minTail, end = minTip, strokeWidth = 4.5f, cap = StrokeCap.Round)

            if (!isAod) {
                val secTip = Offset(center.x + cos(secondAngle) * secondLen, center.y + sin(secondAngle) * secondLen)
                val secTail = Offset(center.x - cos(secondAngle) * (secondLen * 0.22f), center.y - sin(secondAngle) * (secondLen * 0.22f))
                drawLine(color = Color(0xFFFFFFFF), start = secTail, end = secTip, strokeWidth = 2.0f, cap = StrokeCap.Round)
                val redTipStart = Offset(center.x + cos(secondAngle) * (secondLen * 0.75f), center.y + sin(secondAngle) * (secondLen * 0.75f))
                drawLine(color = Color(0xFFE53935), start = redTipStart, end = secTip, strokeWidth = 2.5f, cap = StrokeCap.Round)

                drawCircle(color = Color(0xFF15181D), radius = 6.0f, center = center)
                drawCircle(color = Color(0xFFE53935), radius = 3.5f, center = center)
            } else {
                drawCircle(color = Color(0xFFE2E8F0), radius = 4.0f, center = center)
            }
        }
        "gold" -> {
            val handGold = if (isAod) Color(0xFFE5C158) else Color(0xFFD4AF37)

            drawLine(color = handGold, start = hourTail, end = hourTip, strokeWidth = 6.0f, cap = StrokeCap.Round)
            drawLine(color = Color(0xFF000000), start = Offset(center.x + cos(hourAngle) * (hourLen * 0.25f), center.y + sin(hourAngle) * (hourLen * 0.25f)), end = Offset(center.x + cos(hourAngle) * (hourLen * 0.78f), center.y + sin(hourAngle) * (hourLen * 0.78f)), strokeWidth = 2.2f, cap = StrokeCap.Round)

            drawLine(color = handGold, start = minTail, end = minTip, strokeWidth = 5.2f, cap = StrokeCap.Round)
            drawLine(color = Color(0xFF000000), start = Offset(center.x + cos(minuteAngle) * (minuteLen * 0.20f), center.y + sin(minuteAngle) * (minuteLen * 0.20f)), end = Offset(center.x + cos(minuteAngle) * (minuteLen * 0.85f), center.y + sin(minuteAngle) * (minuteLen * 0.85f)), strokeWidth = 2.0f, cap = StrokeCap.Round)

            if (!isAod) {
                val secTip = Offset(center.x + cos(secondAngle) * secondLen, center.y + sin(secondAngle) * secondLen)
                val secTail = Offset(center.x - cos(secondAngle) * (secondLen * 0.22f), center.y - sin(secondAngle) * (secondLen * 0.22f))
                drawLine(color = Color(0xFFFFF0B3), start = secTail, end = secTip, strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawCircle(color = Color(0xFF15181D), radius = 6.5f, center = center)
                drawCircle(color = Color(0xFFD4AF37), radius = 4.0f, center = center)
            } else {
                drawCircle(color = Color(0xFFE5C158), radius = 4.5f, center = center)
            }
        }
        else -> {
            val handColor = if (isAod) Color(0xFFE2E8F0) else accentColor

            drawLine(color = handColor, start = hourTail, end = hourTip, strokeWidth = 5.5f, cap = StrokeCap.Round)
            drawLine(color = if (isAod) Color(0xFF000000) else Color(0xAA000000), start = Offset(center.x + cos(hourAngle) * (hourLen * 0.25f), center.y + sin(hourAngle) * (hourLen * 0.25f)), end = Offset(center.x + cos(hourAngle) * (hourLen * 0.78f), center.y + sin(hourAngle) * (hourLen * 0.78f)), strokeWidth = 2.0f, cap = StrokeCap.Round)

            drawLine(color = handColor, start = minTail, end = minTip, strokeWidth = 4.8f, cap = StrokeCap.Round)
            drawLine(color = if (isAod) Color(0xFF000000) else Color(0xAA000000), start = Offset(center.x + cos(minuteAngle) * (minuteLen * 0.20f), center.y + sin(minuteAngle) * (minuteLen * 0.20f)), end = Offset(center.x + cos(minuteAngle) * (minuteLen * 0.85f), center.y + sin(minuteAngle) * (minuteLen * 0.85f)), strokeWidth = 1.8f, cap = StrokeCap.Round)

            if (!isAod) {
                val secTip = Offset(center.x + cos(secondAngle) * secondLen, center.y + sin(secondAngle) * secondLen)
                val secTail = Offset(center.x - cos(secondAngle) * (secondLen * 0.22f), center.y - sin(secondAngle) * (secondLen * 0.22f))
                drawLine(color = Color(0xFFD4AF37), start = secTail, end = secTip, strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawCircle(color = Color(0xFF15181D), radius = 6.0f, center = center)
                drawCircle(color = Color(0xFFD4AF37), radius = 3.5f, center = center)
            } else {
                drawCircle(color = Color(0xFFE2E8F0), radius = 4.0f, center = center)
            }
        }
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
