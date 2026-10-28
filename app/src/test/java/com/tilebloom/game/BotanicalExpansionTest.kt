package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class BotanicalExpansionTest {
    @Test fun catalogAndIntroductionsAreStableAndPaired() {
        assertEquals((0..15).toList(), Symbol.entries.take(16).map { it.stableId })
        assertEquals(Symbol.entries.size, Symbol.entries.map { it.stableId }.distinct().size)
        val additions = listOf(Symbol.CACTUS, Symbol.LAVENDER, Symbol.LADYBUG, Symbol.SNAIL)
        assertEquals(listOf(6L,11L,21L,41L), additions.map { it.introducedAt })
        for (symbol in additions) {
            assertFalse(symbol in DifficultyPolicy.symbols(symbol.introducedAt - 1))
            for (id in listOf(symbol.introducedAt, symbol.introducedAt + 1, 121L)) {
                val level = Levels.get(id)
                assertEquals(level, Levels.get(id))
                assertTrue(level.tiles.count { it.symbol == symbol } >= 2)
                val game = Game(level.tiles)
                level.witness.forEach { pair ->
                    assertTrue(game.isAvailable(pair.first)); assertTrue(game.select(pair.first))
                    assertTrue(game.isAvailable(pair.second)); assertTrue(game.select(pair.second))
                    assertTrue(game.tray.isEmpty()); assertNotEquals(Status.LOST, game.status)
                }
                assertEquals(Status.WON, game.status); assertTrue(game.remaining.isEmpty())
            }
        }
        assertTrue((1L..5).all { id -> Levels.get(id).tiles.all { it.symbol.stableId < 6 } })
    }
    @Test fun historicalVersionFiveAndCurrentSavesRestoreTrayAndCreditsExactly() {
        for (id in listOf(6L,11L,21L,41L,101L)) for (level in listOf(VersionFiveGenerator.generate(id), Levels.get(id))) {
            val game = Game(level.tiles)
            assertTrue(game.select(level.witness.first().first))
            val saved = PlayerProgress(active = SessionSnapshot(id, level.seed, level.version, game.snapshot()),
                highestCompleted = id - 1, undoCredits = 2, hintCredits = 1)
            val restored = ProgressController(ProgressCodec.decode(ProgressCodec.encode(saved)))
            assertFalse(restored.recovered); assertEquals(level, restored.session.level)
            assertEquals(game.snapshot(), restored.session.game.snapshot()); assertEquals(game.tray, restored.session.game.tray)
            assertEquals(2, restored.wallet.balance); assertEquals(1, restored.hintCredits)
            assertTrue(restored.undo()); assertTrue(restored.session.game.tray.isEmpty())
            assertEquals(1, restored.wallet.balance)
        }
    }
    @Test fun newSymbolsSupportHintMatchingAndUndo() {
        for (symbol in Symbol.entries.filter { it.stableId >= 16 }) {
            val level = Levels.get(symbol.introducedAt)
            val game = Game(level.tiles)
            for (pair in level.witness) {
                if (level.tiles.first { it.id == pair.first }.symbol == symbol) {
                    val before = game.snapshot()
                    assertTrue(game.select(pair.first)); assertEquals(symbol, game.tray.single().symbol)
                    assertEquals(pair.second, HintPolicy.suggest(level.tiles, game.snapshot())?.id)
                    assertTrue(game.select(pair.second)); assertTrue(game.tray.isEmpty())
                    assertTrue(game.undo()); assertEquals(symbol, game.tray.single().symbol)
                    assertTrue(game.undo()); assertEquals(before, game.snapshot())
                    break
                }
                assertTrue(game.select(pair.first)); assertTrue(game.select(pair.second))
            }
        }
    }
    @Test fun handoverBlocksInputImmediatelyWithoutFlashingOrExtendingTheDeadline() {
        val state = ConnectionState()
        state.update(true, 0); assertTrue(state.online); assertFalse(state.panelVisible(0))
        state.update(false, 100); assertFalse(state.online); assertFalse(state.panelVisible(100))
        state.update(false, 400); assertEquals(300L, state.panelDelay(400))
        state.update(true, 500); assertTrue(state.online); assertFalse(state.panelVisible(800))
        state.update(false, 1000); state.update(false, 1500)
        assertFalse(state.panelVisible(1599)); assertTrue(state.panelVisible(1600))
        state.update(false, 1700); assertTrue(state.panelVisible(1700))
        state.update(true, 1800); assertTrue(state.online); assertFalse(state.panelVisible(1800))
    }
    @Test fun connectivityRequiresBothCapabilitiesRegardlessOfTransport() {
        assertTrue(InternetPolicy.validated(true, true)) // Wi-Fi or cellular: no transport whitelist.
        assertFalse(InternetPolicy.validated(true, false)) // Captive/unvalidated Wi-Fi.
        assertFalse(InternetPolicy.validated(false, false))
        assertFalse(InternetPolicy.validated(false, true))
    }
}
