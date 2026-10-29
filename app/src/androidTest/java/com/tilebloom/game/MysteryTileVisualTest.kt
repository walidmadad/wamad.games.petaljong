package com.tilebloom.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.test.InstrumentationTestCase
import android.view.MotionEvent
import android.view.View
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.MahjongBoardView
import com.tilebloom.game.presentation.TileTrayView
import java.io.File

/** Isolated Canvas/touch fixtures: never opens or writes real player progression. */
@Suppress("DEPRECATION")
class MysteryTileVisualTest : InstrumentationTestCase() {
    fun testJadeConcealmentRevealRapidTapAndDeliberateCollection() {
        instrumentation.runOnMainSync {
            val context=instrumentation.targetContext
            val tiles=listOf(Tile(0,Symbol.LEAF,0.0,0.0,0,mystery=true),
                Tile(1,Symbol.LEAF,2.0,0.0,0),Tile(2,Symbol.DOUBLE_LEAF,4.0,0.0,0,mystery=true),
                Tile(3,Symbol.DOUBLE_LEAF,5.0,0.0,0),Tile(4,Symbol.VEINED_LEAF,3.0,0.0,0),
                Tile(5,Symbol.VEINED_LEAF,0.0,2.0,0))
            val game=Game(tiles); val view=MahjongBoardView(context,game) {}
            prepare(view,960,1280)
            capture(view,"available-and-laterally-blocked-backs")
            fun tap(id:Int,time:Long) {
                val p=view.tileCenter(id)!!
                for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                    val e=MotionEvent.obtain(time,time,action,p.x,p.y,0)
                    try { assertTrue(view.onTouchEvent(e)) } finally { e.recycle() }
                }
            }
            val now=SystemClock.uptimeMillis()
            tap(2,now); assertTrue(game.tiles.first { it.id==2 }.concealed)
            tap(0,now); assertTrue(game.tiles.first().revealed); assertEquals(6,game.remaining.size)
            assertTrue(game.tray.isEmpty()); capture(view,"reveal-animation-start")
            tap(0,now+40); assertEquals(6,game.remaining.size); assertTrue(game.tray.isEmpty())
            // Removing ephemeral feedback exposes the authoritative face immediately.
            view.clearFeedback(); capture(view,"revealed-still-on-board")
            tap(0,now+500); assertEquals(5,game.remaining.size); assertEquals(0,game.tray.single().id)
            capture(view,"collected-on-second-deliberate-tap")
            val tray=TileTrayView(context) { listOf(Symbol.LEAF,Symbol.VEINED_LEAF,Symbol.DOUBLE_LEAF,Symbol.YELLOW_DAISY).mapIndexed { i,s -> Tile(i,s,0.0,0.0,0) } }
            prepare(tray,960,220); capture(tray,"tray-symbol-readability")
        }
    }
    fun testOptionCSwitchMismatchDirectMatchTrayMatchAndUndoTouches() {
        instrumentation.runOnMainSync {
            val game=Game(listOf(
                Tile(0,Symbol.LEAF,0.0,0.0,0,mystery=true),
                Tile(1,Symbol.LEAF,2.0,0.0,0,mystery=true),
                Tile(2,Symbol.LEAF,4.0,0.0,0), Tile(3,Symbol.LEAF,6.0,0.0,0),
                Tile(4,Symbol.FLOWER,0.0,2.0,0), Tile(5,Symbol.FLOWER,2.0,2.0,0)))
            val view=MahjongBoardView(instrumentation.targetContext,game) {}
            prepare(view,960,1280)
            var time=SystemClock.uptimeMillis()
            fun tap(id:Int) {
                time+=600
                val p=view.tileCenter(id)!!
                for (action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                    val e=MotionEvent.obtain(time,time,action,p.x,p.y,0)
                    try { assertTrue(view.onTouchEvent(e)) } finally { e.recycle() }
                }
            }
            tap(0); tap(1)
            assertEquals(1,game.activeMysteryId); assertEquals(6,game.remaining.size)
            assertTrue(game.tray.isEmpty()); assertTrue(game.tiles.first().concealed)
            tap(4); assertNull(game.activeMysteryId); assertEquals(4,game.tray.single().id)
            tap(0); tap(2)
            assertEquals(PairMove(0,2),game.snapshot().moves.single())
            assertEquals(4,game.tray.single().id)
            assertTrue(game.undo()); view.clearFeedback()
            assertTrue(game.isAvailable(0)); assertTrue(game.isAvailable(2))
            tap(2); tap(0); tap(0)
            assertEquals(PairMove(2,0),game.snapshot().moves.single())
            assertEquals(4,game.tray.single().id)
            tap(1); tap(1); assertEquals(listOf(4,1),game.tray.map { it.id })
            tap(3); assertEquals(4,game.tray.single().id)
            tap(5); assertEquals(Status.WON,game.status)
        }
    }
    fun testHiddenSymbolsHaveIdenticalPixelsAndCaptureDifficultyBoards() {
        instrumentation.runOnMainSync {
            val context=instrumentation.targetContext
            fun image(symbol:Symbol):Bitmap {
                val view=MahjongBoardView(context,Game(listOf(Tile(0,symbol,0.0,0.0,0,mystery=true)))) {}
                prepare(view,480,480)
                return Bitmap.createBitmap(480,480,Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
            }
            val a=image(Symbol.LEAF); val b=image(Symbol.SUNFLOWER)
            assertTrue(a.sameAs(b)); a.recycle(); b.recycle()
        }
        // Generation stays off the Android main thread.
        for(id in listOf(12L,23,40,88,151)) {
            val level=Levels.get(id)
            instrumentation.runOnMainSync {
                val view=MahjongBoardView(instrumentation.targetContext,Game(level.tiles)) {}
                view.theme=GardenTheme.forLevel(id); prepare(view,1080,1440)
                capture(view,"level-$id-${DifficultyPolicy.difficulty(id)}")
                val revealed=Game(level.tiles)
                // Complete witness includes legal reveals; capture a recognition fixture with every face shown.
                val faces=level.tiles.map { it.copy(mystery=false) }
                view.load(Game(faces)); capture(view,"level-$id-similar-families")
                assertTrue(revealed.tray.isEmpty())
            }
        }
    }
    private fun prepare(view:View,w:Int,h:Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
        view.layout(0,0,w,h)
    }
    private fun capture(view:View,name:String) {
        val directory=File(instrumentation.targetContext.filesDir,"codex35-visual").apply { mkdirs() }
        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap); canvas.drawColor(android.graphics.Color.rgb(25,48,40)); view.draw(canvas)
        File(directory,"$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        bitmap.recycle()
    }
}
