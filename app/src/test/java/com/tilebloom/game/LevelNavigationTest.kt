package com.tilebloom.game

import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.*
import org.junit.Assert.*
import org.junit.Test

class LevelNavigationTest {
    private fun reload(c: ProgressController) = ProgressController(ProgressCodec.decode(ProgressCodec.encode(c.capture())))
    @Test fun freshEntryUsesAuthoritativeIdentityIncludingLongLevels() {
        for (frontier in listOf(0L,20L,46L,99L,999L,Long.MAX_VALUE-1)) {
            val c=ProgressController(PlayerProgress(highestCompleted=frontier,undoCredits=1))
            val nav=LevelNavigation(c.session.level.id)
            assertEquals(frontier+1,nav.state.levelId); assertEquals(GardenScreen.START,nav.state.screen)
            nav.play(c.session.level.id); nav.play(c.session.level.id)
            assertEquals(frontier+1,c.session.level.id); assertEquals(1,c.wallet.balance)
        }
    }
    @Test fun homeAndPlayReuseExactBoardTrayHistorySeedAndWallet() {
        val c=ProgressController(PlayerProgress(highestCompleted=20,undoCredits=2))
        val nav=LevelNavigation(c.session.level.id); val game=c.session.game
        val move=c.session.level.witness.first()
        nav.play(c.session.level.id); assertTrue(game.select(move.first))
        val before=c.capture(); val tiles=game.tiles; val tray=game.tray
        repeat(5) {
            nav.home(c.session.level.id); assertEquals(GardenScreen.START,nav.state.screen)
            nav.play(c.session.level.id); assertSame(game,c.session.game)
            assertEquals(before,c.capture()); assertEquals(tiles,game.tiles); assertEquals(tray,game.tray)
        }
        val restored=reload(c); val fresh=LevelNavigation(restored.session.level.id)
        assertEquals(GardenScreen.START,fresh.state.screen); fresh.play(restored.session.level.id)
        assertEquals(before,restored.capture()); assertEquals(game.tray,restored.session.game.tray)
    }
    @Test fun recreationRestoresOnlyNavigationForTheSameLevel() {
        val c=ProgressController(); val nav=LevelNavigation(c.session.level.id)
        nav.play(c.session.level.id); c.session.game.select(c.session.level.witness.first().first)
        val saved=c.capture(); val bundleState=nav.state
        val restored=reload(c); val rotated=LevelNavigation(restored.session.level.id,bundleState)
        assertEquals(GardenScreen.GAME,rotated.state.screen); assertEquals(saved,restored.capture())
        rotated.home(restored.session.level.id)
        assertEquals(GardenScreen.START,LevelNavigation(restored.session.level.id,rotated.state).state.screen)
        assertEquals(GardenScreen.START,LevelNavigation(2,bundleState).state.screen)
        assertEquals(2L,LevelNavigation(2,bundleState).state.levelId)
        assertEquals(GardenScreen.START,LevelNavigation(c.session.level.id).state.screen)
    }
    @Test fun pendingVictoryCannotReplayAndNextWaitsForPlayExactlyOnce() {
        val c=ProgressController(PlayerProgress(highestCompleted=19,undoCredits=1))
        c.session.level.witness.forEach { assertTrue(c.session.game.select(it.first)); assertTrue(c.session.game.select(it.second)) }
        val restored=reload(c); val nav=LevelNavigation(restored.session.level.id)
        nav.play(restored.session.level.id)
        assertEquals(Status.WON,restored.session.game.status); assertFalse(restored.session.restart())
        assertFalse(restored.select(19)); assertFalse(restored.select(20)); assertEquals(1,restored.wallet.balance)
        val definition=restored.prepareNext()!!
        assertTrue(restored.commitNext(definition)); assertFalse(restored.commitNext(definition)); assertFalse(restored.advance())
        nav.synchronize(restored.session.level.id)
        assertEquals(NavigationState(21,GardenScreen.START),nav.state)
        nav.play(21); assertEquals(21L,restored.session.level.id); assertEquals(20L,restored.capture().frontier)
        assertTrue(restored.session.game.tray.isEmpty()); assertEquals(1,restored.wallet.balance)
    }
    @Test fun staleRecreationDuringAdvanceOpensNewStart() {
        val c=ProgressController(); val nav=LevelNavigation(1); nav.play(1)
        val oldUi=nav.state
        c.session.level.witness.forEach { c.session.game.select(it.first); c.session.game.select(it.second) }
        val next=c.prepareNext()!!; val committedSave=c.nextProgress(next)!!
        val afterProcessDeath=ProgressController(ProgressCodec.decode(ProgressCodec.encode(committedSave)))
        val restored=LevelNavigation(afterProcessDeath.session.level.id,oldUi)
        assertEquals(NavigationState(2,GardenScreen.START),restored.state)
        assertEquals(1L,afterProcessDeath.capture().frontier); assertEquals(3,afterProcessDeath.wallet.balance)
    }
    @Test fun savedDefeatHomeAndResumeDoNotResetAttemptOrSpendCredits() {
        val c=ProgressController()
        listOf(23,21,18,20).forEach { assertTrue(c.session.game.select(it)) }
        assertEquals(Status.LOST,c.session.game.status)
        val before=c.capture(); val restored=reload(c); val nav=LevelNavigation(restored.session.level.id)
        nav.play(1); nav.home(1); nav.play(1)
        assertEquals(before,restored.capture()); assertEquals(4,restored.session.game.tray.size)
        assertTrue(restored.undo()); assertEquals(2,restored.wallet.balance)
        nav.home(1); nav.play(1); assertEquals(Status.PLAYING,restored.session.game.status)
        val seed=restored.session.level.seed; assertTrue(restored.session.restart())
        assertEquals(seed,restored.session.level.seed); assertTrue(restored.session.game.tray.isEmpty())
        assertEquals(2,restored.wallet.balance); assertEquals(0L,restored.capture().frontier)
    }
}
