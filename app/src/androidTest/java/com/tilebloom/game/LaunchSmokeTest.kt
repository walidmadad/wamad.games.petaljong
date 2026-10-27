package com.tilebloom.game

import android.app.Instrumentation
import android.os.SystemClock
import android.test.ActivityInstrumentationTestCase2
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.TextView
import com.tilebloom.game.domain.Levels
import com.tilebloom.game.presentation.MainActivity
import com.tilebloom.game.presentation.MahjongBoardView
import com.tilebloom.game.presentation.board.BoardPoint
import java.util.concurrent.atomic.AtomicReference

@Suppress("DEPRECATION")
class LaunchSmokeTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    override fun setUp() {
        super.setUp()
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress").delete()
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress.bak").delete()
    }
    override fun tearDown() {
        super.tearDown()
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress").delete()
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress.bak").delete()
        com.tilebloom.game.storage.ProgressRepository.useTestStorage(null)
    }
    fun testDurableStoreWithoutBundle() {
        activity
        tap(16); tap(20)
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        // Drop process memory cache, then read the real AtomicFile without any Bundle.
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        val p = com.tilebloom.game.domain.ProgressController(com.tilebloom.game.storage.ProgressRepository.load(instrumentation.targetContext))
        assertEquals(11, p.session.game.remainingPairs)
        assertEquals(listOf(com.tilebloom.game.domain.PairMove(16,20)), p.session.game.snapshot().moves)
        assertFalse(p.select(2))
    }
    fun testCompletionDurableAndReplayPreservesIt() {
        activity
        Levels.get(1).witness.forEach { tap(it.first); tap(it.second) }
        ui { activity.findViewById<Button>(R.id.replay_level).performClick() }
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        val p = com.tilebloom.game.domain.ProgressController(com.tilebloom.game.storage.ProgressRepository.load(instrumentation.targetContext))
        assertEquals(setOf(1), p.capture().completed)
        assertTrue(p.select(2)); assertFalse(p.select(3))
        assertEquals(2,p.session.level.id)
    }
    fun testStorageFailureIsReportedAndRecovers() {
        val repository = com.tilebloom.game.storage.ProgressRepository
        val blocker = java.io.File(instrumentation.targetContext.filesDir, "test-blocked")
        blocker.writeText("blocks directory creation")
        try {
            repository.useTestStorage("test-blocked/progress")
            val token = repository.acquire()
            repository.save(instrumentation.targetContext, token, com.tilebloom.game.domain.ProgressController().capture())
            repository.awaitWrites()
            assertTrue(repository.lastWriteFailed)
            assertEquals(1,repository.load(instrumentation.targetContext).highestUnlocked)
        } finally { blocker.delete() }
    }
    fun testFreshActivityLaunchWithoutBundleRestoresMoves() {
        val original = activity
        tap(16); tap(20)
        ui { original.finish() }
        instrumentation.waitForIdleSync()
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val relaunched = instrumentation.startActivitySync(intent) as MainActivity
        try {
            instrumentation.waitForIdleSync()
            assertCount(22,relaunched)
            ui { assertEquals(0,board(relaunched).activeMatchEffectCount) }
        } finally { ui { relaunched.finish() } }
    }
    private fun <T> ui(action: () -> T): T {
        val result = AtomicReference<T>()
        instrumentation.runOnMainSync { result.set(action()) }
        return result.get()
    }
    private fun board(screen: MainActivity = activity) = screen.findViewById<MahjongBoardView>(R.id.board)
    private fun target(id: Int, screen: MainActivity = activity): BoardPoint = ui {
        val view = board(screen)
        val local = view.tileCenter(id) ?: error("Tile $id no longer exists")
        val location = IntArray(2); view.getLocationOnScreen(location)
        BoardPoint(local.x + location[0], local.y + location[1])
    }
    private fun send(action: Int, point: BoardPoint, down: Long) {
        val event = MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point.x,point.y,0)
        try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
    }
    private fun tap(id: Int, screen: MainActivity = activity) {
        instrumentation.waitForIdleSync()
        val p = target(id,screen); val now = SystemClock.uptimeMillis()
        send(MotionEvent.ACTION_DOWN,p,now); send(MotionEvent.ACTION_UP,p,now)
        instrumentation.waitForIdleSync()
    }
    private fun assertCount(expected: Int, screen: MainActivity = activity) {
        ui {
            assertEquals(expected,board(screen).remainingTileCount)
            assertEquals(screen.getString(R.string.pairs,expected/2),screen.findViewById<TextView>(R.id.counter).text.toString())
        }
    }
    fun testLaunchAndRestart() {
        val screen = activity; instrumentation.waitForIdleSync()
        ui {
            assertTrue(board().isShown); assertTrue(board().width > 0); assertTrue(board().height > 0)
            assertNotNull(screen.findViewById<Button>(R.id.restart))
            screen.findViewById<Button>(R.id.restart).performClick()
            assertFalse(screen.isFinishing)
        }
        assertCount(24)
    }
    fun testCoordinateWitnessAndPlayAgain() {
        activity; instrumentation.waitForIdleSync(); assertCount(24)
        Levels.get(1).witness.forEachIndexed { index,pair ->
            tap(pair.first); ui { assertEquals(pair.first,board().selectedTileId) }
            tap(pair.second); assertCount(22-index*2); ui { assertNull(board().selectedTileId) }
        }
        ui {
            assertFalse(activity.findViewById<TextView>(R.id.feedback).isShown)
            assertFalse(activity.findViewById<Button>(R.id.restart).isShown)
            assertTrue(activity.findViewById<Button>(R.id.next_level).isShown)
            assertTrue(activity.findViewById<Button>(R.id.replay_level).isShown)
            fun countTitles(view: android.view.View): Int {
                if (!view.isShown) return 0
                val own = if (view is TextView && view.text.toString() == activity.getString(R.string.won)) 1 else 0
                return own + if (view is android.view.ViewGroup) (0 until view.childCount).sumOf { countTitles(view.getChildAt(it)) } else 0
            }
            assertEquals(1, countTitles(activity.window.decorView))
            activity.findViewById<Button>(R.id.replay_level).performClick()
        }
        assertCount(24); tap(16); ui { assertEquals(16,board().selectedTileId) }
    }
    fun testCoordinateDeadlockAndRestart() {
        activity; instrumentation.waitForIdleSync()
        for ((a,b) in listOf(16 to 20,0 to 4,8 to 12,17 to 21,1 to 5,9 to 13,15 to 18,19 to 23)) { tap(a); tap(b) }
        assertCount(8)
        ui {
            assertTrue(activity.findViewById<TextView>(R.id.feedback).text.toString().contains(activity.getString(R.string.no_moves)))
            assertFalse(activity.findViewById<Button>(R.id.next_level).isShown)
            assertFalse(activity.findViewById<Button>(R.id.replay_level).isShown)
            assertTrue(activity.findViewById<Button>(R.id.restart).isShown)
            activity.findViewById<Button>(R.id.restart).performClick()
        }
        assertCount(24); ui { assertNull(board().selectedTileId) }
    }
    fun testSelectionBlockedTapAndDrag() {
        activity; instrumentation.waitForIdleSync()
        tap(16); tap(17) // 17 is visible but laterally blocked; it must not replace selection.
        ui { assertEquals(16,board().selectedTileId) }; assertCount(24)
        tap(16); ui { assertNull(board().selectedTileId) }
        tap(16); tap(19) // Available but different symbols: move selection.
        ui { assertEquals(19,board().selectedTileId) }; assertCount(24)
        tap(19)
        val p = target(16); val down = SystemClock.uptimeMillis()
        val distance = ui { ViewConfiguration.get(activity).scaledTouchSlop + 12f }
        send(MotionEvent.ACTION_DOWN,p,down)
        send(MotionEvent.ACTION_MOVE,BoardPoint(p.x+distance,p.y),down)
        send(MotionEvent.ACTION_MOVE,p,down); send(MotionEvent.ACTION_UP,p,down)
        instrumentation.waitForIdleSync(); ui { assertNull(board().selectedTileId) }
        assertCount(24)
    }
    fun testRestartCancelsPendingTouchAndBlockedFeedback() {
        activity; instrumentation.waitForIdleSync(); tap(17)
        val p = target(16); val down = SystemClock.uptimeMillis()
        send(MotionEvent.ACTION_DOWN,p,down)
        ui { activity.findViewById<Button>(R.id.restart).performClick() }
        send(MotionEvent.ACTION_UP,p,down)
        SystemClock.sleep(180); instrumentation.waitForIdleSync()
        assertCount(24); ui { assertNull(board().selectedTileId) }
        tap(16); ui { assertEquals(16,board().selectedTileId) }
    }
    fun testRapidTouchesDuringBlockedFeedbackAndAfterRestart() {
        activity; instrumentation.waitForIdleSync()
        tap(16); tap(17); tap(20)
        assertCount(22); ui { assertNull(board().selectedTileId) }
        // Rapidly toggle one available tile; it must never match itself.
        val p = target(17)
        repeat(12) {
            val now = SystemClock.uptimeMillis()
            send(MotionEvent.ACTION_DOWN,p,now); send(MotionEvent.ACTION_UP,p,now)
        }
        instrumentation.waitForIdleSync(); assertCount(22)
        ui { activity.findViewById<Button>(R.id.restart).performClick() }
        tap(16); tap(20); assertCount(22)
    }
    fun testBatchedReleaseMovementCancelsTap() {
        activity; instrumentation.waitForIdleSync()
        ui {
            val view = board()
            val p = view.tileCenter(16)!!
            val now = SystemClock.uptimeMillis()
            val press = MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,p.x,p.y,0)
            try { view.dispatchTouchEvent(press) } finally { press.recycle() }
            val distance = ViewConfiguration.get(activity).scaledTouchSlop + 12f
            val release = MotionEvent.obtain(now,now+1,MotionEvent.ACTION_MOVE,p.x+distance,p.y,0)
            release.addBatch(now+2,p.x,p.y,1f,1f,0)
            release.action = MotionEvent.ACTION_UP
            try {
                assertEquals(1,release.historySize)
                view.dispatchTouchEvent(release)
            } finally { release.recycle() }
            assertNull(view.selectedTileId)
        }
        assertCount(24); tap(16); ui { assertEquals(16,board().selectedTileId) }
    }
    fun testMatchEffectsRestartAndCompletion() {
        activity; instrumentation.waitForIdleSync()
        // Dispatch through the real view in one UI turn so no frame can expire copies early.
        ui {
            val view = board()
            fun touch(id: Int) {
                val p = view.tileCenter(id)!!
                val now = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(now, now, action, p.x, p.y, 0)
                    try { view.dispatchTouchEvent(event) } finally { event.recycle() }
                }
            }
            touch(16); touch(20)
            assertEquals(22, view.remainingTileCount)
            assertNull(view.tileCenter(16))
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
                assertEquals(2, view.activeMatchEffectCount)
            } else assertEquals(0, view.activeMatchEffectCount)
            touch(17); touch(21)
            assertEquals(20, view.remainingTileCount)
            activity.findViewById<Button>(R.id.restart).performClick()
            assertEquals(0, view.activeMatchEffectCount)
            assertEquals(24, view.remainingTileCount)
            touch(16); touch(20)
        }
        SystemClock.sleep(220); instrumentation.waitForIdleSync()
        ui { assertEquals(0, board().activeMatchEffectCount) }
        assertCount(22)
        tap(17); tap(21); assertCount(20)
    }
    fun testActivityRecreationRestoresPlayedState() {
        val original = activity; instrumentation.waitForIdleSync()
        tap(16); tap(20); tap(17); assertCount(22)
        val monitor = Instrumentation.ActivityMonitor(MainActivity::class.java.name,null,false)
        instrumentation.addMonitor(monitor)
        var replacement: MainActivity? = null
        try {
            ui { original.recreate() }
            replacement = monitor.waitForActivityWithTimeout(5000) as? MainActivity
            assertNotNull("Recreated activity did not launch",replacement)
            instrumentation.waitForIdleSync()
            val restored = replacement!!
            assertCount(22,restored); ui {
                assertEquals(17,board(restored).selectedTileId)
                assertEquals(0,board(restored).activeMatchEffectCount)
            }
            tap(21,restored); assertCount(20,restored)
        } finally {
            instrumentation.removeMonitor(monitor)
            replacement?.let { screen -> ui { screen.finish() } }
        }
    }
    fun testCoordinateProgressionThroughFiveGardens() {
        activity; instrumentation.waitForIdleSync()
        for (id in 1..5) {
            val definition = com.tilebloom.game.domain.Levels.get(id)
            assertCount(definition.tiles.size)
            definition.witness.forEach { tap(it.first); tap(it.second) }
            assertCount(0)
            ui {
                val next = activity.findViewById<Button>(R.id.next_level)
                assertEquals(if (id < 5) android.view.View.VISIBLE else android.view.View.GONE, next.visibility)
                assertEquals(0, board().remainingTileCount)
                if (id < 5) {
                    next.performClick()
                    assertEquals(0, board().activeMatchEffectCount)
                    assertNull(board().selectedTileId)
                }
            }
            if (id < 5) {
                val nextDefinition = com.tilebloom.game.domain.Levels.get(id + 1)
                assertFalse(definition.layout == nextDefinition.layout)
                val pair = nextDefinition.witness.first()
                tap(pair.first); tap(pair.second)
                assertCount(nextDefinition.tiles.size - 2)
                ui { activity.findViewById<Button>(R.id.restart).performClick() }
                assertCount(nextDefinition.tiles.size)
            }
        }
    }

}
