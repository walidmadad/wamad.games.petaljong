package com.tilebloom.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.test.InstrumentationTestCase
import android.view.View
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.MahjongBoardView
import com.tilebloom.game.presentation.TileTrayView
import java.io.File

/** Isolated production views; never starts MainActivity or reads/writes a player save. */
@Suppress("DEPRECATION")
class Botanical54VisualTest : InstrumentationTestCase() {
    fun testNewCatalogAndFamilyShowcasesAtGameplaySize() {
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            fun showcase(symbols: List<Symbol>, name: String) {
                val tiles = symbols.mapIndexed { i, s -> Tile(i,s,(i%6).toDouble(),(i/6*2).toDouble(),0) }
                capture(MahjongBoardView(context,Game(tiles)) {},name,1080,780)
            }
            showcase(BotanicalPalette.additions,"all-12-new-symbols")
            showcase(listOf(Symbol.FLOWER,Symbol.PINK_PEONY,Symbol.PINK_CAMELLIA,
                Symbol.SUNFLOWER,Symbol.YELLOW_MARIGOLD,Symbol.GOLDEN_CHRYSANTHEMUM,
                Symbol.LEAF,Symbol.MONSTERA_LEAF,Symbol.EUCALYPTUS_LEAF,
                Symbol.MAPLE_LEAF,Symbol.RED_JAPANESE_MAPLE,Symbol.ORANGE_SUGAR_MAPLE),"related-variants")
            // Every new resource goes through the same production tray renderer.
            BotanicalPalette.additions.chunked(3).forEachIndexed { i, group ->
                capture(TileTrayView(context) { group.mapIndexed { id,s -> Tile(id,s,0.0,0.0,0) } },"tray-$i",1080,220)
            }
        }
    }
    fun testDifficultyBoardsAndPressureUseActualGeneratedPuzzles() {
        for (id in listOf(12L,23,40,75,150,501)) {
            val level = Levels.get(id) // outside main thread
            LevelValidator.validate(level)
            val game = Game(level.tiles)
            instrumentation.runOnMainSync {
                val board=MahjongBoardView(instrumentation.targetContext,game) {}
                board.theme=GardenTheme.forLevel(id)
                capture(board,"level-$id-initial",1080,1440)
                for (action in level.winningActions) {
                    if (action.kind==ActionKind.REVEAL) assertTrue(game.reveal(action.tileId)) else assertTrue(game.select(action.tileId))
                    if(game.tray.size==3) break
                }
                assertEquals(3,game.tray.size)
                capture(board,"level-$id-pressure",1080,1440)
                capture(TileTrayView(instrumentation.targetContext) { game.tray },"level-$id-tray",1080,220)
            }
        }
    }
    private fun capture(view: View,name:String,w:Int,h:Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
        view.layout(0,0,w,h)
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val directory=File(instrumentation.targetContext.getExternalFilesDir(null),"codex54").apply { mkdirs() }
        File(directory,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
}
