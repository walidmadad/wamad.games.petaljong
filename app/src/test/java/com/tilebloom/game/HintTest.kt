package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class HintTest {
    @Test fun migrationAndZeroAreStable() {
        val p = ProgressController()
        val before = p.capture().copy(undoCredits = 0)
        val legacy = ProgressCodec.encode(before).split('\n').take(5).toMutableList().also { it[0] = "4" }.joinToString("\n")
        val migrated = ProgressCodec.decode(legacy)
        assertEquals(3, migrated.hintCredits)
        assertEquals(0, migrated.undoCredits)
        assertEquals(before.active, migrated.active)
        assertEquals(before.frontier, migrated.frontier)
        var zero = migrated.copy(hintCredits = 0)
        repeat(4) { zero = ProgressCodec.decode(ProgressCodec.encode(zero)) }
        assertEquals(0, ProgressController(zero).hintCredits)
    }
    @Test fun chargesOnlyAcceptedHintsAndNeverRefills() {
        val p = ProgressController()
        val snapshot = p.session.game.snapshot()
        assertFalse(p.acceptHint(-1, snapshot))
        assertEquals(3, p.hintCredits)
        repeat(3) { n ->
            val suggestion = HintPolicy.suggest(p.session.level.tiles, snapshot)
            assertTrue(p.acceptHint(suggestion.id!!, snapshot))
            assertEquals(2-n, p.hintCredits)
            assertEquals(snapshot, p.session.game.snapshot())
            p.session.restart()
        }
        assertFalse(p.acceptHint(HintPolicy.suggest(p.session.level.tiles, snapshot).id!!, snapshot))
        assertEquals(0, ProgressController(ProgressCodec.decode(ProgressCodec.encode(p.capture()))).hintCredits)
    }
    @Test fun trayMatchAndCoveredRemovedSafety() {
        val tiles = listOf(Tile(0, Symbol.LEAF, 0.0, 0.0, 0), Tile(1, Symbol.LEAF, 0.0, 0.0, 1),
            Tile(2, Symbol.FERN, 2.0, 0.0, 0), Tile(3, Symbol.FERN, 4.0, 0.0, 0))
        val g = Game(tiles)
        assertEquals(1, HintPolicy.suggest(tiles, g.snapshot(), 0).id)
        g.select(1)
        val before = g.snapshot()
        val hint = HintPolicy.suggest(tiles, before)
        assertEquals(0, hint.id)
        assertEquals(before, g.snapshot())
        assertEquals(hint, HintPolicy.suggest(tiles, before))
    }
    @Test fun budgetsAndGeneratedSymbols() {
        for (id in listOf(1L, 6L, 21L, 41L, 81L, 101L)) {
            val level = Levels.get(id)
            val g = Game(level.tiles)
            val hint = HintPolicy.suggest(level.tiles, g.snapshot(), 0)
            assertEquals(0, hint.nodes)
            assertNotEquals(Solvability.SOLVABLE, hint.outcome)
            assertTrue(g.isAvailable(hint.id!!))
            assertTrue(if (hint.kind == ActionKind.REVEAL) g.reveal(hint.id) else g.select(hint.id))
            assertNotEquals(Status.LOST, g.status)
        }
    }
    @Test fun staleRequestsAndTerminalBoardsCannotCharge() {
        val p = ProgressController()
        val old = p.session.game.snapshot()
        val id = HintPolicy.suggest(p.session.level.tiles, old).id!!
        assertTrue(p.session.game.select(id))
        assertFalse(p.acceptHint(id, old))
        assertEquals(3, p.hintCredits)
        assertEquals(null, HintPolicy.suggest(emptyList(), GameSnapshot(emptyList(), null)).id)
    }
    @Test fun migrationPreservesTrayHistoryAndAdvancementKeepsBalance() {
        val p = ProgressController()
        val g = p.session.game
        val id = HintPolicy.suggest(p.session.level.tiles, g.snapshot()).id!!
        assertTrue(p.acceptHint(id, g.snapshot()))
        assertTrue(g.select(id))
        val saved = p.capture()
        val raw = ProgressCodec.encode(saved).split('\n').take(5).toMutableList().also { it[0] = "4" }.joinToString("\n")
        val migrated = ProgressController(ProgressCodec.decode(raw))
        assertEquals(g.tray, migrated.session.game.tray)
        assertEquals(g.snapshot(), migrated.session.game.snapshot())
        assertEquals(saved.active!!.seed, migrated.capture().active!!.seed)
        assertEquals(saved.active.version, migrated.capture().active!!.version)
        p.session.restart()
        for (pair in p.session.level.witness) assertTrue(g.match(pair.first, pair.second))
        assertTrue(p.advance())
        assertEquals(2, p.hintCredits)
        assertEquals(1L, p.capture().frontier)
        assertEquals(2, ProgressController(p.capture()).hintCredits)
    }
    @Test fun avoidsFourthDistinctSymbol() {
        val tiles = (0..7).map { Tile(it, Symbol.entries[it % 4], it * 2.0, 0.0, 0) }
        val g = Game(tiles)
        listOf(0,1,2).forEach { assertTrue(g.select(it)) }
        val suggestion = HintPolicy.suggest(tiles, g.snapshot())
        assertTrue(suggestion.id in listOf(4,5,6))
        g.select(suggestion.id!!)
        assertEquals(2, g.tray.size)
    }
}
