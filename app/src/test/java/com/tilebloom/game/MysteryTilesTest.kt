package com.tilebloom.game

import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.MysteryRevealFeedback
import org.junit.Assert.*
import org.junit.Test

class MysteryTilesTest {
    private fun tiles() = listOf(Tile(0, Symbol.LEAF, 0.0, 0.0, 0, mystery = true),
        Tile(1, Symbol.LEAF, 2.0, 0.0, 0), Tile(2, Symbol.VEINED_LEAF, 4.0, 0.0, 0),
        Tile(3, Symbol.VEINED_LEAF, 6.0, 0.0, 0))
    @Test fun revealIsInformationOnlyThenCollectionUsesExactIdentity() {
        val g = Game(tiles()); val before = g.snapshot()
        assertFalse(g.select(0)); assertTrue(g.interact(0)); assertFalse(g.reveal(0))
        assertEquals(4, g.remaining.size); assertTrue(g.tray.isEmpty()); assertEquals(before.moves, g.snapshot().moves)
        assertTrue(g.snapshot().selections!!.isEmpty()); assertFalse(g.canUndo); assertEquals(Status.PLAYING, g.status)
        assertFalse(g.tiles.first().concealed); assertTrue(g.interact(0)); assertEquals(0, g.tray.single().id)
        assertTrue(g.select(2)); assertEquals(2, g.tray.size) // Similar leaves never match.
        assertTrue(g.select(1)); assertEquals(Symbol.VEINED_LEAF, g.tray.single().symbol)
        assertTrue(g.select(3)); assertEquals(Status.WON, g.status)
    }
    @Test fun revealCannotOverflowAFullPressureTrayOrClearAPair() {
        val definition = tiles() + listOf(Tile(4, Symbol.FLOWER, 8.0, 0.0, 0), Tile(5, Symbol.LOTUS, 10.0, 0.0, 0))
        val g = Game(definition); listOf(2,4,5).forEach { assertTrue(g.select(it)) }
        val tray = g.tray; assertTrue(g.reveal(0)); assertEquals(tray, g.tray)
        assertEquals(Status.PLAYING, g.status); assertTrue(g.snapshot().moves.isEmpty())
    }
    @Test fun sameGeometryBlocksRevelationAndCollection() {
        val covered = Game(tiles() + Tile(4,Symbol.FLOWER,0.0,0.0,1))
        assertFalse(covered.reveal(0)); assertFalse(covered.interact(0)); assertFalse(covered.select(0))
        val lateral = Game(listOf(Tile(0,Symbol.LEAF,1.0,0.0,0,mystery=true),
            Tile(1,Symbol.FLOWER,0.0,0.0,0), Tile(2,Symbol.LOTUS,2.0,0.0,0)))
        assertFalse(lateral.reveal(0)); assertTrue(lateral.select(1)); assertTrue(lateral.reveal(0))
    }
    @Test fun savesReplayOrderedRevealsAndUndoRestoresBeforeLastCollection() {
        val g = Game(tiles()); assertTrue(g.reveal(0)); val revealed = g.snapshot()
        assertTrue(g.select(0)); assertTrue(g.select(2)); assertTrue(g.undo()); assertTrue(g.undo())
        assertEquals(revealed,g.snapshot()); assertTrue(g.tiles.first().revealed)
        val restored = Game(tiles()); assertTrue(restored.restore(g.snapshot())); assertEquals(g.tiles,restored.tiles)
        g.restart(); assertTrue(g.tiles.first().concealed); assertTrue(g.snapshot().actionLog!!.isEmpty())
        val invalid = revealed.copy(actionLog = revealed.actionLog!! + GameAction(ActionKind.REVEAL,0))
        assertFalse(restored.restore(invalid)); assertEquals(revealed,restored.snapshot())
    }
    @Test fun rapidTapGuardIsIndependentOfAnimationAndLifecycle() {
        val f = MysteryRevealFeedback(); f.revealed(0,1000)
        assertFalse(f.accepts(0,1000)); assertFalse(f.accepts(0,1300)); assertTrue(f.accepts(0,1420))
        assertEquals(null,f.progress(0,1100,false)); assertFalse(f.accepts(0,1100))
        assertTrue(f.progress(0,1130,true)!! > 0f); assertFalse(f.active(1260,true))
        f.clear(); assertTrue(f.accepts(0,1100))
    }
    @Test fun persistedGameAndWalletsSurviveNewSchemaAndOldV9() {
        val l = Levels.get(12); val g = Game(l.tiles)
        val target = l.tiles.first { it.concealed && g.isAvailable(it.id) }
        assertTrue(g.reveal(target.id))
        val saved = PlayerProgress(active=SessionSnapshot(l.id,l.seed,l.version,g.snapshot()),highestCompleted=11,undoCredits=2,hintCredits=1)
        val restored=ProgressController(ProgressCodec.decode(ProgressCodec.encode(saved)))
        assertFalse(restored.recovered); assertEquals(g.snapshot(),restored.session.game.snapshot())
        assertEquals(2,restored.wallet.balance); assertEquals(1,restored.hintCredits)
        assertFalse(restored.undo()); assertEquals(2,restored.wallet.balance)
        val old = VersionNineGenerator.generate(12); val oldGame=Game(old.tiles)
        assertTrue(oldGame.select(old.winningSelections.first()))
        val raw="5\n11\n12,${old.seed},9,2\n${oldGame.snapshot().selections!!.joinToString(";")}\n2\n1"
        val legacy=ProgressController(ProgressCodec.decode(raw)); assertFalse(legacy.recovered)
        assertEquals(old,legacy.session.level); assertEquals(oldGame.snapshot(),legacy.session.game.snapshot())
    }
    @Test fun hintsRecommendRevealWithoutDoingItOrSpendingUndo() {
        val l=Levels.get(12); val saved=PlayerProgress(active=SessionSnapshot(l.id,l.seed,l.version,Game(l.tiles).snapshot()),highestCompleted=11)
        val p=ProgressController(saved); val before=p.session.game.snapshot()
        val hint=HintPolicy.suggest(l.tiles,before,20); assertEquals(ActionKind.REVEAL,hint.kind)
        assertTrue(p.acceptHint(hint.id!!,before)); assertEquals(before,p.session.game.snapshot())
        assertEquals(2,p.hintCredits); assertEquals(3,p.wallet.balance)
        assertTrue(p.session.game.interact(hint.id)); assertEquals(2,p.hintCredits); assertEquals(3,p.wallet.balance)
    }
    @Test fun deterministicMysteriesFamilyMixingAndCompleteWitnesses() {
        for (id in listOf(7L,8,16,31,51,76,101,151,1001)) {
            val l=Levels.get(id); LevelValidator.validate(l)
            assertEquals(l,EndlessGenerator.generate(id))
            assertTrue(l.tiles.groupingBy { it.symbol }.eachCount().values.all { it%2==0 })
            assertEquals(l.tiles.size,l.tiles.map { it.id }.distinct().size)
            if(id<=7) assertFalse(l.tiles.any { it.mystery }) else {
                assertTrue(l.tiles.count { it.mystery } in 2..10)
                assertTrue(l.tiles.map { it.symbol }.containsAll(SymbolFamilies.required(id)))
                assertTrue(SymbolFamilies.mixedCount(l.tiles) >= if(id>=31) 3 else if(id>=16) 2 else 1)
                assertEquals(l.tiles.count { it.mystery },l.winningActions.count { it.kind==ActionKind.REVEAL })
            }
            val g=Game(l.tiles)
            l.winningActions.forEach { a ->
                val tray=g.tray; val size=g.remaining.size
                assertTrue(if(a.kind==ActionKind.REVEAL) g.reveal(a.tileId) else g.select(a.tileId))
                if(a.kind==ActionKind.REVEAL) { assertEquals(tray,g.tray); assertEquals(size,g.remaining.size) }
                assertNotEquals(Status.LOST,g.status)
            }
            assertEquals(Status.WON,g.status); assertTrue(g.tray.isEmpty())
        }
    }
    @Test fun expertWindowsEvolveAndSaturateWithoutOverflow() {
        val targets=listOf(31L,51,76,101,126,151,501).map { ExpertProgression.targets(it) }
        assertTrue(targets.zipWithNext().all { (a,b) -> b.delayed>a.delayed && b.pressure>a.pressure })
        assertEquals(18,ExpertProgression.targets(Long.MAX_VALUE).delayed)
        assertEquals(ExpertProgression.targets(101),ExpertProgression.targets(125))
    }
}
