package com.wamad.petaljong.presentation

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Framework locale authority on API 33+; one legacy preference only on API 26–32. */
object AppLanguage {
    val tags = listOf("en", "fr", "es", "pt-BR", "ar", "hi", "ja", "de")
    val nativeNames = listOf("English", "Français", "Español", "Português (Brasil)", "العربية", "हिन्दी", "日本語", "Deutsch")
    private fun preferences(context: Context) = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
    fun selected(context: Context): String = if (Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags().split(',').firstOrNull().orEmpty()
    } else preferences(context).getString("tag", "").orEmpty()

    fun attach(context: Context): Context {
        val prefs = preferences(context)
        if (Build.VERSION.SDK_INT >= 33) {
            // One-time handoff after an OS upgrade; never replace an existing framework selection.
            val legacy = prefs.getString("tag", "").orEmpty()
            val manager = context.getSystemService(LocaleManager::class.java)
            if (legacy.isNotEmpty()) {
                if (manager.applicationLocales.isEmpty) manager.applicationLocales = LocaleList.forLanguageTags(legacy)
                prefs.edit().remove("tag").apply()
            }
            return context
        }
        val tag = selected(context)
        if (tag.isEmpty()) return context
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tag))
            setLayoutDirection(Locale.forLanguageTag(tag))
        })
    }

    fun select(context: Context, tag: String) {
        require(tag.isEmpty() || tag in tags)
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            check(preferences(context).edit().putString("tag", tag).commit())
        }
    }
}
