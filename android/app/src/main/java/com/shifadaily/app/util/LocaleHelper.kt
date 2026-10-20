package com.shifadaily.app.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    val SUPPORTED = listOf("en", "ar")

    fun normalize(tag: String?): String? = tag?.substringBefore('-')?.lowercase()?.takeIf { it in SUPPORTED }

    /** Default for first launch: Arabic if the device is in Arabic, otherwise English. */
    fun deviceDefault(): String = if (Locale.getDefault().language == "ar") "ar" else "en"

    fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Language currently applied by AppCompat / the system per-app setting (API 33+), if any. */
    fun appliedLanguage(): String? {
        val list = AppCompatDelegate.getApplicationLocales()
        return if (list.isEmpty) null else normalize(list[0]?.language)
    }

    fun locale(tag: String): Locale = Locale.forLanguageTag(tag)

    /**
     * Context whose resources use the chosen language. Needed for notifications built
     * from broadcast receivers: below API 33 AppCompat only localises activities.
     */
    fun localizedContext(context: Context, tag: String?): Context {
        val lang = normalize(tag) ?: return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale(lang))
        config.setLayoutDirection(locale(lang))
        return context.createConfigurationContext(config)
    }

    fun isRtl(context: Context): Boolean =
        context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
}
