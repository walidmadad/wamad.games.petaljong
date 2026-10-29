package com.tilebloom.game

import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.*
import org.junit.Assert.*
import org.junit.Test

class GardenRenderingTest {
    @Test fun palettesAreCompleteOpaqueDistinctAndReadable() {
        assertEquals(5, GardenTheme.all.map { it.palette.backgroundTop }.distinct().size)
        GardenTheme.all.forEach {
            val p = it.palette
            val colors = listOf(p.backgroundTop,p.backgroundBottom,p.backgroundAccent,p.headerSurface,
                p.traySurface,p.emptyTraySlot,p.availableTileFace,p.blockedTileFace,p.tileSide,
                p.tileShadow,p.border,p.primaryAccent,p.secondaryAccent,p.textPrimary,p.textSecondary,p.buttonSurface)
            assertTrue(colors.all { c -> c ushr 24 == 255 })
            fun brightness(c: Int) = ((c ushr 16 and 255)*.2126 + (c ushr 8 and 255)*.7152 + (c and 255)*.0722)
            assertTrue(brightness(p.availableTileFace) > brightness(p.blockedTileFace))
            assertTrue(brightness(p.textPrimary) - brightness(p.headerSurface) > 140)
            assertTrue(brightness(p.textSecondary) - brightness(p.buttonSurface) > 100)
        }
    }
    @Test fun restartAndSnapshotRestoreKeepTheSameThemeAndIdentity() {
        for (id in 17L..20L) {
            val level = Levels.get(id)
            val game = Game(level.tiles)
            val theme = GardenTheme.forLevel(id)
            assertTrue(game.collectRevealed(level.winningSelections.first()))
            val snapshot = game.snapshot()
            val restored = Game(level.tiles)
            assertTrue(restored.restore(snapshot))
            assertEquals(game.tiles, restored.tiles)
            val session = GardenSession()
            assertTrue(session.restore(SessionSnapshot(id, level.seed, level.version, snapshot)))
            assertTrue(session.restart())
            assertEquals(level.tiles, session.game.tiles)
            assertEquals(theme, GardenTheme.forLevel(session.level.id))
            val relaunched = GardenSession()
            assertTrue(relaunched.restore(session.snapshot()))
            assertEquals(theme, GardenTheme.forLevel(relaunched.level.id))
            assertEquals(level, relaunched.level)
            assertEquals(theme, GardenTheme.forLevel(id + 5))
        }
    }
    @Test fun visibleBlockedArtworkDoesNotChangeSelectionOrHint() {
        val tiles = (0..2).map { Tile(it, Symbol.LEAF, it.toDouble(), 0.0, 0) }
        val game = Game(tiles)
        val geometry = BoardGeometry(360f,480f,tiles=tiles)
        val c = geometry.face(tiles[1]).center
        assertTrue(TileVisibility.faceVisibleAt(tiles[1],tiles,geometry,c.x,c.y))
        assertFalse(game.isAvailable(1))
        assertFalse(game.collectRevealed(1))
        HintPolicy.suggest(tiles,game.snapshot()).id?.let { assertTrue(game.isAvailable(it)) }
    }
    @Test fun physicalCoverageRemovalAndUndoAreDeterministicAcrossWidths() {
        val lower = Tile(0,Symbol.LEAF,0.0,0.0,0)
        val upper = Tile(1,Symbol.LOTUS,.5,0.0,1)
        val tiles = listOf(lower,upper)
        for (width in listOf(320f,360f,400f,600f)) {
            val geometry = BoardGeometry(width,480f,tiles=tiles)
            val face = geometry.face(lower)
            val x = face.left + geometry.scale * .2f
            val y = face.center.y
            assertTrue(TileVisibility.faceVisibleAt(lower,tiles,geometry,x,y))
            val coveredX = face.right - geometry.scale * .2f
            assertFalse(TileVisibility.faceVisibleAt(lower,tiles,geometry,coveredX,y))
            assertEquals(upper, geometry.hitTest(tiles,coveredX,y))
            val game = Game(tiles)
            assertFalse(game.collectRevealed(lower.id))
            assertTrue(game.collectRevealed(upper.id))
            assertTrue(TileVisibility.faceVisibleAt(lower,game.tiles,geometry,coveredX,y))
            assertTrue(game.undo())
            assertEquals(tiles,game.tiles)
            assertFalse(TileVisibility.faceVisibleAt(lower,game.tiles,geometry,coveredX,y))
            assertEquals(geometry.drawOrder(tiles),geometry.drawOrder(tiles.reversed()))
        }
    }
}
