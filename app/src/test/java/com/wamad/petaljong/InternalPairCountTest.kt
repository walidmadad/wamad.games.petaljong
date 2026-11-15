package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import org.junit.Assert.*
import org.junit.Test

/** Pair accounting remains authoritative even though gameplay no longer presents a counter. */
class InternalPairCountTest {
    private fun game(mystery: Boolean) = Game(listOf(
        Tile(0, Symbol.LEAF, 0.0, 0.0, 0, mystery = mystery),
        Tile(1, Symbol.LEAF, 2.0, 0.0, 0),
        Tile(2, Symbol.FLOWER, 0.0, 2.0, 0),
        Tile(3, Symbol.FLOWER, 2.0, 2.0, 0)))

    @Test fun trayPairUndoRestoreAndVictoryWithoutPresentation() {
        val g = game(false)
        assertEquals(2, g.remainingPairs)
        assertTrue(g.interact(0)); assertEquals(2, g.remainingPairs)
        assertTrue(g.interact(1)); assertEquals(1, g.remainingPairs)
        assertTrue(g.undo()); assertEquals(2, g.remainingPairs)
        assertEquals(listOf(0), g.tray.map { it.id })
        val restored = game(false)
        assertTrue(restored.restore(g.snapshot()))
        assertEquals(2, restored.remainingPairs)
        assertTrue(restored.interact(1)); assertEquals(1, restored.remainingPairs)
        assertTrue(restored.interact(2)); assertTrue(restored.interact(3))
        assertEquals(0, restored.remainingPairs); assertEquals(Status.WON, restored.status)
    }

    @Test fun mysteryRevealAndDirectMatchPreserveAccountingAndUndo() {
        val g = game(true)
        assertTrue(g.interact(0)); assertEquals(2, g.remainingPairs)
        assertTrue(g.interact(1)); assertEquals(1, g.remainingPairs)
        assertTrue(g.tray.isEmpty()); assertEquals(Status.PLAYING, g.status)
        assertTrue(g.undo()); assertEquals(2, g.remainingPairs)
        assertTrue(g.tiles.first().concealed)
        assertTrue(g.interact(0)); assertTrue(g.interact(0))
        assertEquals(2, g.remainingPairs); assertEquals(1, g.tray.size)
        assertTrue(g.interact(1)); assertEquals(1, g.remainingPairs)
        assertTrue(g.interact(2)); assertTrue(g.interact(3))
        assertEquals(Status.WON, g.status); assertEquals(0, g.remainingPairs)
    }
}
