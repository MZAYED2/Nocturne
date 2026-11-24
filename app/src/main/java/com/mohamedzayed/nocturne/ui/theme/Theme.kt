package com.mohamedzayed.nocturne.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme(val id: String) {
    MIDNIGHT_GOLD("midnight_gold"),
    OBSIDIAN_SAGE("obsidian_sage");

    companion object {
        fun fromId(id: String): AppTheme = when (id) {
            OBSIDIAN_SAGE.id -> OBSIDIAN_SAGE
            else -> MIDNIGHT_GOLD
        }
    }
}

object NocturneTheme {
    val colors: NocturneColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNocturneColors.current
}

@Composable
fun NocturneTheme(
    appTheme: AppTheme = AppTheme.MIDNIGHT_GOLD,
    content: @Composable () -> Unit
) {
    val colors = if (appTheme == AppTheme.OBSIDIAN_SAGE) ObsidianSageColors else MidnightGoldColors

    val colorScheme = darkColorScheme(
        primary = colors.primaryAccent,
        onPrimary = colors.mainBackground,
        primaryContainer = colors.elevatedSurface,
        onPrimaryContainer = colors.primaryAccent,
        secondary = colors.secondaryAccent,
        onSecondary = colors.mainBackground,
        secondaryContainer = colors.primarySurface,
        onSecondaryContainer = colors.secondaryAccent,
        background = colors.mainBackground,
        onBackground = colors.primaryText,
        surface = colors.primarySurface,
        onSurface = colors.primaryText,
        surfaceVariant = colors.elevatedSurface,
        onSurfaceVariant = colors.secondaryText,
        outline = colors.borders,
        outlineVariant = colors.borders,
        error = colors.error,
        onError = colors.primaryText
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colors.mainBackground.toArgb()
                window.navigationBarColor = colors.primarySurface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalNocturneColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
