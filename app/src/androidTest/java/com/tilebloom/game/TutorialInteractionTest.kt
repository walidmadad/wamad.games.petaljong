package com.tilebloom.game

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.test.ActivityInstrumentationTestCase2
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import com.tilebloom.game.ads.*
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.*
import com.tilebloom.game.storage.*
import java.io.File

/** Run on a narrow emulator in French and Arabic. Screenshots require human inspection. */
@Suppress("DEPRECATION")
class TutorialInteractionTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private val storage = "test-tutorial51"
    private val originalNetwork = ConnectivityService.factory
    private val originalAds = AdvertisingService.provider
    private val originalInterstitial = InterstitialService.provider
    private var originalLanguage = ""
    private var adShows = 0
    private fun ui(block: () -> Unit) = instrumentation.runOnMainSync(block)
    override fun setUp() {
        super.setUp()
        val context = instrumentation.targetContext
        originalLanguage = AppLanguage.selected(context)
        ui {
            context.getSharedPreferences(storage, android.content.Context.MODE_PRIVATE).edit().clear().commit()
            AdExposure.useTestStorage(context, storage)
        }
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        File(context.filesDir, storage).delete(); File(context.filesDir, "$storage.bak").delete()
        ConnectivityService.factory = { _, changed -> object : NetworkObserver {
            override fun start() { changed(true, false) }
            override fun stop() {}
            override fun retry() { changed(true, false) }
            override fun destroy() {}
        } }
        AdvertisingService.provider = object : Advertising {
            override val state = RewardedAdManager.State.READY
            override val privacyRequired = false
            override fun attach(activity: Activity) {}
            override fun detach(activity: Activity) {}
            override fun privacyOptions(activity: Activity) {}
            override fun show(activity: Activity, purpose: RewardPurpose): Boolean { adShows++; return false }
        }
        InterstitialService.provider = object : VictoryAdvertising {
            override val ready = true
            override fun prepare(activity: Activity) {}
            override fun show(activity: Activity): Boolean { adShows++; return false }
        }
    }
    override fun tearDown() {
        super.tearDown()
        ConnectivityService.factory = originalNetwork
        AdvertisingService.provider = originalAds; InterstitialService.provider = originalInterstitial
        ui {
            AppLanguage.select(instrumentation.targetContext, originalLanguage)
            AdExposure.useTestStorage(instrumentation.targetContext, null)
            instrumentation.targetContext.deleteSharedPreferences(storage)
        }
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(null)
        File(instrumentation.targetContext.filesDir, storage).delete()
        File(instrumentation.targetContext.filesDir, "$storage.bak").delete()
    }
    private fun waitFor(check: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < deadline) {
            var ready = false; ui { ready = check() }
            if (ready) { instrumentation.waitForIdleSync(); return }
            SystemClock.sleep(20)
        }
        fail("Tutorial did not reach its expected state")
    }
    private fun controller(screen: MainActivity): ProgressController = MainActivity::class.java.getDeclaredField("progress")
        .apply { isAccessible = true }.get(screen) as ProgressController
    private fun click(screen: MainActivity, id: Int) { ui { screen.findViewById<View>(id).performClick() }; instrumentation.waitForIdleSync() }
    private fun screenshot(screen: MainActivity, name: String, target: View? = null, tile: Int? = null) {
        instrumentation.waitForIdleSync()
        ui {
            val overlay = screen.findViewById<TutorialOverlay>(R.id.tutorial_overlay)
            if (target != null) {
                val bubble = overlay.getChildAt(0) as ScrollView
                val bubbleRect = Rect(); bubble.getGlobalVisibleRect(bubbleRect)
                val spot = Rect(); target.getGlobalVisibleRect(spot)
                if (tile != null) {
                    val location = IntArray(2); target.getLocationOnScreen(location)
                    val face = (target as MahjongBoardView).tutorialBounds(tile)
                    face.offset(location[0].toFloat(), location[1].toFloat()); face.roundOut(spot)
                }
                assertFalse("Bubble must leave the target exposed", Rect.intersects(bubbleRect, spot))
            }
        }
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "tutorial51/$name.png")
        file.parentFile!!.mkdirs()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(bitmap)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun tapTarget(screen: MainActivity) {
        val point = FloatArray(2)
        ui {
            val state = controller(screen)
            val id = requireNotNull(state.tutorial.target)
            assertTrue(state.session.game.isAvailable(id))
            val board = screen.findViewById<MahjongBoardView>(R.id.board)
            val center = board.tileCenter(id)!!
            val location = IntArray(2); board.getLocationOnScreen(location)
            point[0] = center.x + location[0]; point[1] = center.y + location[1]
        }
        val now = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, point[0], point[1], 0)
            try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
        }
        instrumentation.waitForIdleSync()
    }
    private fun guided(screen: MainActivity, language: String) {
        waitFor { screen.findViewById<View>(R.id.tutorial_action)?.isShown == true }
        screenshot(screen, "$language-01-welcome")
        click(screen, R.id.tutorial_action)
        val board = screen.findViewById<MahjongBoardView>(R.id.board)
        screenshot(screen, "$language-02-select", board, controller(screen).tutorial.target)
        tapTarget(screen)
        screenshot(screen, "$language-03-tray", screen.findViewById(R.id.tray))
        click(screen, R.id.tutorial_action)
        screenshot(screen, "$language-04-undo", screen.findViewById(R.id.undo))
        click(screen, R.id.undo)
        assertTrue(controller(screen).session.game.tray.isEmpty())
        screenshot(screen, "$language-05-hint", screen.findViewById(R.id.hint))
        click(screen, R.id.hint)
        waitFor { controller(screen).tutorial.step == TutorialStep.COMPLETE_PAIR }
        ui {
            assertNotNull(board.hintedTileId)
            assertTrue(controller(screen).session.game.isAvailable(board.hintedTileId!!))
            assertEquals(board.hintedTileId, controller(screen).tutorial.target)
        }
        screenshot(screen, "$language-06-pair", board, controller(screen).tutorial.target)
        tapTarget(screen); tapTarget(screen)
        assertEquals(TutorialStep.FINISHED, controller(screen).tutorial.step)
        assertEquals(3, controller(screen).wallet.balance); assertEquals(3, controller(screen).hintCredits)
        assertEquals(2, controller(screen).session.game.clearedTiles.size)
        screenshot(screen, "$language-07-complete")
        click(screen, R.id.tutorial_action)
    }
    private fun scenario(language: String) {
        ui { AppLanguage.select(instrumentation.targetContext, language) }
        val screen = activity
        waitFor { screen.isPuzzleReady }
        click(screen, R.id.continue_level)
        guided(screen, language)
        if (language == "ar") ui { assertEquals(View.LAYOUT_DIRECTION_RTL, screen.findViewById<View>(R.id.gameplay).layoutDirection) }
        val before = controller(screen).capture()
        click(screen, R.id.settings)
        val dialog = MainActivity::class.java.getDeclaredField("settingsDialog").apply { isAccessible = true }.get(screen) as AlertDialog
        assertTrue(dialog.findViewById<View>(R.id.how_to_play).isShown)
        // Settings is a separate window; capture through UiAutomation.
        screenshot(screen, "$language-08-settings")
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        var training: MainActivity? = null
        try {
            ui { dialog.findViewById<View>(R.id.how_to_play).performClick() }
            training = monitor.waitForActivityWithTimeout(10000) as? MainActivity
            assertNotNull(training)
            waitFor { training!!.isPuzzleReady }
            guided(training!!, "$language-practice")
            waitFor { screen.hasWindowFocus() }
            assertEquals(before, controller(screen).capture())
            ProgressRepository.awaitWrites()
            assertEquals(before, ProgressController(ProgressRepository.load(instrumentation.targetContext)).capture())
            assertEquals(0, adShows)
        } finally { ui { training?.finish() }; instrumentation.removeMonitor(monitor) }
    }
    fun testPracticePreservesAnExistingPlayersRevealedMysteryAndHistory() {
        val context = instrumentation.targetContext
        File(context.filesDir, storage).writeText(ProgressCodec.encode(PlayerProgress(
            highestCompleted = 36, undoCredits = 2, hintCredits = 1, tutorial = TutorialStep.SKIPPED)))
        val screen = activity
        waitFor { screen.isPuzzleReady }
        click(screen, R.id.continue_level)
        val state = controller(screen)
        ui {
            assertNull(screen.findViewById<View>(R.id.tutorial_overlay))
            val game = state.session.game
            for (action in state.session.level.winningActions) {
                val mystery = game.remaining.firstOrNull { it.mystery && game.isAvailable(it.id) }
                if (mystery != null) { assertTrue(game.reveal(mystery.id)); break }
                assertTrue(game.interact(action.tileId))
            }
            assertNotNull(game.activeMysteryId)
        }
        val before = state.capture()
        click(screen, R.id.settings)
        val dialog = MainActivity::class.java.getDeclaredField("settingsDialog").apply { isAccessible = true }.get(screen) as AlertDialog
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        var training: MainActivity? = null
        try {
            ui { dialog.findViewById<View>(R.id.how_to_play).performClick() }
            training = monitor.waitForActivityWithTimeout(10000) as? MainActivity
            assertNotNull(training)
            waitFor { training!!.findViewById<View>(R.id.tutorial_skip)?.isShown == true }
            click(training!!, R.id.tutorial_skip)
            waitFor { screen.hasWindowFocus() }
            assertEquals(before, state.capture())
            assertEquals(37L, state.session.level.id)
            assertEquals(0, adShows)
        } finally { ui { training?.finish() }; instrumentation.removeMonitor(monitor) }
    }
    fun testFrenchGuidanceAndIsolatedSettingsPractice() = scenario("fr")
    fun testArabicGuidanceAndIsolatedSettingsPractice() = scenario("ar")
}
