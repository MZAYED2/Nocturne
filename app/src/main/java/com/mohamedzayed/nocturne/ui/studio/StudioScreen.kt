package com.mohamedzayed.nocturne.ui.studio

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.data.DialSettings
import com.mohamedzayed.nocturne.data.ThemeLibrary
import com.mohamedzayed.nocturne.data.ThemePreset
import com.mohamedzayed.nocturne.model.DisplayMode
import com.mohamedzayed.nocturne.model.WatchFaceItem
import com.mohamedzayed.nocturne.model.accentOptions
import com.mohamedzayed.nocturne.model.backgroundOptions
import com.mohamedzayed.nocturne.model.dialOptions
import com.mohamedzayed.nocturne.model.ornamentOptions
import com.mohamedzayed.nocturne.preview.LunarCalculation
import com.mohamedzayed.nocturne.resolveStudioVariant
import com.mohamedzayed.nocturne.ui.components.CircularWatchPreview
import com.mohamedzayed.nocturne.ui.components.MidnightConfirmDialog
import com.mohamedzayed.nocturne.ui.components.MidnightInputDialog
import com.mohamedzayed.nocturne.ui.components.MidnightOutlineButton
import com.mohamedzayed.nocturne.ui.components.MidnightPrimaryButton
import com.mohamedzayed.nocturne.ui.components.MidnightSecondaryButton
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    targetFace: WatchFaceItem,
    themeLibrary: ThemeLibrary,
    onBack: () -> Unit,
    onApplyToWatch: (WatchFaceItem) -> Unit,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = NocturneTheme.colors
    val scrollState = rememberScrollState()

    // Undo / Redo Draft History Stack
    var historyStack by remember { mutableStateOf(listOf(DialSettings())) }
    var historyIndex by remember { mutableIntStateOf(0) }
    val currentSettings = historyStack.getOrElse(historyIndex) { DialSettings() }

    val canUndo = historyIndex > 0
    val canRedo = historyIndex < historyStack.lastIndex

    fun pushSettings(newSettings: DialSettings) {
        if (newSettings == currentSettings) return
        val truncated = historyStack.subList(0, historyIndex + 1)
        val updated = (truncated + newSettings).takeLast(30)
        historyStack = updated
        historyIndex = updated.lastIndex
    }

    fun undo() {
        if (canUndo) {
            historyIndex--
        }
    }

    fun redo() {
        if (canRedo) {
            historyIndex++
        }
    }

    // Studio state
    var displayMode by remember { mutableStateOf(DisplayMode.ACTIVE) }
    var moonPhaseIndex by remember { mutableIntStateOf(14) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Appearance, 1 = Complications, 2 = Presets

    // Dialogs
    var showSaveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<ThemePreset?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<ThemePreset?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Intercept back navigation
    BackHandler(onBack = onBack)

    // Accurate Editor-Derived Deployability Calculation
    val isMasterSettings = currentSettings == DialSettings()
    val isSageNavy = targetFace.id == "sage_nocturne" &&
            currentSettings.background == "navy" &&
            currentSettings.accent == "sage" &&
            currentSettings.ornament == "balanced" &&
            currentSettings.dial == "full"

    val isDeployable = isMasterSettings || isSageNavy

    val displayName = if (isArabic && targetFace.nameAr.isNotBlank()) targetFace.nameAr else targetFace.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.studio_title),
                            fontSize = 11.sp,
                            color = colors.primaryAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = colors.primaryAccent
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { undo() },
                        enabled = canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.btn_undo),
                            tint = if (canUndo) colors.primaryAccent else colors.secondaryText.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = { redo() },
                        enabled = canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = stringResource(R.string.btn_redo),
                            tint = if (canRedo) colors.primaryAccent else colors.secondaryText.copy(alpha = 0.35f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.mainBackground
                )
            )
        },
        containerColor = colors.mainBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interactive Live Circular Dial Preview (Prominent & Balanced)
            val selectedBgColor = backgroundOptions.firstOrNull { it.id == currentSettings.background }?.color
                ?: Color(0xFF101317)
            val selectedAccentColor = accentOptions.firstOrNull { it.id == currentSettings.accent }?.color
                ?: Color(0xFFC6AC79)

            CircularWatchPreview(
                faceId = targetFace.id,
                displayMode = displayMode,
                size = 240.dp,
                showToggle = true,
                onToggleDisplayMode = { displayMode = it },
                bgColor = selectedBgColor,
                accentColor = selectedAccentColor,
                ornamentType = currentSettings.ornament,
                dialType = currentSettings.dial,
                moonPhaseIndex = moonPhaseIndex
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Deployment Honesty Banner (State-Derived)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDeployable) colors.elevatedSurface else colors.elevatedSurface.copy(alpha = 0.8f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDeployable) colors.success.copy(alpha = 0.5f) else colors.warning.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDeployable) Icons.Default.Check else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDeployable) colors.success else colors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val statusText = when {
                            isMasterSettings -> stringResource(R.string.deployment_status_master)
                            isSageNavy -> stringResource(R.string.deployment_status_variant)
                            else -> stringResource(R.string.deployment_status_preview_only)
                        }
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeployable) colors.success else colors.warning
                        )
                    }

                    if (!isDeployable) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.deployment_note_preview_only),
                            fontSize = 11.sp,
                            color = colors.secondaryText,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Actions Row (Reset / Save Preset / Apply to Watch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MidnightOutlineButton(
                    onClick = { pushSettings(DialSettings()) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_reset_defaults), fontSize = 11.sp, maxLines = 1)
                }

                MidnightSecondaryButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_save_current_preset), fontSize = 11.sp, maxLines = 1)
                }

                MidnightPrimaryButton(
                    onClick = {
                        if (isDeployable) {
                            val deployableFace = if (isSageNavy) {
                                runCatching {
                                    resolveStudioVariant(
                                        context,
                                        targetFace,
                                        ThemePreset.create("sage_nocturne", "Sage Navy", currentSettings)
                                    )
                                }.getOrElse { targetFace }
                            } else {
                                targetFace
                            }
                            onApplyToWatch(deployableFace)
                        }
                    },
                    enabled = isDeployable,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    val btnLabel = if (isDeployable) stringResource(R.string.btn_apply_to_watch) else "Preview Only"
                    Text(btnLabel, fontSize = 11.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.primarySurface,
                contentColor = colors.primaryAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = colors.primaryAccent
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_appearance), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_complications), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.tab_presets), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            when (selectedTab) {
                0 -> AppearanceTabContent(
                    settings = currentSettings,
                    onUpdateSettings = { pushSettings(it) }
                )
                1 -> ComplicationsTabContent(
                    moonPhaseIndex = moonPhaseIndex,
                    onUpdateMoonPhase = { moonPhaseIndex = it },
                    isArabic = isArabic
                )
                2 -> PresetsTabContent(
                    masterFaceId = targetFace.id,
                    themeLibrary = themeLibrary,
                    onLoadPreset = { preset -> pushSettings(preset.settings) },
                    onRename = { preset -> showRenameDialog = preset },
                    onDelete = { preset -> showDeleteConfirmDialog = preset },
                    onExport = { preset ->
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Nocturne Preset: ${preset.name}")
                            putExtra(Intent.EXTRA_TEXT, preset.json().toString(2))
                        }
                        context.startActivity(Intent.createChooser(share, "Export Preset"))
                    },
                    onImportClick = { showImportDialog = true }
                )
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        MidnightInputDialog(
            title = stringResource(R.string.btn_save_current_preset),
            hint = stringResource(R.string.preset_name_hint),
            onConfirm = { name ->
                val newPreset = ThemePreset.create(
                    parent = targetFace.id,
                    name = name,
                    settings = currentSettings
                )
                themeLibrary.save(newPreset)
                Toast.makeText(context, R.string.preset_saved, Toast.LENGTH_SHORT).show()
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    // Rename Preset Dialog
    showRenameDialog?.let { preset ->
        MidnightInputDialog(
            title = stringResource(R.string.btn_rename_preset),
            initialValue = preset.name,
            hint = stringResource(R.string.preset_name_hint),
            onConfirm = { newName ->
                themeLibrary.rename(preset.id, newName)
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        )
    }

    // Delete Confirm Dialog
    showDeleteConfirmDialog?.let { preset ->
        MidnightConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_preset, preset.name),
            isDestructive = true,
            confirmText = stringResource(R.string.btn_delete_preset),
            onConfirm = {
                themeLibrary.delete(preset.id)
                Toast.makeText(context, R.string.preset_deleted, Toast.LENGTH_SHORT).show()
                showDeleteConfirmDialog = null
            },
            onDismiss = { showDeleteConfirmDialog = null }
        )
    }

    // Import Preset Dialog
    if (showImportDialog) {
        MidnightInputDialog(
            title = stringResource(R.string.btn_import_preset),
            hint = "Paste JSON preset here...",
            onConfirm = { jsonStr ->
                runCatching {
                    val json = JSONObject(jsonStr)
                    val preset = ThemePreset.parse(json)
                    themeLibrary.save(preset)
                    Toast.makeText(context, R.string.preset_imported, Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Invalid JSON preset", Toast.LENGTH_SHORT).show()
                }
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
private fun AppearanceTabContent(
    settings: DialSettings,
    onUpdateSettings: (DialSettings) -> Unit
) {
    val colors = NocturneTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Background Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.section_background),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    backgroundOptions.forEach { opt ->
                        val isSelected = settings.background == opt.id
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(opt.color)
                                .border(
                                    BorderStroke(
                                        if (isSelected) 3.dp else 1.dp,
                                        if (isSelected) colors.primaryAccent else colors.borders
                                    ),
                                    CircleShape
                                )
                                .clickable { onUpdateSettings(settings.copy(background = opt.id)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Accent Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.section_accent),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accentOptions.forEach { opt ->
                        val isSelected = settings.accent == opt.id
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(opt.color)
                                .border(
                                    BorderStroke(
                                        if (isSelected) 3.dp else 1.dp,
                                        if (isSelected) colors.primaryAccent else colors.borders
                                    ),
                                    CircleShape
                                )
                                .clickable { onUpdateSettings(settings.copy(accent = opt.id)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.mainBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ornament Style
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.section_ornament),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ornamentOptions.forEach { (id, resId) ->
                        val isSelected = settings.ornament == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.primaryAccent else colors.elevatedSurface)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) colors.primaryAccent else colors.borders),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onUpdateSettings(settings.copy(ornament = id)) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(resId),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.mainBackground else colors.primaryText
                            )
                        }
                    }
                }
            }
        }

        // Dial Numerals
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.section_dial),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dialOptions.forEach { (id, resId) ->
                        val isSelected = settings.dial == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.primaryAccent else colors.elevatedSurface)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) colors.primaryAccent else colors.borders),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onUpdateSettings(settings.copy(dial = id)) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(resId),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.mainBackground else colors.primaryText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplicationsTabContent(
    moonPhaseIndex: Int,
    onUpdateMoonPhase: (Int) -> Unit,
    isArabic: Boolean
) {
    val colors = NocturneTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
        border = BorderStroke(1.dp, colors.borders)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.moon_phase_slider),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            val moonName = LunarCalculation.getMoonPhaseName(moonPhaseIndex, isArabic)
            Text(
                text = stringResource(R.string.moon_phase_day, moonPhaseIndex, moonName),
                fontSize = 12.sp,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            Slider(
                value = moonPhaseIndex.toFloat(),
                onValueChange = { onUpdateMoonPhase(it.toInt().coerceIn(0, 28)) },
                valueRange = 0f..28f,
                steps = 27,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primaryAccent,
                    activeTrackColor = colors.primaryAccent,
                    inactiveTrackColor = colors.elevatedSurface
                )
            )
        }
    }
}

