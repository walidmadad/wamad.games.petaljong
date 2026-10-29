package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class MysteryOptionCTest {
    private fun game() = Game(listOf(
        Tile(0,Symbol.FLOWER,0.0,0.0,0,mystery=true),
        Tile(1,Symbol.FLOWER,2.0,0.0,0,mystery=true),
        Tile(2,Symbol.FLOWER,4.0,0.0,0), Tile(3,Symbol.FLOWER,6.0,0.0,0),
        Tile(4,Symbol.LEAF,8.0,0.0,0), Tile(5,Symbol.LOTUS,10.0,0.0,0),
        Tile(6,Symbol.FERN,12.0,0.0,0), Tile(7,Symbol.BERRIES,14.0,0.0,0)))
    @Test fun revealSwitchNeverPairsMysteries() {
        val g=game()
        assertTrue(g.interact(0)); assertEquals(0,g.activeMysteryId)
        assertTrue(g.tray.isEmpty()); assertEquals(8,g.remaining.size); assertFalse(g.canUndo)
        assertTrue(g.interact(1)); assertEquals(1,g.activeMysteryId)
        assertTrue(g.tiles.first().concealed); assertEquals(1,g.remaining.count { it.revealed })
        assertTrue(g.snapshot().moves.isEmpty()); assertTrue(g.tray.isEmpty())
        assertTrue(g.interact(1)); assertEquals(1,g.tray.single().id); assertNull(g.activeMysteryId)
        assertTrue(g.interact(0)); assertTrue(g.interact(0))
        assertTrue(g.tray.isEmpty()); assertEquals(1,g.snapshot().moves.size)
    }
    @Test fun trayMatchPreservesOtherSymbolsAndUndoRestoresMembership() {
        val g=game(); g.interact(4); g.interact(2); g.interact(0)
        val before=g.remainingPairs
        assertTrue(g.interact(0)); assertEquals(before-1,g.remainingPairs)
        assertEquals(listOf(4),g.tray.map { it.id }); assertEquals(PairMove(2,0),g.snapshot().moves.single())
        assertTrue(g.undo()); assertTrue(g.isAvailable(0)); assertTrue(g.tiles.first().concealed)
        assertEquals(listOf(4,2),g.tray.map { it.id }); assertEquals(before,g.remainingPairs)
    }
    @Test fun normalDirectMatchAndMismatchAreAtomic() {
        val g=game(); g.interact(5); g.interact(0)
        assertTrue(g.interact(2)); assertEquals(listOf(5),g.tray.map { it.id })
        assertEquals(PairMove(0,2),g.snapshot().moves.single()); assertNull(g.activeMysteryId)
        assertFalse(g.interact(2)); assertEquals(1,g.snapshot().moves.size)
        assertTrue(g.undo()); assertTrue(g.isAvailable(0)); assertTrue(g.isAvailable(2))
        g.interact(0); assertTrue(g.interact(4)); assertEquals(listOf(5,4),g.tray.map { it.id })
        assertTrue(g.tiles.first().concealed); assertTrue(g.isAvailable(0))
    }
    @Test fun collectionUsesOrdinaryTrayAndFourDistinctLose() {
        val g=game(); g.interact(0); g.interact(0); g.interact(2)
        assertTrue(g.tray.isEmpty()); assertEquals(1,g.snapshot().moves.size)
        listOf(4,5,6,7).forEach { assertTrue(g.interact(it)) }; assertEquals(Status.LOST,g.status)
    }
    @Test fun restoreHidesOnlyTemporaryRevealsAndRestartRestoresBacks() {
        val g=game(); g.interact(0); g.interact(2); g.interact(1)
        val snapshot=g.snapshot(); val restored=game(); assertTrue(restored.restore(snapshot))
        assertNull(restored.activeMysteryId); assertEquals(g.snapshot().moves,restored.snapshot().moves)
        assertEquals(g.remaining.map { it.id },restored.remaining.map { it.id })
        restored.restart(); assertEquals(8,restored.remaining.size); assertTrue(restored.tiles.filter { it.mystery }.all { it.concealed })
    }
    @Test fun hiddenSelectionStaysHiddenAcrossSubsequentSaveAndNormalCollection() {
        val g=game(); g.interact(0); g.hideMystery()
        assertTrue(g.interact(2)); assertEquals(2,g.tray.single().id)
        val restored=game(); assertTrue(restored.restore(g.snapshot()))
        assertEquals(g.snapshot(),restored.snapshot()); assertTrue(restored.isAvailable(0))
        assertTrue(restored.snapshot().moves.isEmpty())
    }
    @Test fun directMatchUndoRestoresCoverAndSideBlocking() {
        val g=Game(listOf(Tile(0,Symbol.LEAF,0.0,0.0,1,mystery=true),
            Tile(1,Symbol.LEAF,2.0,0.0,0),Tile(2,Symbol.FLOWER,0.0,0.0,0),
            Tile(3,Symbol.FLOWER,4.0,0.0,0)))
        assertFalse(g.isAvailable(2)); g.interact(0); g.interact(1)
        assertTrue(g.isAvailable(2)); assertTrue(g.undo()); assertFalse(g.isAvailable(2))
        assertTrue(g.tiles.first().concealed); assertTrue(g.tray.isEmpty()); assertEquals(2,g.remainingPairs)
    }
    @Test fun hintCanFinishAnActiveRevealWithoutOscillatingOrExposingAnotherIdentity() {
        val g=game(); g.interact(0)
        val before=g.snapshot(); val hint=HintPolicy.suggest(g.tiles,before,40)
        assertEquals(ActionKind.COLLECT,hint.kind); assertNotNull(hint.id)
        assertFalse(g.tiles.first { it.id==hint.id }.concealed)
        assertEquals(before,g.snapshot()); assertTrue(g.isAvailable(hint.id!!))
    }
    @Test fun oldMultipleRevealsAndPermanentMatchesMigrateWithoutReinterpretation() {
        val old=GameSnapshot(emptyList(),null,listOf(2,0,1),3,listOf(
            GameAction(ActionKind.REVEAL,0),GameAction(ActionKind.REVEAL,1),
            GameAction(ActionKind.COLLECT,2),GameAction(ActionKind.COLLECT,0),GameAction(ActionKind.COLLECT,1)))
        val g=game(); assertTrue(g.restore(old)); assertEquals(PairMove(2,0),g.snapshot().moves.single())
        assertEquals(1,g.tray.single().id); assertNull(g.activeMysteryId)
        val again=game(); assertTrue(again.restore(g.snapshot())); assertEquals(g.snapshot(),again.snapshot())
    }
}
