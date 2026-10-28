package com.tilebloom.game

import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.*
import org.junit.Assert.*
import org.junit.Test

class HintPresentationTest {
    @Test fun authoritativeSingleTargetAndWalletAreIndependentOfFrames() {
        val progress = ProgressController()
        val game = progress.session.game
        val snapshot = game.snapshot()
        val id = HintPolicy.suggest(game.tiles, snapshot).id!!
        val effect = HintPresentation()
        assertTrue(progress.acceptHint(id, snapshot))
        assertTrue(effect.show(id, game, 100L))
        assertEquals(id, effect.targetId)
        repeat(10) { assertFalse(effect.show(id, game, 200L)) }
        for (now in 100L..4099L step 16L) {
            effect.validate(game, now)
            assertTrue(effect.brightness(now, true) in 0f..1f)
            assertEquals(id, effect.targetId)
            assertTrue(game.isAvailable(effect.targetId!!))
        }
        effect.validate(game, 4100L)
        assertNull(effect.targetId)
        assertEquals(2, progress.hintCredits)
        assertEquals(snapshot, game.snapshot())
    }
    @Test fun exactGeometryAndHitTestingOnCompactLayeredBoard() {
        val game = Game(Levels.get(7).tiles)
        val effect = HintPresentation()
        val id = HintPolicy.suggest(game.tiles, game.snapshot()).id!!
        assertTrue(effect.show(id, game, 0L))
        val layout = BoardGeometry(320f, 420f, tiles = game.tiles)
        val face = layout.face(game.remaining.single { it.id == id })
        val center = face.center
        val hit = layout.hitTest(game.tiles, center.x, center.y)
        assertEquals(face, effect.face(game, layout))
        val bulb = effect.indicator(face, 320f, 420f, 11f, game, layout)
        assertTrue(bulb.x in 11f..309f)
        assertTrue(bulb.y in 11f..409f)
        effect.clear()
        assertEquals(hit, layout.hitTest(game.tiles, center.x, center.y))
    }
    @Test fun invalidCoveredAndCollectedTargetsNeverDisplay() {
        val game = Game(listOf(Tile(0, Symbol.LEAF, 0.0, 0.0, 0), Tile(1, Symbol.LEAF, 0.0, 0.0, 1)))
        val effect = HintPresentation()
        assertFalse(effect.show(-1, game, 0L))
        assertFalse(effect.show(0, game, 0L))
        assertTrue(effect.show(1, game, 0L))
        assertTrue(game.select(1))
        effect.validate(game, 1L)
        assertNull(effect.targetId)
        assertFalse(effect.show(1, game, 2L))
    }
    @Test fun terminalBoardsInvalidatePreviouslyAvailableTargets() {
        val level = Levels.get(1)
        val won = Game(level.tiles)
        val effect = HintPresentation()
        effect.show(HintPolicy.suggest(won.tiles, won.snapshot()).id!!, won, 0L)
        level.witness.forEach { assertTrue(won.match(it.first, it.second)) }
        assertEquals(Status.WON, won.status)
        effect.validate(won, 1L)
        assertNull(effect.targetId)
        val lost = Game((0..7).map { Tile(it, Symbol.entries[it % 4], it * 2.0, 0.0, 0) })
        assertTrue(effect.show(7, lost, 0L))
        (0..3).forEach { assertTrue(lost.select(it)) }
        assertEquals(Status.LOST, lost.status)
        effect.validate(lost, 1L)
        assertNull(effect.targetId)
    }
    @Test fun staticModeAndNewViewHaveNoSavedAnimationState() {
        val game = Game(Levels.get(7).tiles)
        val id = HintPolicy.suggest(game.tiles, game.snapshot()).id!!
        val effect = HintPresentation()
        effect.show(id, game, 0L)
        assertEquals(1f, effect.brightness(500L, false))
        assertEquals(1f, effect.brightness(3000L, false))
        assertNull(HintPresentation().targetId)
        effect.clear()
        assertNull(effect.targetId)
        assertTrue(effect.show(id, game, 4000L))
        effect.validate(game, 8000L)
        assertNull(effect.targetId)
    }
}
