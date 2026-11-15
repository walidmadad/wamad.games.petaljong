package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import org.junit.Assert.*
import org.junit.Test

class AdRewardTest {
    private class FakeAds(private val controller: ProgressController) {
        val gate = AdPresentation()
        var ready = false
        var active: AdReward? = null
        var disk = controller.capture()
        var saveWorks = true
        fun load(success: Boolean = true) { ready = success }
        fun show(purpose: RewardPurpose): Boolean {
            if (!ready) return false
            val ticket = gate.begin(purpose) { controller.grantReward(it) { state ->
                if (saveWorks) disk = ProgressCodec.decode(ProgressCodec.encode(state))
                saveWorks
            } } ?: return false
            active = ticket; ready = false
            return true
        }
        fun dismiss() { active?.let { gate.finish(it) }; active = null }
    }
    @Test fun selectedWalletOnlyGetsExactlyOneDurableCredit() {
        for (purpose in RewardPurpose.values()) {
            val controller = ProgressController(PlayerProgress(undoCredits = 0, hintCredits = 0))
            val ads = FakeAds(controller)
            val board = controller.capture().active
            ads.load(); assertTrue(ads.show(purpose))
            assertEquals(0, controller.wallet.balance); assertEquals(0, controller.hintCredits)
            val callback = ads.active!!
            assertTrue(callback.earned()); repeat(10) { assertFalse(callback.earned()) }
            ads.dismiss(); assertFalse(callback.earned())
            val expectedUndo = if (purpose == RewardPurpose.UNDO) 1 else 0
            val expectedHint = if (purpose == RewardPurpose.HINT) 1 else 0
            assertEquals(expectedUndo, controller.wallet.balance); assertEquals(expectedHint, controller.hintCredits)
            assertEquals(board, controller.capture().active)
            val restored = ProgressController(ads.disk)
            assertEquals(expectedUndo, restored.wallet.balance); assertEquals(expectedHint, restored.hintCredits)
            restored.session.restart()
            assertEquals(expectedUndo, restored.wallet.balance); assertEquals(expectedHint, restored.hintCredits)
        }
    }
    @Test fun loadShowDismissAndFailuresNeverGrant() {
        val c = ProgressController(PlayerProgress(undoCredits = 0, hintCredits = 0))
        val ads = FakeAds(c)
        assertFalse(ads.show(RewardPurpose.HINT))
        ads.load(false); assertFalse(ads.show(RewardPurpose.UNDO))
        ads.load(); assertTrue(ads.show(RewardPurpose.HINT))
        ads.load(); repeat(20) { assertFalse(ads.show(RewardPurpose.UNDO)) }
        ads.dismiss()
        assertEquals(0, c.wallet.balance); assertEquals(0, c.hintCredits)
    }
    @Test fun failedPersistenceDoesNotPublishWalletOrReplayCallback() {
        val c = ProgressController(PlayerProgress(undoCredits = 0, hintCredits = 0))
        val before = c.capture()
        val ads = FakeAds(c); ads.saveWorks = false; ads.load(); ads.show(RewardPurpose.UNDO)
        assertFalse(ads.active!!.earned()); ads.saveWorks = true
        assertFalse(ads.active!!.earned()); assertEquals(before, c.capture()); assertEquals(before, ads.disk)
    }
    @Test fun preservedBalancesAndProgressAreNeverReallocated() {
        val saved = PlayerProgress(undoCredits = 0, hintCredits = 0, highestCompleted = 20)
        val c = ProgressController(ProgressCodec.decode(ProgressCodec.encode(saved)))
        assertEquals(0, c.wallet.balance); assertEquals(0, c.hintCredits); assertEquals(21L, c.session.level.id)
        assertTrue(c.grantReward(RewardPurpose.HINT) { true })
        assertFalse(c.grantReward(RewardPurpose.HINT) { fail("Nonempty wallet must not be rewarded"); true })
        assertEquals(3, ProgressController().wallet.balance); assertEquals(3, ProgressController().hintCredits)
    }
    @Test fun displayFailureAndRecreatedObserversCannotDeliverAgain() {
        var grants = 0
        val gate = AdPresentation()
        val ticket = gate.begin(RewardPurpose.HINT) { grants++; true }!!
        ticket.cancel(); gate.finish(ticket)
        assertFalse(ticket.earned()); assertEquals(0, grants)
        val next = gate.begin(RewardPurpose.UNDO) { grants++; true }!!
        assertTrue(next.earned()); gate.finish(next)
        assertFalse(next.earned()); assertEquals(1, grants)
    }
    @Test fun nextLevelAndHomeKeepRewardedWallets() {
        val c = ProgressController(PlayerProgress(undoCredits = 0, hintCredits = 0))
        c.grantReward(RewardPurpose.UNDO) { true }; c.grantReward(RewardPurpose.HINT) { true }
        val level = c.session.level
        assertTrue(c.session.game.restore(GameSnapshot(level.witness, null)))
        assertTrue(c.advance())
        val restored = ProgressController(ProgressCodec.decode(ProgressCodec.encode(c.capture())))
        assertEquals(1, restored.wallet.balance); assertEquals(1, restored.hintCredits)
        assertEquals(2L, restored.session.level.id)
    }
}
