package com.mohamedzayed.nocturne.localization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "nocturne_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    const val LANG_SYSTEM = "system"
    const val LANG_AR = "ar"
    const val LANG_EN = "en"

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setSelectedLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun getEffectiveLocale(context: Context, langPreference: String): Locale {
        return when (langPreference) {
            LANG_AR -> Locale("ar")
            LANG_EN -> Locale("en")
            else -> {
                // Follow system
                val systemLocale = context.resources.configuration.locales.get(0)
                if (systemLocale.language == "ar") Locale("ar") else Locale("en")
            }
        }
    }

    fun isRtl(locale: Locale): Boolean {
        return locale.language == "ar"
    }
}

@Composable
fun ProvideLocalizedApp(
    languagePreference: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val effectiveLocale = LocaleHelper.getEffectiveLocale(context, languagePreference)
    val layoutDirection = if (LocaleHelper.isRtl(effectiveLocale)) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Update configuration locale for string resources
    val configuration = LocalConfiguration.current
    val localizedConfig = android.content.res.Configuration(configuration).apply {
        setLocale(effectiveLocale)
        setLayoutDirection(effectiveLocale)
    }
    val localizedContext = context.createConfigurationContext(localizedConfig)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalConfiguration provides localizedConfig,
        LocalContext provides localizedContext
    ) {
        content()
    }
}
