package com.mohamedzayed.nocturne.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mohamedzayed.nocturne.ui.theme.AppTheme

class AppThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("nocturne_app_theme_prefs", Context.MODE_PRIVATE)

    var currentTheme by mutableStateOf(loadTheme())
        private set

    private fun loadTheme(): AppTheme {
        val id = prefs.getString("selected_theme", AppTheme.MIDNIGHT_GOLD.id) ?: AppTheme.MIDNIGHT_GOLD.id
        return AppTheme.fromId(id)
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("selected_theme", theme.id).apply()
        currentTheme = theme
    }
}
