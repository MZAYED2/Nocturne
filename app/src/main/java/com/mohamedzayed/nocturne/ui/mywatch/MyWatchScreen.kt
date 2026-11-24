package com.mohamedzayed.nocturne.ui.mywatch

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.data.FavoritesManager
import com.mohamedzayed.nocturne.model.DisplayMode
import com.mohamedzayed.nocturne.model.PushState
import com.mohamedzayed.nocturne.model.WatchFaceItem
import com.mohamedzayed.nocturne.preview.WatchFacePreviewCanvas
import com.mohamedzayed.nocturne.ui.components.MidnightConfirmDialog
import com.mohamedzayed.nocturne.ui.components.MidnightOutlineButton
import com.mohamedzayed.nocturne.ui.components.MidnightPrimaryButton
import com.mohamedzayed.nocturne.ui.components.MidnightSecondaryButton
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme
import com.mohamedzayed.nocturne.wear.ConnectedBridge
import com.mohamedzayed.nocturne.wear.WatchSlot
import com.mohamedzayed.nocturne.wear.WearBridgeManager

@Composable
fun MyWatchScreen(
    bridge: ConnectedBridge?,
    isScanning: Boolean,
    slots: List<WatchSlot>,
    remainingSlotCount: Int?,
    pushState: PushState,
    catalog: List<WatchFaceItem>,
    favoritesManager: FavoritesManager,
    onRefreshBridge: () -> Unit,
    onRemoveSlot: (String) -> Unit,
    onActivateSlot: (String) -> Unit,
    onApplyFace: (WatchFaceItem) -> Unit,
    onExploreLibrary: () -> Unit,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = NocturneTheme.colors
    val scrollState = rememberScrollState()
    var slotToRemove by remember { mutableStateOf<String?>(null) }

    val favoriteFaces = remember(catalog, favoritesManager.favorites) {
        catalog.filter { favoritesManager.isFavorite(it.id) }
    }
    val installedPackages = remember(slots) { slots.map { it.packageName }.toSet() }
    val activePackage = remember(slots) { slots.firstOrNull { it.active }?.packageName }

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
            text = stringResource(R.string.my_watch_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primaryAccent
        )

        // 1. Watch Connection Card
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (bridge != null) colors.primaryAccent.copy(alpha = 0.15f) else colors.elevatedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (bridge != null) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (bridge != null) colors.primaryAccent else colors.secondaryText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            val title = when {
                                isScanning -> stringResource(R.string.watch_status_scanning)
                                bridge != null -> stringResource(R.string.watch_connected)
                                else -> stringResource(R.string.watch_disconnected)
                            }
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            val deviceName = bridge?.node?.displayName ?: if (isScanning) "Searching..." else "None"
                            Text(
                                text = stringResource(R.string.watch_device_name, deviceName),
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                        }
                    }

                    IconButton(onClick = onRefreshBridge) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.primaryAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.btn_retry_connection),
                                tint = colors.primaryAccent
                            )
                        }
                    }
                }

                if (bridge != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.elevatedSurface)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.bridge_protocol_active)} (Protocol ${bridge.protocol}, Code ${bridge.versionCode})",
                            fontSize = 11.sp,
                            color = colors.success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Watch Face Push Progress Card (when pushing)
        if (pushState.isInProgress || pushState is PushState.Success || pushState is PushState.Error) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (pushState) {
                        is PushState.Success -> colors.success.copy(alpha = 0.12f)
                        is PushState.Error -> colors.error.copy(alpha = 0.12f)
                        else -> colors.primarySurface
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when (pushState) {
                        is PushState.Success -> colors.success
                        is PushState.Error -> colors.error
                        else -> colors.primaryAccent.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val statusText = when (pushState) {
                        is PushState.Downloading -> stringResource(R.string.push_progress_downloading, (pushState.progress * 100).toInt())
                        is PushState.Validating -> stringResource(R.string.push_progress_validating)
                        is PushState.Connecting -> stringResource(R.string.push_progress_connecting)
                        is PushState.Transferring -> stringResource(R.string.push_progress_transferring, (pushState.progress * 100).toInt())
                        is PushState.Installing -> stringResource(R.string.push_progress_installing)
                        is PushState.Success -> stringResource(R.string.push_success)
                        is PushState.Error -> stringResource(R.string.push_error, pushState.message)
                        else -> ""
                    }

                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (pushState) {
                            is PushState.Success -> colors.success
                            is PushState.Error -> colors.error
                            else -> colors.primaryText
                        }
                    )

                    if (pushState is PushState.Downloading || pushState is PushState.Transferring) {
                        val progressVal = if (pushState is PushState.Downloading) pushState.progress else (pushState as PushState.Transferring).progress
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progressVal },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.primaryAccent,
                            trackColor = colors.elevatedSurface
                        )
                    } else if (pushState.isInProgress) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.primaryAccent,
                            trackColor = colors.elevatedSurface
                        )
                    }
                }
            }
        }

        // 3. Quick Switch & Favorites on Watch
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.section_favorites_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (favoriteFaces.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.empty_favorites_msg),
                                fontSize = 12.sp,
                                color = colors.secondaryText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MidnightOutlineButton(
                                onClick = onExploreLibrary,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(stringResource(R.string.btn_browse_library), fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        favoriteFaces.forEach { face ->
                            val isInstalled = installedPackages.contains(face.packageName)
                            val isActive = activePackage == face.packageName
                            val faceName = if (isArabic && face.nameAr.isNotBlank()) face.nameAr else face.name

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.elevatedSurface)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(colors.mainBackground)
                                            .border(BorderStroke(1.dp, colors.primaryAccent.copy(alpha = 0.5f)), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        WatchFacePreviewCanvas(
                                            faceId = face.id,
                                            displayMode = DisplayMode.ACTIVE,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = faceName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = when {
                                                isActive -> stringResource(R.string.status_active_watch)
                                                isInstalled -> stringResource(R.string.status_installed_watch)
                                                else -> stringResource(R.string.status_available)
                                            },
                                            fontSize = 11.sp,
                                            color = when {
                                                isActive -> colors.success
                                                isInstalled -> colors.primaryAccent
                                                else -> colors.secondaryText
                                            }
                                        )
                                    }
                                }

                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.success.copy(alpha = 0.2f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.status_active_watch),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.success
                                        )
                                    }
                                } else if (isInstalled) {
                                    MidnightPrimaryButton(
                                        onClick = { onActivateSlot(face.packageName) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.btn_switch), fontSize = 11.sp)
                                    }
                                } else {
                                    MidnightSecondaryButton(
                                        onClick = { onApplyFace(face) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_apply_to_watch), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Installed Watch Face Slots (Wear OS Push Slots)
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
                    Text(
                        text = stringResource(R.string.slots_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent
                    )
                    val slotInfoStr = if (bridge != null) {
                        val rem = remainingSlotCount?.toString() ?: "—"
                        "${slots.size} installed ($rem rem.)"
                    } else {
                        stringResource(R.string.slots_status_unknown)
                    }
                    Text(
                        text = slotInfoStr,
                        fontSize = 11.sp,
                        color = if (bridge != null) colors.secondaryText else colors.error.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.slots_explanation),
                    fontSize = 11.sp,
                    color = colors.secondaryText,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (slots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (bridge != null) {
                                "No Nocturne watch faces currently installed in slots."
                            } else {
                                stringResource(R.string.slots_disconnected_desc)
                            },
                            fontSize = 12.sp,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    slots.forEach { slot ->
                        val matchingFace = catalog.firstOrNull { it.packageName == slot.packageName }
                        val displayName = matchingFace?.let {
                            if (isArabic && it.nameAr.isNotBlank()) it.nameAr else it.name
                        } ?: slot.packageName.substringAfterLast('.')

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.elevatedSurface)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Watch,
                                    contentDescription = null,
                                    tint = if (slot.active) colors.success else colors.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (slot.active) {
                                        Text(
                                            text = stringResource(R.string.status_active_watch),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.success
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!slot.active) {
                                    MidnightOutlineButton(
                                        onClick = { onActivateSlot(slot.packageName) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_switch), fontSize = 11.sp)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (slot.active) {
                                            Toast.makeText(
                                                context,
                                                "Switch to another watch face on your watch before removing the active face.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            slotToRemove = slot.packageName
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.btn_remove_slot),
                                        tint = if (slot.active) colors.secondaryText.copy(alpha = 0.3f) else colors.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Galaxy Wearable Shortcut Button
        MidnightPrimaryButton(
            onClick = { WearBridgeManager.openGalaxyWearable(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.btn_open_galaxy_wearable),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // 6. Troubleshooting Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
            border = BorderStroke(1.dp, colors.borders)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.troubleshooting_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.troubleshooting_step1),
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.troubleshooting_step2),
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.troubleshooting_step3),
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    lineHeight = 17.sp
                )
            }
        }
    }

    // Confirm Removal Dialog
    slotToRemove?.let { targetPkg ->
        MidnightConfirmDialog(
            title = stringResource(R.string.btn_remove_slot),
            message = "Are you sure you want to uninstall '$targetPkg' from your watch?",
            isDestructive = true,
            confirmText = stringResource(R.string.btn_delete),
            onConfirm = {
                onRemoveSlot(targetPkg)
                slotToRemove = null
            },
            onDismiss = { slotToRemove = null }
        )
    }
}
