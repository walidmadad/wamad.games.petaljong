package com.tilebloom.game
import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test
class ProgressTest {
    private fun win(c: ProgressController) { c.session.level.witness.forEach { assertTrue(c.session.game.match(it.first,it.second)) }; c.capture() }
    private fun reload(c: ProgressController) = ProgressController(ProgressCodec.decode(ProgressCodec.encode(c.capture())))
    @Test fun freshStartsAtOne() { assertEquals(1L,ProgressController().session.level.id) }
    @Test fun exactNextAndDoubleTap() { val c=ProgressController(); assertFalse(c.advance()); win(c); assertTrue(c.advance()); assertFalse(c.advance()); assertEquals(2L,c.session.level.id) }
    @Test fun pendingVictoryRestoresAndCannotRestart() { val c=ProgressController(); win(c); val r=reload(c); assertEquals(Status.WON,r.session.game.status); assertFalse(r.session.restart()); assertFalse(r.select(1)); assertTrue(r.advance()) }
    @Test fun cannotSelectAnyOtherLevel() { val c=ProgressController(PlayerProgress(setOf(1,2))); assertEquals(3L,c.session.level.id); for(id in listOf(1,2,4,99)) assertFalse(c.select(id)) }
    @Test fun unfinishedHistoryAndCreditsRestore() { val c=ProgressController(PlayerProgress(setOf(1,2),undoCredits=1)); c.session.level.witness.take(5).forEach { c.session.game.match(it.first,it.second) }; assertEquals(c.capture(),reload(c).capture()); assertTrue(c.undo()); assertEquals(0,reload(c).wallet.balance) }
    @Test fun corruptHistoryResetsOnlyCurrentAttempt() { val c=ProgressController(PlayerProgress(setOf(1,2))); val p=c.capture(); val r=ProgressController(p.copy(active=p.active!!.copy(game=GameSnapshot(listOf(PairMove(-1,999)),null)))); assertTrue(r.recovered); assertEquals(3L,r.session.level.id); assertEquals(2L,r.capture().frontier) }
    @Test fun incompatibleIdentityResetsOnlyAttempt() { val c=ProgressController(PlayerProgress(setOf(1,2))); val p=c.capture(); val r=ProgressController(p.copy(active=p.active!!.copy(version=99))); assertTrue(r.recovered); assertEquals(3L,r.session.level.id) }
    @Test fun noncontiguousLegacyDoesNotInventCompletions() { val p=ProgressCodec.decode("2\n1,3,4,5\n\n\n2"); val c=ProgressController(p); assertEquals(2L,c.session.level.id); assertEquals(1L,c.capture().frontier); assertEquals(2,c.wallet.balance) }
    @Test fun allFiveLegacyAdvancesToSix() { val c=ProgressController(ProgressCodec.decode("2\n1,2,3,4,5\n\n\n0")); assertEquals(6L,c.session.level.id); assertEquals(0,c.wallet.balance) }
    @Test fun unfinishedLegacySurvives() { val l=Levels.get(4); val m=l.witness.first(); val raw="2\n1,2,3\n4,${l.seed},${l.version}\n${m.first},${m.second}\n1"; val c=ProgressController(ProgressCodec.decode(raw)); assertEquals(listOf(m),c.session.game.snapshot().moves); assertEquals(4L,c.session.level.id) }
    @Test fun completedLegacySelectionMovesToFirstUnfinished() { val l=Levels.get(1); val c=ProgressController(ProgressCodec.decode("2\n1,2\n1,${l.seed},${l.version}\n\n2")); assertEquals(3L,c.session.level.id) }
    @Test fun generatedTransitionsAndUndoIsolation() { for(id in listOf(5L,99L,999L,9999L)) { val l=Levels.get(id); val c=ProgressController(PlayerProgress(active=SessionSnapshot(id,l.seed,l.version,GameSnapshot(emptyList(),null)),highestCompleted=id-1,undoCredits=1)); win(c); val next=c.prepareNext()!!; assertTrue(c.commitNext(next)); assertFalse(c.commitNext(next)); assertEquals(id+1,c.session.level.id); assertEquals(id,c.capture().frontier); assertEquals(1,c.wallet.balance); assertFalse(c.undo()); assertEquals(c.capture(),reload(c).capture()) } }
    @Test fun deadlockNeverAdvances() { val c=ProgressController(); deadlockPath(c.session.game).forEach { c.session.game.match(it.first,it.second) }; assertEquals(Status.NO_MOVES,c.session.game.status); assertFalse(c.advance()); assertEquals(0L,reload(c).capture().frontier) }
    @Test fun restartSameIdentityAndBalance() { val c=ProgressController(PlayerProgress(highestCompleted=99,undoCredits=0)); val l=c.session.level; val m=l.witness.first(); c.session.game.match(m.first,m.second); assertTrue(c.session.restart()); assertEquals(l.tiles,c.session.game.tiles); assertEquals(100L,c.session.level.id); assertEquals(0,c.wallet.balance) }
    @Test fun corruptPendingHistoryNeverReplaysCompleted() { val c=ProgressController(); win(c); val p=c.capture(); val r=ProgressController(p.copy(active=p.active!!.copy(game=GameSnapshot(emptyList(),null)))); assertEquals(2L,r.session.level.id); assertEquals(1L,r.capture().frontier) }
    @Test fun invalidCompletionsCannotUnlock() { assertEquals(1L,ProgressController(PlayerProgress(setOf(2,5,999))).highestUnlocked) }
    @Test fun unsupportedSchemaRejected() { assertThrows(IllegalArgumentException::class.java) { ProgressCodec.decode("99\n1\n") } }
    @Test fun malformedBoardRetainsCompletion() { val c=ProgressController(ProgressCodec.decode("1\n1,2\ngarbage\nbad")); assertEquals(2L,c.capture().frontier); assertEquals(3L,c.session.level.id) }
    @Test fun staleDefinitionCannotSkip() { val c=ProgressController(); win(c); assertFalse(c.commitNext(Levels.get(3))); assertEquals(1L,c.session.level.id) }    @Test fun secondVictoryUnlocksExactlyThree() { val c=ProgressController(); win(c); assertTrue(c.advance()); win(c); assertEquals(3L,c.highestUnlocked); assertEquals(2L,reload(c).capture().frontier) }
    @Test fun invalidLevelAndSeedResetOnlyAttempt() {
        val c=ProgressController(PlayerProgress(setOf(1,2))); val p=c.capture()
        for(bad in listOf(p.active!!.copy(id=999L),p.active!!.copy(seed=-55))) {
            val r=ProgressController(p.copy(active=bad)); assertTrue(r.recovered); assertEquals(3L,r.session.level.id); assertEquals(2L,r.capture().frontier)
        }
    }
    @Test fun duplicateAcceptedMoveIsRejectedAtomically() {
        val c=ProgressController(); val m=c.session.level.witness.first(); c.session.game.match(m.first,m.second)
        val p=c.capture(); val s=p.active!!; val r=ProgressController(p.copy(active=s.copy(game=s.game.copy(moves=listOf(m,m)))))
        assertTrue(r.recovered); assertTrue(r.session.game.snapshot().moves.isEmpty()); assertEquals(12,r.session.game.remainingPairs)
    }
    @Test fun repeatedRestoresKeepIdentityCounterAndCredits() {
        var c=ProgressController(PlayerProgress(highestCompleted=499,undoCredits=2))
        c.session.level.witness.take(5).forEach { c.session.game.match(it.first,it.second) }
        val p=c.capture(); repeat(10) { c=reload(c) }; assertEquals(p,c.capture()); assertEquals(c.session.level.tiles.size/2-5,c.session.game.remainingPairs)
    }
    @Test fun optionalLegacyMovesDefaultEmpty() {
        val l=Levels.get(1); val c=ProgressController(ProgressCodec.decode("1\n\n1,${l.seed},${l.version}"))
        assertEquals(12,c.session.game.remainingPairs); assertEquals(3,c.wallet.balance)
    }
    @Test fun nextSavePreparedBeforeSessionMutation() {
        val c=ProgressController(); win(c); val before=c.capture(); val l=c.prepareNext()!!
        val state=c.nextProgress(l)!!; assertEquals(before,c.capture()); assertEquals(2L,state.active!!.id); assertEquals(1L,state.frontier)
        val recovered=ProgressController(ProgressCodec.decode(ProgressCodec.encode(state)))
        assertEquals(2L,recovered.session.level.id); assertFalse(recovered.select(1)); assertFalse(recovered.advance())
    }

}
