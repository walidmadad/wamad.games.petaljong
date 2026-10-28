package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class GameReliabilityTest {
    private fun assertInitial(g: Game) {
        assertEquals(LevelOne.tiles(),g.tiles); assertEquals(12,g.remainingPairs)
        assertNull(g.selected); assertEquals(Status.PLAYING,g.status); assertTrue(g.snapshot().moves.isEmpty())
    }
    @Test fun repeatedRestartAfterOneAndSeveralMatchesAndSelection() {
        val g = Game(LevelOne.tiles())
        repeat(15) {
            for (p in LevelOne.witness.take(1 + it % 4)) assertTrue(g.match(p.first,p.second))
            g.remaining.firstOrNull { g.isAvailable(it.id) }?.let { g.select(it.id) }
            g.restart(); g.restart(); assertInitial(g)
        }
    }
    @Test fun restartAfterVictory() {
        val g = Game(LevelOne.tiles()); for (p in LevelOne.witness) assertTrue(g.match(p.first,p.second))
        assertEquals(Status.WON,g.status); g.restart(); assertInitial(g)
    }
    @Test fun fixedLevelDeadlockRestoresAndRestarts() {
        val g = Game(LevelOne.tiles())
        val moves = listOf(PairMove(12,15),PairMove(14,23),PairMove(16,20),PairMove(13,21),PairMove(0,4))
        for (p in moves) assertTrue(g.match(p.first,p.second))
        assertEquals(14,g.remaining.size); assertEquals(Status.PLAYING,g.status)
        val restored = Game(LevelOne.tiles()); assertTrue(restored.restore(g.snapshot()))
        assertEquals(g.tiles,restored.tiles); assertEquals(Status.PLAYING,restored.status)
        repeat(5) { restored.restart(); assertInitial(restored) }
    }
    @Test fun restartAfterDeadlock() {
        val definition = listOf(Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.LEAF,2.0,0.0,0),Tile(2,Symbol.FERN,4.0,0.0,0))
        val g = Game(definition); assertTrue(g.match(0,1)); assertEquals(Status.PLAYING,g.status)
        repeat(5) { g.restart(); assertEquals(definition,g.tiles); assertNull(g.selected); assertEquals(Status.PLAYING,g.status) }
    }
    @Test fun roundTripEveryWitnessPrefixAndSelection() {
        val g = Game(LevelOne.tiles())
        for (step in 0..12) {
            if (step > 0) LevelOne.witness[step-1].let { assertTrue(g.match(it.first,it.second)) }
            val restored = Game(LevelOne.tiles()); assertTrue(restored.restore(g.snapshot()))
            assertEquals(g.tiles,restored.tiles); assertEquals(g.selected,restored.selected)
            assertEquals(g.status,restored.status); assertEquals(g.snapshot(),restored.snapshot())
        }
    }
    @Test fun historyTracksExactPlayerChoicesThroughSelect() {
        val g = Game(LevelOne.tiles()); g.select(20); g.select(16); g.select(17)
        assertEquals(listOf(20,16,17),g.snapshot().selections)
        val restored = Game(LevelOne.tiles()); assertTrue(restored.restore(g.snapshot())); assertEquals(g.snapshot(),restored.snapshot())
    }
    @Test fun corruptSnapshotIsRejectedAtomically() {
        val g=Game(LevelOne.tiles()); g.select(16); val before=g.snapshot()
        for(ids in listOf(listOf(16,16),listOf(-1),listOf(0))) {
            assertFalse(g.restore(GameSnapshot(emptyList(),null,ids))); assertEquals(before,g.snapshot())
        }
    }
    @Test fun savedMoveCollectionCannotCorruptGame() {
        val g = Game(LevelOne.tiles()); g.match(16,20); g.match(17,21)
        val saved = g.snapshot(); (saved.moves as? MutableList<PairMove>)?.clear()
        assertEquals(2,g.snapshot().moves.size); assertEquals(20,g.remaining.size)
    }
    @Test fun blockedTapPreservesExistingSelection() {
        val g = Game(LevelOne.tiles()); g.select(16); g.select(1)
        assertNull(g.selected); assertEquals(23,g.remaining.size)
    }
    @Test fun repeatedRemovedAndInvalidSelectionsCannotReplayMatch() {
        val g = Game(LevelOne.tiles())
        g.select(16); g.select(20)
        g.select(17)
        val expected = g.snapshot()
        repeat(100) { g.select(16); g.select(20); g.select(-1) }
        assertEquals(expected,g.snapshot()); assertEquals(21,g.remaining.size)
        g.restart(); g.select(16); g.select(20)
        assertEquals(listOf(PairMove(16,20)),g.snapshot().moves)
        assertEquals(22,g.remaining.size); assertNull(g.selected)
    }
}
