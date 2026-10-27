package com.tilebloom.game

import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.MatchEffects
import org.junit.Assert.*
import org.junit.Test

class UndoTest {
    private fun play(g: Game, p: PairMove) { assertTrue(g.match(p.first, p.second)) }
    @Test fun initialAndRejectedMovesHaveNoUndo() {
        val g = Game(Levels.get(1).tiles)
        val before = g.snapshot()
        assertFalse(g.canUndo); assertFalse(g.undo())
        assertFalse(g.match(-1, 0)); assertFalse(g.match(0, 0))
        assertFalse(g.canUndo); assertEquals(before, g.snapshot())
    }
    @Test fun everyLayoutRestoresExactTilesGeometryBlockingAndPairs() {
        for (id in 1..5) {
            val l = Levels.get(id); val original = l.tiles.toList(); val g = Game(l.tiles)
            val states = mutableListOf(g.tiles)
            val pairs = mutableListOf(g.matchingPairs())
            val availability = mutableListOf(g.tiles.map { g.isAvailable(it.id) })
            l.witness.dropLast(1).forEach {
                play(g, it); states.add(g.tiles); pairs.add(g.matchingPairs())
                availability.add(g.tiles.map { t -> g.isAvailable(t.id) })
            }
            for (n in states.lastIndex downTo 1) {
                val remaining = g.remaining.size
                assertTrue(g.undo())
                assertEquals(remaining + 2, g.remaining.size)
                assertEquals(states[n-1], g.tiles)
                assertEquals(pairs[n-1], g.matchingPairs())
                assertEquals(availability[n-1], g.tiles.map { g.isAvailable(it.id) })
                assertEquals(l.tiles.size/2 - n + 1, g.remainingPairs)
                assertEquals(g.tiles.size, g.tiles.map { it.id }.toSet().size)
            }
            assertFalse(g.undo()); assertEquals(original, l.tiles); assertEquals(original, g.tiles)
        }
    }
    @Test fun selectedTileClearedOnUndo() {
        val l=Levels.get(1); val g=Game(l.tiles); play(g,l.witness.first())
        g.select(g.matchingPairs().first().first); assertNotNull(g.selected)
        assertTrue(g.undo()); assertNull(g.selected)
        g.select(l.witness.first().first); assertEquals(l.witness.first().first,g.selected)
    }
    @Test fun deadlockRecoversWithoutRegeneration() {
        val s=GardenSession(); val identity=s.snapshot()
        deadlockPath(s.game).forEach { play(s.game,it) }
        assertEquals(Status.NO_MOVES,s.game.status)
        assertTrue(s.game.undo()); assertEquals(Status.PLAYING,s.game.status)
        assertEquals(identity.copy(game=s.game.snapshot()),s.snapshot())
    }
    @Test fun durableHistoryTruncatesAndStaysTruncatedAfterSecondReload() {
        val c=ProgressController(PlayerProgress(setOf(1,2))); assertTrue(c.select(3))
        c.session.level.witness.take(5).forEach { play(c.session.game,it) }
        fun reload(p: PlayerProgress) = ProgressController(ProgressCodec.decode(ProgressCodec.encode(p)))
        val before=c.capture(); val restored=reload(before)
        assertTrue(restored.session.game.undo())
        val after=restored.capture(); val s=before.active!!
        assertEquals(s.copy(game=GameSnapshot(s.game.moves.dropLast(1),null)),after.active)
        assertEquals(before.completed,after.completed); assertEquals(3,restored.highestUnlocked)
        val again=reload(after); assertEquals(after,again.capture()); assertTrue(again.session.game.canUndo)
    }
    @Test fun restartReplayAndNavigationCannotUndoPreviousAttempt() {
        val c=ProgressController(); val s=c.session
        play(s.game,s.level.witness.first()); s.restart(); assertFalse(s.game.canUndo)
        s.level.witness.forEach { play(s.game,it) }; c.capture()
        assertFalse(s.game.canUndo); assertFalse(s.game.undo())
        s.restart(); assertFalse(s.game.undo()); assertEquals(setOf(1),c.capture().completed)
        play(s.game,s.level.witness.first()); assertTrue(c.select(2)); assertFalse(s.game.undo())
        s.level.witness.forEach { play(s.game,it) }; c.capture(); assertTrue(s.advance()); assertFalse(s.game.undo())
    }
    @Test fun invalidReplayLeavesValidBoardAndUndoHistoryUntouched() {
        val l=Levels.get(1); val g=Game(l.tiles); play(g,l.witness.first())
        val before=g.snapshot(); val tiles=g.tiles
        assertFalse(g.restore(GameSnapshot(before.moves + PairMove(-1,999),null)))
        assertEquals(before,g.snapshot()); assertEquals(tiles,g.tiles)
        assertTrue(g.undo()); assertEquals(l.tiles,g.tiles)
    }
    @Test fun rapidUndoDuringEffectsDoesNotDuplicateTilesOrMoves() {
        val l=Levels.get(1); val g=Game(l.tiles); val effects=MatchEffects()
        repeat(100) {
            val before=g.tiles; play(g,l.witness.first()); effects.record(before,g.tiles,0,true)
            assertEquals(2,effects.active.size)
            assertTrue(g.undo()); effects.clear()
            repeat(5) { assertFalse(g.undo()) }
            assertTrue(effects.active.isEmpty()); assertEquals(l.tiles,g.tiles); assertTrue(g.snapshot().moves.isEmpty())
        }
    }
    @Test fun witnessesAndDeterministicIntroductoryIdentityRemainValid() {
        for (id in 1..5) { val l=Levels.get(id); LevelValidator.validate(l) }
        val l=Levels.get(1)
        assertEquals(3,l.version); assertEquals(123L,l.seed); assertEquals("winding",l.layout.id)
        assertEquals(l.tiles,LevelGenerator.generate(1,123L,Layouts.winding,1).tiles)
    }
    @Test fun measureWarmUndoReplay() {
        val l=Levels.get(5); val g=Game(l.tiles)
        val prefix=l.witness.dropLast(1)
        val start=System.nanoTime()
        repeat(1000) { assertTrue(g.restore(GameSnapshot(prefix,null))); assertTrue(g.undo()) }
        println("UNDO benchmark: 1000 restore+undo cycles, ms="+(System.nanoTime()-start)/1_000_000.0)
    }
}
