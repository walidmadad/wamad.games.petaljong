package com.tilebloom.game

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.test.ActivityInstrumentationTestCase2
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.*
import com.tilebloom.game.storage.*
import java.io.File

@Suppress("DEPRECATION")
class LevelStartTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private val storage = "test-level-start"
    override fun setUp() {
        super.setUp(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        File(instrumentation.targetContext.filesDir,storage).delete()
        File(instrumentation.targetContext.filesDir,"$storage.bak").delete()
    }
    override fun tearDown() {
        super.tearDown(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(null)
        File(instrumentation.targetContext.filesDir,storage).delete()
        File(instrumentation.targetContext.filesDir,"$storage.bak").delete()
    }
    private fun ui(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun state() = GameStore.get(instrumentation.targetContext)
    private fun seed(controller: ProgressController) {
        File(instrumentation.targetContext.filesDir,storage).writeText(ProgressCodec.encode(controller.capture()))
    }
    private fun waitVisible(screen: MainActivity, id: Int) {
        val until=SystemClock.uptimeMillis()+30000
        while(SystemClock.uptimeMillis()<until) {
            var ready=false
            ui { ready=screen.findViewById<View>(id)?.let { it.isShown && it.width>0 }==true }
            if(ready) { instrumentation.waitForIdleSync(); return }
            SystemClock.sleep(20)
        }
        fail("View $id did not appear")
    }
    private fun enter(screen: MainActivity) {
        waitVisible(screen,R.id.play); ui { screen.findViewById<Button>(R.id.play).performClick() }
        waitVisible(screen,R.id.board)
    }
    private fun tapTile(screen: MainActivity, id: Int) {
        val point=FloatArray(2)
        ui {
            val board=screen.findViewById<MahjongBoardView>(R.id.board)
            val center=board.tileCenter(id)!!; val offset=IntArray(2); board.getLocationOnScreen(offset)
            point[0]=center.x+offset[0]; point[1]=center.y+offset[1]
        }
        val down=SystemClock.uptimeMillis()
        for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
            val event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point[0],point[1],0)
            try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
        }
        instrumentation.waitForIdleSync()
    }
    fun testFreshEntryRequiresPlayAndInitializesOnlyOnce() {
        val screen=activity; waitVisible(screen,R.id.play)
        ui {
            assertEquals(screen.getString(R.string.start_level,1L),screen.findViewById<TextView>(R.id.start_level).text.toString())
            assertNull(screen.findViewById<MahjongBoardView>(R.id.board))
            val play=screen.findViewById<Button>(R.id.play)
            assertNotNull(play.compoundDrawablesRelative[0]); assertTrue(play.isEnabled)
            play.performClick(); val board=screen.findViewById<MahjongBoardView>(R.id.board)
            play.performClick(); assertSame(board,screen.findViewById<MahjongBoardView>(R.id.board))
        }
        waitVisible(screen,R.id.board); assertEquals(24,state().session.game.remaining.size)
    }
    fun testHomeResumeIconsAndBadgeKeepExactAttempt() {
        seed(ProgressController(PlayerProgress(highestCompleted=20)))
        val screen=activity; waitVisible(screen,R.id.play)
        ui { assertEquals(screen.getString(R.string.start_level,21L),screen.findViewById<TextView>(R.id.start_level).text.toString()) }
        enter(screen)
        val move=state().session.level.witness.first(); val game=state().session.game
        tapTile(screen,move.first); val snapshot=state().capture()
        var board: MahjongBoardView?=null
        ui {
            board=screen.findViewById(R.id.board)
            val undo=screen.findViewById<GardenIconButton>(R.id.undo)
            assertEquals(3,undo.badge); assertEquals("",undo.text.toString())
            assertEquals(screen.getString(R.string.undo_description,3),undo.contentDescription.toString())
            for(id in listOf(R.id.undo,R.id.restart,R.id.home)) {
                val control=screen.findViewById<View>(id)
                assertTrue(control.width>=48*screen.resources.displayMetrics.density)
                assertTrue(control.height>=48*screen.resources.displayMetrics.density)
                assertFalse(control.contentDescription.isNullOrEmpty())
            }
            screen.findViewById<Button>(R.id.home).performClick()
        }
        waitVisible(screen,R.id.play); assertEquals(snapshot,state().capture())
        ui { assertFalse(board!!.isShown) }
        enter(screen); assertSame(game,state().session.game)
        ui { assertSame(board,screen.findViewById(R.id.board)) }
        tapTile(screen,move.second); assertTrue(game.tray.isEmpty())
        ui { screen.findViewById<Button>(R.id.undo).performClick(); assertEquals(2,screen.findViewById<GardenIconButton>(R.id.undo).badge) }
        assertEquals(listOf(move.first),game.tray.map{it.id})
        val identity=state().session.level
        ui { screen.findViewById<Button>(R.id.restart).performClick() }
        assertEquals(identity,state().session.level); assertTrue(game.tray.isEmpty()); assertEquals(2,state().wallet.balance)
    }
    fun testCoordinateVictoryNextWaitsAtNextStartAndDoubleTapCannotSkip() {
        val screen=activity; enter(screen)
        Levels.get(1).witness.forEach { tapTile(screen,it.first); tapTile(screen,it.second) }
        waitVisible(screen,R.id.next_level)
        ui {
            assertFalse(screen.findViewById<Button>(R.id.restart).isShown)
            assertFalse(screen.findViewById<Button>(R.id.home).isShown)
            assertNull(screen.findViewById<View>(R.id.replay_level))
            val next=screen.findViewById<Button>(R.id.next_level); next.performClick(); next.performClick()
        }
        waitVisible(screen,R.id.play)
        ui {
            assertEquals(screen.getString(R.string.start_level,2L),screen.findViewById<TextView>(R.id.start_level).text.toString())
            assertFalse(screen.findViewById<MahjongBoardView>(R.id.board).isShown)
        }
        assertEquals(2L,state().session.level.id); assertFalse(state().select(1))
        enter(screen); assertEquals(Levels.get(2).tiles,state().session.game.tiles)
    }
    fun testRecreationKeepsGameButFreshRelaunchRequiresPlayAndRestoresTray() {
        val screen=activity; enter(screen)
        val id=Levels.get(1).witness.first().first; tapTile(screen,id)
        val before=state().capture()
        val monitor=Instrumentation.ActivityMonitor(MainActivity::class.java.name,null,false)
        instrumentation.addMonitor(monitor)
        var rotated: MainActivity?=null
        try {
            ui { screen.recreate() }
            rotated=monitor.waitForActivityWithTimeout(10000) as? MainActivity
            assertNotNull(rotated); waitVisible(rotated!!,R.id.board)
            val replacement=rotated!!
            ui { assertFalse(replacement.findViewById<View>(R.id.play).isShown) }
            assertEquals(before,state().capture()); ui { replacement.finish() }
        } finally { instrumentation.removeMonitor(monitor); rotated?.let { ui { it.finish() } } }
        instrumentation.waitForIdleSync(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        val fresh=instrumentation.startActivitySync(Intent(instrumentation.targetContext,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            waitVisible(fresh,R.id.play); assertEquals(before,state().capture())
            enter(fresh); assertEquals(listOf(id),state().session.game.tray.map{it.id})
        } finally { ui { fresh.finish() } }
    }
    fun testSavedDefeatIsRetainedUntilExplicitUndoOrRestart() {
        val c=ProgressController(PlayerProgress(undoCredits=2))
        listOf(23,21,18,20).forEach { check(c.session.game.select(it)) }; seed(c)
        val screen=activity; waitVisible(screen,R.id.play); assertEquals(Status.LOST,state().session.game.status)
        enter(screen); waitVisible(screen,R.id.outcome)
        ui {
            assertTrue(screen.findViewById<View>(R.id.tray).isShown)
            assertFalse(screen.findViewById<View>(R.id.next_level).isShown)
            screen.findViewById<Button>(R.id.undo).performClick()
        }
        assertEquals(Status.PLAYING,state().session.game.status); assertEquals(1,state().wallet.balance)
        assertEquals(3,state().session.game.tray.size)
    }
    fun testSavedPendingVictoryOpensOutcomeAndAdvancesOnce() {
        val c=ProgressController(PlayerProgress(highestCompleted=19,undoCredits=1))
        c.session.level.witness.forEach { check(c.session.game.select(it.first)); check(c.session.game.select(it.second)) }; seed(c)
        val screen=activity; waitVisible(screen,R.id.play)
        ui { assertEquals(screen.getString(R.string.start_level,20L),screen.findViewById<TextView>(R.id.start_level).text.toString()) }
        enter(screen); waitVisible(screen,R.id.next_level)
        assertEquals(Status.WON,state().session.game.status); assertFalse(state().session.restart())
        ui { val next=screen.findViewById<Button>(R.id.next_level); next.performClick(); next.performClick() }
        waitVisible(screen,R.id.play); assertEquals(21L,state().session.level.id); assertEquals(1,state().wallet.balance)
    }
    fun testLargestLevelLabelAndPlayRemainReachable() {
        seed(ProgressController(PlayerProgress(highestCompleted=Long.MAX_VALUE-1,undoCredits=0)))
        val screen=activity; waitVisible(screen,R.id.play)
        ui {
            val level=screen.findViewById<TextView>(R.id.start_level)
            assertEquals(screen.getString(R.string.start_level,Long.MAX_VALUE),level.text.toString())
            for(line in 0 until level.layout.lineCount) assertEquals(0,level.layout.getEllipsisCount(line))
            screen.findViewById<ScrollView>(R.id.level_start).fullScroll(View.FOCUS_DOWN)
        }
        instrumentation.waitForIdleSync()
        ui {
            val play=screen.findViewById<Button>(R.id.play)
            val rect=Rect(); assertTrue(play.getGlobalVisibleRect(rect)); assertTrue(rect.height()>0)
            assertTrue(play.height>=48*screen.resources.displayMetrics.density)
        }
    }
}
