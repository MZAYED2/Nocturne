package com.mohamedzayed.nocturne.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.data.CategoryItem
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme

@Composable
fun CategoryChips(
    categories: List<CategoryItem>,
    selectedCategoryId: String,
    onSelectCategory: (String) -> Unit,
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val colors = NocturneTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val name = category.getDisplayName(context)
            val isSelected = selectedCategoryId == category.id
            CategoryChipItem(
                name = name,
                isSelected = isSelected,
                onClick = { onSelectCategory(category.id) }
            )
        }

        // Add Category Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.elevatedSurface)
                .border(BorderStroke(1.dp, colors.borders), RoundedCornerShape(20.dp))
                .clickable { onAddCategory() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.category_add_new),
                    tint = colors.primaryAccent,
                    modifier = Modifier.padding(end = 2.dp)
                )
                Text(
                    text = stringResource(R.string.category_add_new),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryAccent
                )
            }
        }
    }
}

@Composable
private fun CategoryChipItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = NocturneTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) colors.primaryAccent else colors.elevatedSurface)
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) colors.primaryAccent else colors.borders
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.mainBackground else colors.primaryText
        )
    }
}
