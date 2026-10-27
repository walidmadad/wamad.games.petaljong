package com.tilebloom.game.presentation

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.tilebloom.game.R
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.*
import kotlin.math.*

class MahjongBoardView(context: Context, private val game: Game, private val changed: () -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gesture = TileTapGesture(ViewConfiguration.get(context).scaledTouchSlop.toFloat())
    private var blockedTileId: Int? = null
    private val clearBlocked = Runnable { blockedTileId = null; invalidate() }
    private fun geometry() = BoardGeometry(width.toFloat(), height.toFloat(),
        BoardPadding(paddingLeft.toFloat(), paddingTop.toFloat(), paddingRight.toFloat(), paddingBottom.toFloat()))
    // Read-only presentation queries also allow instrumentation to assert real touch results.
    val selectedTileId get() = game.selected
    val remainingTileCount get() = game.remaining.size
    fun tileCenter(id: Int): BoardPoint? = game.remaining.find { it.id == id }?.let { geometry().face(it).center }
    init {
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        refresh()
    }
    fun refresh() {
        val selected = game.tiles.find { it.id == game.selected }
        val selectedText = selected?.let {
            val symbol = when (it.symbol) {
                Symbol.LEAF -> R.string.symbol_leaf
                Symbol.LOTUS -> R.string.symbol_lotus
                Symbol.FERN -> R.string.symbol_fern
                Symbol.FLOWER -> R.string.symbol_flower
                Symbol.BERRIES -> R.string.symbol_berries
                Symbol.SUNFLOWER -> R.string.symbol_sunflower
            }
            context.getString(R.string.board_selected, context.getString(symbol))
        } ?: context.getString(R.string.board_none_selected)
        contentDescription = context.getString(R.string.board_state, remainingTileCount, selectedText)
        invalidate()
    }
    fun clearFeedback() {
        removeCallbacks(clearBlocked)
        blockedTileId = null
        gesture.cancel()
        invalidate()
    }
    private fun BoardRect.android() = RectF(left, top, right, bottom)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = geometry()
        val scale = layout.scale
        if (scale <= 0f) return
        for (tile in layout.drawOrder(game.tiles)) {
            val r = layout.footprint(tile).android()
            val face = layout.face(tile)
            val available = game.isAvailable(tile.id)
            val selected = game.selected == tile.id
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(6, 27, 22)
            canvas.drawRoundRect(layout.shadow(tile).android(), layout.radius, layout.radius, paint)
            paint.color = Color.rgb(187, 173, 139)
            canvas.drawRoundRect(layout.edge(tile).android(), layout.radius, layout.radius, paint)
            paint.color = if (available) Color.rgb(249, 242, 219) else Color.rgb(220, 220, 200)
            canvas.drawRoundRect(face.android(), layout.radius, layout.radius, paint)
            // Keep all border strokes inside the physical face so selection never changes hit bounds.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (selected) scale * .045f else scale * .014f
            paint.color = when {
                selected -> Color.rgb(193, 148, 48)
                blockedTileId == tile.id -> Color.rgb(192, 170, 111)
                else -> Color.rgb(151, 145, 112)
            }
            canvas.drawRoundRect(face.inset(paint.strokeWidth / 2).android(), layout.radius, layout.radius, paint)
            if (selected) {
                paint.strokeWidth = scale * .012f
                paint.color = Color.rgb(227, 201, 135)
                canvas.drawRoundRect(face.inset(scale * .075f).android(), scale * .06f, scale * .06f, paint)
            }
            paint.style = Paint.Style.FILL
            canvas.save()
            canvas.translate(r.centerX(), r.centerY())
            canvas.scale(scale * .34f, scale * .34f)
            BotanicalSymbols.draw(canvas, paint, tile.symbol)
            canvas.restore()
            if (!available) {
                // Engraved corner bars distinguish blocking without relying on color.
                paint.color = Color.rgb(103, 113, 91); paint.strokeWidth = scale * .025f
                canvas.drawLine(face.left + scale * .12f, face.top + scale * .12f, face.left + scale * .25f, face.top + scale * .12f, paint)
                canvas.drawLine(face.right - scale * .25f, face.top + scale * .12f, face.right - scale * .12f, face.top + scale * .12f, paint)
            }
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Batched movement can accompany the final UP as well as MOVE.
        // Check every sample before completing a tap, including an out-and-back drag.
        if (event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_UP) {
            for (i in 0 until event.historySize) {
                gesture.move(event.getPointerId(0), event.getHistoricalX(0, i), event.getHistoricalY(0, i))
            }
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val tile = geometry().hitTest(game.tiles, event.x, event.y)
                gesture.down(event.getPointerId(0), tile?.id, event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                gesture.move(event.getPointerId(0), event.x, event.y)
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> gesture.cancel()
            MotionEvent.ACTION_UP -> {
                val tile = geometry().hitTest(game.tiles, event.x, event.y)
                val id = gesture.up(event.getPointerId(0), tile?.id, event.x, event.y)
                if (id != null) {
                    if (game.isAvailable(id)) {
                        removeCallbacks(clearBlocked); blockedTileId = null
                        game.select(id)
                        changed()
                    } else {
                        // The touched upper tile still consumes the tap; never search below it.
                        removeCallbacks(clearBlocked); blockedTileId = id
                        postDelayed(clearBlocked, 160L)
                        invalidate()
                    }
                    performClick()
                }
            }
        }
        return true
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clearFeedback()
    }
    override fun onDetachedFromWindow() {
        clearFeedback()
        super.onDetachedFromWindow()
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}

private object BotanicalSymbols {
    private val green = Color.rgb(43, 100, 69)
    fun draw(c: Canvas, p: Paint, symbol: Symbol) {
        p.color = green; p.style = Paint.Style.FILL
        when (symbol) {
            Symbol.LEAF -> {
                val path = Path().apply { moveTo(-.7f, .85f); cubicTo(-1.2f, -.4f, -.2f, -1.1f, .8f, -.9f); cubicTo(1.1f, .2f, .3f, .9f, -.7f, .85f) }
                c.drawPath(path, p)
                p.color = Color.rgb(210, 220, 166); p.strokeWidth = .07f
                c.drawLine(-.65f, .8f, .6f, -.65f, p)
            }
            Symbol.LOTUS -> {
                p.color = Color.rgb(161, 92, 119)
                for (angle in listOf(-55f, -28f, 0f, 28f, 55f)) {
                    c.save(); c.rotate(angle, 0f, .6f)
                    c.drawOval(-.27f, -.95f, .27f, .65f, p); c.restore()
                }
                p.color = green; c.drawOval(-.85f, .6f, .85f, .85f, p)
            }
            Symbol.FERN -> {
                p.strokeWidth = .09f; c.drawLine(0f, 1f, 0f, -1f, p)
                for (i in 0..4) {
                    val y = -.65f + i * .32f; val w = .25f + i * .12f
                    c.save(); c.rotate(-25f, 0f, y); c.drawOval(-w, y - .18f, 0f, y + .1f, p); c.restore()
                    c.save(); c.rotate(25f, 0f, y); c.drawOval(0f, y - .18f, w, y + .1f, p); c.restore()
                }
            }
            Symbol.FLOWER -> {
                p.color = Color.rgb(167, 101, 72)
                repeat(5) { c.save(); c.rotate(it * 72f); c.drawOval(-.33f, -.95f, .33f, .15f, p); c.restore() }
                p.color = Color.rgb(238, 203, 104); c.drawCircle(0f, 0f, .28f, p)
            }
            Symbol.BERRIES -> {
                p.strokeWidth = .09f; c.drawLine(0f, -.85f, 0f, .65f, p)
                c.drawOval(0f, -.95f, .75f, -.55f, p)
                p.color = Color.rgb(128, 71, 111)
                listOf(-.43f to -.25f, .35f to -.1f, -.4f to .45f, .32f to .6f).forEach { (x, y) -> c.drawCircle(x, y, .32f, p) }
            }
            Symbol.SUNFLOWER -> {
                p.color = Color.rgb(193, 147, 40)
                repeat(12) { c.save(); c.rotate(it * 30f); c.drawOval(-.15f, -1f, .15f, -.25f, p); c.restore() }
                p.color = Color.rgb(91, 76, 43); c.drawCircle(0f, 0f, .48f, p)
                p.color = Color.rgb(231, 204, 133)
                repeat(5) { val a = it * 2 * PI / 5; c.drawCircle((cos(a) * .23).toFloat(), (sin(a) * .23).toFloat(), .055f, p) }
            }
        }
    }
}
