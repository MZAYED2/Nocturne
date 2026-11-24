package com.mohamedzayed.nocturne.ui.detail

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.model.DisplayMode
import com.mohamedzayed.nocturne.model.WatchFaceItem
import com.mohamedzayed.nocturne.ui.components.CircularWatchPreview
import com.mohamedzayed.nocturne.ui.components.MidnightOutlineButton
import com.mohamedzayed.nocturne.ui.components.MidnightPrimaryButton
import com.mohamedzayed.nocturne.ui.components.MidnightSecondaryButton
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchFaceDetailScreen(
    item: WatchFaceItem,
    isFavorite: Boolean,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit,
    onCustomizeInStudio: (WatchFaceItem) -> Unit,
    onApplyToWatch: (WatchFaceItem) -> Unit,
    isInstalled: Boolean,
    isActive: Boolean,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = NocturneTheme.colors
    var displayMode by remember { mutableStateOf(DisplayMode.ACTIVE) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.detail_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
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
                    IconButton(onClick = { onToggleFavorite(item.id) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.category_favorites),
                            tint = if (isFavorite) colors.primaryAccent else colors.secondaryText
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
            // Interactive Circular Preview Dial (Prominent & Balanced)
            CircularWatchPreview(
                faceId = item.id,
                displayMode = displayMode,
                size = 250.dp,
                showToggle = true,
                onToggleDisplayMode = { displayMode = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Single Word Title
            val name = if (isArabic && item.nameAr.isNotBlank()) item.nameAr else item.name
            Text(
                text = name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.success.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, colors.success), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.status_active_watch),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                    }
                } else if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, colors.primaryAccent), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.status_installed_watch),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryAccent
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.elevatedSurface)
                            .border(BorderStroke(1.dp, colors.borders), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.status_available),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.secondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MidnightPrimaryButton(
                    onClick = { onApplyToWatch(item) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_apply_to_watch),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                MidnightSecondaryButton(
                    onClick = { onCustomizeInStudio(item) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_customize),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Share Button
            MidnightOutlineButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Nocturne: ${item.name}")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Discover '${item.name}' from Nocturne Haute Horlogerie for Wear OS.\nhttps://github.com/MZAYED2/Nocturne"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Nocturne Face"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.btn_share),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Overview Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                border = BorderStroke(1.dp, colors.borders)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.section_overview),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val description = if (isArabic && item.descriptionAr.isNotBlank()) item.descriptionAr else item.description
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = colors.primaryText,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Specifications Section (WFF 1 Verified)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                border = BorderStroke(1.dp, colors.borders)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.section_specifications),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SpecRow(label = stringResource(R.string.spec_format_label), value = stringResource(R.string.format_wff_verified))
                    SpecRow(label = stringResource(R.string.package_label), value = item.packageName)
                    SpecRow(label = stringResource(R.string.version_label), value = "${item.versionName} (${item.versionCode})")
                    if (item.sizeBytes > 0) {
                        val mb = String.format("%.1f MB", item.sizeBytes / (1024.0 * 1024.0))
                        SpecRow(label = stringResource(R.string.size_label), value = mb)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    val colors = NocturneTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.secondaryText
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText
        )
    }
}
