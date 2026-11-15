package com.wamad.petaljong

import android.content.res.Configuration
import android.os.LocaleList
import android.test.InstrumentationTestCase
import android.view.View
import android.widget.LinearLayout
import com.wamad.petaljong.presentation.GardenIconButton

/** Measures production controls at real dp widths and scalable credit badges and compact video indicators. */
@Suppress("DEPRECATION")
class ControlReadabilityTest : InstrumentationTestCase() {
    fun testControlsFitWithCreditsAndAdsAtLargerFontScales() {
        for (tag in listOf("en", "fr", "es", "pt-BR", "ar", "hi", "ja", "de")) {
            for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
                val context = instrumentation.targetContext.createConfigurationContext(
                    Configuration(instrumentation.targetContext.resources.configuration).apply {
                        setLocales(LocaleList.forLanguageTags(tag)); fontScale = scale
                    })
                instrumentation.runOnMainSync {
                    val d = context.resources.displayMetrics.density
                    val row = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutDirection = context.resources.configuration.layoutDirection
                    }
                    val buttons = listOf(R.drawable.ic_home, R.drawable.ic_hint, R.drawable.ic_undo, R.drawable.ic_restart)
                        .map { GardenIconButton(context, it) }
                    buttons.forEach { row.addView(it, LinearLayout.LayoutParams(0, -2, 1f)) }
                    for (widthDp in listOf(320, 360, 400, 800)) {
                        for (credit in listOf(0, 1, 3, 10, 99, 999)) {
                            buttons[1].badge = credit; buttons[2].badge = credit
                            row.measure(View.MeasureSpec.makeMeasureSpec(((widthDp - 48) * d).toInt(), View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                            row.layout(0, 0, row.measuredWidth, row.measuredHeight)
                            buttons.forEach {
                                assertTrue(it.width >= 48 * d); assertTrue(it.height >= 48 * d)
                                assertTrue(it.left >= 0 && it.right <= row.width)
                                assertEquals(credit == 0, buttons[1].showsAdIndicator)
                                assertEquals(credit, buttons[1].badge)
                                assertEquals(credit == 0, buttons[2].showsAdIndicator)
                                if (scale == 1f) assertTrue("Compact toolbar", it.height <= 80 * d)
                                // Exercise production Canvas drawing in both layout directions.
                                val bitmap = android.graphics.Bitmap.createBitmap(it.width, it.height, android.graphics.Bitmap.Config.ARGB_8888)
                                it.draw(android.graphics.Canvas(bitmap)); bitmap.recycle()
                            }
                            if (widthDp == 320 && scale == 1f && tag in listOf("en", "ar") && credit in listOf(0, 3)) {
                                val bitmap = android.graphics.Bitmap.createBitmap(row.width, row.height, android.graphics.Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(com.wamad.petaljong.domain.GardenTheme.forLevel(47).palette.headerSurface)
                                row.draw(android.graphics.Canvas(bitmap))
                                val folder = java.io.File(instrumentation.targetContext.filesDir, "codex61-visual").apply { mkdirs() }
                                java.io.File(folder, "toolbar-$tag-$credit.png").outputStream().use {
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                                }
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        }
    }
}
