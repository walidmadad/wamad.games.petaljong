package com.wamad.petaljong

import android.content.res.Configuration
import android.test.AndroidTestCase
import android.view.View
import com.wamad.petaljong.presentation.ContinueLevelCard
import java.util.Locale

@Suppress("DEPRECATION")
class ContinueCardPresentationTest : AndroidTestCase() {
    fun testSharedCardAcrossWidthsAndEightLocales() {
        for (language in listOf("en", "fr", "es", "pt-BR", "ar", "hi", "ja", "de")) {
            val locale = Locale.forLanguageTag(language)
            val config = Configuration(context.resources.configuration).apply {
                setLocale(locale); setLayoutDirection(locale)
            }
            val localized = context.createConfigurationContext(config)
            for (width in listOf(232, 272, 312, 400, 720)) {
                val home = ContinueLevelCard(localized)
                val victory = ContinueLevelCard(localized)
                home.present(38); victory.present(38)
                assertEquals(home.levelLabel.text, victory.levelLabel.text)
                assertEquals(home.levelLabel.currentTextColor, victory.levelLabel.currentTextColor)
                assertEquals(home.continuationLabel.currentTextColor, victory.continuationLabel.currentTextColor)
                assertEquals(localized.getString(R.string.start_level, 38L), victory.levelLabel.text)
                assertEquals(localized.getString(R.string.continue_garden), victory.continuationLabel.text)
                assertFalse(victory.levelLabel.text.contains('→'))
                var taps = 0
                victory.setOnClickListener { taps++ }
                victory.performClick(); assertEquals(1, taps)
                val px = (width * localized.resources.displayMetrics.density).toInt()
                victory.measure(View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                victory.layout(0, 0, px, victory.measuredHeight)
                assertTrue(victory.levelLabel.width <= px)
                assertTrue(victory.continuationLabel.bottom <= victory.height - victory.paddingBottom)
                assertTrue(victory.isFocusable && victory.isClickable)
                assertEquals(home.background.javaClass, victory.background.javaClass)
            }
        }
    }
}
