package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class UndoCreditTest {
    private fun match(c: ProgressController) {
        val p = c.session.game.matchingPairs().first()
        assertTrue(c.session.game.match(p.first, p.second))
    }
    private fun reload(c: ProgressController) = ProgressController(ProgressCodec.decode(ProgressCodec.encode(c.capture())))
    @Test fun threeCreditsThenRejectWithoutChangingBoard() {
        val c = ProgressController(); assertEquals(3,c.wallet.balance)
        repeat(3) { match(c); assertTrue(c.undo()); assertEquals(2-it,c.wallet.balance) }
        match(c); val before = c.capture()
        assertFalse(c.undo()); assertEquals(before,c.capture())
        repeat(20) { assertFalse(c.undo()) }; assertEquals(0,c.wallet.balance)
    }
    @Test fun emptyAndInvalidRestorationDoNotCharge() {
        val c = ProgressController(); assertFalse(c.undo()); assertEquals(3,c.wallet.balance)
        val w = UndoCreditWallet(); assertFalse(w.undo { false }); assertEquals(3,w.balance)
    }
    @Test fun callbackExactlyOnceAndCreditAllowsExactlyOneUndo() {
        val c = ProgressController(PlayerProgress(undoCredits=0)); match(c)
        val ticket = c.wallet.rewardTicket()
        assertTrue(ticket.earned()); assertFalse(ticket.earned()); assertEquals(1,c.wallet.balance)
        assertTrue(c.undo()); assertEquals(0,c.wallet.balance)
        assertFalse(c.undo())
        val deadlocked=ProgressController(PlayerProgress(undoCredits=0))
        val identity=deadlocked.session.snapshot()
        listOf(15 to 23, 10 to 22, 9 to 20, 6 to 16, 17 to 18, 0 to 14).forEach {
            assertTrue(deadlocked.session.game.match(it.first,it.second))
        }
        assertEquals(Status.NO_MOVES,deadlocked.session.game.status)
        val blocked=deadlocked.capture(); assertFalse(deadlocked.undo()); assertEquals(blocked,deadlocked.capture())
        deadlocked.wallet.rewardTicket().earned(); assertTrue(deadlocked.undo())
        assertEquals(0,deadlocked.wallet.balance); assertEquals(Status.PLAYING,deadlocked.session.game.status)
        assertEquals(identity.copy(game=deadlocked.session.game.snapshot()),deadlocked.session.snapshot())
    }
    @Test fun unearnedTicketsDoNotRewardAndSeparatePresentationsCanReward() {
        val w=UndoCreditWallet(0)
        repeat(4) { w.rewardTicket() } // loading, display, dismissal and failure never invoke earned
        assertEquals(0,w.balance)
        repeat(5) { val t=w.rewardTicket(); assertTrue(t.earned()); assertFalse(t.earned()) }
        assertEquals(5,w.balance)
    }
    @Test fun restartReplayAndNavigationKeepBalance() {
        val c=ProgressController(PlayerProgress(setOf(1,2),undoCredits=1))
        match(c); assertTrue(c.undo()); c.session.restart(); assertEquals(0,c.wallet.balance)
        assertFalse(c.select(2)); assertEquals(0,c.wallet.balance)
        c.session.level.witness.forEach { assertTrue(c.session.game.match(it.first,it.second)) }
        assertTrue(c.session.advance()); assertEquals(0,c.wallet.balance)
        assertEquals(0,reload(c).wallet.balance)
    }
    @Test fun replayAfterVictoryKeepsSpentCreditsAndUnlockedLevel() {
        val c = ProgressController()
        match(c); assertTrue(c.undo()); assertEquals(2, c.wallet.balance)
        c.session.level.witness.forEach { assertTrue(c.session.game.match(it.first, it.second)) }
        assertEquals(Status.WON, c.session.game.status)
        assertEquals(setOf(1), c.capture().completed)
        c.session.restart() // Same operation as the victory Replay Level button.
        val restored = reload(c)
        assertEquals(2, restored.wallet.balance)
        assertEquals(2L, restored.highestUnlocked)
        assertEquals(setOf(1), restored.capture().completed)
        assertEquals(Status.WON,restored.session.game.status)
        assertFalse(restored.undo()); assertEquals(2, restored.wallet.balance)
    }
    @Test fun migrationKeepsCompletionsIdentityAndMovesExactlyOnce() {
        val c=ProgressController(PlayerProgress(setOf(1,2))); c.select(3); match(c)
        val old=ProgressCodec.encode(c.capture()).split('\n').take(4).toMutableList(); old[0]="1"; old[1]="1,2"
        val migrated=ProgressController(ProgressCodec.decode(old.joinToString("\n")))
        assertEquals(c.capture(),migrated.capture()); assertEquals(3,migrated.wallet.balance)
        assertTrue(migrated.undo()); assertEquals(2,reload(migrated).wallet.balance)
    }
    @Test fun repeatedRoundTripsKeepZeroAndExactRestoredPair() {
        var c=ProgressController(); val before=c.session.game.tiles
        repeat(3) { match(c); assertTrue(c.undo()); c=reload(c); assertEquals(before,c.session.game.tiles) }
        repeat(30) { c=reload(c); assertEquals(0,c.wallet.balance) }
    }
    @Test fun invalidStoredBalancesRejected() {
        for (value in listOf("-1","2147483648","bad","")) {
            assertTrue(runCatching { ProgressCodec.decode("2\n1,2\n\n\n$value") }.isFailure)
        }
        assertTrue(runCatching { UndoCreditWallet(-1) }.isFailure)
    }
    private class FakeAds(private val gate: RewardPresentationGate) : RewardedAdProvider<Unit> {
        override var loading = false
        override var available = false
        var starts = 0
        var ticket: RewardTicket? = null
        override fun load() { loading = true }
        fun loaded() { loading = false; available = true }
        fun loadFailure() { loading = false; available = false }
        override fun show(host: Unit): Boolean {
            if (!available) return false
            val t = gate.begin() ?: return false
            ticket = t; starts++; available = false
            return true
        }
        fun reward() = ticket!!.earned()
        fun dismiss() { gate.finish(ticket!!) }
        fun displayFailure() { gate.finish(ticket!!) }
    }
    @Test fun fakeProviderFailuresDismissalAndDuplicateTaps() {
        val w = UndoCreditWallet(0); val ads = FakeAds(RewardPresentationGate(w))
        ads.load(); assertFalse(ads.show(Unit)); ads.loadFailure(); assertEquals(0,w.balance)
        ads.loaded(); assertTrue(ads.show(Unit)); repeat(10) { assertFalse(ads.show(Unit)) }
        assertEquals(1,ads.starts); ads.dismiss(); assertEquals(0,w.balance)
        ads.loaded(); assertTrue(ads.show(Unit)); ads.displayFailure(); assertEquals(0,w.balance)
        ads.loaded(); assertTrue(ads.show(Unit)); assertTrue(ads.reward()); assertFalse(ads.reward())
        ads.dismiss(); assertEquals(1,w.balance)
    }
    @Test fun recreatedScreenDoesNotLoseOrDuplicatePendingReward() {
        val w = UndoCreditWallet(0); val gate = RewardPresentationGate(w); val ads=FakeAds(gate)
        ads.load(); val replacementScreen=ads
        replacementScreen.loaded(); assertTrue(replacementScreen.show(Unit))
        assertNull(gate.begin()); assertTrue(ads.reward()); ads.dismiss()
        assertEquals(1,w.balance); assertFalse(ads.reward())
        assertEquals(1,ProgressCodec.decode(ProgressCodec.encode(PlayerProgress(undoCredits=w.balance))).undoCredits)
    }
}
