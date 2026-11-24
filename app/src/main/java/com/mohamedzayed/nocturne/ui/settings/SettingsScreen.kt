package com.mohamedzayed.nocturne.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.BuildConfig
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.data.BackupManager
import com.mohamedzayed.nocturne.data.CategoryManager
import com.mohamedzayed.nocturne.data.FavoritesManager
import com.mohamedzayed.nocturne.data.RotationPreferences
import com.mohamedzayed.nocturne.data.ThemeLibrary
import com.mohamedzayed.nocturne.localization.LocaleHelper
import com.mohamedzayed.nocturne.model.WatchFaceItem
import com.mohamedzayed.nocturne.rotation.WatchFaceRotationWorker
import com.mohamedzayed.nocturne.ui.components.MidnightInputDialog
import com.mohamedzayed.nocturne.ui.components.MidnightOutlineButton
import com.mohamedzayed.nocturne.ui.components.MidnightSecondaryButton
import com.mohamedzayed.nocturne.ui.theme.AppTheme
import com.mohamedzayed.nocturne.ui.theme.MidnightGoldColors
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme
import com.mohamedzayed.nocturne.ui.theme.ObsidianSageColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    currentLanguageCode: String,
    onLanguageChange: (String) -> Unit,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onCheckForUpdates: () -> Unit,
    rotationPrefs: RotationPreferences,
    catalog: List<WatchFaceItem>,
    themeLibrary: ThemeLibrary,
    favoritesManager: FavoritesManager,
    categoryManager: CategoryManager,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = NocturneTheme.colors
    val scrollState = rememberScrollState()
    var showImportBackupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.mainBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primaryAccent
        )

        // 1. Dual Visual Theme Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.settings_theme_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = stringResource(R.string.settings_theme_subtitle),
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Two Visual Theme Preview Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Theme A: Midnight Gold Preview Card
                    val isGoldSelected = currentTheme == AppTheme.MIDNIGHT_GOLD
                    ThemePreviewCard(
                        title = stringResource(R.string.theme_midnight_gold),
                        subtitle = "Charcoal • Gold",
                        bgColor = MidnightGoldColors.mainBackground,
                        surfaceColor = MidnightGoldColors.primarySurface,
                        accentColor = MidnightGoldColors.primaryAccent,
                        secondaryAccent = MidnightGoldColors.secondaryAccent,
                        textColor = MidnightGoldColors.primaryText,
                        isSelected = isGoldSelected,
                        onClick = { onThemeChange(AppTheme.MIDNIGHT_GOLD) },
                        modifier = Modifier.weight(1f)
                    )

                    // Theme B: Obsidian Sage Preview Card
                    val isSageSelected = currentTheme == AppTheme.OBSIDIAN_SAGE
                    ThemePreviewCard(
                        title = stringResource(R.string.theme_obsidian_sage),
                        subtitle = "Slate • Sage",
                        bgColor = ObsidianSageColors.mainBackground,
                        surfaceColor = ObsidianSageColors.primarySurface,
                        accentColor = ObsidianSageColors.primaryAccent,
                        secondaryAccent = ObsidianSageColors.secondaryAccent,
                        textColor = ObsidianSageColors.primaryText,
                        isSelected = isSageSelected,
                        onClick = { onThemeChange(AppTheme.OBSIDIAN_SAGE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Language Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.settings_language_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = stringResource(R.string.settings_language_subtitle),
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val languageOptions = listOf(
                    LocaleHelper.LANG_SYSTEM to stringResource(R.string.language_system),
                    LocaleHelper.LANG_AR to stringResource(R.string.language_arabic),
                    LocaleHelper.LANG_EN to stringResource(R.string.language_english)
                )

                languageOptions.forEach { (code, label) ->
                    val isSelected = currentLanguageCode == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.elevatedSurface else colors.primarySurface)
                            .clickable { onLanguageChange(code) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.primaryAccent else colors.primaryText
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onLanguageChange(code) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primaryAccent,
                                unselectedColor = colors.secondaryText
                            )
                        )
                    }
                }
            }
        }

        // 3. Automatic Watch-Face Rotation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.rotation_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            Text(
                                text = stringResource(R.string.rotation_subtitle),
                                fontSize = 11.sp,
                                color = colors.secondaryText
                            )
                        }
                    }

                    Switch(
                        checked = rotationPrefs.enabled,
                        onCheckedChange = { isChecked ->
                            rotationPrefs.setRotationEnabled(isChecked)
                            if (isChecked) {
                                WatchFaceRotationWorker.schedule(context, rotationPrefs.intervalHours)
                            } else {
                                WatchFaceRotationWorker.cancel(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.mainBackground,
                            checkedTrackColor = colors.primaryAccent,
                            uncheckedThumbColor = colors.secondaryText,
                            uncheckedTrackColor = colors.elevatedSurface
                        )
                    )
                }

                if (rotationPrefs.enabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Interval Selector
                    Text(
                        text = stringResource(R.string.rotation_interval_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1 to "1h", 3 to "3h", 6 to "6h", 12 to "12h", 24 to "24h").forEach { (hrs, label) ->
                            val isSelected = rotationPrefs.intervalHours == hrs
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.primaryAccent else colors.elevatedSurface)
                                    .clickable {
                                        rotationPrefs.setInterval(hrs)
                                        WatchFaceRotationWorker.schedule(context, hrs)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.mainBackground else colors.primaryText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rotation Order (Sequential vs Random)
                    Text(
                        text = stringResource(R.string.rotation_order_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            "sequential" to stringResource(R.string.rotation_order_sequential),
                            "random" to stringResource(R.string.rotation_order_random)
                        ).forEach { (ord, label) ->
                            val isSelected = rotationPrefs.order == ord
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.elevatedSurface)
                                    .clickable { rotationPrefs.setRotationOrder(ord) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { rotationPrefs.setRotationOrder(ord) },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primaryAccent)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (isSelected) colors.primaryAccent else colors.primaryText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Participating Watch Faces
                    Text(
                        text = stringResource(R.string.rotation_participating_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        catalog.forEach { face ->
                            val isParticipating = rotationPrefs.participatingFaces.contains(face.id)
                            val name = if (isArabic && face.nameAr.isNotBlank()) face.nameAr else face.name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isParticipating) colors.primaryAccent.copy(alpha = 0.2f) else colors.elevatedSurface)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isParticipating) colors.primaryAccent else colors.borders
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { rotationPrefs.toggleFaceParticipation(face.id) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isParticipating) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isParticipating) colors.primaryAccent else colors.secondaryText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Connectivity Note
                    Text(
                        text = stringResource(R.string.rotation_note_connection),
                        fontSize = 11.sp,
                        color = colors.secondaryText,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Last Rotation Status
                    val lastStatus = if (rotationPrefs.lastRotationTimestamp > 0) {
                        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(rotationPrefs.lastRotationTimestamp))
                        val faceName = catalog.firstOrNull { it.id == rotationPrefs.lastRotatedFaceId }?.name
                            ?: (rotationPrefs.lastRotatedFaceId ?: "")
                        stringResource(R.string.rotation_last_switch, dateStr, faceName)
                    } else {
                        stringResource(R.string.rotation_last_none)
                    }
                    Text(
                        text = lastStatus,
                        fontSize = 11.sp,
                        color = colors.primaryAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.backup_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = stringResource(R.string.backup_subtitle),
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MidnightSecondaryButton(
                        onClick = {
                            val backupJson = BackupManager.exportBackup(themeLibrary, favoritesManager, categoryManager)
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Nocturne Backup", backupJson))
                            Toast.makeText(context, R.string.backup_exported_toast, Toast.LENGTH_SHORT).show()

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "Nocturne Preferences Backup")
                                putExtra(Intent.EXTRA_TEXT, backupJson)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Nocturne Backup"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_export_backup), fontSize = 11.sp)
                    }

                    MidnightOutlineButton(
                        onClick = { showImportBackupDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_import_backup), fontSize = 11.sp)
                    }
                }
            }
        }

        // 5. Updates & GitHub Integration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.settings_updates_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "GitHub Releases & WatchBridge Updates",
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                MidnightOutlineButton(
                    onClick = onCheckForUpdates,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_updates_check),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 6. About Nocturne Card with Integrated Brand Logo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.nocturne_logo),
                        contentDescription = "Nocturne Master Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.settings_about_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_about_version,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE
                            ),
                            fontSize = 11.sp,
                            color = colors.primaryAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.settings_about_desc),
                    fontSize = 11.sp,
                    color = colors.secondaryText,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.settings_developer),
                    fontSize = 11.sp,
                    color = colors.primaryText.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                MidnightOutlineButton(
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/MZAYED2/Nocturne")
                        )
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_github_repo),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Import Backup Dialog
    if (showImportBackupDialog) {
        MidnightInputDialog(
            title = stringResource(R.string.btn_import_backup),
            hint = stringResource(R.string.backup_import_hint),
            onConfirm = { jsonStr ->
                val res = BackupManager.importBackup(jsonStr, themeLibrary, favoritesManager, categoryManager)
                if (res.isSuccess) {
                    Toast.makeText(context, R.string.backup_imported_toast, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Import failed", Toast.LENGTH_LONG).show()
                }
                showImportBackupDialog = false
            },
            onDismiss = { showImportBackupDialog = false }
        )
    }
}

@Composable
private fun ThemePreviewCard(
    title: String,
    subtitle: String,
    bgColor: Color,
    surfaceColor: Color,
    accentColor: Color,
    secondaryAccent: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColors = NocturneTheme.colors
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = activeColors.elevatedSurface),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) activeColors.primaryAccent else activeColors.borders
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Representative visual palette swatch mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(BorderStroke(1.dp, activeColors.borders), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(BorderStroke(1.dp, accentColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(textColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(secondaryAccent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) activeColors.primaryAccent else activeColors.primaryText,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = activeColors.secondaryText,
                        maxLines = 1
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = activeColors.primaryAccent,
                        unselectedColor = activeColors.secondaryText
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
