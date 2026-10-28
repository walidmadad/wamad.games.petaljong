package com.tilebloom.game

import android.test.ActivityInstrumentationTestCase2
import android.os.SystemClock
import android.view.View
import android.view.MotionEvent
import com.tilebloom.game.presentation.*
import com.tilebloom.game.domain.*
import com.tilebloom.game.storage.*
import com.tilebloom.game.ads.*
import java.io.File

@Suppress("DEPRECATION")
class ConnectivityInteractionTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private val storage = "test-connectivity25"
    private lateinit var source: Source
    private var connected = false
    private val originalFactory = ConnectivityService.factory
    private val originalAds = AdvertisingService.provider
    private inner class Source(val changed: (Boolean, Boolean) -> Unit) : NetworkObserver {
        var started = false
        override fun start() { started = true; emit() }
        override fun stop() { started = false }
        override fun destroy() { stop() }
        override fun retry() { emit() }
        fun emit() { if (started) changed(connected, !connected) }
    }
    private fun ui(block: () -> Unit) = instrumentation.runOnMainSync(block)
    override fun setUp() {
        super.setUp()
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        File(instrumentation.targetContext.filesDir, storage).delete()
        File(instrumentation.targetContext.filesDir, "$storage.bak").delete()
        ConnectivityService.factory = { _, changed -> Source(changed).also { source = it } }
        // Internet access must work even with consent denied and no ad inventory.
        AdvertisingService.provider = object : Advertising {
            override val state = RewardedAdManager.State.IDLE
            override val privacyRequired = true
            override fun attach(activity: android.app.Activity) {}
            override fun detach(activity: android.app.Activity) {}
            override fun privacyOptions(activity: android.app.Activity) {}
            override fun show(activity: android.app.Activity, purpose: RewardPurpose) = false
        }
    }
    override fun tearDown() {
        super.tearDown()
        ConnectivityService.factory = originalFactory; AdvertisingService.provider = originalAds
        ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(null)
        File(instrumentation.targetContext.filesDir, storage).delete()
        File(instrumentation.targetContext.filesDir, "$storage.bak").delete()
    }
    private fun ready(screen: MainActivity) {
        val end = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < end) {
            var ready = false
            ui { ready = screen.isPuzzleReady }
            if (ready) { instrumentation.waitForIdleSync(); return }
            SystemClock.sleep(20)
        }
        fail("Puzzle did not restore")
    }
    private fun network(value: Boolean) { ui { connected = value; source.emit() } }
    fun testOfflineHomeAndLossPreserveExactStateAndCredits() {
        val screen = activity; ready(screen)
        ui {
            assertFalse(screen.findViewById<View>(R.id.play).isEnabled)
            assertTrue(screen.findViewById<View>(R.id.connection_panel).isShown)
            assertTrue(screen.findViewById<View>(R.id.start_level).isShown)
            assertTrue(screen.findViewById<View>(R.id.privacy_options).isShown)
            screen.findViewById<View>(R.id.play).performClick()
            assertNull(screen.findViewById<View>(R.id.board))
        }
        network(true)
        ui { screen.findViewById<View>(R.id.play).performClick() }
        instrumentation.waitForIdleSync()
        val controller = GameStore.get(instrumentation.targetContext)
        ui {
            assertTrue(controller.session.game.select(controller.session.level.witness.first().first))
            GameStore.persist(screen)
        }
        val before = controller.capture()
        network(false)
        ui {
            assertTrue(screen.findViewById<View>(R.id.connection_overlay).isShown)
            for (id in listOf(R.id.hint, R.id.undo, R.id.restart, R.id.home)) {
                assertFalse(screen.findViewById<View>(id).isEnabled)
                screen.findViewById<View>(id).performClick() // also exercise listener guards
            }
            val board = screen.findViewById<View>(R.id.board)
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(0, 0, action, board.width / 2f, board.height / 2f, 0)
                board.dispatchTouchEvent(event); event.recycle()
            }
            assertEquals(before, controller.capture())
            assertEquals(1, controller.session.game.tray.size)
        }
        network(true)
        ui {
            assertEquals(View.GONE, screen.findViewById<View>(R.id.connection_overlay).visibility)
            assertTrue(screen.findViewById<View>(R.id.board).isEnabled)
            assertEquals(before, controller.capture())
        }
    }
    fun testOfflineVictoryCannotAdvance() {
        connected = true
        val screen = activity; ready(screen)
        ui { screen.findViewById<View>(R.id.play).performClick() }
        val controller = GameStore.get(instrumentation.targetContext)
        ui { controller.session.level.witness.forEach { pair ->
            assertTrue(controller.session.game.select(pair.first)); assertTrue(controller.session.game.select(pair.second))
        } }
        network(false)
        val before = controller.capture()
        ui { screen.findViewById<View>(R.id.next_level).performClick(); assertEquals(before, controller.capture()) }
        network(true)
        ui { assertEquals(before, controller.capture()); assertTrue(screen.findViewById<View>(R.id.next_level).isEnabled) }
    }
    fun testActivityRecreationKeepsOfflinePresentationAndPuzzle() {
        connected = true
        val screen = activity; ready(screen)
        ui { screen.findViewById<View>(R.id.play).performClick() }
        val controller = GameStore.get(instrumentation.targetContext)
        ui { assertTrue(controller.session.game.select(controller.session.level.witness.first().first)) }
        network(false)
        val before = controller.capture()
        val previous = source
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        ui { screen.recreate() }
        val replacement = instrumentation.waitForMonitorWithTimeout(monitor, 10000) as? MainActivity
            ?: error("Activity recreation failed")
        instrumentation.removeMonitor(monitor)
        try {
            ready(replacement)
            ui {
                assertFalse(previous.started)
                assertTrue(source.started)
                assertTrue(replacement.findViewById<View>(R.id.connection_overlay).isShown)
                assertEquals(before, controller.capture())
            }
            network(true)
            ui {
                assertTrue(replacement.findViewById<View>(R.id.board).isEnabled)
                assertEquals(before, controller.capture())
            }
        } finally { ui { replacement.finish() } }
    }
    fun testAllSymbolsRenderAtBoardAndTrayScale() {
        val bitmap = android.graphics.Bitmap.createBitmap(160, 160, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        for (symbol in Symbol.entries) for (scale in listOf(10f, 30f)) {
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            canvas.save(); canvas.translate(80f, 80f); canvas.scale(scale, scale)
            BotanicalSymbols.draw(canvas, paint, symbol); canvas.restore()
            val pixels = IntArray(160 * 160); bitmap.getPixels(pixels, 0, 160, 0, 0, 160, 160)
            assertTrue(symbol.name, pixels.count { it != 0 } > 30)
            assertTrue(BotanicalSymbols.label(instrumentation.targetContext, symbol).isNotBlank())
        }
        bitmap.recycle()
    }
}
