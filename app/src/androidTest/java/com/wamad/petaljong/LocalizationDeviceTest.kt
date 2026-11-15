package com.wamad.petaljong

import android.content.res.Configuration
import android.os.LocaleList
import android.test.InstrumentationTestCase
import android.view.View
import android.widget.TextView
import com.wamad.petaljong.presentation.AppLanguage
import java.util.Locale

/** Resource and shaping checks use isolated contexts; they never change device/player preferences. */
@Suppress("DEPRECATION")
class LocalizationDeviceTest : InstrumentationTestCase() {
    private fun localized(tags: String) = instrumentation.targetContext.createConfigurationContext(
        Configuration(instrumentation.targetContext.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tags))
            setLayoutDirection(Locale.forLanguageTag(tags.substringBefore(',')))
        })
    fun testAllEightResourcesAndLevelFormatting() {
        val expected = listOf("Settings", "Paramètres", "Ajustes", "Configurações", "الإعدادات", "सेटिंग", "設定", "Einstellungen")
        AppLanguage.tags.forEachIndexed { index, tag ->
            val context = localized(tag)
            assertEquals(expected[index], context.getString(R.string.settings_label))
            val message = context.getString(R.string.level_number, 32)
            assertFalse(message.contains("%"))
            assertTrue(message.isNotBlank())
            for (count in listOf(0, 1, 2, 3, 11, 100)) {
                assertFalse(context.resources.getQuantityString(R.plurals.pairs_remaining, count, count).contains("%"))
            }
        }
    }
    fun testUnsupportedLanguageFallsBackToEnglish() {
        assertEquals("Settings", localized("ko").getString(R.string.settings_label))
    }
    fun testResourceLocalePriority() {
        assertEquals("Paramètres", localized("fr,de").getString(R.string.settings_label))
        assertEquals("Einstellungen", localized("de,fr").getString(R.string.settings_label))
    }
    fun testArabicDirectionAndComplexScriptLayout() {
        for (tag in listOf("ar", "hi", "ja")) {
            val context = localized(tag)
            if (tag == "ar") assertEquals(View.LAYOUT_DIRECTION_RTL, context.resources.configuration.layoutDirection)
            instrumentation.runOnMainSync {
                val label = TextView(context).apply {
                    text = context.getString(R.string.language_automatic)
                    textSize = 16f
                    measure(View.MeasureSpec.makeMeasureSpec(280, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    layout(0, 0, measuredWidth, measuredHeight)
                }
                assertTrue(label.layout.lineCount > 0)
                assertTrue(label.height >= label.layout.height)
                for (line in 0 until label.layout.lineCount) assertTrue(label.layout.getLineWidth(line) <= label.width)
            }
        }
    }
}