@Composable
private fun PresetsTabContent(
    masterFaceId: String,
    themeLibrary: ThemeLibrary,
    onLoadPreset: (ThemePreset) -> Unit,
    onRename: (ThemePreset) -> Unit,
    onDelete: (ThemePreset) -> Unit,
    onExport: (ThemePreset) -> Unit,
    onImportClick: () -> Unit
) {
    val colors = NocturneTheme.colors
    val matchingPresets = themeLibrary.presets.filter { it.parent == masterFaceId }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header & Import action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.section_presets),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryAccent
            )
            MidnightOutlineButton(
                onClick = onImportClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_import_preset), fontSize = 11.sp)
            }
        }

        if (matchingPresets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved presets for this watch face yet. Customize appearance and tap 'Save as Preset'.",
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
            }
        } else {
            matchingPresets.forEach { preset ->
                val isArchived = themeLibrary.hidden.contains(preset.id)
                val presetBgColor = backgroundOptions.firstOrNull { it.id == preset.settings.background }?.color
                    ?: Color(0xFF101317)
                val presetAccentColor = accentOptions.firstOrNull { it.id == preset.settings.accent }?.color
                    ?: Color(0xFFC6AC79)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isArchived) colors.elevatedSurface.copy(alpha = 0.5f) else colors.primarySurface
                    ),
                    border = BorderStroke(1.dp, colors.borders)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail Preview Dial
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.mainBackground)
                                .border(BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.5f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularWatchPreview(
                                faceId = masterFaceId,
                                displayMode = DisplayMode.ACTIVE,
                                size = 44.dp,
                                showToggle = false,
                                bgColor = presetBgColor,
                                accentColor = presetAccentColor,
                                ornamentType = preset.settings.ornament,
                                dialType = preset.settings.dial
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isArchived) colors.secondaryText else colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(preset.modified))
                            Text(
                                text = dateStr,
                                fontSize = 11.sp,
                                color = colors.secondaryText
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Apply / Load
                            IconButton(
                                onClick = { onLoadPreset(preset) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Load", tint = colors.primaryAccent, modifier = Modifier.size(16.dp))
                            }
                            // Duplicate
                            IconButton(
                                onClick = { themeLibrary.duplicate(preset) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                            }
                            // Rename
                            IconButton(
                                onClick = { onRename(preset) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                            }
                            // Archive / Restore
                            IconButton(
                                onClick = { themeLibrary.hide(preset.id, !isArchived) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = "Archive",
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            // Export
                            IconButton(
                                onClick = { onExport(preset) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = colors.secondaryText, modifier = Modifier.size(16.dp))
                            }
                            // Delete
                            IconButton(
                                onClick = { onDelete(preset) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colors.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
