package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import org.junit.Assert.*
import org.junit.Test

class FullScreenAdvertisingTest {
    @Test fun rewardedAndInterstitialCannotOverlap() {
        val gate = FullScreenAdGate()
        assertTrue(gate.acquire()); assertFalse(gate.acquire())
        gate.release(); assertTrue(gate.acquire()); assertFalse(gate.acquire())
    }
    @Test fun duplicateShownCallbacksResetCooldownOnlyOnce() {
        val lifecycle = FullScreenAdLifecycle(); val policy = InterstitialFrequencyPolicy(180_000)
        if (lifecycle.shown()) policy.displayed()
        policy.active(1000,true)
        if (lifecycle.shown()) policy.displayed()
        assertEquals(1000,policy.duration(2000))
        assertTrue(lifecycle.finish()); assertFalse(lifecycle.finish()); assertFalse(lifecycle.shown())
    }
    @Test fun failedDisplayDoesNotResetAndLateCallbacksAreIgnored() {
        val lifecycle = FullScreenAdLifecycle(); val policy = InterstitialFrequencyPolicy(180_000)
        assertTrue(lifecycle.finish())
        if (lifecycle.shown()) policy.displayed()
        assertEquals(180_000,policy.duration(0)); assertFalse(lifecycle.finish())
    }
    @Test fun failureAfterDisplayRetainsNormalCooldown() {
        val lifecycle = FullScreenAdLifecycle(); val policy = InterstitialFrequencyPolicy(180_000)
        if (lifecycle.shown()) policy.displayed()
        assertTrue(lifecycle.finish()); assertEquals(0,policy.duration(1_000_000))
    }
    @Test fun interstitialCallbacksCannotChangeBoardCreditsOrProgression() {
        val controller = ProgressController(PlayerProgress(undoCredits=7,hintCredits=9))
        val before = controller.capture(); val lifecycle = FullScreenAdLifecycle()
        val policy = InterstitialFrequencyPolicy(180_000)
        if (lifecycle.shown()) policy.displayed()
        lifecycle.finish(); lifecycle.shown(); lifecycle.finish()
        assertEquals(before,controller.capture())
        assertEquals(7,controller.wallet.balance); assertEquals(9,controller.hintCredits)
        assertFalse(controller.session.canAdvance)
    }
}
