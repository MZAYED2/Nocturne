package com.mohamedzayed.nocturne

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mohamedzayed.nocturne.data.AppThemeManager
import com.mohamedzayed.nocturne.data.CatalogRepository
import com.mohamedzayed.nocturne.data.CategoryManager
import com.mohamedzayed.nocturne.data.FavoritesManager
import com.mohamedzayed.nocturne.data.RecentlyUsedManager
import com.mohamedzayed.nocturne.data.RotationPreferences
import com.mohamedzayed.nocturne.data.ThemeLibrary
import com.mohamedzayed.nocturne.localization.LocaleHelper
import com.mohamedzayed.nocturne.localization.ProvideLocalizedApp
import com.mohamedzayed.nocturne.model.PushState
import com.mohamedzayed.nocturne.model.WatchFaceItem
import com.mohamedzayed.nocturne.rotation.WatchFaceRotationWorker
import com.mohamedzayed.nocturne.ui.detail.WatchFaceDetailScreen
import com.mohamedzayed.nocturne.ui.library.LibraryScreen
import com.mohamedzayed.nocturne.ui.mywatch.MyWatchScreen
import com.mohamedzayed.nocturne.ui.settings.SettingsScreen
import com.mohamedzayed.nocturne.ui.studio.StudioScreen
import com.mohamedzayed.nocturne.ui.theme.AppTheme
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme
import com.mohamedzayed.nocturne.update.ReleaseInfo
import com.mohamedzayed.nocturne.update.UpdateCenterDialog
import com.mohamedzayed.nocturne.update.UpdateCheckWorker
import com.mohamedzayed.nocturne.update.UpdateManager
import com.mohamedzayed.nocturne.wear.ConnectedBridge
import com.mohamedzayed.nocturne.wear.WatchSlot
import com.mohamedzayed.nocturne.wear.WearBridgeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UpdateCheckWorker.schedule(this)

        setContent {
            NocturneApp()
        }
    }
}

