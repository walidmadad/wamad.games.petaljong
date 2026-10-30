package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class ConnectivityStateTest {
    @Test fun unknownStartsChecking() { assertEquals(ConnectivityStatus.CHECKING, ConnectivityState().status) }
    @Test fun immediateValidationNeverVisitsOffline() {
        val state = ConnectivityState(); state.begin(0)
        assertEquals(ConnectivityStatus.CHECKING, state.status)
        state.observe(true, 0)
        assertEquals(ConnectivityStatus.ONLINE, state.status)
        state.tick(10000); assertEquals(ConnectivityStatus.ONLINE, state.status)
    }
    @Test fun delayedValidationCancelsDeadline() {
        val state = ConnectivityState(100); state.begin(0); state.observe(false, 10)
        state.tick(99); assertEquals(ConnectivityStatus.CHECKING, state.status)
        state.observe(true, 99); state.tick(100)
        assertEquals(ConnectivityStatus.ONLINE, state.status)
    }
    @Test fun missingAndCaptiveNetworksAreBounded() {
        for (capabilities in listOf(false to false, true to false, false to true)) {
            val state = ConnectivityState(100); state.begin(0)
            state.observe(InternetPolicy.validated(capabilities.first, capabilities.second), 1)
            state.observe(false, 99); state.tick(100)
            assertEquals(ConnectivityStatus.OFFLINE, state.status)
        }
    }
    @Test fun noCallbackAlsoTimesOut() {
        val state = ConnectivityState(100); state.begin(0); state.tick(100)
        assertEquals(ConnectivityStatus.OFFLINE, state.status)
    }
    @Test fun staleSnapshotCannotOverwriteCapabilities() {
        val state = ConnectivityState(); state.begin(0); val revision = state.revision
        state.observe(true, 1); state.snapshot(false, 2, revision); state.tick(5000)
        assertEquals(ConnectivityStatus.ONLINE, state.status)
    }
    @Test fun handoverBlocksInputAndCancelsOffline() {
        val state = ConnectivityState(handoverMs = 60); state.begin(0); state.observe(true, 0)
        state.observe(false, 10); assertEquals(ConnectivityStatus.CHECKING, state.status)
        state.tick(69); assertEquals(ConnectivityStatus.CHECKING, state.status)
        state.observe(true, 69); state.tick(70); assertEquals(ConnectivityStatus.ONLINE, state.status)
    }
    @Test fun sustainedLossDoesNotExtendDeadlineAndRestoresAutomatically() {
        val state = ConnectivityState(handoverMs = 60); state.begin(0); state.observe(true, 0)
        state.observe(false, 10); state.observe(false, 69); state.tick(70)
        assertEquals(ConnectivityStatus.OFFLINE, state.status)
        state.observe(true, 71); assertEquals(ConnectivityStatus.ONLINE, state.status)
    }
    @Test fun retryAndResumeReverifyWithoutOfflineFlash() {
        val state = ConnectivityState(100); state.begin(0); state.tick(100)
        state.begin(101); assertEquals(ConnectivityStatus.CHECKING, state.status)
        state.observe(true, 101); assertEquals(ConnectivityStatus.ONLINE, state.status)
        state.begin(200); assertEquals(ConnectivityStatus.CHECKING, state.status)
        state.observe(true, 200); assertEquals(ConnectivityStatus.ONLINE, state.status)
    }
}
