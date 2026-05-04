package com.example.crowdalert.ui.settings

import android.content.Context
import java.util.Locale

enum class AppThemeMode {
    Light,
    Dark,
    Device,
}

object AppSettings {
    const val DEFAULT_LOCALE_TAG = "en"

    val supportedLocaleTags = listOf("en", "de", "pl", "fr", "tr")

    fun activeLocaleTag(context: Context): String {
        getSavedLocaleTag(context)?.let { return it }
        return deviceLocaleTag()
    }

    fun setLocaleTag(
        context: Context,
        localeTag: String,
    ) {
        val normalizedTag = localeTag.takeIf { it in supportedLocaleTags } ?: DEFAULT_LOCALE_TAG
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SELECTED_LOCALE, normalizedTag)
            .commit()
    }

    fun getSavedLocaleTag(context: Context): String? =
        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_LOCALE, null)
            ?.takeIf { it in supportedLocaleTags }

    fun getThemeMode(context: Context): AppThemeMode =
        runCatching {
            AppThemeMode.valueOf(
                preferences(context).getString(KEY_THEME_MODE, AppThemeMode.Device.name)
                    ?: AppThemeMode.Device.name,
            )
        }.getOrDefault(AppThemeMode.Device)

    fun setThemeMode(
        context: Context,
        mode: AppThemeMode,
    ) {
        preferences(context)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    private fun deviceLocaleTag(): String =
        Locale.getDefault()
            .language
            .takeIf { it in supportedLocaleTags }
            ?: DEFAULT_LOCALE_TAG

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private const val PREFERENCES_NAME = "app_settings"
    private const val KEY_SELECTED_LOCALE = "selected_locale"
    private const val KEY_THEME_MODE = "theme_mode"
}
