package com.wamad.petaljong

import android.view.View
import android.content.Intent
import android.os.SystemClock
import android.test.ActivityInstrumentationTestCase2
import android.view.MotionEvent
import android.widget.Button
import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.*
import com.wamad.petaljong.storage.*
import java.io.File

@Suppress("DEPRECATION")
class HintInteractionTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private val storage = "test-hints"
    private fun ui(block: () -> Unit) = instrumentation.runOnMainSync(block)
    override fun setUp() {
        super.setUp(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        File(instrumentation.targetContext.filesDir, storage).delete()
        File(instrumentation.targetContext.filesDir, "$storage.bak").delete()
        seedReturningPlayer(instrumentation.targetContext, storage)
    }
    override fun tearDown() {
        super.tearDown(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(null)
        File(instrumentation.targetContext.filesDir, storage).delete()
        File(instrumentation.targetContext.filesDir, "$storage.bak").delete()
    }
    private fun waitFor(check: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < end) {
            var ready = false; ui { ready = check() }
            if (ready) return
            SystemClock.sleep(20)
        }
        fail("Hint UI did not become ready")
    }
    private fun enter(screen: MainActivity) {
        waitFor { screen.isPuzzleReady }
        ui { screen.findViewById<View>(R.id.continue_level).performClick() }
        waitFor { screen.findViewById<MahjongBoardView>(R.id.board)?.width?.let { it > 0 } == true }
    }
    fun testHintChargesOnceAndTileRemainsSelectable() {
        val screen = activity; enter(screen)
        val state = GameStore.get(instrumentation.targetContext)
        val before = state.session.game.snapshot()
        ui {
            val button = screen.findViewById<GardenIconButton>(R.id.hint)
            assertEquals(3, button.badge)
            repeat(10) { button.performClick() }
        }
        waitFor { screen.findViewById<MahjongBoardView>(R.id.board).hintedTileId != null }
        var id = -1
        ui {
            val button = screen.findViewById<GardenIconButton>(R.id.hint)
            repeat(10) { button.performClick() }
            assertEquals(2, button.badge)
            assertEquals(3, state.wallet.balance)
            assertEquals(before, state.session.game.snapshot())
            val board = screen.findViewById<MahjongBoardView>(R.id.board)
            id = board.hintedTileId!!
            assertEquals(HintPolicy.suggest(state.session.level.tiles, before).id, id)
            assertTrue(state.session.game.isAvailable(id))
            val center = board.tileCenter(id)!!
            val now = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(now, now + 10, action, center.x, center.y, 0)
                board.dispatchTouchEvent(event); event.recycle()
            }
            assertNull(board.hintedTileId)
            assertEquals(listOf(id), state.session.game.snapshot().selections)
            screen.findViewById<Button>(R.id.restart).performClick()
            assertEquals(2, state.hintCredits)
        }
        ProgressRepository.awaitWrites()
        assertEquals(2, ProgressRepository.load(instrumentation.targetContext).hintCredits)
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        var replacement: MainActivity? = null
        try {
            ui { screen.recreate() }
            replacement = monitor.waitForActivityWithTimeout(10000) as? MainActivity
            assertNotNull(replacement)
            val rotated = replacement!!
            waitFor { rotated.findViewById<GardenIconButton>(R.id.hint)?.isShown == true }
            ui {
                assertEquals(2, rotated.findViewById<GardenIconButton>(R.id.hint).badge)
                assertNull(rotated.findViewById<MahjongBoardView>(R.id.board).hintedTileId)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            replacement?.let { rotated -> ui { rotated.finish() } }
        }
        instrumentation.waitForIdleSync(); ProgressRepository.awaitWrites()
        ProgressRepository.useTestStorage(storage) // Drop process cache, retain actual disk save.
        val fresh = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            enter(fresh)
            ui { assertEquals(2, fresh.findViewById<GardenIconButton>(R.id.hint).badge) }
        } finally { ui { fresh.finish() } }
    }
    fun testExpirationRestartAndHomeClearFeedbackWithoutCharging() {
        val screen = activity; enter(screen)
        val state = GameStore.get(instrumentation.targetContext)
        val board = screen.findViewById<MahjongBoardView>(R.id.board)
        val id = HintPolicy.suggest(state.session.level.tiles, state.session.game.snapshot()).id!!
        ui { screen.findViewById<GardenIconButton>(R.id.hint).performClick() }
        waitFor { board.hintedTileId != null }
        ui { assertEquals(id, board.hintedTileId); assertEquals(2, state.hintCredits) }
        waitFor { board.hintedTileId == null }
        ui {
            assertEquals(2, state.hintCredits)
            board.showHint(id)
            screen.findViewById<GardenIconButton>(R.id.restart).performClick()
            assertNull(board.hintedTileId)
            board.showHint(id)
            board.load(Game(Levels.get(7).tiles))
            assertNull(board.hintedTileId)
            board.load(state.session.game)
            board.showHint(id)
            screen.findViewById<GardenIconButton>(R.id.home).performClick()
            assertNull(board.hintedTileId)
            assertEquals(2, state.hintCredits)
        }
    }
    fun testZeroOffersRewardAfterRestore() {
        File(instrumentation.targetContext.filesDir, storage).writeText(ProgressCodec.encode(PlayerProgress(tutorial = com.wamad.petaljong.domain.TutorialStep.SKIPPED, hintCredits = 0)))
        val screen = activity; enter(screen)
        ui {
            val button = screen.findViewById<GardenIconButton>(R.id.hint)
            assertEquals(0, button.badge); assertTrue(button.isEnabled)
            button.performClick()
            assertNull(screen.findViewById<MahjongBoardView>(R.id.board).hintedTileId)
        }
    }
}
