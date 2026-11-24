package com.mohamedzayed.nocturne.model

import androidx.compose.ui.graphics.Color
import com.mohamedzayed.nocturne.R

enum class DisplayMode {
    ACTIVE,
    AOD
}

data class WatchFaceItem(
    val id: String,
    val name: String,
    val nameAr: String,
    val description: String,
    val descriptionAr: String,
    val packageName: String,
    val validationToken: String,
    val apkFilename: String,
    val downloadUrl: String,
    val preview: String,
    val versionCode: Int,
    val versionName: String,
    val sha256: String = "",
    val sizeBytes: Long = 0,
    val defaultCategories: List<String> = listOf("Classic")
)

sealed class PushState {
    object Idle : PushState()
    data class Downloading(val progress: Float, val detail: String) : PushState()
    data class Validating(val detail: String) : PushState()
    data class Connecting(val detail: String) : PushState()
    data class Transferring(val progress: Float, val detail: String) : PushState()
    data class Installing(val detail: String) : PushState()
    data class Success(val message: String, val slotId: String?) : PushState()
    data class Error(val stage: String, val message: String, val hint: String) : PushState()

    val isInProgress: Boolean
        get() = this is Downloading || this is Validating || this is Connecting ||
                this is Transferring || this is Installing
}

data class ColorOption(val id: String, val nameRes: Int, val color: Color)
data class AccentOption(val id: String, val nameRes: Int, val color: Color)
data class LabeledOption(val id: String, val labelRes: Int)

val backgroundOptions = listOf(
    ColorOption("black", R.string.bg_black, Color(0xFF000000)),
    ColorOption("navy", R.string.bg_navy, Color(0xFF070F19)),
    ColorOption("teal", R.string.bg_teal, Color(0xFF061713)),
    ColorOption("warm", R.string.bg_warm, Color(0xFF17110B)),
    ColorOption("graphite", R.string.bg_graphite, Color(0xFF15171B))
)

val accentOptions = listOf(
    AccentOption("sage", R.string.accent_sage, Color(0xFF8FA89B)),
    AccentOption("blue", R.string.accent_blue, Color(0xFF5B84A6)),
    AccentOption("gold", R.string.accent_gold, Color(0xFFD4AF37)),
    AccentOption("emerald", R.string.accent_emerald, Color(0xFF388E3C)),
    AccentOption("silver", R.string.accent_silver, Color(0xFFCFD8DC))
)

val ornamentOptions = listOf(
    LabeledOption("balanced", R.string.ornament_balanced),
    LabeledOption("minimal", R.string.ornament_minimal),
    LabeledOption("ornate", R.string.ornament_ornate),
    LabeledOption("none", R.string.ornament_none)
)

val dialOptions = listOf(
    LabeledOption("full", R.string.dial_full),
    LabeledOption("cardinal", R.string.dial_cardinal),
    LabeledOption("markers", R.string.dial_markers)
)
