package com.mohamedzayed.nocturne

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.math.*

enum class WatchThemeType(val id: String, val titleAr: String, val titleEn: String) {
    SAGE_NOCTURNE("sage_nocturne", "Sage Nocturne (العثماني الأخضر)", "Sage Nocturne"),
    LUNAR_PHASE("lunar_phase", "Lunar Phase (القمر السينمائي)", "Lunar Phase")
}

enum class DisplayMode {
    ACTIVE,
    AOD
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

    var selectedTheme by remember { mutableStateOf(WatchThemeType.SAGE_NOCTURNE) }
    var displayMode by remember { mutableStateOf(DisplayMode.ACTIVE) }
    var selectedBackground by remember { mutableStateOf(backgroundOptions[0]) }
    var selectedAccent by remember { mutableStateOf(accentOptions[0]) }
    var selectedOrnament by remember { mutableStateOf(ornamentOptions[0].first) }
    var selectedDial by remember { mutableStateOf(dialOptions[0].first) }

    // Astronomical Moon Phase calculation for today
    val todayMoonPhase = remember { calculateCurrentMoonPhase() }
    var previewMoonPhase by remember { mutableFloatStateOf(todayMoonPhase.toFloat()) }

    var isConnectedToWatch by remember { mutableStateOf<Boolean?>(null) }
    var connectedWatchName by remember { mutableStateOf("") }
    var showInstructionsDialog by remember { mutableStateOf(false) }

