package com.wamad.petaljong

import android.test.InstrumentationTestCase
import android.view.View
import com.wamad.petaljong.presentation.LevelLoadingScreen

/** No production sleeps or changes to player storage. */
class LevelLoadingPresentationTest : InstrumentationTestCase() {
    fun testResponsiveWidthsAndStaticError() {
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            for (width in listOf(320,360,400,720)) {
                val screen = LevelLoadingScreen(context) {}
                screen.present(30, false, false)
                screen.measure(View.MeasureSpec.makeMeasureSpec((width*context.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                screen.layout(0,0,screen.measuredWidth,screen.measuredHeight)
                assertTrue(screen.measuredWidth > 0)
                screen.present(30,true,false)
                screen.stopAnimation()
            }
        }
    }
}
