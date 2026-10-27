package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class ProgressTest {
    private fun win(c: ProgressController) {
        c.session.level.witness.forEach { assertTrue(c.session.game.match(it.first, it.second)) }
        c.capture()
    }
    private fun reload(c: ProgressController) = ProgressController(ProgressCodec.decode(ProgressCodec.encode(c.capture())))
    @Test fun freshStartsAtOne() { assertEquals(1, ProgressController().session.level.id) }
    @Test fun firstVictoryUnlocksTwo() { val c = ProgressController(); win(c); assertEquals(2,c.highestUnlocked) }
    @Test fun secondVictoryUnlocksThree() { val c = ProgressController(); win(c); c.select(2); win(c); assertEquals(3,c.highestUnlocked) }
    @Test fun completionsSurviveReload() { val c = ProgressController(); win(c); assertEquals(setOf(1),reload(c).capture().completed) }
    private fun third(): ProgressController {
        val c = ProgressController(); win(c); c.select(2); win(c); c.select(3)
        c.session.level.witness.take(5).forEach { c.session.game.match(it.first,it.second) }
        return c
    }
    @Test fun unfinishedHistoryIsExact() { val c=third(); assertEquals(c.capture(),reload(c).capture()) }
    @Test fun counterIsRecomputed() { val c=third(); assertEquals(c.session.level.tiles.size/2-5,reload(c).session.game.remainingPairs) }
    @Test fun identityIsPreserved() { val c=third(); assertEquals(c.session.level,reload(c).session.level) }
    @Test fun incompatibleVersionResetsOnlyBoard() {
        val c=third(); val p=c.capture(); val r=ProgressController(p.copy(active=p.active!!.copy(version=99)))
        assertTrue(r.recovered); assertEquals(setOf(1,2),r.capture().completed); assertTrue(r.session.game.snapshot().moves.isEmpty())
    }
    @Test fun corruptHistoryIsAtomic() {
        val c=third(); val p=c.capture(); val s=p.active!!
        val r=ProgressController(p.copy(active=s.copy(game=GameSnapshot(s.game.moves+PairMove(-1,999),null))))
        assertTrue(r.recovered); assertTrue(r.session.game.snapshot().moves.isEmpty())
    }
    @Test fun invalidLevelIsRejected() {
        val l=Levels.get(1); val r=ProgressController(PlayerProgress(active=SessionSnapshot(999,l.seed,l.version,GameSnapshot(emptyList(),null))))
        assertTrue(r.recovered); assertEquals(1,r.session.level.id)
    }
    @Test fun invalidCompletionsCannotUnlock() { assertEquals(1,ProgressController(PlayerProgress(setOf(2,5,999))).highestUnlocked) }
    @Test fun replayKeepsCompletion() { val c=ProgressController(); win(c); c.session.restart(); assertEquals(setOf(1),reload(c).capture().completed) }
    @Test fun restartKeepsEarlierCompletions() { val c=third(); c.session.restart(); assertEquals(setOf(1,2),reload(c).capture().completed) }
    @Test fun deadlockDoesNotUnlock() {
        val c=ProgressController()
        deadlockPath(c.session.game).forEach { assertTrue(c.session.game.match(it.first,it.second)) }
        assertEquals(Status.NO_MOVES,c.session.game.status); assertEquals(1,reload(c).highestUnlocked)
    }
    @Test fun victoryRestores() { val c=ProgressController(); win(c); assertEquals(Status.WON,reload(c).session.game.status) }
    @Test fun repeatedRestoresNeverDuplicate() { var c=third(); val p=c.capture(); repeat(10) { c=reload(c) }; assertEquals(p,c.capture()) }
    @Test fun missingOptionalMovesDefaultEmpty() { val l=Levels.get(1); val c=ProgressController(ProgressCodec.decode("1\n\n1,${l.seed},${l.version}")); assertEquals(12,c.session.game.remainingPairs) }
    @Test fun unsupportedSchemaRejected() { assertThrows(IllegalArgumentException::class.java) { ProgressCodec.decode("99\n1\n") } }
    @Test fun malformedBoardRetainsCompletion() { val c=ProgressController(ProgressCodec.decode("1\n1,2\ngarbage\nbad")); assertEquals(setOf(1,2),c.capture().completed); assertEquals(1,c.session.level.id) }
    @Test fun lockedSelectionRejected() { val c=ProgressController(); assertFalse(c.select(2)); assertEquals(1,c.session.level.id) }
    @Test fun duplicateMoveRejected() {
        val p = third().capture()
        val s = p.active!!
        val bad = s.copy(game = s.game.copy(moves = s.game.moves + s.game.moves.first()))
        assertTrue(ProgressController(p.copy(active = bad)).recovered)
    }
    @Test fun invalidSeedRejected() { val c=third(); val p=c.capture(); assertTrue(ProgressController(p.copy(active=p.active!!.copy(seed=-55))).recovered) }
    @Test fun finalVictoryRestores() { val c=ProgressController(); for (id in 1..5) { c.select(id); win(c) }; val r=reload(c); assertEquals((1..5).toSet(),r.capture().completed); assertEquals(Status.WON,r.session.game.status); assertFalse(r.session.canAdvance) }
}
