package com.tilebloom.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.test.InstrumentationTestCase
import android.view.View
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.MahjongBoardView
import java.io.File

/** Production Canvas fixtures, isolated from player storage. Pixel comparisons exclude system UI. */
@Suppress("DEPRECATION")
class CeramicArtworkVisualTest : InstrumentationTestCase() {
    private fun render(tiles: List<Tile>, width: Int, theme: GardenTheme): Bitmap {
        val view = MahjongBoardView(instrumentation.targetContext, Game(tiles)) {}
        view.theme = theme
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(width * 3 / 2, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, width * 3 / 2)
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it); canvas.drawColor(Color.rgb(25, 48, 40)); view.draw(canvas)
        }
    }
    fun testCatalogIsDistinctDeterministicAndHiddenBacksConcealEveryIdentity() {
        instrumentation.runOnMainSync {
            val theme = GardenTheme.forLevel(1)
            assertEquals(23, Symbol.entries.size)
            assertEquals(23, Symbol.entries.map { it.stableId }.distinct().size)
            val fronts = Symbol.entries.map { symbol ->
                val tiles = listOf(Tile(0, symbol, 0.0, 0.0, 0))
                val first = render(tiles, 320, theme)
                val second = render(tiles, 320, theme)
                assertTrue("Non-deterministic $symbol", first.sameAs(second)); second.recycle()
                first
            }
            for (a in fronts.indices) for (b in 0 until a)
                assertFalse("Identical artwork: ${Symbol.entries[a]}, ${Symbol.entries[b]}", fronts[a].sameAs(fronts[b]))
            fronts.forEach { it.recycle() }
            val back = render(listOf(Tile(0, Symbol.LEAF, 0.0, 0.0, 0, mystery = true)), 320, theme)
            for (symbol in Symbol.entries) {
                val other = render(listOf(Tile(0, symbol, 0.0, 0.0, 0, mystery = true)), 320, theme)
                assertTrue("Back leaked $symbol", back.sameAs(other)); other.recycle()
            }
            back.recycle()
        }
    }
    fun testBlockedSymbolsRemainRecognizableAcrossThemesAndUndoRestoresPixels() {
        instrumentation.runOnMainSync {
            for (theme in GardenTheme.all) {
                fun blocked(symbol: Symbol) = listOf(Tile(0, Symbol.FERN, 0.0, 0.0, 0),
                    Tile(1, symbol, 1.0, 0.0, 0), Tile(2, Symbol.FERN, 2.0, 0.0, 0))
                val leaf = render(blocked(Symbol.LEAF), 360, theme)
                val flower = render(blocked(Symbol.FLOWER), 360, theme)
                assertFalse("Blocked symbols lost in ${theme.id}", leaf.sameAs(flower))
                leaf.recycle(); flower.recycle()
            }
            val game = Game(listOf(Tile(0, Symbol.LEAF, 0.0, 0.0, 0),
                Tile(1, Symbol.LEAF, 2.0, 0.0, 0)))
            val before = render(game.tiles, 360, GardenTheme.forLevel(1))
            assertTrue(game.select(0)); assertTrue(game.select(1))
            assertEquals(Status.WON, game.status)
            assertTrue(game.undo()); assertTrue(game.undo())
            val restored = render(game.tiles, 360, GardenTheme.forLevel(1))
            assertTrue("Undo changed ceramic presentation", before.sameAs(restored))
            before.recycle(); restored.recycle()
        }
    }
    fun testResponsiveCatalogAndDenseLevel33Fixtures() {
        val level = Levels.get(33L) // Generate off the UI thread, exactly as production does.
        val atlas = Symbol.entries.mapIndexed { i, s -> Tile(i, s, (i % 6 * 2).toDouble(), (i / 6 * 2).toDouble(), 0) }
        val overlap = listOf(Tile(0, Symbol.LEAF, 0.0, 0.0, 0),
            Tile(1, Symbol.LOTUS, .5, 0.0, 1), Tile(2, Symbol.BUTTERFLY, 3.0, 0.0, 0),
            Tile(3, Symbol.LADYBUG, 3.0, 0.0, 1),
            Tile(4, Symbol.LAVENDER, 5.0, 0.0, 0, mystery = true),
            Tile(5, Symbol.LAVENDER, 7.0, 0.0, 0, mystery = true, revealed = true))
        instrumentation.runOnMainSync {
            val output = File(instrumentation.targetContext.filesDir, "codex43-visual").apply { mkdirs() }
            for (width in listOf(320, 360, 400, 800)) for (theme in GardenTheme.all) {
                for ((name, tiles) in listOf("catalog" to atlas, "level33" to level.tiles, "occlusion" to overlap)) {
                    val pixels = (width * instrumentation.targetContext.resources.displayMetrics.density).toInt()
                    val bitmap = render(tiles, pixels, theme)
                    File(output, "$name-${theme.id}-${width}dp.png").outputStream().use {
                        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                    bitmap.recycle()
                }
            }
        }
    }
}