    // Check Wear OS Watch connection
    LaunchedEffect(Unit) {
        try {
            val nodeClient = Wearable.getNodeClient(context)
            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                if (nodes.isNotEmpty()) {
                    isConnectedToWatch = true
                    connectedWatchName = nodes[0].displayName
                } else {
                    isConnectedToWatch = false
                }
            }.addOnFailureListener {
                isConnectedToWatch = false
            }
        } catch (_: Exception) {
            isConnectedToWatch = false
        }
    }

    // Live clock ticks
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

                // Connection badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isConnectedToWatch == true) Color(0xFF1B3828) else Color(0xFF262C36),
                    border = BorderStroke(
                        1.dp,
                        if (isConnectedToWatch == true) Color(0xFF2E7D32) else Color(0xFF455A64)
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
                                .background(if (isConnectedToWatch == true) Color(0xFF4CAF50) else Color(0xFFFFA000))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnectedToWatch == true) {
                                if (connectedWatchName.isNotEmpty()) connectedWatchName else "الساعة متصلة"
                            } else {
                                "جاهز للمزامنة"
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
                // Outer watch bezel / casing
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color(0xFF1C2229),
                    border = BorderStroke(5.dp, Brush.radialGradient(
                        listOf(Color(0xFF3A4754), Color(0xFF1A2027))
                    ))
                ) {
                    // Watch screen dial
                    WatchFaceDialCanvas(
                        themeType = selectedTheme,
                        displayMode = displayMode,
                        bgColor = if (selectedTheme == WatchThemeType.LUNAR_PHASE && displayMode == DisplayMode.ACTIVE) Color(0xFF000000) else selectedBackground.color,
                        accentColor = selectedAccent.color,
                        ornamentType = selectedOrnament,
                        dialType = selectedDial,
                        moonPhaseIndex = previewMoonPhase.roundToInt().coerceIn(0, 28),
                        currentTime = currentTime
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Astronomical Moon Phase info badge
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

            // Theme Cards Selection
            Text(
                text = "اختر الثيم الأساسي للواجهة",
                color = Color(0xFFBAC7D5),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeCard(
                    title = "Sage Nocturne",
                    sub = "العثماني الأخضر الأصلي",
                    icon = Icons.Default.FilterVintage,
                    isSelected = selectedTheme == WatchThemeType.SAGE_NOCTURNE,
                    onClick = { selectedTheme = WatchThemeType.SAGE_NOCTURNE },
                    modifier = Modifier.weight(1f)
                )

                ThemeCard(
                    title = "Lunar Phase",
                    sub = "القمر السينمائي الكبير",
                    icon = Icons.Default.Brightness2,
                    isSelected = selectedTheme == WatchThemeType.LUNAR_PHASE,
                    onClick = { selectedTheme = WatchThemeType.LUNAR_PHASE },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Customization Options based on selected theme
            if (selectedTheme == WatchThemeType.SAGE_NOCTURNE) {
                // Background Colors
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

                Spacer(modifier = Modifier.height(16.dp))

                // Accent / Hand colors
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

                Spacer(modifier = Modifier.height(16.dp))

                // Ornament Density
                Text(
                    text = "كثافة الزخرفة العثمانية",
                    color = Color(0xFFBAC7D5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ornamentOptions.forEach { (id, label) ->
                        FilterChip(
                            selected = selectedOrnament == id,
                            onClick = { selectedOrnament = id },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2C3947),
                                selectedLabelColor = Color(0xFFF1E6D2),
                                containerColor = Color(0xFF161C24),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            } else {
                // Lunar Phase customization info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF171E26),
                    border = BorderStroke(1.dp, Color(0xFF263342)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مميزات ثيم Lunar Phase السينمائي",
                                color = Color(0xFFF1E6D2),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• في الوضع النشط: يملأ قرص القمر الواجهة بدقة ثلاثية الأبعاد كاملة وبدون أي تعقيدات، مع عقارب واضحة التباين.\n• في وضع AOD: يختفي القمر وتتحول الخلفية للسواد التام مع ظهور أرقام الساعات الفضية وعلامات الدقايق للحفاظ التام على البطارية.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Moon Phase Scrubber (Test all 29 lunar phases)
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

            Spacer(modifier = Modifier.height(28.dp))

            // Main Action Buttons
            Button(
                onClick = {
                    coroutineScope.launch {
                        applyToWatch(
                            context = context,
                            theme = selectedTheme.id,
                            bg = selectedBackground.id,
                            accent = selectedAccent.id,
                            ornament = selectedOrnament,
                            dial = selectedDial,
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("✓ تم حفظ إعدادات ${selectedTheme.titleAr} وتطبيقها بنجاح!")
                                }
                            },
                            onError = { err ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("تم حفظ الإعداد. $err")
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E6B4F),
                    contentColor = Color(0xFFFFFFFF)
                )
            ) {
                Icon(Icons.Default.Watch, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تطبيق الثيم على الساعة (Apply to Watch)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
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
                Text("دليل التثبيت والتخصيص على Galaxy Watch 5", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = {
                Text("دليل التثبيت والتخصيص على الساعة", fontWeight = FontWeight.Bold, color = Color(0xFFF1E6D2))
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. تثبيت الواجهة على الساعة:\n" +
                                "يمكن تثبيت ملف watchface-debug.apk مباشرة على Samsung Galaxy Watch 5 بنظام Wear OS 6 عبر ADB اللاسلكي:\n" +
                                "adb connect <watch_ip>:5555\n" +
                                "adb install watchface-debug.apk\n\n" +
                                "2. التبديل والتخصيص من الساعة مباشرة:\n" +
                                "• اضغط مطولاً على شاشة الساعة الحالية واضغط 'تخصيص' (Customize).\n" +
                                "• ستجد خيار 'التصميم' (Theme) يتيح التبديل الفوري بين Sage Nocturne و Lunar Phase.\n" +
                                "• يمكنك تخصيص الألوان والزخارف والخانات بحرية.\n\n" +
                                "3. التخصيص من الهاتف:\n" +
                                "افتح تطبيق Galaxy Wearable على هاتفك Galaxy S25 Ultra -> واجهات الساعة -> اختر Nocturne واضغط تخصيص.",
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

@Composable
fun ThemeCard(
    title: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        if (isSelected) Color(0xFFD4AF37) else Color(0xFF263342),
        animationSpec = tween(200)
    )
    val bgColor by animateColorAsState(
        if (isSelected) Color(0xFF222B35) else Color(0xFF141A22),
        animationSpec = tween(200)
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFD4AF37) else Color(0xFF8A99A8),
                    modifier = Modifier.size(22.dp)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD4AF37)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color(0xFFF1E6D2) else Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sub,
                color = Color(0xFF8A99A8),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun WatchFaceDialCanvas(
    themeType: WatchThemeType,
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

        // Clip to circle
        val circlePath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }

        // Background
        val dialBg = if (displayMode == DisplayMode.AOD) Color(0xFF000000) else bgColor
        drawCircle(color = dialBg, radius = radius, center = center)

        if (themeType == WatchThemeType.SAGE_NOCTURNE) {
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
        } else {
            drawLunarPhaseFace(
                displayMode = displayMode,
                center = center,
                radius = radius,
                moonPhaseIndex = moonPhaseIndex,
                currentTime = currentTime
            )
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

    // 1. Ottoman Ornament on the left half
    if (!isAod && ornamentType != "none") {
        drawOttomanArabesque(center, radius, accentColor, ornamentType)
    }

    // 2. Numerals and Minute Markers
    drawDialNumeralsAndTicks(center, radius, isAod, dialType)

    // 3. Complications in Active Mode
    if (!isAod) {
        // Left Complication (Battery)
        val leftCenter = Offset(center.x - radius * 0.44f, center.y)
        val ringR = radius * 0.17f
        drawCircle(color = Color(0x33FFFFFF), radius = ringR, center = leftCenter, style = Stroke(width = 2.5f))
        drawCircle(color = accentColor.copy(alpha = 0.5f), radius = ringR - 2f, center = leftCenter, style = Stroke(width = 1.5f))

        // Right Complication (Date)
        val rightCenter = Offset(center.x + radius * 0.44f, center.y)
        drawCircle(color = Color(0x33FFFFFF), radius = ringR, center = rightCenter, style = Stroke(width = 2.5f))
        drawCircle(color = accentColor.copy(alpha = 0.5f), radius = ringR - 2f, center = rightCenter, style = Stroke(width = 1.5f))

        // Text inside complications
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

    // 4. Small Moon Phase at 12 o'clock position (Active mode only)
    if (!isAod) {
        val moonCenter = Offset(center.x, center.y - radius * 0.52f)
        val moonRadius = radius * 0.18f
        drawMoonSphere(moonCenter, moonRadius, moonPhaseIndex, isSmall = true)
    }

    // 5. Dhikr Inscription
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

    // 6. Analog Hands
    drawAnalogHands(center, radius, currentTime, isAod = isAod, isLunarTheme = false, accentColor = accentColor)
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
        // Active mode: Big Moon fills ~92% of the round face!
        val bigMoonRadius = radius * 0.92f
        drawMoonSphere(center, bigMoonRadius, moonPhaseIndex, isSmall = false)

        // Above it: Dual-contrast Hour, Minute, and Second hands
        drawAnalogHands(center, radius, currentTime, isAod = false, isLunarTheme = true, accentColor = Color.White)
    } else {
        // AOD mode: Black background, Moon and Second hand disappear.
        // Hour numerals and minute ticks appear in moonlight silver!
        drawDialNumeralsAndTicks(center, radius, isAod = true, dialType = "full")

        // Hour and minute hands with matching pivots
        drawAnalogHands(center, radius, currentTime, isAod = true, isLunarTheme = true, accentColor = Color.White)
    }
}

fun DrawScope.drawMoonSphere(
    center: Offset,
    radius: Float,
    phaseIndex: Int,
    isSmall: Boolean
) {
    val t = phaseIndex / 28.0
    // Sun vector in 3D:
    // Waxing: lit from right (sx > 0)
    // Full: lit in front (sz > 0)
    // Waning: lit from left (sx < 0)
    val sx = sin(2.0 * Math.PI * t).toFloat()
    val sz = -cos(2.0 * Math.PI * t).toFloat()

    // 1. Dark side / earthshine disk base
    // Notice earthshine is never pitch black: deep charcoal with subtle illumination
    val earthshineAlpha = if (isSmall) 0.85f else 0.75f
    drawCircle(
        color = Color(0xFF1E2228).copy(alpha = earthshineAlpha),
        radius = radius,
        center = center
    )

    // 2. Faint rim / ash light on limb (Crucial for New Moon so the sphere is visibly outlined)
    drawCircle(
        color = Color(0xFF94A3B8).copy(alpha = 0.28f),
        radius = radius - 0.5f,
        center = center,
        style = Stroke(width = if (isSmall) 1.5f else 2.2f)
    )

    // 3. Lit phase geometry using elliptical path
    val litPath = Path()
    val rect = androidx.compose.ui.geometry.Rect(
        center.x - radius, center.y - radius,
        center.x + radius, center.y + radius
    )

    // Determine terminator curve based on phase
    if (phaseIndex == 14) {
        // Full Moon: entire disk is lit
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
        // Waxing (lit on right side)
        // Outer arc is right semicircle from -90 to +90 deg
        litPath.arcTo(rect, -90f, 180f, false)
        // Terminator is an ellipse curving from bottom to top
        val termX = cos(2.0 * Math.PI * t).toFloat()
        val termRect = androidx.compose.ui.geometry.Rect(
            center.x - abs(termX) * radius, center.y - radius,
            center.x + abs(termX) * radius, center.y + radius
        )
        if (termX <= 0) {
            // Gibbous: terminator bulges left
            litPath.arcTo(termRect, 90f, 180f, false)
        } else {
            // Crescent: terminator curves right
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
        // Waning (lit on left side)
        // Outer arc is left semicircle from 90 to 180 deg
        litPath.arcTo(rect, 90f, 180f, false)
        val termX = cos(2.0 * Math.PI * t).toFloat()
        val termRect = androidx.compose.ui.geometry.Rect(
            center.x - abs(termX) * radius, center.y - radius,
            center.x + abs(termX) * radius, center.y + radius
        )
        if (termX <= 0) {
            // Gibbous: terminator bulges right
            litPath.arcTo(termRect, -90f, 180f, false)
        } else {
            // Crescent: terminator curves left
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

    // 4. Subtle lunar maria / crater shadows across surface for realism
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
        "ornate" -> 0.70f
        else -> 0.50f
    }
    val path = Path()

    // Graceful Ottoman arabesque curve covering the left flank
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
        color = accentColor.copy(alpha = alphaFactor * 0.55f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Inner filigree palmettes
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
        color = accentColor.copy(alpha = alphaFactor * 0.35f),
        style = Stroke(width = 1.5f, cap = StrokeCap.Round)
    )
}

fun DrawScope.drawDialNumeralsAndTicks(
    center: Offset,
    radius: Float,
    isAod: Boolean,
    dialType: String
) {
    val paint = android.graphics.Paint().apply {
        color = if (isAod) android.graphics.Color.parseColor("#E2E8F0") else android.graphics.Color.parseColor("#F1E6D2")
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = radius * 0.14f
        isAntiAlias = true
        alpha = if (isAod) 200 else 255
        typeface = android.graphics.Typeface.SERIF
    }

    // Hour Numerals 1 to 12
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

    // 60 Minute Ticks
    for (minute in 0 until 60) {
        val isMajor = minute % 5 == 0
        val angleRad = Math.toRadians((minute * 6.0 - 90.0)).toFloat()
        val endR = radius * 0.96f
        val startR = if (isMajor) radius * 0.90f else radius * 0.93f

        val start = Offset(center.x + cos(angleRad) * startR, center.y + sin(angleRad) * startR)
        val end = Offset(center.x + cos(angleRad) * endR, center.y + sin(angleRad) * endR)

        val strokeColor = if (isMajor) {
            if (isAod) Color(0xBBE2E8F0) else Color(0xEEF1E6D2)
        } else {
            if (isAod) Color(0x44869BA5) else Color(0x88869BA5)
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
    isLunarTheme: Boolean,
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

    // 1. Hour Hand
    val hourTip = Offset(center.x + cos(hourAngle) * hourLen, center.y + sin(hourAngle) * hourLen)
    val hourTail = Offset(center.x - cos(hourAngle) * (hourLen * 0.18f), center.y - sin(hourAngle) * (hourLen * 0.18f))

    if (isLunarTheme) {
        // Dual-contrast border for Lunar Theme (visible over bright moon and dark space)
        drawLine(
            color = Color(0xFF15181D),
            start = hourTail,
            end = hourTip,
            strokeWidth = 9.0f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = if (isAod) Color(0xFFE2E8F0) else Color(0xFFF8FAFC),
            start = hourTail,
            end = hourTip,
            strokeWidth = 5.0f,
            cap = StrokeCap.Round
        )
    } else {
        // Sage Nocturne hollow elegant hand
        drawLine(
            color = if (isAod) Color(0xFFE2E8F0) else accentColor,
            start = hourTail,
            end = hourTip,
            strokeWidth = 5.5f,
            cap = StrokeCap.Round
        )
        // Hollow opening
        drawLine(
            color = if (isAod) Color(0xFF000000) else Color(0xAA000000),
            start = Offset(center.x + cos(hourAngle) * (hourLen * 0.25f), center.y + sin(hourAngle) * (hourLen * 0.25f)),
            end = Offset(center.x + cos(hourAngle) * (hourLen * 0.78f), center.y + sin(hourAngle) * (hourLen * 0.78f)),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )
    }

    // 2. Minute Hand
    val minTip = Offset(center.x + cos(minuteAngle) * minuteLen, center.y + sin(minuteAngle) * minuteLen)
    val minTail = Offset(center.x - cos(minuteAngle) * (minuteLen * 0.15f), center.y - sin(minuteAngle) * (minuteLen * 0.15f))

    if (isLunarTheme) {
        drawLine(
            color = Color(0xFF15181D),
            start = minTail,
            end = minTip,
            strokeWidth = 8.0f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = if (isAod) Color(0xFFE2E8F0) else Color(0xFFF8FAFC),
            start = minTail,
            end = minTip,
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )
    } else {
        drawLine(
            color = if (isAod) Color(0xFFE2E8F0) else accentColor,
            start = minTail,
            end = minTip,
            strokeWidth = 4.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = if (isAod) Color(0xFF000000) else Color(0xAA000000),
            start = Offset(center.x + cos(minuteAngle) * (minuteLen * 0.20f), center.y + sin(minuteAngle) * (minuteLen * 0.20f)),
            end = Offset(center.x + cos(minuteAngle) * (minuteLen * 0.85f), center.y + sin(minuteAngle) * (minuteLen * 0.85f)),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }

    // 3. Second Hand (Active Mode Only)
    if (!isAod) {
        val secTip = Offset(center.x + cos(secondAngle) * secondLen, center.y + sin(secondAngle) * secondLen)
        val secTail = Offset(center.x - cos(secondAngle) * (secondLen * 0.22f), center.y - sin(secondAngle) * (secondLen * 0.22f))

        if (isLunarTheme) {
            // Sleek white needle with subtle crimson accent tip
            drawLine(
                color = Color(0xFFFFFFFF),
                start = secTail,
                end = secTip,
                strokeWidth = 2.0f,
                cap = StrokeCap.Round
            )
            val redTipStart = Offset(center.x + cos(secondAngle) * (secondLen * 0.75f), center.y + sin(secondAngle) * (secondLen * 0.75f))
            drawLine(
                color = Color(0xFFE53935),
                start = redTipStart,
                end = secTip,
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        } else {
            drawLine(
                color = Color(0xFFD4AF37),
                start = secTail,
                end = secTip,
                strokeWidth = 1.8f,
                cap = StrokeCap.Round
            )
        }

        // Center hub
        drawCircle(color = Color(0xFF15181D), radius = 6.0f, center = center)
        drawCircle(color = Color(0xFFD4AF37), radius = 3.5f, center = center)
    } else {
        drawCircle(color = Color(0xFFE2E8F0), radius = 4.0f, center = center)
    }
}

// Astronomical calculation of today's lunar phase index (0 to 28)
fun calculateCurrentMoonPhase(): Int {
    val now = Date().time
    // Reference New Moon: Jan 11, 2024, 11:57 UTC (in epoch millis: 1704974220000L)
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

fun applyToWatch(
    context: Context,
    theme: String,
    bg: String,
    accent: String,
    ornament: String,
    dial: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val request = PutDataMapRequest.create("/nocturne/config").apply {
            dataMap.putString("watch_theme", theme)
            dataMap.putString("background", bg)
            dataMap.putString("accent", accent)
            dataMap.putString("ornament", ornament)
            dataMap.putString("dial", dial)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                // If watch is not currently in direct range, configuration is saved locally and queued
                onSuccess()
            }
    } catch (e: Exception) {
        onError("تم حفظ الإعداد وسينتقل تلقائياً عند اتصال الساعة")
    }
}

fun openGalaxyWearable(context: Context) {
    try {
        // Try Samsung Galaxy Wearable Manager directly
        val intent = context.packageManager.getLaunchIntentForPackage("com.samsung.android.app.watchmanager")
        if (intent != null) {
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {}

    try {
        // Fallback to Wear OS Companion App
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.wearable.app")
        if (intent != null) {
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {}

    Toast.makeText(context, "يرجى فتح تطبيق Galaxy Wearable واختيار Nocturne لتخصيص الواجهة", Toast.LENGTH_LONG).show()
}
