package com.mohamedzayed.nocturne.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.model.DisplayMode
import com.mohamedzayed.nocturne.preview.WatchFacePreviewCanvas
import com.mohamedzayed.nocturne.ui.theme.ChampagneGold
import com.mohamedzayed.nocturne.ui.theme.MidnightCharcoal
import com.mohamedzayed.nocturne.ui.theme.MutedSilver
import com.mohamedzayed.nocturne.ui.theme.SubtleGraphiteBorder
import com.mohamedzayed.nocturne.ui.theme.WarmIvory
import java.util.Calendar

@Composable
fun CircularWatchPreview(
    faceId: String,
    displayMode: DisplayMode,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    showToggle: Boolean = true,
    onToggleDisplayMode: ((DisplayMode) -> Unit)? = null,
    bgColor: Color = Color(0xFF101317),
    accentColor: Color = Color(0xFFC6AC79),
    ornamentType: String = "balanced",
    dialType: String = "full",
    moonPhaseIndex: Int = 14,
    currentTime: Calendar = Calendar.getInstance()
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer bezel ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(MidnightCharcoal)
                .border(BorderStroke(2.dp, SubtleGraphiteBorder), CircleShape)
        ) {
            WatchFacePreviewCanvas(
                faceId = faceId,
                displayMode = displayMode,
                modifier = Modifier.fillMaxSize(),
                bgColor = bgColor,
                accentColor = accentColor,
                ornamentType = ornamentType,
                dialType = dialType,
                moonPhaseIndex = moonPhaseIndex,
                currentTime = currentTime
            )
        }

        // Active / AOD mode pill selector
        if (showToggle && onToggleDisplayMode != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightCharcoal.copy(alpha = 0.85f))
                    .border(BorderStroke(1.dp, SubtleGraphiteBorder), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isActive = displayMode == DisplayMode.ACTIVE
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) ChampagneGold else Color.Transparent)
                            .clickable { onToggleDisplayMode(DisplayMode.ACTIVE) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.preview_active),
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) MidnightCharcoal else MutedSilver
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isActive) ChampagneGold else Color.Transparent)
                            .clickable { onToggleDisplayMode(DisplayMode.AOD) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.preview_aod),
                            fontSize = 11.sp,
                            fontWeight = if (!isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isActive) MidnightCharcoal else MutedSilver
                        )
                    }
                }
            }
        }
    }
}
