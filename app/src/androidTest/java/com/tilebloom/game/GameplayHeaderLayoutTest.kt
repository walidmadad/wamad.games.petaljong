package com.tilebloom.game

import android.content.res.Configuration
import android.os.LocaleList
import android.test.InstrumentationTestCase
import android.view.View
import android.widget.ImageButton
import com.tilebloom.game.presentation.GameplayHeader
import java.util.Locale

@Suppress("DEPRECATION")
class GameplayHeaderLayoutTest : InstrumentationTestCase() {
    fun testMinimalLocalizedHeaderAcrossPortraitWidthsAndRtl() {
        for (tag in listOf("en", "fr", "es", "pt-BR", "ar", "hi", "ja", "de")) {
            val context = instrumentation.targetContext.createConfigurationContext(
                Configuration(instrumentation.targetContext.resources.configuration).apply {
                    setLocales(LocaleList.forLanguageTags(tag))
                    setLayoutDirection(Locale.forLanguageTag(tag))
                })
            instrumentation.runOnMainSync {
                val density = context.resources.displayMetrics.density
                val touch = (48 * density + .5f).toInt()
                val settings = ImageButton(context).apply {
                    contentDescription = context.getString(R.string.settings_label)
                    minimumWidth = touch
                    setOnClickListener { }
                }
                val header = GameplayHeader(context, settings).apply {
                    layoutDirection = context.resources.configuration.layoutDirection
                    levelLabel.text = context.getString(R.string.level_number, 37)
                }
                for (widthDp in listOf(320, 360, 400, 800)) {
                    val width = ((widthDp - 32) * density).toInt()
                    header.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(touch, View.MeasureSpec.EXACTLY))
                    header.layout(0, 0, width, touch)
                    assertEquals(2, header.childCount)
                    assertEquals(context.getString(R.string.level_number, 37), header.levelLabel.text.toString())
                    assertTrue(settings.isClickable)
                    assertTrue(settings.width >= touch && settings.height >= touch)
                    val label = header.levelLabel
                    assertEquals(1, label.layout.lineCount)
                    assertEquals(0, label.layout.getEllipsisCount(0))
                    assertTrue(label.layout.getLineWidth(0) <= label.width)
                    assertTrue(label.left >= 0 && label.right <= width)
                    if (tag == "ar") assertTrue(label.right <= settings.left)
                    else assertTrue(settings.right <= label.left)
                }
            }
        }
    }
}
