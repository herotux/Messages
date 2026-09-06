package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.fossify.messages.extensions.config

object AppLanguageManager {
    private const val PERSIAN = "fa"
    private const val ENGLISH = "en"

    fun initialize(context: Context) {
        val config = context.config
        if (!config.wasUseEnglishToggled) {
            // On first launch the app follows the device language. We only
            // have an explicit English/Persian switch in the Messages UI, so
            // unsupported system languages fall back to English.
            val systemLanguage = context.resources.configuration.locales
                .takeIf { !it.isEmpty }
                ?.get(0)
                ?.language
                ?.lowercase()
            config.useEnglish = systemLanguage != PERSIAN
        }
        apply(context)
    }

    fun apply(context: Context) {
        val language = if (context.config.useEnglish) ENGLISH else PERSIAN
        val current = AppCompatDelegate.getApplicationLocales()
            .takeIf { !it.isEmpty }
            ?.get(0)
            ?.language
            ?.lowercase()
        if (current != language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
    }

    fun applyAndRestart(activity: Activity) {
        apply(activity)
        activity.recreate()
    }
}
