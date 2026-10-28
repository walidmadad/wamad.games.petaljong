package com.tilebloom.game
import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test
class TrayTest {
    private fun fixture() = Symbol.entries.flatMapIndexed { i, s -> listOf(Tile(i*2,s,i*3.0,0.0,0),Tile(i*2+1,s,i*3.0,2.0,0)) }
    @Test fun slotsMatchingOrderingAndDuplicateProtection() {
        val g=Game(fixture()); assertEquals(List<Tile?>(4){null},g.traySlots)
        assertTrue(g.select(0)); assertFalse(g.select(0)); assertFalse(g.isAvailable(0))
        g.select(2); g.select(4); assertEquals(Status.PLAYING,g.status)
        g.select(1); assertEquals(listOf(2,4),g.tray.map{it.id}); assertEquals(1,g.snapshot().moves.size)
        assertTrue(g.undo()); assertEquals(listOf(0,2,4),g.tray.map{it.id}); assertTrue(g.isAvailable(1))
    }
    @Test fun defeatUndoWalletPersistenceAndRestart() {
        val g=Game(fixture()); val wallet=UndoCreditWallet(3)
        listOf(0,2,4,6).forEach{assertTrue(g.select(it))}
        assertEquals(Status.LOST,g.status); assertEquals(4,g.tray.size); assertFalse(g.select(8))
        val restored=Game(fixture()); assertTrue(restored.restore(g.snapshot())); assertEquals(g.tray,restored.tray)
        assertTrue(wallet.undo{g.undo()}); assertEquals(2,wallet.balance); assertEquals(Status.PLAYING,g.status)
        g.restart(); assertTrue(g.tray.isEmpty()); assertFalse(wallet.undo{g.undo()}); assertEquals(2,wallet.balance)
    }
    @Test fun singleRemovalUnlocksAndCoveredCannotTransfer() {
        val g=Game(listOf(Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.FERN,0.0,0.0,1)))
        assertFalse(g.select(0)); assertTrue(g.select(1)); assertTrue(g.isAvailable(0)); assertTrue(g.select(0))
        assertEquals(Status.LOST,g.status)
    }
    @Test fun generatedWitnessesAndAllSymbols() {
        for(id in listOf(1L,6L,21L,41L,81L,101L,1001L)) {
            val l=Levels.get(id); val g=Game(l.tiles)
            l.witness.forEach{ assertTrue(g.select(it.first)); assertTrue(g.select(it.second)); assertTrue(g.tray.isEmpty()) }
            assertEquals(Status.WON,g.status)
        }
        val g=Game(fixture()); fixture().forEach{ assertTrue(g.select(it.id)) }; assertEquals(Status.WON,g.status)
    }
    @Test fun codecAndLegacyMigration() {
        val c=ProgressController(); c.session.game.select(c.session.level.witness.first().first)
        val p=c.capture(); assertEquals(p,ProgressController(ProgressCodec.decode(ProgressCodec.encode(p))).capture())
        val l=Levels.get(1); val pair=l.witness.first()
        val old="3\n0\n1,${l.seed},${l.version}\n${pair.first},${pair.second}\n2"
        val migrated=ProgressController(ProgressCodec.decode(old)); assertEquals(2,migrated.wallet.balance)
        assertEquals(2,migrated.session.game.snapshot().selections!!.size); assertTrue(migrated.session.game.tray.isEmpty())
    }
    @Test fun analysisBudgetDoesNotProveLoss() {
        val g=Game(fixture()); assertEquals(Solvability.UNKNOWN_BUDGET_EXCEEDED,TrayAnalyzer.analyze(fixture(),g.snapshot(),0).result.outcome)
        assertEquals(Status.PLAYING,g.status)
    }
}
