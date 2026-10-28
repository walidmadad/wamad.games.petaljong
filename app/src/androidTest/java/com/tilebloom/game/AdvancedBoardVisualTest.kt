package com.tilebloom.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.test.InstrumentationTestCase
import android.view.MotionEvent
import android.view.View
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.MahjongBoardView
import java.io.File

/** Real Android Canvas captures, isolated from player storage. Execute before visual approval. */
@Suppress("DEPRECATION")
class AdvancedBoardVisualTest : InstrumentationTestCase() {
    fun testCaptureStagesFamiliesAndUpperRemovalAtPortraitWidths() {
        val directory = File(instrumentation.targetContext.filesDir,"codex30-visual").apply { mkdirs() }
        for(levelId in (6L..20L).toList() + listOf(31L,32L,100L)) {
            val level = Levels.get(levelId)
            for(widthDp in listOf(320,360,400,600)) {
                instrumentation.runOnMainSync {
                    val context = instrumentation.targetContext
                    val density = context.resources.displayMetrics.density
                    val width = ((widthDp-32)*density).toInt(); val height = (480*density).toInt()
                    val game = Game(level.tiles)
                    val view = MahjongBoardView(context,game) {}
                    view.theme = GardenTheme.forLevel(levelId)
                    view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
                    view.layout(0,0,width,height)
                    fun capture(suffix: String) {
                        val bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap); canvas.drawColor(android.graphics.Color.rgb(40,77,57))
                        view.draw(canvas)
                        File(directory,"level-$levelId-${widthDp}dp-$suffix.png").outputStream().use {
                            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))
                        }
                        bitmap.recycle()
                    }
                    capture("initial")
                    val id = level.witness.first().first
                    val center = view.tileCenter(id)!!
                    val now = SystemClock.uptimeMillis()
                    for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                        val event = MotionEvent.obtain(now,now,action,center.x,center.y,0)
                        try { assertTrue(view.onTouchEvent(event)) } finally { event.recycle() }
                    }
                    assertEquals(level.tiles.size-1,game.remaining.size)
                    assertEquals(id,game.tray.single().id)
                    assertTrue(game.undo()); assertEquals(level.tiles,game.tiles)
                    level.witness.take(3).forEach { assertTrue(game.match(it.first,it.second)) }
                    view.load(game); capture("opened")
                    assertEquals(4,game.traySlots.size)
                }
            }
        }
    }
}
