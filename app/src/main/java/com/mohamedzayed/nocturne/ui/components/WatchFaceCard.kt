package com.mohamedzayed.nocturne.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.model.DisplayMode
import com.mohamedzayed.nocturne.model.WatchFaceItem
import com.mohamedzayed.nocturne.preview.WatchFacePreviewCanvas
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme

@Composable
fun WatchFaceCard(
    item: WatchFaceItem,
    isFavorite: Boolean,
    onToggleFavorite: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInstalled: Boolean = false,
    isActive: Boolean = false,
    isArabic: Boolean = false
) {
    val colors = NocturneTheme.colors

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.primarySurface
        ),
        border = BorderStroke(1.dp, colors.borders)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top action bar (Badge & Favorite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.success.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, colors.success), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.status_active_watch),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                    }
                } else if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.primaryAccent.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, colors.primaryAccent), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.status_installed_watch),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryAccent
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                // Favorite icon (Theme-consistent styling)
                IconButton(
                    onClick = { onToggleFavorite(item.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.category_favorites),
                        tint = if (isFavorite) colors.primaryAccent else colors.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Circular Preview Dial
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(colors.mainBackground)
                    .border(BorderStroke(1.2.dp, colors.borders), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                WatchFacePreviewCanvas(
                    faceId = item.id,
                    displayMode = DisplayMode.ACTIVE,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Face Title (Single Word)
            val title = if (isArabic && item.nameAr.isNotBlank()) item.nameAr else item.name
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle / Categories
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = item.defaultCategories.firstOrNull() ?: item.versionName
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
