package com.tilebloom.game

import android.app.Instrumentation
import android.os.SystemClock
import android.test.ActivityInstrumentationTestCase2
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.TextView
import com.tilebloom.game.domain.LevelOne
import com.tilebloom.game.presentation.MainActivity
import com.tilebloom.game.presentation.MahjongBoardView
import com.tilebloom.game.presentation.board.BoardPoint
import java.util.concurrent.atomic.AtomicReference

@Suppress("DEPRECATION")
class LaunchSmokeTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
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
        LevelOne.witness.forEachIndexed { index,pair ->
            tap(pair.first); ui { assertEquals(pair.first,board().selectedTileId) }
            tap(pair.second); assertCount(22-index*2); ui { assertNull(board().selectedTileId) }
        }
        ui {
            assertTrue(activity.findViewById<TextView>(R.id.feedback).text.toString().contains(activity.getString(R.string.won)))
            assertEquals(activity.getString(R.string.play_again),activity.findViewById<Button>(R.id.restart).text.toString())
            activity.findViewById<Button>(R.id.restart).performClick()
        }
        assertCount(24); tap(16); ui { assertEquals(16,board().selectedTileId) }
    }
    fun testCoordinateDeadlockAndRestart() {
        activity; instrumentation.waitForIdleSync()
        for ((a,b) in listOf(12 to 15,14 to 23,16 to 20,13 to 21,0 to 4)) { tap(a); tap(b) }
        assertCount(14)
        ui {
            assertTrue(activity.findViewById<TextView>(R.id.feedback).text.toString().contains(activity.getString(R.string.no_moves)))
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
            assertCount(22,restored); ui { assertEquals(17,board(restored).selectedTileId) }
            tap(21,restored); assertCount(20,restored)
        } finally {
            instrumentation.removeMonitor(monitor)
            replacement?.let { screen -> ui { screen.finish() } }
        }
    }
}
