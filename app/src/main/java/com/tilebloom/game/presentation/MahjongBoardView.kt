package com.tilebloom.game.presentation

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.tilebloom.game.R
import com.tilebloom.game.domain.*
import kotlin.math.*

class MahjongBoardView(context: Context, private val game: Game, private val changed: () -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var downX = 0f
    private var downY = 0f
    init { contentDescription = context.getString(R.string.board_description); isClickable = true }
    private val scale: Float get() = min(width / 4.85f, height / 5.85f).coerceAtLeast(0f)
    private fun rect(t: Tile): RectF {
        val s = scale
        val ox = (width - 4.5f * s) / 2
        val oy = (height - 5.4f * s) / 2 + .12f * s
        return RectF(ox + t.x.toFloat() * s, oy + t.y.toFloat() * s * 1.3f - t.layer * s * .09f,
            ox + (t.x.toFloat() + 1) * s, oy + (t.y.toFloat() + 1) * s * 1.3f - t.layer * s * .09f)
    }
    private fun hit(t: Tile, x: Float, y: Float): Boolean {
        val r = rect(t).apply { inset(scale * .035f, scale * .035f); bottom += scale * .045f }
        if (!r.contains(x, y)) return false
        val radius = scale * .1f
        val cx = x.coerceIn(r.left + radius, r.right - radius)
        val cy = y.coerceIn(r.top + radius, r.bottom - radius)
        return hypot(x - cx, y - cy) <= radius
    }
    private fun ordered() = game.remaining.sortedWith(compareBy<Tile> { it.layer }.thenBy { it.y }.thenBy { it.x }.thenBy { it.id })
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (tile in ordered()) {
            val r = rect(tile)
            val face = RectF(r).apply { inset(scale * .035f, scale * .035f) }
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(6, 27, 22)
            canvas.drawRoundRect(RectF(face).apply { offset(0f, scale * .09f) }, scale * .1f, scale * .1f, paint)
            paint.color = Color.rgb(187, 173, 139)
            canvas.drawRoundRect(RectF(face).apply { offset(0f, scale * .045f) }, scale * .1f, scale * .1f, paint)
            paint.color = if (game.isAvailable(tile.id)) Color.rgb(249, 242, 219) else Color.rgb(191, 192, 170)
            canvas.drawRoundRect(face, scale * .1f, scale * .1f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (game.selected == tile.id) scale * .045f else scale * .014f
            paint.color = if (game.selected == tile.id) Color.rgb(219, 174, 64) else Color.rgb(151, 145, 112)
            canvas.drawRoundRect(face, scale * .1f, scale * .1f, paint)
            paint.style = Paint.Style.FILL
            canvas.save()
            canvas.translate(r.centerX(), r.centerY())
            canvas.scale(scale * .34f, scale * .34f)
            BotanicalSymbols.draw(canvas, paint, tile.symbol)
            canvas.restore()
            if (!game.isAvailable(tile.id)) {
                // Two engraved corner bars supplement the muted unavailable surface.
                paint.color = Color.rgb(103, 113, 91); paint.strokeWidth = scale * .025f
                canvas.drawLine(face.left + scale * .12f, face.top + scale * .12f, face.left + scale * .25f, face.top + scale * .12f, paint)
                canvas.drawLine(face.right - scale * .25f, face.top + scale * .12f, face.right - scale * .12f, face.top + scale * .12f, paint)
            }
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; return true }
            MotionEvent.ACTION_UP -> {
                if (hypot(event.x - downX, event.y - downY) <= android.view.ViewConfiguration.get(context).scaledTouchSlop) {
                    ordered().asReversed().firstOrNull { hit(it, event.x, event.y) }?.let {
                        game.select(it.id); changed()
                    }
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return true
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
