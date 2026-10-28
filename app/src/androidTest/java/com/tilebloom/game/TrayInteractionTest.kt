package com.tilebloom.game

import android.test.ActivityInstrumentationTestCase2
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.Button
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.*
import com.tilebloom.game.storage.*

@Suppress("DEPRECATION")
class TrayInteractionTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    override fun setUp() {
        super.setUp(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage("test-tray")
        java.io.File(instrumentation.targetContext.filesDir,"test-tray").delete()
    }
    override fun tearDown() {
        super.tearDown(); ProgressRepository.awaitWrites(); ProgressRepository.useTestStorage(null)
    }
    private fun ready() {
        val screen=activity
        val limit=SystemClock.uptimeMillis()+30000
        while(SystemClock.uptimeMillis()<limit) {
            var ok=false
            instrumentation.runOnMainSync { ok=screen.findViewById<MahjongBoardView>(R.id.board)?.width?.let{it>0}==true }
            if(ok)return
            SystemClock.sleep(20)
        }
        fail("Board timeout")
    }
    private fun tap(id:Int) {
        val point=FloatArray(2)
        instrumentation.runOnMainSync {
            val v=activity.findViewById<MahjongBoardView>(R.id.board)
            val p=v.tileCenter(id)!!; val location=IntArray(2); v.getLocationOnScreen(location)
            point[0]=location[0]+p.x; point[1]=location[1]+p.y
        }
        val now=SystemClock.uptimeMillis()
        for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
            val e=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,point[0],point[1],0)
            try{instrumentation.sendPointerSync(e)}finally{e.recycle()}
        }
        instrumentation.waitForIdleSync()
    }
    private fun game()=GameStore.get(instrumentation.targetContext).session.game
    fun testCoordinateInsertionMatchingUndoRestartAndDurability() {
        ready(); assertEquals(4,game().traySlots.size); assertTrue(game().tray.isEmpty())
        instrumentation.runOnMainSync {
            val tray=activity.findViewById<TileTrayView>(R.id.tray)
            assertTrue(tray.isShown); assertTrue(tray.width>0 && tray.height>0)
            assertTrue(tray.contentDescription.toString().contains("Four tile tray slots"))
            val trayPosition=IntArray(2); val boardPosition=IntArray(2)
            tray.getLocationOnScreen(trayPosition)
            activity.findViewById<MahjongBoardView>(R.id.board).getLocationOnScreen(boardPosition)
            assertTrue(trayPosition[1]+tray.height<=boardPosition[1])
        }
        val pair=Levels.get(1).witness.first(); tap(pair.first)
        assertEquals(listOf(pair.first),game().tray.map{it.id}); assertEquals(23,game().remaining.size)
        ProgressRepository.awaitWrites()
        val saved=ProgressRepository.load(instrumentation.targetContext)
        assertEquals(game().snapshot(),ProgressController(saved).session.game.snapshot())
        tap(pair.second); assertTrue(game().tray.isEmpty()); assertEquals(22,game().remaining.size)
        instrumentation.runOnMainSync{activity.findViewById<Button>(R.id.undo).performClick()}
        assertEquals(listOf(pair.first),game().tray.map{it.id}); assertEquals(2,GameStore.get(instrumentation.targetContext).wallet.balance)
        instrumentation.runOnMainSync{activity.findViewById<Button>(R.id.restart).performClick()}
        assertTrue(game().tray.isEmpty()); assertEquals(24,game().remaining.size)
    }
    fun testCoordinateFourSymbolsDefeatAndUndo() {
        ready()
        // Search a short distinct-symbol opening using geometry; all actual actions are screen taps.
        fun path(g:Game):List<Int>? {
            if(g.status==Status.LOST)return if(g.tray.size==4) emptyList() else null
            if(g.tray.size>=4)return null
            for(t in g.remaining.filter{g.isAvailable(it.id)&&g.tray.none{held->held.symbol==it.symbol}}) {
                val next=Game(Levels.get(1).tiles); check(next.restore(g.snapshot())); next.select(t.id)
                path(next)?.let{return listOf(t.id)+it}
            }
            return null
        }
        val ids=path(Game(Levels.get(1).tiles)) ?: error("Fixture needs four-symbol opening")
        ids.forEach{tap(it)}; assertEquals(Status.LOST,game().status); assertEquals(4,game().tray.size)
        instrumentation.runOnMainSync {
            assertEquals("Restart Level",activity.findViewById<Button>(R.id.next_level).text.toString())
            activity.findViewById<Button>(R.id.undo).performClick()
        }
        assertEquals(Status.PLAYING,game().status); assertEquals(3,game().tray.size)
    }
}
