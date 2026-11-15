package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.board.*
import org.junit.Assert.*
import org.junit.Test

class BoardGeometryTest {
    private val tiles = LevelOne.tiles()
    private fun layout() = BoardGeometry(360f, 420f)
    @Test fun visibleTileCenter() {
        val l = layout(); val tile = tiles.first { it.id == 16 }; val p = l.face(tile).center
        assertEquals(16, l.hitTest(tiles, p.x, p.y)?.id)
    }
    @Test fun faceBorderAndSolidThicknessAreTargets() {
        val l = layout(); val tile = tiles.first { it.id == 16 }; val f = l.face(tile); val e = l.edge(tile)
        assertEquals(16, l.hitTest(tiles, f.left + l.scale * .005f, f.center.y)?.id)
        assertEquals(16, l.hitTest(tiles, e.center.x, e.bottom - l.scale * .005f)?.id)
    }
    @Test fun roundedCornerIsNotAFullRectangle() {
        val l = layout(); val t = tiles.first { it.id == 16 }; val f = l.face(t)
        assertNull(l.hitTest(listOf(t), f.left + l.scale * .001f, f.top + l.scale * .001f))
    }
    @Test fun thickerBodyIsDecorativeAndTransformStable() {
        val t = tiles.first { it.id == 16 }; val l = layout()
        assertTrue(l.body(t).bottom > l.edge(t).bottom)
        assertNull(l.hitTest(listOf(t), l.body(t).center.x, l.body(t).bottom - .001f))
        assertEquals(l.face(t).right - l.face(t).left, l.body(t).right - l.body(t).left, .001f)
        assertEquals(l.drawOrder(tiles), l.drawOrder(tiles.reversed()))
    }
    @Test fun shadowIsNotInteractive() {
        val l = layout(); val t = tiles.first { it.id == 16 }; val s = l.shadow(t)
        assertNull(l.hitTest(listOf(t), s.center.x, s.bottom - l.scale * .005f))
    }
    @Test fun emptySpaceAndEmptyBoard() {
        val l = layout(); assertNull(l.hitTest(tiles, 0f, 0f)); assertNull(l.hitTest(emptyList(), 180f, 200f))
    }
    @Test fun topmostTileConsumesCoveredLowerCenter() {
        val l = layout(); val lower = tiles.first { it.id == 5 }; val upper = tiles.first { it.id == 16 }
        val p = l.toScreen(1.2,1.2)
        assertEquals(lower.id, l.hitTest(listOf(lower), p.x, p.y)?.id)
        assertEquals(upper.id, l.hitTest(listOf(lower, upper), p.x, p.y)?.id)
    }
    @Test fun unavailableUpperTileStillConsumesTap() {
        val g = Game(tiles); val l = layout(); val tile = tiles.first { it.id == 17 }; val p = l.face(tile).center
        assertFalse(g.isAvailable(17)); val hit = l.hitTest(g.tiles, p.x, p.y)
        assertEquals(17, hit?.id); g.select(hit!!.id); assertNull(g.selected); assertEquals(24, g.remaining.size)
    }
    @Test fun adjacentFacesHaveSeparateTargetsAndGap() {
        val l = layout(); val a = tiles.first { it.id == 16 }; val b = tiles.first { it.id == 17 }
        val fa = l.face(a); val fb = l.face(b)
        assertEquals(16, l.hitTest(listOf(a,b), fa.right - .1f, fa.center.y)?.id)
        assertEquals(17, l.hitTest(listOf(a,b), fb.left + .1f, fb.center.y)?.id)
        assertNull(l.hitTest(listOf(a,b), (fa.right + fb.left)/2, fa.center.y))
    }
    @Test fun overlapBoundaryUsesSameReverseOrderAsDrawing() {
        val l = layout(); val lower = tiles.first { it.id == 5 }; val upper = tiles.first { it.id == 16 }
        val p = l.toScreen(1.2,1.2)
        assertEquals(16, l.hitTest(listOf(upper, lower), p.x, p.y)?.id)
        assertEquals(16, l.drawOrder(listOf(upper, lower)).last().id)
    }
    @Test fun compactAndLargeViewportsPreserveTargetsAndFit() {
        for ((w,h) in listOf(284f to 260f, 376f to 480f, 720f to 960f)) {
            val l = BoardGeometry(w,h)
            for (t in tiles) {
                val r = l.shadow(t)
                assertTrue(r.left >= 0); assertTrue(r.top >= 0); assertTrue(r.right <= w); assertTrue(r.bottom <= h)
            }
            for (id in 16..23) {
                val p = l.face(tiles.first { it.id == id }).center
                assertEquals(id, l.hitTest(tiles,p.x,p.y)?.id)
            }
        }
    }
    @Test fun paddingIsPartOfSharedTransform() {
        val base = BoardGeometry(320f,400f)
        val padded = BoardGeometry(360f,460f,BoardPadding(10f,20f,30f,40f))
        val t = tiles.first { it.id == 16 }; val a = base.face(t).center; val b = padded.face(t).center
        assertEquals(a.x+10f,b.x,.001f); assertEquals(a.y+20f,b.y,.001f)
        assertEquals(16,padded.hitTest(tiles,b.x,b.y)?.id)
        assertNull(padded.hitTest(tiles,5f,5f))
    }
    @Test fun coordinateRoundTripIncludesLayerLift() {
        val l = layout()
        for (layer in 0..1) {
            val p = l.toScreen(1.5, 2.5, layer); val b = l.toBoard(p.x,p.y,layer)!!
            assertEquals(1.5f,b.x,.00001f); assertEquals(2.5f,b.y,.00001f)
        }
    }
    @Test fun removingUpperTilesRevealsLowerHitTarget() {
        val g = Game(tiles); val l = layout(); val p = l.toScreen(.7,.7)
        assertEquals(16,l.hitTest(g.tiles,p.x,p.y)?.id)
        for (pair in LevelOne.witness.take(4)) assertTrue(g.match(pair.first,pair.second))
        assertEquals(0,l.hitTest(g.tiles,p.x,p.y)?.id); assertTrue(g.isAvailable(0))
    }
    @Test fun restartRestoresHitTargetsWithoutDuplicates() {
        val g = Game(tiles); val l = layout(); val p = l.face(tiles.first { it.id == 16 }).center
        repeat(10) {
            assertTrue(g.match(16,20)); assertNotEquals(16,l.hitTest(g.tiles,p.x,p.y)?.id)
            g.restart(); assertEquals(16,l.hitTest(g.tiles,p.x,p.y)?.id); assertEquals(24,l.drawOrder(g.tiles).size)
        }
    }
    @Test fun zeroOrOverPaddedSizeHasNoTargets() {
        for (l in listOf(BoardGeometry(0f,0f), BoardGeometry(10f,10f,BoardPadding(20f,20f,20f,20f)))) {
            assertNull(l.hitTest(tiles,0f,0f)); assertNull(l.toBoard(0f,0f))
        }
    }
    @Test fun fullWitnessThroughGeometryAndSelection() {
        val g = Game(tiles); val l = layout()
        LevelOne.witness.forEachIndexed { index, pair ->
            for (id in listOf(pair.first,pair.second)) {
                val p = l.face(g.tiles.first { it.id == id }).center
                g.select(l.hitTest(g.tiles,p.x,p.y)!!.id)
            }
            assertEquals(22-index*2,g.remaining.size); assertNull(g.selected)
        }
        assertEquals(Status.WON,g.status)
    }
}
