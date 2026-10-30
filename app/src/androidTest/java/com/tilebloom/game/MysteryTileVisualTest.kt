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
        for(id in listOf(12L,23,40,45,88,151)) {
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
    /** Production Canvas fixture; includes top/lateral blocking and physical overlap. */
    fun testNaturalGreenAllThemesAndReveal() {
        instrumentation.runOnMainSync {
            for (theme in GardenTheme.all) {
                val game = Game(listOf(
                    Tile(0, Symbol.LEAF, 0.0, 0.0, 0, mystery=true),
                    Tile(1, Symbol.FLOWER, 2.0, 0.0, 0, mystery=true),
                    Tile(2, Symbol.LEAF, 1.5, .4, 1),
                    Tile(3, Symbol.FLOWER, 4.0, 0.0, 0),
                    Tile(4, Symbol.SUNFLOWER, 0.0, 2.0, 0, mystery=true),
                    Tile(5, Symbol.LEAF, 1.0, 2.0, 0, mystery=true),
                    Tile(6, Symbol.FLOWER, 2.0, 2.0, 0, mystery=true)))
                val view = MahjongBoardView(instrumentation.targetContext, game) {}
                view.theme = theme
                prepare(view, 1080, 1440)
                assertTrue(game.isAvailable(0))
                assertFalse(game.isAvailable(1))
                assertFalse(game.isAvailable(5))
                // Sample clear face areas away from emblem, bevel, and upper tile.
                // A shader inheriting the blocked ceramic glint alpha fails this:
                // green over ivory becomes roughly #A0B69B instead of #28784B.
                val pixels = Bitmap.createBitmap(1080, 1440, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pixels)
                canvas.drawColor(theme.background)
                view.draw(canvas)
                for ((id, xFraction) in listOf(0 to .15f, 1 to .85f, 5 to .15f)) {
                    val face = view.tutorialBounds(id)
                    val color = pixels.getPixel((face.left + face.width() * xFraction).toInt(),
                        (face.top + face.height() * .82f).toInt())
                    assertEquals("Opaque hidden face $id", 255, android.graphics.Color.alpha(color))
                    assertTrue("Natural green face $id: ${Integer.toHexString(color)}",
                        android.graphics.Color.red(color) in 30..70 &&
                        android.graphics.Color.green(color) in 100..155 &&
                        android.graphics.Color.blue(color) in 55..105)
                }
                pixels.recycle()
                capture(view, "natural-green-${theme.id}-backs", theme.background)
                val center = view.tileCenter(0)!!
                val now = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(now, now, action, center.x, center.y, 0)
                    try { view.onTouchEvent(event) } finally { event.recycle() }
                }
                view.clearFeedback()
                assertTrue(game.tiles.first().revealed)
                assertEquals(Symbol.LEAF, game.tiles.first().symbol)
                assertEquals(7, game.remaining.size)
                capture(view, "natural-green-${theme.id}-revealed", theme.background)
            }
        }
    }
    private fun prepare(view:View,w:Int,h:Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
        view.layout(0,0,w,h)
    }
    private fun capture(view:View,name:String,background:Int=android.graphics.Color.rgb(25,48,40)) {
        val directory=File(instrumentation.targetContext.filesDir,"codex59-visual").apply { mkdirs() }
        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap); canvas.drawColor(background); view.draw(canvas)
        File(directory,"$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        bitmap.recycle()
    }
}
