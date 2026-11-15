package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import org.junit.Assert.*
import org.junit.Test

class TutorialTest {
    private fun started(saved: PlayerProgress = PlayerProgress()): ProgressController = ProgressController(saved).also {
        it.tutorial.begin(it.capture().frontier)
    }
    private fun collect(p: ProgressController) {
        val id = requireNotNull(p.tutorial.target)
        assertTrue(p.session.game.isAvailable(id))
        assertTrue(p.tutorial.allowsTile(id))
        assertTrue(p.session.game.select(id))
        p.tutorial.collected()
    }
    private fun toUndo(p: ProgressController) {
        assertTrue(p.tutorial.continueGuide()); collect(p)
        assertEquals(TutorialStep.EXPLAIN_TRAY, p.tutorial.step)
        assertEquals(4, p.session.game.traySlots.size)
        assertEquals(1, p.session.game.tray.size)
        assertTrue(p.tutorial.continueGuide())
    }
    private fun roundTrip(p: ProgressController) = started(ProgressCodec.decode(ProgressCodec.encode(p.capture())))
    @Test fun newPlayerAndExistingPlayerEligibility() {
        assertEquals(TutorialStep.INTRODUCTION, started().tutorial.step)
        assertEquals(TutorialStep.SKIPPED, started(PlayerProgress(highestCompleted = 36)).tutorial.step)
        val played = ProgressController()
        assertTrue(played.session.game.select(played.session.level.winningSelections.first()))
        assertEquals(TutorialStep.SKIPPED, started(played.capture()).tutorial.step)
    }
    @Test fun realInteractionsFreeOnceAndSolvableRemainder() {
        val p = started(); val initial = p.session.game.snapshot()
        val credits = p.wallet.balance to p.hintCredits
        toUndo(p)
        assertTrue(p.undo()); assertEquals(initial, p.session.game.snapshot())
        assertFalse(p.undo())
        assertEquals(TutorialStep.DEMONSTRATE_HINT, p.tutorial.step)
        val suggestion = HintPolicy.suggest(p.session.level.tiles, p.session.game.snapshot())
        val id = requireNotNull(suggestion.id)
        assertTrue(p.session.game.isAvailable(id))
        assertTrue(p.acceptHint(id, initial));
        assertEquals(id, p.tutorial.target); assertFalse(p.acceptHint(id, initial))
        assertEquals(credits, p.wallet.balance to p.hintCredits)
        collect(p); collect(p)
        assertEquals(TutorialStep.FINISHED, p.tutorial.step)
        assertEquals(0, p.session.game.tray.size)
        assertEquals(2, p.session.game.clearedTiles.size)
        assertEquals(TutorialStep.FINISHED, roundTrip(p).tutorial.step)
        val remainder = Game(p.session.level.tiles)
        assertTrue(remainder.restore(p.session.game.snapshot()))
        p.session.level.winningSelections.filterNot { id -> p.session.game.clearedTiles.any { it.id == id } }.forEach { assertTrue(remainder.select(it)) }
        assertEquals(Status.WON, remainder.status)
        assertTrue(p.undo()); assertEquals(credits.first - 1, p.wallet.balance)
        val next = HintPolicy.suggest(p.session.level.tiles, p.session.game.snapshot())
        assertTrue(p.acceptHint(requireNotNull(next.id), p.session.game.snapshot()))
        assertEquals(credits.second - 1, p.hintCredits)
    }
    @Test fun restorationAtEveryStepKeepsBoardAndBalances() {
        var p = roundTrip(started()); assertEquals(TutorialStep.INTRODUCTION, p.tutorial.step)
        p.tutorial.continueGuide(); p = roundTrip(p)
        collect(p); p = roundTrip(p); assertEquals(TutorialStep.EXPLAIN_TRAY, p.tutorial.step)
        p.tutorial.continueGuide(); p = roundTrip(p); assertTrue(p.undo())
        p = roundTrip(p); assertEquals(TutorialStep.DEMONSTRATE_HINT, p.tutorial.step)
        val snapshot = p.session.game.snapshot()
        assertTrue(p.acceptHint(requireNotNull(HintPolicy.suggest(p.session.level.tiles, snapshot).id), snapshot))
        p = roundTrip(p); collect(p); p = roundTrip(p); collect(p)
        assertEquals(TutorialStep.FINISHED, roundTrip(p).tutorial.step)
        assertEquals(3, p.wallet.balance); assertEquals(3, p.hintCredits)
    }
    @Test fun skipPersistsAtEveryGuidedStageWithoutChangingBoardOrWallet() {
        for (stage in 0..6) {
            val p = started()
            if (stage > 0) p.tutorial.continueGuide()
            if (stage > 1) collect(p)
            if (stage > 2) p.tutorial.continueGuide()
            if (stage > 3) p.undo()
            if (stage > 4) p.acceptHint(requireNotNull(HintPolicy.suggest(p.session.level.tiles, p.session.game.snapshot()).id), p.session.game.snapshot())
            if (stage > 5) collect(p)
            val before = p.capture(); p.tutorial.skip()
            assertEquals(before.copy(tutorial = TutorialStep.SKIPPED), p.capture())
            assertEquals(TutorialStep.SKIPPED, roundTrip(p).tutorial.step)
            assertFalse(p.tutorial.begin(0))
        }
    }
    @Test fun zeroBalancesStillAllowOnlyTheGuidedDemonstrations() {
        val p = started(PlayerProgress(undoCredits = 0, hintCredits = 0)); toUndo(p)
        assertTrue(p.undo())
        assertTrue(p.acceptHint(requireNotNull(HintPolicy.suggest(p.session.level.tiles, p.session.game.snapshot()).id), p.session.game.snapshot()))
        collect(p); collect(p)
        assertFalse(p.undo()); assertFalse(p.acceptHint(p.session.level.winningSelections[2], p.session.game.snapshot()))
        assertEquals(0, p.wallet.balance); assertEquals(0, p.hintCredits)
    }
    @Test fun practiceControllerNeverMutatesRealGame() {
        val real = ProgressController(PlayerProgress(highestCompleted = 36, hintCredits = 1, undoCredits = 2))
        val before = real.capture()
        val practice = started(); toUndo(practice); practice.undo(); practice.tutorial.skip()
        assertEquals(before, real.capture())
    }
    @Test fun unrelatedActionsAndRewardsCannotBypassGuidance() {
        val p = started(PlayerProgress(undoCredits = 0, hintCredits = 0))
        val before = p.capture()
        assertFalse(p.undo())
        val tile = p.session.level.winningSelections.first()
        assertFalse(p.tutorial.allowsTile(tile))
        assertFalse(p.acceptHint(tile, p.session.game.snapshot()))
        for (purpose in RewardPurpose.entries) assertFalse(p.grantReward(purpose) { error("No reward save is allowed in guidance") })
        assertEquals(before, p.capture())
    }
    @Test fun legacyAndUnknownGuidanceMetadataPreserveProgress() {
        val p = ProgressController(PlayerProgress(highestCompleted = 36, hintCredits = 1, undoCredits = 2))
        val encoded = ProgressCodec.encode(p.capture()).lines()
        val legacy = (listOf("7") + encoded.drop(1).dropLast(1)).joinToString("\n")
        val migrated = ProgressController(ProgressCodec.decode(legacy))
        assertEquals(p.capture(), migrated.capture())
        val corruptGuidance = (encoded.dropLast(1) + "UNKNOWN_FUTURE_STEP").joinToString("\n")
        val safe = ProgressController(ProgressCodec.decode(corruptGuidance))
        assertEquals(p.capture().copy(tutorial = TutorialStep.SKIPPED), safe.capture())
    }
    @Test fun mismatchedRestorationExitsWithoutResettingPuzzle() {
        val p = started(); toUndo(p)
        p.session.game.undo()
        val before = p.capture()
        val restored = roundTrip(p)
        assertEquals(TutorialStep.SKIPPED, restored.tutorial.step)
        assertEquals(before.active, restored.capture().active)
    }
}
