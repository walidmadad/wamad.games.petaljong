package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class GameTest {
    private fun t(id: Int, x: Double = 0.0, y: Double = 0.0, z: Int = 0, s: Symbol = Symbol.LEAF) = Tile(id, s, x, y, z)
    @Test fun openTile() { assertTrue(Game(listOf(t(0))).isAvailable(0)) }
    @Test fun bothSidesBlocked() { assertFalse(Game(listOf(t(0), t(1, -1.0), t(2, 1.0))).isAvailable(0)) }
    @Test fun leftOpen() { assertTrue(Game(listOf(t(0), t(1, 1.0))).isAvailable(0)) }
    @Test fun rightOpen() { assertTrue(Game(listOf(t(0), t(1, -1.0))).isAvailable(0)) }
    @Test fun higherOverlap() { assertFalse(Game(listOf(t(0), t(1, .5, .5, 1))).isAvailable(0)) }
    @Test fun removingBlockersUnlocks() {
        val g = Game(listOf(t(0), t(1, .5, .5, 1), t(2, 3.0, 0.0, 1)))
        assertFalse(g.isAvailable(0)); assertTrue(g.match(1, 2)); assertTrue(g.isAvailable(0))
    }
    @Test fun distantHigherTile() { assertTrue(Game(listOf(t(0), t(1, 1.1, .5, 2))).isAvailable(0)) }
    @Test fun sameSymbolMatch() { val g = Game(listOf(t(0), t(1, 2.0))); assertTrue(g.match(0, 1)); assertEquals(0, g.remaining.size) }
    @Test fun differentSymbolsRejected() { val g = Game(listOf(t(0), t(1, 2.0, s = Symbol.FERN))); val before = g.tiles; assertFalse(g.match(0, 1)); assertEquals(before, g.tiles) }
    @Test fun blockedMatchRejected() { val g = Game(listOf(t(0), t(1, -1.0), t(2, 1.0))); assertFalse(g.match(0, 1)) }
    @Test fun duplicateRemovalRejected() { val g = Game(listOf(t(0), t(1, 2.0))); g.match(0, 1); assertFalse(g.match(0, 1)); assertEquals(0, g.remaining.size) }
    @Test fun selfMatchRejected() { assertFalse(Game(listOf(t(0))).match(0, 0)) }
    @Test fun countDropsByTwo() { val g = Game(LevelOne.tiles()); assertTrue(g.match(16, 20)); assertEquals(22, g.remaining.size); assertEquals(11, g.remainingPairs) }
    @Test fun emptyIsWon() { assertEquals(Status.WON, Game(emptyList()).status) }
    @Test fun unmatchedFreeTilesDeadlock() { assertEquals(Status.PLAYING, Game(listOf(t(0), t(1, 2.0, s = Symbol.LOTUS))).status) }
    @Test fun restartRestoresEverything() { val g = Game(LevelOne.tiles()); val initial = g.tiles; g.match(16, 20); g.select(17); g.restart(); assertEquals(initial, g.tiles); assertNull(g.selected); assertEquals(12, g.remainingPairs); assertEquals(Status.PLAYING, g.status) }
    @Test fun uniqueIdentifiers() { assertEquals(24, LevelOne.tiles().map { it.id }.distinct().size) }
    @Test fun symbolDistribution() { assertEquals(24, LevelOne.tiles().size); assertEquals(List(6) { 4 }, LevelOne.tiles().groupBy { it.symbol }.values.map { it.size }) }
    @Test fun fullWitness() {
        val g = Game(LevelOne.tiles()); assertFalse(g.isAvailable(0)); var matches = 0
        for (p in LevelOne.witness) {
            assertEquals(g.tiles.first { it.id == p.first }.symbol, g.tiles.first { it.id == p.second }.symbol)
            assertTrue(g.isAvailable(p.first)); assertTrue(g.isAvailable(p.second)); assertTrue(g.match(p.first, p.second)); matches++
        }
        assertEquals(12, matches); assertTrue(g.remaining.isEmpty()); assertEquals(Status.WON, g.status)
    }
    @Test fun invalidActionsPreserveSelectionAndBoard() { val g = Game(LevelOne.tiles()); g.select(16); val before = g.tiles; for (p in listOf(-1 to 16, 16 to 16, 0 to 16, 16 to 17)) assertFalse(g.match(p.first, p.second)); assertEquals(before, g.tiles); assertEquals(listOf(16), g.tray.map { it.id }) }
    @Test fun halfOffsetOverlap() { assertTrue(Rules.overlaps(t(0), t(1, .5, .5))) }
    @Test fun edgeContactNotOverlap() { assertFalse(Rules.overlaps(t(0), t(1, 1.0, 0.0))) }
    @Test fun cornerContactNotOverlapOrNeighbor() { val a = t(0); val b = t(1, 1.0, 1.0); assertFalse(Rules.overlaps(a, b)); assertTrue(Game(listOf(a, b, t(2, -1.0))).isAvailable(0)) }
    @Test fun higherEdgeDoesNotBlock() { assertTrue(Game(listOf(t(0), t(1, 1.0, 0.0, 1))).isAvailable(0)) }
    @Test fun fractionalLateralNeighborsBlock() { assertFalse(Game(listOf(t(0), t(1, -1.0, -.5), t(2, 1.0, .5))).isAvailable(0)) }
    @Test fun nonAdjacentDoesNotBlock() { assertTrue(Game(listOf(t(0), t(1, -1.01), t(2, 1.0))).isAvailable(0)) }
    @Test fun enumerationUsesOnlyLegalDistinctPairs() { val g = Game(listOf(t(0), t(1, 2.0), t(2, 4.0))); assertEquals(setOf(PairMove(0,1), PairMove(0,2), PairMove(1,2)), g.matchingPairs().toSet()) }
    @Test fun statusTransitionPriority() { val g = Game(listOf(t(0), t(1, 2.0))); assertEquals(Status.PLAYING, g.status); g.match(0, 1); assertTrue(g.matchingPairs().isEmpty()); assertEquals(Status.WON, g.status) }
    @Test fun selectionBehavior() {
        val g = Game(listOf(t(0),t(1,2.0,s=Symbol.LOTUS),t(2,4.0,s=Symbol.LOTUS)))
        assertTrue(g.select(0)); assertFalse(g.select(0)); assertTrue(g.select(1)); assertTrue(g.select(2))
        assertEquals(listOf(0),g.tray.map{it.id}); assertEquals(Status.LOST,g.status)
    }
    @Test fun everyHigherBlockerMustBeRemoved() { val g = Game(listOf(t(0), t(1,.1,.1,1), t(2,3.0,0.0,1), t(3,.2,.2,2,Symbol.FERN), t(4,5.0,0.0,2,Symbol.FERN))); assertTrue(g.match(3,4)); assertFalse(g.isAvailable(0)); assertTrue(g.match(1,2)); assertTrue(g.isAvailable(0)) }
    @Test fun epsilonBoundary() { assertFalse(Rules.overlaps(t(0), t(1, 1.0 - Rules.EPS / 2))); assertTrue(Rules.overlaps(t(0), t(1, 1.0 - Rules.EPS * 2))) }
}
