package com.tilebloom.game

import android.app.Activity
import android.app.AlertDialog
import android.test.ActivityInstrumentationTestCase2
import android.os.SystemClock
import android.widget.Button
import com.tilebloom.game.ads.*
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.MainActivity
import com.tilebloom.game.presentation.GardenIconButton
import com.tilebloom.game.storage.*
import java.io.File

/** All ad behavior here is deterministic; this does not verify Google ad delivery. */
@Suppress("DEPRECATION")
class RewardInteractionTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private val storage = "test-rewarded-ads"
    private lateinit var fake: FakeAds
    private class FakeAds : Advertising {
        override var state = RewardedAdManager.State.READY
        override var privacyRequired = true
        var privacyCalls = 0
        var retryCalls = 0
        override fun retry() { retryCalls++ }
        var showCalls = 0
        var ticket: AdReward? = null
        override fun attach(activity: Activity) {}
        override fun detach(activity: Activity) {}
        override fun privacyOptions(activity: Activity) { privacyCalls++ }
        override fun show(activity: Activity, purpose: RewardPurpose): Boolean {
            showCalls++
            if (state != RewardedAdManager.State.READY) return false
            state = RewardedAdManager.State.SHOWING
            val context = activity.applicationContext
            ticket = AdReward(purpose) { GameStore.reward(context, it) }
            return true
        }
    }
    override fun setUp() {
        super.setUp()
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        File(instrumentation.targetContext.filesDir, storage).writeText(ProgressCodec.encode(PlayerProgress(undoCredits = 0, hintCredits = 0)))
        fake = FakeAds(); AdvertisingService.provider = fake
    }
    override fun tearDown() {
        super.tearDown()
        AdvertisingService.provider = RewardedAdManager
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(null)
        File(instrumentation.targetContext.filesDir, storage).delete()
        File(instrumentation.targetContext.filesDir, "$storage.bak").delete()
    }
    private fun ui(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun waitReady(screen: MainActivity) {
        val deadline = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false; ui { ready = screen.isPuzzleReady }
            if (ready) return
            SystemClock.sleep(25)
        }
        fail("Puzzle not ready")
    }
    private fun dialog(screen: MainActivity): AlertDialog {
        val field = MainActivity::class.java.getDeclaredField("rewardDialog").apply { isAccessible = true }
        return field.get(screen) as AlertDialog
    }
    fun testSelectedRewardsPersistAndDoNotExecuteActions() {
        val screen = activity; waitReady(screen)
        ui {
            screen.findViewById<Button>(R.id.play).performClick()
            val state = GameStore.get(screen)
            val board = state.session.game.snapshot()
            for ((id, purpose) in listOf(R.id.undo to RewardPurpose.UNDO, R.id.hint to RewardPurpose.HINT)) {
                fake.ticket = null
                fake.state = RewardedAdManager.State.READY
                val button = screen.findViewById<GardenIconButton>(id)
                assertTrue(button.isEnabled); assertTrue(button.isClickable)
                assertTrue(button.showsAdIndicator)
                assertEquals(screen.getString(if (purpose == RewardPurpose.HINT) R.string.hint_zero_description else R.string.undo_zero_description), button.contentDescription)
                button.performClick()
                assertTrue(dialog(screen).isShowing)
                val expected = screen.getString(if (purpose == RewardPurpose.HINT) R.string.reward_hint else R.string.reward_undo)
                fun containsReward(view: android.view.View): Boolean =
                    (view is android.widget.TextView && view.text.toString() == expected) ||
                        (view is android.view.ViewGroup && (0 until view.childCount).any { containsReward(view.getChildAt(it)) })
                assertTrue(containsReward(dialog(screen).window!!.decorView))
                assertNull(fake.ticket)

                dialog(screen).findViewById<Button>(android.R.id.button1).performClick()
                assertEquals(purpose, fake.ticket!!.purpose)
                assertTrue(fake.ticket!!.earned()); assertFalse(fake.ticket!!.earned())
                assertEquals(1, button.badge)
                assertFalse(button.showsAdIndicator)
                assertEquals(board, state.session.game.snapshot())
            }
            assertEquals(1, ProgressRepository.load(screen).undoCredits)
            assertEquals(1, ProgressRepository.load(screen).hintCredits)
            screen.findViewById<Button>(R.id.restart).performClick()
            assertEquals(1, state.wallet.balance); assertEquals(1, state.hintCredits)
        }
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        try {
            ui { screen.recreate() }
            val rotated = monitor.waitForActivityWithTimeout(10000) as MainActivity
            waitReady(rotated)
            ui {
                assertEquals(1, GameStore.get(rotated).wallet.balance)
                assertEquals(1, GameStore.get(rotated).hintCredits)
                assertFalse(fake.ticket!!.earned())
                rotated.finish()
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }
    fun testDockDimensionsAndDialogLayout() {
        val screen = activity; waitReady(screen)
        ui { screen.findViewById<Button>(R.id.play).performClick() }
        instrumentation.waitForIdleSync()
        ui {
            val buttons = listOf(R.id.home, R.id.hint, R.id.undo, R.id.restart).map {
                screen.findViewById<GardenIconButton>(it)
            }
            val density = screen.resources.displayMetrics.density
            for (button in buttons) {
                assertEquals(buttons.first().width, button.width)
                assertEquals(buttons.first().height, button.height)
                assertTrue(button.width >= 48 * density)
                assertTrue(button.height >= 48 * density)
                assertFalse(button.contentDescription.isNullOrBlank())
            }
            buttons[1].performClick()
        }
        instrumentation.waitForIdleSync()
        ui {
            val dialog = dialog(screen)
            val watch = dialog.findViewById<Button>(android.R.id.button1)
            val close = dialog.findViewById<Button>(android.R.id.button2)
            assertFalse(close.contentDescription.isNullOrBlank())
            assertEquals(android.view.Gravity.CENTER, watch.gravity)
            assertTrue(watch.width > close.width)
            assertTrue(close.top < watch.top)
            assertTrue(watch.text is android.text.Spanned)
            close.performClick()
            assertEquals(0, GameStore.get(screen).hintCredits)
        }
    }
    fun testPositiveCreditsHideAdvertisingIndicators() {
        val screen = activity; waitReady(screen)
        ui {
            screen.findViewById<Button>(R.id.play).performClick()
            for (id in listOf(R.id.hint, R.id.undo)) {
                val button = screen.findViewById<GardenIconButton>(id)
                assertTrue(button.showsAdIndicator)
                for (credits in 0..3) {
                    button.badge = credits
                    assertEquals(credits, button.badge)
                    assertEquals(credits == 0, button.showsAdIndicator)
                }
            }
        }
    }
    fun testLoadingNeverQueuesAnAdvertisement() {
        val screen = activity; waitReady(screen)
        ui {
            screen.findViewById<Button>(R.id.play).performClick()
            fake.state = RewardedAdManager.State.LOADING
            screen.findViewById<GardenIconButton>(R.id.hint).performClick()
        }
        instrumentation.waitForIdleSync()
        ui {
            assertFalse(dialog(screen).findViewById<Button>(android.R.id.button1).isEnabled)
            assertEquals(0, fake.showCalls)
            assertEquals(0, GameStore.get(screen).hintCredits)
            fake.state = RewardedAdManager.State.READY
        }
        SystemClock.sleep(350)
        instrumentation.waitForIdleSync()
        ui {
            assertTrue(dialog(screen).findViewById<Button>(android.R.id.button1).isEnabled)
            assertEquals(0, fake.showCalls)
            assertNull(fake.ticket)
            dialog(screen).findViewById<Button>(android.R.id.button2).performClick()
            assertFalse(dialog(screen).isShowing)
            assertEquals(0, GameStore.get(screen).hintCredits)
        }
    }
    fun testCancelUnavailableAndPrivacy() {
        val screen = activity; waitReady(screen)
        ui {
            screen.findViewById<Button>(R.id.privacy_options).performClick(); assertEquals(1, fake.privacyCalls)
            screen.findViewById<Button>(R.id.play).performClick()
            screen.findViewById<GardenIconButton>(R.id.undo).performClick()
            dialog(screen).findViewById<Button>(android.R.id.button2).performClick()
            assertNull(fake.ticket)
            fake.state = RewardedAdManager.State.FAILED
            screen.findViewById<GardenIconButton>(R.id.hint).performClick()
            dialog(screen).findViewById<Button>(android.R.id.button1).performClick()
            assertNull(fake.ticket)
            assertEquals(1, fake.retryCalls)
            assertEquals(0, fake.showCalls)
            assertEquals(0, GameStore.get(screen).hintCredits)
            assertEquals(0, GameStore.get(screen).wallet.balance)
            assertTrue(screen.findViewById<Button>(R.id.restart).isEnabled)
        }
    }
}
