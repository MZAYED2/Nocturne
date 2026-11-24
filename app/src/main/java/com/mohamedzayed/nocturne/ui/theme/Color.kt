package com.mohamedzayed.nocturne.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Semantic Status Colors
val NocturneError = Color(0xFFCF6679)
val NocturneWarning = Color(0xFFE5A93C)
val NocturneSuccess = Color(0xFF6BBF84)

@Immutable
data class NocturneColors(
    val mainBackground: Color,
    val primarySurface: Color,
    val elevatedSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val borders: Color,
    val error: Color = NocturneError,
    val warning: Color = NocturneWarning,
    val success: Color = NocturneSuccess,
    val isObsidianSage: Boolean = false
)

// Theme A: Midnight Gold Palette
val MidnightGoldColors = NocturneColors(
    mainBackground = Color(0xFF101317),
    primarySurface = Color(0xFF1B2026),
    elevatedSurface = Color(0xFF252B32),
    primaryText = Color(0xFFEAE6DD),
    secondaryText = Color(0xFFA4A7AA),
    primaryAccent = Color(0xFFC6AC79),
    secondaryAccent = Color(0xFF8FA89B),
    borders = Color(0xFF343A40),
    isObsidianSage = false
)

// Theme B: Obsidian Sage Palette
val ObsidianSageColors = NocturneColors(
    mainBackground = Color(0xFF111A1C),
    primarySurface = Color(0xFF1C292B),
    elevatedSurface = Color(0xFF273638),
    primaryText = Color(0xFFEDF1ED),
    secondaryText = Color(0xFFA6B0AD),
    primaryAccent = Color(0xFFA6BFB4),
    secondaryAccent = Color(0xFFC1B394),
    borders = Color(0xFF374548),
    isObsidianSage = true
)

// Backward compatibility constants
val MidnightCharcoal = MidnightGoldColors.mainBackground
val GraphiteSurface = MidnightGoldColors.primarySurface
val SoftSlateElevated = MidnightGoldColors.elevatedSurface
val WarmIvory = MidnightGoldColors.primaryText
val MutedSilver = MidnightGoldColors.secondaryText
val ChampagneGold = MidnightGoldColors.primaryAccent
val ChampagneGoldVariant = Color(0xFFB89B60)
val MutedSage = MidnightGoldColors.secondaryAccent
val SubtleGraphiteBorder = MidnightGoldColors.borders

val LocalNocturneColors = staticCompositionLocalOf { MidnightGoldColors }
