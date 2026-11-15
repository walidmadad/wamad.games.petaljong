package com.wamad.petaljong

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
import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.*
import com.wamad.petaljong.storage.*
import java.io.File

@Suppress("DEPRECATION")
class LevelStartTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private val storage = "test-level-start"
    override fun setUp() {
        super.setUp(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        File(instrumentation.targetContext.filesDir,storage).delete()
        File(instrumentation.targetContext.filesDir,"$storage.bak").delete()
        seedReturningPlayer(instrumentation.targetContext, storage)
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
            ui { ready=(id != R.id.continue_level || screen.isPuzzleReady) && screen.findViewById<View>(id)?.let { it.isShown && it.width>0 }==true }
            if(ready) { instrumentation.waitForIdleSync(); return }
            SystemClock.sleep(20)
        }
        fail("View $id did not appear")
    }
    private fun waitAdvanced(screen: MainActivity, level: Long) {
        val until=SystemClock.uptimeMillis()+30000
        while(SystemClock.uptimeMillis()<until) {
            var ready=false
            ui { ready=state().session.level.id==level && !GameStore.transitioning &&
                screen.findViewById<View>(R.id.board)?.isShown==true }
            if(ready) return
            SystemClock.sleep(20)
        }
        fail("Next level did not enter gameplay")
    }
    private fun enter(screen: MainActivity) {
        waitVisible(screen,R.id.continue_level); ui { screen.findViewById<View>(R.id.continue_level).performClick() }
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
    fun testSharedSettingsRetainsBoardAndClosesWithBack() {
        val screen = activity
        waitVisible(screen, R.id.continue_level)
        fun dialog(): android.app.AlertDialog {
            val field = MainActivity::class.java.getDeclaredField("settingsDialog")
            field.isAccessible = true
            return field.get(screen) as android.app.AlertDialog
        }
        ui {
            val card = screen.findViewById<android.widget.LinearLayout>(R.id.continue_level)
            assertEquals("Only level and subtitle remain", 2, card.childCount)
            assertFalse(screen.findViewById<View>(R.id.privacy_options)?.isShown == true)
            val gear = screen.findViewById<View>(R.id.settings)
            assertEquals("Settings", gear.contentDescription)
            gear.performClick()
            assertTrue(dialog().isShowing)
            assertNotNull(dialog().findViewById<View>(R.id.settings_privacy))
            dialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        }
        enter(screen)
        val before = state().session.game.snapshot()
        ui {
            val gameplay = screen.findViewById<android.view.ViewGroup>(R.id.gameplay)
            gameplay.findViewById<View>(R.id.settings).performClick()
            assertTrue(dialog().isShowing)
            assertFalse(dialog().window!!.attributes.flags and
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL != 0)
            val toggle = dialog().findViewById<android.widget.Switch>(R.id.settings_sound)
            val initial = toggle.isChecked
            toggle.isChecked = !initial
            toggle.isChecked = initial // Restore the player's sound preference.
        }
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        instrumentation.waitForIdleSync()
        ui {
            assertTrue(screen.findViewById<View>(R.id.board).isShown)
            assertEquals(before, state().session.game.snapshot())
        }
    }

    fun testPortraitTrayFitsRepresentativeWidthsAndRendersTile() {
        val screen = activity
        ui {
            assertNotNull(screen.findViewById<View>(R.id.level_start))
            val tile = Levels.get(1).tiles.first()
            val tray = TileTrayView(screen) { listOf(tile) }
            val density = screen.resources.displayMetrics.density
            for (widthDp in listOf(320,360,400,480)) {
                val w = ((widthDp - 32) * density).toInt()
                val h = (76 * density).toInt()
                tray.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
                tray.layout(0,0,w,h)
                val slots = tray.slotBounds()
                assertEquals(4,slots.size)
                slots.forEachIndexed { index, r ->
                    assertEquals(.72f,r.width()/r.height(),.001f)
                    assertTrue(r.left>=0 && r.right<=w && r.top>=0 && r.bottom<=h)
                    if (index>0) assertTrue(slots[index-1].right<r.left)
                }
                val bitmap = android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888)
                tray.draw(android.graphics.Canvas(bitmap))
                val filled = slots[0]; val empty = slots[1]
                assertTrue(bitmap.getPixel((filled.left+3*density).toInt(),filled.centerY().toInt()) !=
                    bitmap.getPixel((empty.left+3*density).toInt(),empty.centerY().toInt()))
                bitmap.recycle()
            }
        }
    }
    fun testLevelCardCornerTouchEntersSavedPuzzleOnlyOnce() {
        val screen=activity; waitVisible(screen,R.id.continue_level)
        ui {
            assertEquals(screen.getString(R.string.start_level,1L),screen.findViewById<TextView>(R.id.start_level).text.toString())
            assertNull(screen.findViewById<MahjongBoardView>(R.id.board))
            val play=screen.findViewById<View>(R.id.continue_level)
            assertTrue(play.isClickable); assertTrue(play.isFocusable); assertTrue(play.isEnabled)
            assertEquals(android.widget.Button::class.java.name, play.accessibilityClassName)
            val level = screen.findViewById<View>(R.id.start_level)
            val p = IntArray(2); val l = IntArray(2)
            play.getLocationOnScreen(p); level.getLocationOnScreen(l)
            assertEquals(l[0]+level.width/2f,p[0]+play.width/2f,2f)
            val node = play.createAccessibilityNodeInfo()
            assertTrue(node.isClickable)
            assertEquals(screen.getString(R.string.continue_level_description, 1L), node.contentDescription.toString())
            val down = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(down, down + 20, action, 2f, 2f, 0)
                try { assertTrue(play.dispatchTouchEvent(event)) } finally { event.recycle() }
            }
        }
        waitVisible(screen,R.id.board)
        ui {
            val board=screen.findViewById<MahjongBoardView>(R.id.board)
            val card=screen.findViewById<View>(R.id.continue_level)
            card.performClick(); card.performClick()
            assertSame(board,screen.findViewById<MahjongBoardView>(R.id.board))
        }
        assertEquals(24,state().session.game.remaining.size)
    }
    fun testHomeResumeIconsAndBadgeKeepExactAttempt() {
        seed(ProgressController(PlayerProgress(tutorial = com.wamad.petaljong.domain.TutorialStep.SKIPPED, highestCompleted=20)))
        val screen=activity; waitVisible(screen,R.id.continue_level)
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
        waitVisible(screen,R.id.continue_level); assertEquals(snapshot,state().capture())
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
    fun testCoordinateVictoryNextEntersGameplayAndDoubleTapCannotSkip() {
        seed(ProgressController(PlayerProgress(tutorial = com.wamad.petaljong.domain.TutorialStep.SKIPPED, highestCompleted=3,undoCredits=1)))
        val screen=activity; enter(screen)
        state().session.level.winningSelections.forEach { tapTile(screen,it) }
        waitVisible(screen,R.id.next_level)
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        waitVisible(screen,R.id.next_level)
        assertEquals(Status.WON,state().session.game.status)
        ui {
            assertFalse(screen.findViewById<Button>(R.id.restart).isShown)
            assertFalse(screen.findViewById<Button>(R.id.home).isShown)
            assertNull(screen.findViewById<View>(R.id.replay_level))
            val next=screen.findViewById<com.wamad.petaljong.presentation.ContinueLevelCard>(R.id.next_level); next.performClick(); next.performClick()
        }
        waitAdvanced(screen,5)
        ui {
            assertFalse(screen.findViewById<View>(R.id.level_start).isShown)
            assertTrue(screen.findViewById<MahjongBoardView>(R.id.board).isShown)
        }
        assertFalse(state().select(4)); assertTrue(state().session.game.tray.isEmpty())
        assertEquals(1,state().wallet.balance)
        assertEquals(Levels.get(5).tiles,state().session.game.tiles)
        tapTile(screen,state().session.level.winningSelections.first())
        val before=state().capture()
        ui { screen.findViewById<View>(R.id.home).performClick() }
        waitVisible(screen,R.id.continue_level); enter(screen)
        assertEquals(before,state().capture())
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        waitVisible(screen,R.id.continue_level); assertEquals(before,state().capture())
    }
    fun testRecreationKeepsGameButFreshRelaunchRequiresPlayAndRestoresTray() {
        val screen=activity; enter(screen)
        val id=Levels.get(1).winningSelections.first(); tapTile(screen,id)
        val before=state().capture()
        val monitor=Instrumentation.ActivityMonitor(MainActivity::class.java.name,null,false)
        instrumentation.addMonitor(monitor)
        var rotated: MainActivity?=null
        try {
            ui { screen.recreate() }
            rotated=monitor.waitForActivityWithTimeout(10000) as? MainActivity
            assertNotNull(rotated); waitVisible(rotated!!,R.id.board)
            val replacement=rotated!!
            ui { assertFalse(replacement.findViewById<View>(R.id.continue_level).isShown) }
            assertEquals(before,state().capture()); ui { replacement.finish() }
        } finally { instrumentation.removeMonitor(monitor); rotated?.let { ui { it.finish() } } }
        instrumentation.waitForIdleSync(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(storage)
        val fresh=instrumentation.startActivitySync(Intent(instrumentation.targetContext,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            waitVisible(fresh,R.id.continue_level); assertEquals(before,state().capture())
            enter(fresh); assertEquals(listOf(id),state().session.game.tray.map{it.id})
        } finally { ui { fresh.finish() } }
    }
    fun testSavedDefeatIsRetainedUntilExplicitUndoOrRestart() {
        val c=ProgressController(PlayerProgress(tutorial = com.wamad.petaljong.domain.TutorialStep.SKIPPED, undoCredits=2))
        listOf(23,21,18,20).forEach { check(c.session.game.select(it)) }; seed(c)
        val screen=activity; waitVisible(screen,R.id.continue_level); assertEquals(Status.LOST,state().session.game.status)
        enter(screen); waitVisible(screen,R.id.outcome)
        ui {
            assertTrue(screen.findViewById<View>(R.id.tray).isShown)
            assertFalse(screen.findViewById<View>(R.id.next_level).isShown)
            val card = screen.findViewById<View>(R.id.outcome)
            val undo = screen.findViewById<Button>(R.id.undo)
            val restart = screen.findViewById<Button>(R.id.restart)
            assertTrue((undo.parent as View).parent === card)
            assertSame(undo.parent, restart.parent)
            assertFalse(screen.findViewById<View>(R.id.home).isShown)
            val tray = screen.findViewById<TileTrayView>(R.id.tray)
            val slots = tray.slotBounds()
            assertEquals(4, slots.size)
            slots.forEach {
                assertTrue(it.height() > it.width())
                assertEquals(.72f, it.width() / it.height(), .001f)
                assertEquals(slots.first().width(), it.width(), .01f)
                assertTrue(it.left >= 0 && it.right <= tray.width)
            }
            assertEquals(4, state().session.game.tray.size)
            undo.performClick()
        }
        assertEquals(Status.PLAYING,state().session.game.status); assertEquals(1,state().wallet.balance)
        assertEquals(3,state().session.game.tray.size)
    }
    fun testZeroCreditDefeatRestartPreservesIdentityAndWallet() {
        val c = ProgressController(PlayerProgress(tutorial = com.wamad.petaljong.domain.TutorialStep.SKIPPED, undoCredits=0))
        listOf(23,21,18,20).forEach { check(c.session.game.select(it)) }; seed(c)
        val screen = activity; enter(screen); waitVisible(screen,R.id.outcome)
        val identity = state().session.level
        ui {
            val undo = screen.findViewById<GardenIconButton>(R.id.undo)
            assertFalse(undo.isEnabled); assertEquals(0, undo.badge)
            screen.findViewById<Button>(R.id.restart).performClick()
            assertFalse(screen.findViewById<View>(R.id.outcome).isShown)
            assertTrue(screen.findViewById<View>(R.id.home).isShown)
        }
        assertEquals(identity, state().session.level)
        assertTrue(state().session.game.tray.isEmpty())
        assertEquals(0, state().wallet.balance)
    }
    fun testSavedPendingVictoryFinalizesBeforePlay() {
        val c=ProgressController(PlayerProgress(tutorial = com.wamad.petaljong.domain.TutorialStep.SKIPPED, highestCompleted=19,undoCredits=1))
        c.session.level.winningSelections.forEach { check(c.session.game.select(it)) }; seed(c)
        val screen=activity; waitVisible(screen,R.id.continue_level)
        ui { assertEquals(screen.getString(R.string.start_level,21L),screen.findViewById<TextView>(R.id.start_level).text.toString()) }
        assertEquals(21L,state().session.level.id)
        enter(screen)
        assertEquals(Status.PLAYING,state().session.game.status)
        assertFalse(screen.findViewById<View>(R.id.outcome).isShown)
        assertEquals(1,state().wallet.balance)
        assertEquals(20L,ProgressRepository.load(instrumentation.targetContext).frontier)
    }
    fun testCoordinateLevelSixVictoryFreshRelaunchFinalizesOnlyOnce() {
        seed(ProgressController(PlayerProgress(tutorial = com.wamad.petaljong.domain.TutorialStep.SKIPPED, highestCompleted=5,undoCredits=1,hintCredits=0)))
        val screen=activity; enter(screen)
        state().session.level.winningSelections.forEach { tapTile(screen,it) }
        waitVisible(screen,R.id.next_level)
        assertEquals(6L,ProgressRepository.load(instrumentation.targetContext).frontier)
        ui { screen.finish() }
        ProgressRepository.awaitWrites()
        repeat(3) {
            // Discard process cache and read the isolated durable save.
            ProgressRepository.useTestStorage(storage)
            val reopened=instrumentation.startActivitySync(Intent(instrumentation.targetContext,
                MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            try {
                waitVisible(reopened,R.id.continue_level)
                assertEquals(7L,state().session.level.id)
                ui { assertEquals(reopened.getString(R.string.start_level,7L),
                    reopened.findViewById<TextView>(R.id.start_level).text.toString()) }
                enter(reopened)
                assertEquals(Status.PLAYING,state().session.game.status)
                assertEquals(1,state().wallet.balance); assertEquals(0,state().hintCredits)
            } finally { ui { reopened.finish() }; ProgressRepository.awaitWrites() }
        }
    }
    fun testLargestLevelLabelAndPlayRemainReachable() {
        seed(ProgressController(PlayerProgress(tutorial = com.wamad.petaljong.domain.TutorialStep.SKIPPED, highestCompleted=Long.MAX_VALUE-1,undoCredits=0)))
        val screen=activity; waitVisible(screen,R.id.continue_level)
        ui {
            val level=screen.findViewById<TextView>(R.id.start_level)
            assertEquals(screen.getString(R.string.start_level,Long.MAX_VALUE),level.text.toString())
            for(line in 0 until level.layout.lineCount) assertEquals(0,level.layout.getEllipsisCount(line))
            screen.findViewById<ScrollView>(R.id.level_start).fullScroll(View.FOCUS_DOWN)
        }
        instrumentation.waitForIdleSync()
        ui {
            val play=screen.findViewById<View>(R.id.continue_level)
            val rect=Rect(); assertTrue(play.getGlobalVisibleRect(rect)); assertTrue(rect.height()>0)
            assertTrue(play.height>=48*screen.resources.displayMetrics.density)
        }
    }
}
