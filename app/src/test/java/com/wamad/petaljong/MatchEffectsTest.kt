package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.board.MatchEffects
import com.wamad.petaljong.presentation.board.BoardGeometry
import org.junit.Assert.*
import org.junit.Test

class MatchEffectsTest {
    @Test fun acceptedMatchIsImmediateAndCopiesExpireWithoutChangingGame() {
        val game = Game(LevelOne.tiles())
        val effects = MatchEffects()
        val before = game.tiles
        game.select(16); game.select(20)
        effects.record(before, game.tiles, 100, true)
        assertEquals(setOf(16, 20), effects.active.map { it.tile.id }.toSet())
        assertEquals(22, game.remaining.size)
        assertFalse(game.isAvailable(16))
        val layout = BoardGeometry(360f, 440f)
        val removedCenter = layout.face(before.first { it.id == 16 }).center
        assertNotEquals(16, layout.hitTest(game.tiles, removedCenter.x, removedCenter.y)?.id)
        val state = game.snapshot()
        game.select(16); game.select(20)
        effects.record(game.tiles, game.tiles, 110, true)
        assertEquals(2, effects.active.size)
        assertEquals(state, game.snapshot())
        effects.expire(249); assertEquals(2, effects.active.size)
        effects.expire(250); assertTrue(effects.active.isEmpty())
        assertEquals(state, game.snapshot())
    }
    @Test fun restartClearsCopiesAndRapidSuccessivePairsRemainIndependent() {
        val game = Game(LevelOne.tiles())
        val effects = MatchEffects()
        LevelOne.witness.take(2).forEachIndexed { index, pair ->
            val before = game.tiles
            assertTrue(game.match(pair.first, pair.second))
            effects.record(before, game.tiles, index * 50L, true)
        }
        assertEquals(4, effects.active.size)
        effects.expire(150)
        assertEquals(setOf(17, 21), effects.active.map { it.tile.id }.toSet())
        game.restart(); effects.clear()
        assertEquals(24, game.remaining.size)
        assertTrue(effects.active.isEmpty())
        assertTrue(game.match(16, 20))
    }
    @Test fun disabledEffectsAndRecreatedPresentationNeverRetainGhosts() {
        val game = Game(LevelOne.tiles())
        val before = game.tiles
        game.match(16, 20)
        val effects = MatchEffects()
        effects.record(before, game.tiles, 0, true)
        assertEquals(.5f, effects.progress(effects.active.first(), 75), .001f)
        effects.record(game.tiles, game.tiles, 10, false)
        assertTrue(effects.active.isEmpty())
        val restored = Game(LevelOne.tiles())
        assertTrue(restored.restore(game.snapshot()))
        assertEquals(22, restored.remaining.size)
        assertTrue(MatchEffects().active.isEmpty())
    }
}
