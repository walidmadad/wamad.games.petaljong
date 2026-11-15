package com.wamad.petaljong

import android.content.res.Configuration
import android.os.LocaleList
import android.test.InstrumentationTestCase
import android.view.View
import android.widget.ImageButton
import com.wamad.petaljong.domain.Game
import com.wamad.petaljong.domain.Levels
import com.wamad.petaljong.presentation.GameplayBoardArea
import com.wamad.petaljong.presentation.MahjongBoardView
import com.wamad.petaljong.presentation.GameplayHeader
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
                for (level in listOf(14, 23, 110, 203)) {
                    header.levelLabel.text = context.getString(R.string.level_number, level)
                    for (widthDp in listOf(320, 360, 400, 800)) {
                        val width = ((widthDp - 32) * density).toInt()
                        header.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(touch, View.MeasureSpec.EXACTLY))
                        header.layout(0, 0, width, touch)
                        assertEquals(1, header.childCount)
                        // The single title belongs to the measured board gap, never the settings row.
                        assertNull(header.levelLabel.parent)
                        header.levelLabel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                        header.levelLabel.layout(0, 0, width, header.levelLabel.measuredHeight)
                        assertEquals(context.getString(R.string.level_number, level), header.levelLabel.text.toString())
                        assertTrue(settings.isClickable)
                        assertTrue(settings.width >= touch && settings.height >= touch)
                        val label = header.levelLabel
                        assertNull(label.background)
                        assertEquals(android.view.Gravity.CENTER_HORIZONTAL, label.gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK)
                        assertEquals(1, label.layout.lineCount)
                        assertEquals(0, label.layout.getEllipsisCount(0))
                        assertTrue(label.layout.getLineWidth(0) <= label.width)
                        assertTrue(label.left >= 0 && label.right <= width)
                        val board = MahjongBoardView(context, Game(Levels.get(level.toLong()).tiles)) {}
                        val area = GameplayBoardArea(context, board, label)
                        for (heightDp in listOf(280, 480, 640)) {
                            val height = (heightDp * density).toInt()
                            area.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                            area.layout(0, 0, width, height)
                            val artworkTop = board.top + board.artworkTop(board.width, board.height)
                            assertTrue(kotlin.math.abs((label.top + label.bottom) / 2f - artworkTop / 2f) <= 1.5f)
                            assertEquals(width / 2f, (label.left + label.right) / 2f, .01f)
                            assertTrue(label.top >= 7 * density)
                            assertTrue(label.bottom <= artworkTop - 7 * density)
                            assertEquals(0, label.layout.getEllipsisCount(0))
                            assertTrue(label.layout.getLineWidth(0) <= label.width - label.totalPaddingLeft - label.totalPaddingRight)
                            val available = label.height - label.compoundPaddingTop - label.compoundPaddingBottom
                            val minimum = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 34f, context.resources.displayMetrics)
                            val font = android.text.TextPaint(label.paint).apply { textSize = minimum }
                            if (font.fontMetrics.descent - font.fontMetrics.ascent <= available &&
                                font.measureText(label.text.toString()) <= width - label.totalPaddingLeft - label.totalPaddingRight) {
                                assertTrue(label.textSize >= minimum - 1f)
                            }
                            val before = label.top
                            area.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                            area.layout(0, 0, width, height)
                            assertEquals(before, label.top)
                        }
                        area.removeView(label)

                    }
                }
            }
        }
    }
}
