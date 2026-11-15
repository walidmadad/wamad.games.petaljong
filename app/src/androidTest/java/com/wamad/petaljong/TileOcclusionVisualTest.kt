package com.wamad.petaljong

import android.graphics.Bitmap
import android.graphics.Canvas
import android.test.InstrumentationTestCase
import android.view.View
import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.MahjongBoardView
import com.wamad.petaljong.presentation.board.*

/** Pixel checks use the production Android renderer, not a simulated host image. */
@Suppress("DEPRECATION")
class TileOcclusionVisualTest : InstrumentationTestCase() {
    fun testOpaqueUpperTilesHideIdentityButExposedLowerSymbolsRemain() {
        instrumentation.runOnMainSync {
            val lower = Tile(0,Symbol.LEAF,0.0,0.0,0)
            for (offset in listOf(0.0, .5)) for (theme in GardenTheme.all) {
                val upper = Tile(1,Symbol.LOTUS,offset,0.0,1)
                val tiles = listOf(lower,upper)
                fun render(symbol: Symbol): Bitmap {
                    val view = MahjongBoardView(instrumentation.targetContext,
                        Game(listOf(lower.copy(symbol=symbol),upper))) {}
                    view.theme = theme
                    view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY))
                    view.layout(0,0,360,480)
                    return Bitmap.createBitmap(360,480,Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
                }
                val a = render(Symbol.LEAF); val b = render(Symbol.LADYBUG)
                val geometry = BoardGeometry(360f,480f,tiles=tiles)
                var exposedDifferences = 0
                val upperFace = geometry.face(upper).inset(geometry.scale * .12f)
                for (y in 0 until 480) for (x in 0 until 360) {
                    if (upperFace.containsRounded(x+.5f,y+.5f,geometry.radius))
                        assertEquals("Hidden identity leaked in ${theme.id}",a.getPixel(x,y),b.getPixel(x,y))
                    if (a.getPixel(x,y) != b.getPixel(x,y)) exposedDifferences++
                }
                if (offset == 0.0) assertEquals("Fully covered artwork leaked", 0, exposedDifferences)
                else assertTrue("Exposed botanical artwork missing", exposedDifferences > 10)
                a.recycle(); b.recycle()
            }
        }
    }
}
