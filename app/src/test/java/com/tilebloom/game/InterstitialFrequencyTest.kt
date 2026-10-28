package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class InterstitialFrequencyTest {
    @Test fun shortLevelsAccumulateOnlyPlayableIntervals() {
        val p = InterstitialFrequencyPolicy()
        p.active(0, true); p.active(45_000, false)
        assertFalse(p.victory(1, true, 45_000))
        p.active(100_000, true); p.active(160_000, false)
        assertFalse(p.victory(2, true, 160_000))
        p.active(300_000, true); p.active(390_000, false)
        assertTrue(p.victory(3, true, 390_000))
    }
    @Test fun exactThresholdAndOneSecondBelow() {
        val p = InterstitialFrequencyPolicy(); p.active(20, true)
        assertEquals(179_000, p.duration(179_020))
        assertFalse(p.victory(1, true, 179_020))
        assertTrue(p.victory(2, true, 180_020))
    }
    @Test fun unfinishedAndLostNeverConsumeVictory() {
        val p = InterstitialFrequencyPolicy(180_000)
        assertFalse(p.victory(1, false, 0)); assertEquals(0, p.lastVictory)
        assertTrue(p.victory(1, true, 0))
    }
    @Test fun duplicateAndOldVictoriesAreConsumedEvenIfNoAdDisplayed() {
        val p = InterstitialFrequencyPolicy(180_000)
        assertTrue(p.victory(9, true, 0))
        repeat(5) { assertFalse(p.victory(9, true, 0)) }
        assertFalse(p.victory(8, true, 0))
        assertTrue(p.victory(10, true, 0))
    }
    @Test fun relaunchRestoresDurationButNeverCountsClosedTime() {
        val p = InterstitialFrequencyPolicy(); p.active(100, true); p.active(90_100, false)
        val restored = InterstitialFrequencyPolicy(p.duration(5_000_000), p.lastVictory)
        assertEquals(90_000, restored.duration(50_000_000))
        restored.active(50_000_000, true)
        assertTrue(restored.victory(1, true, 50_090_000))
    }
    @Test fun restoredVictoryCannotReplayAfterRecreationOrRelaunch() {
        val p = InterstitialFrequencyPolicy(180_000); assertTrue(p.victory(5, true, 0))
        val restored = InterstitialFrequencyPolicy(p.duration(0), p.lastVictory)
        assertFalse(restored.victory(5, true, 10_000_000))
    }
    @Test fun interstitialAndRewardedDisplayBothResetEvenWithoutReward() {
        repeat(2) {
            val p = InterstitialFrequencyPolicy(170_000); p.active(0, true)
            p.displayed()
            assertEquals(0, p.duration(500_000)) // ad viewing is inactive
            p.active(500_000, true)
            assertFalse(p.victory(1, true, 679_000))
            assertTrue(p.victory(2, true, 680_000))
        }
    }
    @Test fun requestAndFailureDoNotResetDuration() {
        val p = InterstitialFrequencyPolicy(180_000)
        p.active(0, false) // load / failed show do not call displayed
        assertEquals(180_000, p.duration(9_000_000))
        assertTrue(p.victory(1, true, 9_000_000))
    }
    @Test fun allBlockingStatesExcludeTime() {
        val active = ActiveGameplayConditions(true, true, true, true, true, false, false, false, false)
        assertTrue(active.eligible)
        val inactive = listOf(active.copy(foreground=false), active.copy(focused=false),
            active.copy(gameScreen=false), active.copy(playing=false), active.copy(online=false),
            active.copy(loading=true), active.copy(fullScreen=true), active.copy(consent=true), active.copy(dialog=true))
        inactive.forEach { state ->
            assertFalse(state.eligible)
            val p = InterstitialFrequencyPolicy(); p.active(0, true)
            p.active(1000, state.eligible); p.active(1_000_000, true)
            assertEquals(1000, p.duration(1_000_000))
        }
    }
    @Test fun launchPlayHomeNextAndReconnectDoNotCreateAnOpportunity() {
        val p = InterstitialFrequencyPolicy(180_000)
        listOf(false, true, false, true, false, true).forEachIndexed { index, active -> p.active(index * 10L, active) }
        assertEquals(0, p.lastVictory) // only victory() consumes an opportunity
    }
    @Test fun invalidPersistenceIsSafeAndDurationIsBounded() {
        listOf(-1L, Long.MIN_VALUE, 180_001L, Long.MAX_VALUE).forEach {
            assertEquals(0, InterstitialFrequencyPolicy(it).duration(0))
        }
        val p = InterstitialFrequencyPolicy(); p.active(0, true)
        assertEquals(180_000, p.duration(Long.MAX_VALUE))
        assertEquals(0, InterstitialFrequencyPolicy(0,-2).lastVictory)
    }
    @Test fun repeatedStartStopDoesNotDoubleCountAndClockRollbackIsSafe() {
        val p = InterstitialFrequencyPolicy(); p.active(100, true); p.active(200, true)
        p.active(300, false); p.active(900, false)
        assertEquals(200, p.duration(2000))
        p.active(4000,true); assertEquals(200,p.duration(3990))
    }
}
