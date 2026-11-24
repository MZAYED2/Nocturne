package com.mohamedzayed.nocturne.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.R
import com.mohamedzayed.nocturne.data.CategoryManager
import com.mohamedzayed.nocturne.data.FavoritesManager
import com.mohamedzayed.nocturne.data.RecentlyUsedManager
import com.mohamedzayed.nocturne.model.DisplayMode
import com.mohamedzayed.nocturne.model.WatchFaceItem
import com.mohamedzayed.nocturne.preview.WatchFacePreviewCanvas
import com.mohamedzayed.nocturne.ui.components.CategoryChips
import com.mohamedzayed.nocturne.ui.components.MidnightInputDialog
import com.mohamedzayed.nocturne.ui.components.WatchFaceCard
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme

@Composable
fun LibraryScreen(
    catalog: List<WatchFaceItem>,
    favoritesManager: FavoritesManager,
    categoryManager: CategoryManager,
    recentlyUsedManager: RecentlyUsedManager,
    onSelectWatchFace: (WatchFaceItem) -> Unit,
    installedPackages: Set<String>,
    activePackage: String?,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = NocturneTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("all") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Recently used faces
    val recentFaces = remember(recentlyUsedManager.recentFaceIds, catalog) {
        recentlyUsedManager.getRecentFaces(catalog)
    }

    // Filter catalog
    val filteredCatalog = remember(catalog, searchQuery, selectedCategoryId, favoritesManager.favorites, categoryManager.categoryAssignments) {
        catalog.filter { item ->
            // Search query filter
            val matchesQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.nameAr.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true) ||
                    item.descriptionAr.contains(searchQuery, ignoreCase = true)

            if (!matchesQuery) return@filter false

            // Category filter
            when (selectedCategoryId) {
                "all" -> true
                "favorites" -> favoritesManager.isFavorite(item.id)
                "presets", "my_presets" -> true
                else -> categoryManager.isFaceInCategory(item.id, selectedCategoryId, item.defaultCategories)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.mainBackground)
    ) {
        // App Header with New Brand Logo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.nocturne_logo),
                        contentDescription = "Nocturne Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = stringResource(R.string.tagline),
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        color = colors.secondaryText,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_hint),
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = colors.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.primaryText,
                    unfocusedTextColor = colors.primaryText,
                    focusedBorderColor = colors.primaryAccent,
                    unfocusedBorderColor = colors.borders,
                    focusedContainerColor = colors.elevatedSurface,
                    unfocusedContainerColor = colors.elevatedSurface
                )
            )
        }

        // Horizontal Category Filter Chips
        CategoryChips(
            categories = categoryManager.allCategories,
            selectedCategoryId = selectedCategoryId,
            onSelectCategory = { selectedCategoryId = it },
            onAddCategory = { showAddCategoryDialog = true },
            modifier = Modifier.fillMaxWidth()
        )

        // Recently Used Carousel (when relevant)
        if (recentFaces.isNotEmpty() && searchQuery.isBlank() && selectedCategoryId == "all") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.section_recently_used),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentFaces.forEach { face ->
                        val faceName = if (isArabic && face.nameAr.isNotBlank()) face.nameAr else face.name
                        val isActive = activePackage == face.packageName
                        Card(
                            modifier = Modifier
                                .width(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    recentlyUsedManager.recordUsed(face.id)
                                    onSelectWatchFace(face)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.primarySurface),
                            border = BorderStroke(1.dp, if (isActive) colors.success else colors.borders)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = faceName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2-Column Watch Faces Grid (Balanced Spacing & Clean Sizing)
        if (filteredCatalog.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = colors.secondaryText.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.no_faces_found),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.primaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.no_faces_found_desc),
                        fontSize = 12.sp,
                        color = colors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredCatalog, key = { it.id }) { item ->
                    val isFav = favoritesManager.isFavorite(item.id)
                    val isInstalled = installedPackages.contains(item.packageName)
                    val isActive = activePackage == item.packageName

                    WatchFaceCard(
                        item = item,
                        isFavorite = isFav,
                        onToggleFavorite = { favoritesManager.toggleFavorite(item.id) },
                        onClick = {
                            recentlyUsedManager.recordUsed(item.id)
                            onSelectWatchFace(item)
                        },
                        isInstalled = isInstalled,
                        isActive = isActive,
                        isArabic = isArabic
                    )
                }
            }
        }
    }

    // Add category dialog
    if (showAddCategoryDialog) {
        MidnightInputDialog(
            title = stringResource(R.string.category_add_new),
            hint = stringResource(R.string.category_name_hint),
            onConfirm = { name ->
                categoryManager.addCustomCategory(name)
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }
}