@Composable
fun NocturneApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositories and Managers
    var catalog by remember { mutableStateOf(CatalogRepository.loadLocalCatalog(context)) }
    val favoritesManager = remember { FavoritesManager(context) }
    val categoryManager = remember { CategoryManager(context) }
    val themeLibrary = remember { ThemeLibrary(context) }
    val recentlyUsedManager = remember { RecentlyUsedManager(context) }
    val rotationPreferences = remember { RotationPreferences(context) }
    val appThemeManager = remember { AppThemeManager(context) }

    // Language state
    var languageCode by rememberSaveable { mutableStateOf(LocaleHelper.getSelectedLanguage(context)) }
    val isArabic = remember(languageCode) {
        LocaleHelper.isRtl(LocaleHelper.getEffectiveLocale(context, languageCode))
    }

    // Navigation state: exactly 3 tabs (0: Library, 1: My Watch, 2: Settings)
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedDetailFace by remember { mutableStateOf<WatchFaceItem?>(null) }
    var studioTargetFace by remember { mutableStateOf<WatchFaceItem?>(null) }

    // Wear & Push state
    var isScanningBridge by remember { mutableStateOf(false) }
    var connectedBridge by remember { mutableStateOf<ConnectedBridge?>(null) }
    var watchSlots by remember { mutableStateOf<List<WatchSlot>>(emptyList()) }
    var pushState by remember { mutableStateOf<PushState>(PushState.Idle) }

    // Update center dialog
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<ReleaseInfo?>(null) }

    // Initial background loads
    LaunchedEffect(Unit) {
        val remote = CatalogRepository.fetchRemoteCatalog()
        if (remote.isNotEmpty()) {
            catalog = remote
        }
        latestRelease = UpdateManager.checkLatestRelease().getOrNull()

        // Schedule rotation if enabled
        if (rotationPreferences.enabled) {
            WatchFaceRotationWorker.schedule(context, rotationPreferences.intervalHours)
        }
    }

    // Bridge background discovery function
    fun refreshBridgeState() {
        scope.launch {
            isScanningBridge = true
            connectedBridge = WearBridgeManager.findWatchBridge(context)
            if (connectedBridge != null) {
                watchSlots = WearBridgeManager.querySlots(context, connectedBridge?.node?.id)
            } else {
                watchSlots = emptyList()
            }
            isScanningBridge = false
        }
    }

    LaunchedEffect(Unit) {
        refreshBridgeState()
        while (true) {
            delay(15000)
            if (!pushState.isInProgress) {
                connectedBridge = WearBridgeManager.findWatchBridge(context)
                if (connectedBridge != null) {
                    watchSlots = WearBridgeManager.querySlots(context, connectedBridge?.node?.id)
                }
            }
        }
    }

    // Push Execution Function
    fun applyWatchFaceToWatch(face: WatchFaceItem) {
        scope.launch {
            val bridge = connectedBridge ?: WearBridgeManager.findWatchBridge(context)
            if (bridge == null) {
                Toast.makeText(context, R.string.watch_disconnected, Toast.LENGTH_LONG).show()
                selectedTab = 1 // Switch to My Watch tab
                return@launch
            }

            try {
                val apkFile = File(context.cacheDir, face.apkFilename)
                // If file doesn't exist or is invalid, download it
                if (!WearBridgeManager.isApkFileValid(apkFile)) {
                    pushState = PushState.Downloading(0f, "Downloading ${face.name}...")
                    val dlRes = WearBridgeManager.downloadApkWithValidation(
                        urlString = face.downloadUrl,
                        destinationFile = apkFile,
                        onProgress = { fraction, current, total ->
                            pushState = PushState.Downloading(fraction, "${(fraction * 100).toInt()}%")
                        }
                    )
                    if (dlRes.isFailure) {
                        pushState = PushState.Error("DOWNLOAD", dlRes.exceptionOrNull()?.localizedMessage ?: "Download failed", "")
                        return@launch
                    }
                }

                // Push to watch
                pushState = PushState.Transferring(0f, "Connecting to watch...")
                val result = WearBridgeManager.pushWatchFace(
                    context = context,
                    bridge = bridge,
                    apkFile = apkFile,
                    packageName = face.packageName,
                    token = face.validationToken,
                    kind = "wff",
                    onProgress = { transferred ->
                        pushState = if (transferred) {
                            PushState.Installing("Installing on watch...")
                        } else {
                            PushState.Transferring(0.5f, "Transferring...")
                        }
                    }
                )

                if (result.optBoolean("success")) {
                    pushState = PushState.Success(
                        message = "Successfully installed ${face.name}",
                        slotId = result.optString("slotId", "")
                    )
                    Toast.makeText(context, R.string.push_success, Toast.LENGTH_LONG).show()
                    delay(1500)
                    watchSlots = WearBridgeManager.querySlots(context, bridge.node.id)
                } else {
                    val err = result.optString("error", "Watch rejected package")
                    pushState = PushState.Error("INSTALL", err, "")
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                pushState = PushState.Error("PUSH", e.localizedMessage ?: "Push failed", "")
                Toast.makeText(context, e.localizedMessage ?: "Push failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Activate slot function
    fun activateSlotOnWatch(packageName: String) {
        val bridge = connectedBridge ?: return
        scope.launch {
            val res = WearBridgeManager.activateSlot(context, bridge, packageName)
            if (res.isSuccess) {
                Toast.makeText(context, "Activated watch face", Toast.LENGTH_SHORT).show()
                delay(1000)
                watchSlots = WearBridgeManager.querySlots(context, bridge.node.id)
            } else {
                Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Activation failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Remove slot function
    fun removeSlotFromWatch(packageName: String) {
        val bridge = connectedBridge ?: return
        scope.launch {
            val res = WearBridgeManager.removeSlot(context, bridge, packageName)
            if (res.isSuccess) {
                Toast.makeText(context, "Slot removed successfully", Toast.LENGTH_SHORT).show()
                watchSlots = WearBridgeManager.querySlots(context, bridge.node.id)
            } else {
                Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Failed to remove slot", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Back button handling
    BackHandler(enabled = studioTargetFace != null || selectedDetailFace != null || selectedTab != 0) {
        if (studioTargetFace != null) {
            selectedDetailFace = studioTargetFace
            studioTargetFace = null
        } else if (selectedDetailFace != null) {
            selectedDetailFace = null
        } else if (selectedTab != 0) {
            selectedTab = 0
        }
    }

    // Installed and Active packages sets for badge rendering
    val installedPackages = remember(watchSlots) { watchSlots.map { it.packageName }.toSet() }
    val activePackage = remember(watchSlots) { watchSlots.firstOrNull { it.active }?.packageName }

    ProvideLocalizedApp(languagePreference = languageCode) {
        NocturneTheme(appTheme = appThemeManager.currentTheme) {
            val colors = NocturneTheme.colors
            Scaffold(
                containerColor = colors.mainBackground,
                bottomBar = {
                    if (selectedDetailFace == null && studioTargetFace == null) {
                        NavigationBar(
                            containerColor = colors.primarySurface,
                            contentColor = colors.primaryAccent
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.GridView, contentDescription = stringResource(R.string.nav_library)) },
                                label = { Text(stringResource(R.string.nav_library), fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.mainBackground,
                                    selectedTextColor = colors.primaryAccent,
                                    indicatorColor = colors.primaryAccent,
                                    unselectedIconColor = colors.secondaryText,
                                    unselectedTextColor = colors.secondaryText
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Watch, contentDescription = stringResource(R.string.nav_my_watch)) },
                                label = { Text(stringResource(R.string.nav_my_watch), fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.mainBackground,
                                    selectedTextColor = colors.primaryAccent,
                                    indicatorColor = colors.primaryAccent,
                                    unselectedIconColor = colors.secondaryText,
                                    unselectedTextColor = colors.secondaryText
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                                label = { Text(stringResource(R.string.nav_settings), fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.mainBackground,
                                    selectedTextColor = colors.primaryAccent,
                                    indicatorColor = colors.primaryAccent,
                                    unselectedIconColor = colors.secondaryText,
                                    unselectedTextColor = colors.secondaryText
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (studioTargetFace != null) {
                        StudioScreen(
                            targetFace = studioTargetFace!!,
                            themeLibrary = themeLibrary,
                            onBack = {
                                selectedDetailFace = studioTargetFace
                                studioTargetFace = null
                            },
                            onApplyToWatch = { deployFace -> applyWatchFaceToWatch(deployFace) },
                            isArabic = isArabic
                        )
                    } else if (selectedDetailFace != null) {
                        val face = selectedDetailFace!!
                        WatchFaceDetailScreen(
                            item = face,
                            isFavorite = favoritesManager.isFavorite(face.id),
                            onToggleFavorite = { favoritesManager.toggleFavorite(face.id) },
                            onBack = { selectedDetailFace = null },
                            onCustomizeInStudio = { targetFace ->
                                studioTargetFace = targetFace
                                selectedDetailFace = null
                            },
                            onApplyToWatch = { targetFace ->
                                applyWatchFaceToWatch(targetFace)
                            },
                            isInstalled = installedPackages.contains(face.packageName),
                            isActive = activePackage == face.packageName,
                            isArabic = isArabic
                        )
                    } else {
                        when (selectedTab) {
                            0 -> LibraryScreen(
                                catalog = catalog,
                                favoritesManager = favoritesManager,
                                categoryManager = categoryManager,
                                recentlyUsedManager = recentlyUsedManager,
                                onSelectWatchFace = { face -> selectedDetailFace = face },
                                installedPackages = installedPackages,
                                activePackage = activePackage,
                                isArabic = isArabic
                            )
                            1 -> MyWatchScreen(
                                bridge = connectedBridge,
                                isScanning = isScanningBridge,
                                slots = watchSlots,
                                remainingSlotCount = WearBridgeManager.getRemainingSlotCount(connectedBridge),
                                pushState = pushState,
                                catalog = catalog,
                                favoritesManager = favoritesManager,
                                onRefreshBridge = { refreshBridgeState() },
                                onRemoveSlot = { targetPkg -> removeSlotFromWatch(targetPkg) },
                                onActivateSlot = { targetPkg -> activateSlotOnWatch(targetPkg) },
                                onApplyFace = { targetFace -> applyWatchFaceToWatch(targetFace) },
                                onExploreLibrary = { selectedTab = 0 },
                                isArabic = isArabic
                            )
                            2 -> SettingsScreen(
                                currentLanguageCode = languageCode,
                                onLanguageChange = { newCode ->
                                    languageCode = newCode
                                    LocaleHelper.setSelectedLanguage(context, newCode)
                                },
                                currentTheme = appThemeManager.currentTheme,
                                onThemeChange = { newTheme ->
                                    appThemeManager.setTheme(newTheme)
                                },
                                onCheckForUpdates = { showUpdateDialog = true },
                                rotationPrefs = rotationPreferences,
                                catalog = catalog,
                                themeLibrary = themeLibrary,
                                favoritesManager = favoritesManager,
                                categoryManager = categoryManager,
                                isArabic = isArabic
                            )
                        }
                    }
                }
            }

            // Update Dialog
            if (showUpdateDialog) {
                UpdateCenterDialog(
                    initialRelease = latestRelease,
                    onDismissRequest = { showUpdateDialog = false }
                )
            }
        }
    }
}
