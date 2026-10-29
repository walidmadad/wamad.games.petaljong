package com.tilebloom.game

import android.app.AlertDialog
import android.app.Activity
import android.os.SystemClock
import android.test.ActivityInstrumentationTestCase2
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import com.tilebloom.game.ads.*
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.*
import com.tilebloom.game.storage.*
import java.io.File

/** No live inventory, test-only board/frequency stores. These tests do not clear player data. */
@Suppress("DEPRECATION")
class InterstitialInteractionTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private val storage = "interstitial28-test"
    private val originalAds = AdvertisingService.provider
    private val originalInterstitial = InterstitialService.provider
    private val originalNetwork = ConnectivityService.factory
    private var networkChanged: ((Boolean, Boolean) -> Unit)? = null
    private var online = true
    private var attempts = 0
    private var available = true
    private var failDisplay = false
    private var dismiss: (() -> Unit)? = null
    private lateinit var lastTile: Tile
    private fun ui(block: () -> Unit) = instrumentation.runOnMainSync(block)
    override fun setUp() {
        super.setUp()
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        val context = instrumentation.targetContext
        File(context.filesDir, storage).delete(); File(context.filesDir, "$storage.bak").delete()
        val controller = ProgressController(PlayerProgress(undoCredits=0,hintCredits=0))
        val witness = controller.session.level.witness
        witness.dropLast(1).forEach { controller.session.game.select(it.first); controller.session.game.select(it.second) }
        controller.session.game.select(witness.last().first)
        lastTile = controller.session.game.tiles.first { it.id == witness.last().second }
        File(context.filesDir, storage).writeText(ProgressCodec.encode(controller.capture()))
        context.getSharedPreferences(storage, 0).edit().clear().putLong("active",180_000).commit()
        ui {
            AdExposure.useTestStorage(context, storage)
            AdvertisingService.provider = object : Advertising {
                override val state = RewardedAdManager.State.READY
                override val privacyRequired = false
                override fun attach(activity: Activity) {}
                override fun detach(activity: Activity) {}
                override fun privacyOptions(activity: Activity) {}
                override fun show(activity: Activity, purpose: RewardPurpose): Boolean {
                    if (!AdExposure.acquire()) return false
                    AdExposure.displayed(); AdExposure.release() // closed without earning
                    return true
                }
            }
            InterstitialService.provider = object : VictoryAdvertising {
                override val ready get() = available
                override fun prepare(activity: Activity) {}
                override fun show(activity: Activity): Boolean {
                    attempts++
                    if (!available || !AdExposure.acquire()) return false
                    if (failDisplay) { AdExposure.release(); return false }
                    AdExposure.displayed()
                    dismiss = { AdExposure.release() }
                    return true
                }
            }
            ConnectivityService.factory = { _, callback ->
                networkChanged = callback
                object : NetworkObserver {
                    override fun start() { callback(online, !online) }
                    override fun stop() {}
                    override fun retry() { callback(online, !online) }
                    override fun destroy() {}
                }
            }
        }
    }
    override fun tearDown() {
        ui { dismiss?.invoke(); dismiss = null }
        super.tearDown()
        ui {
            AdvertisingService.provider = originalAds; InterstitialService.provider = originalInterstitial
            ConnectivityService.factory = originalNetwork
            AdExposure.useTestStorage(instrumentation.targetContext, null)
        }
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(null)
        File(instrumentation.targetContext.filesDir, storage).delete()
        File(instrumentation.targetContext.filesDir, "$storage.bak").delete()
    }
    private fun waitFor(condition: () -> Boolean) {
        val until = SystemClock.uptimeMillis() + 30_000
        while (SystemClock.uptimeMillis() < until) {
            var done = false; ui { done = condition() }
            if (done) return
            SystemClock.sleep(20)
        }
        fail("UI condition timed out")
    }
    private fun enter(): MainActivity {
        val screen = activity
        waitFor { screen.isPuzzleReady && screen.findViewById<View>(R.id.continue_level).isEnabled && screen.hasWindowFocus() }
        ui { screen.findViewById<View>(R.id.continue_level).performClick() }
        waitFor { screen.findViewById<View>(R.id.board)?.isShown == true }
        return screen
    }
    private fun win(screen: MainActivity) {
        val point = FloatArray(2)
        ui {
            val board = screen.findViewById<MahjongBoardView>(R.id.board)
            val center = board.tileCenter(lastTile.id)!!; val location = IntArray(2)
            board.getLocationOnScreen(location); point[0] = location[0]+center.x; point[1] = location[1]+center.y
        }
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point[0],point[1],0)
            try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
        }
        waitFor { GameStore.get(screen).session.game.status == Status.WON }
    }
    fun testEligibleVictoryDismissalPreservesCreditsAndNextAdvancesOnce() {
        val screen = enter(); val before = GameStore.get(screen).capture()
        assertEquals(0, attempts); win(screen)
        waitFor { attempts == 1 && AdExposure.busy }
        ui {
            val current = GameStore.get(screen).capture()
            assertEquals(before.hintCredits, current.hintCredits)
            assertEquals(before.undoCredits, current.undoCredits)
            dismiss!!.invoke(); dismiss = null
            assertTrue(screen.findViewById<View>(R.id.outcome).isShown)
            screen.findViewById<Button>(R.id.next_level).performClick()
        }
        waitFor { GameStore.get(screen).session.level.id == 2L && !GameStore.transitioning }
        assertEquals(1, attempts)
    }
    fun testShortVictorySkipsAd() {
        val screen = enter(); ui { AdExposure.policy.displayed() }
        win(screen); waitFor { screen.findViewById<Button>(R.id.next_level).isEnabled }
        assertEquals(0, attempts)
    }
    fun testMissingAdNeverDelaysVictory() {
        available = false; val screen = enter(); win(screen)
        ui { assertTrue(screen.findViewById<Button>(R.id.next_level).isEnabled) }
        assertEquals(0, attempts)
    }
    fun testFailedDisplayReturnsToVictoryWithoutReset() {
        failDisplay = true; val screen = enter(); win(screen)
        waitFor { attempts == 1 && screen.findViewById<Button>(R.id.next_level).isEnabled }
        ui { assertEquals(180_000L, AdExposure.policy.duration(SystemClock.elapsedRealtime())); assertFalse(AdExposure.busy) }
    }
    fun testReconnectionDoesNotPresentAndOfflineTimeStops() {
        val screen = enter()
        ui { AdExposure.policy.displayed(); online = false; networkChanged!!(false,true) }
        var before = 0L; ui { before = AdExposure.policy.duration(SystemClock.elapsedRealtime()) }
        SystemClock.sleep(100)
        ui {
            assertEquals(before,AdExposure.policy.duration(SystemClock.elapsedRealtime()))
            online = true; networkChanged!!(true,false)
            assertTrue(screen.findViewById<View>(R.id.board).isShown)
        }
        assertEquals(0,attempts)
    }
    fun testVoluntaryRewardedDisplayWithoutRewardResetsInterstitialCooldown() {
        val screen = enter()
        ui {
            screen.findViewById<View>(R.id.hint).performClick()
            val field = MainActivity::class.java.getDeclaredField("rewardDialog").apply { isAccessible=true }
            val dialog = field.get(screen) as AlertDialog
            dialog.findViewById<Button>(android.R.id.button1).performClick()
            assertEquals(0, GameStore.get(screen).hintCredits)
            assertEquals(0, GameStore.get(screen).wallet.balance)
        }
        win(screen)
        waitFor { screen.findViewById<Button>(R.id.next_level).isEnabled }
        assertEquals(0,attempts)
    }
    fun testPersistedFrequencyReloadDoesNotCountClosedTimeOrLaunchAd() {
        val screen = enter()
        ui {
            AdExposure.policy.active(SystemClock.elapsedRealtime(),false)
            AdExposure.save()
            val saved = AdExposure.policy.duration(SystemClock.elapsedRealtime())
            AdExposure.useTestStorage(screen, storage)
            assertEquals(saved,AdExposure.policy.duration(SystemClock.elapsedRealtime()))
        }
        assertEquals(0,attempts)
    }
    fun testRecreationCannotReplayConsumedVictory() {
        failDisplay = true; val screen = enter(); win(screen)
        waitFor { attempts == 1 && !AdExposure.busy }
        ui { screen.recreate() }
        SystemClock.sleep(500); instrumentation.waitForIdleSync()
        assertEquals(1, attempts)
    }
}
