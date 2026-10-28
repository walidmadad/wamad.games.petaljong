package com.tilebloom.game.presentation

import android.content.Context
import android.graphics.*
import android.view.View
import com.tilebloom.game.domain.Tile

/** Presentation only; replayable engine state owns every tile. */
class TileTrayView(context: Context, private val tiles: () -> List<Tile>) : View(context) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private var previous = tiles().map { it.id }
    private var ghosts = emptyList<Tile>()
    private var lastTiles = tiles().toList()
    private var pulseUntil = 0L
    fun refresh() {
        val ids = tiles().map { it.id }
        if (ids != previous && isAttachedToWindow && android.animation.ValueAnimator.areAnimatorsEnabled()) pulseUntil = android.os.SystemClock.uptimeMillis() + 180
        ghosts = if (lastTiles.size > tiles().size) lastTiles.filter { it.id !in ids } else emptyList()
        lastTiles = tiles().toList()
        previous = ids
        contentDescription = "Four tile tray slots. ${ids.size} occupied. " + tiles().joinToString { it.symbol.name }
        invalidate()
    }
    fun clearFeedback() { pulseUntil = 0; ghosts = emptyList(); lastTiles = tiles().toList(); previous = lastTiles.map { it.id }; invalidate() }
    override fun onDetachedFromWindow() { clearFeedback(); super.onDetachedFromWindow() }
    override fun onDraw(c: Canvas) {
        val gap = height * .1f
        val size = minOf((width - gap * 5) / 4f, height - gap * 2)
        val left = (width - size * 4 - gap * 3) / 2
        ink.style = Paint.Style.FILL; ink.color = Color.rgb(22, 55, 43)
        c.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), gap, gap, ink)
        repeat(4) { i ->
            val r = RectF(left + i * (size + gap), gap, left + i * (size + gap) + size, gap + size)
            val tile = tiles().getOrNull(i)
            ink.style = Paint.Style.FILL
            ink.color = if (tile == null) Color.rgb(16, 40, 32) else Color.rgb(250, 244, 224)
            c.drawRoundRect(r, gap, gap, ink)
            ink.style = Paint.Style.STROKE; ink.strokeWidth = resources.displayMetrics.density
            ink.color = if (tiles().size == 4) Color.rgb(199, 126, 94) else Color.rgb(184, 163, 107)
            c.drawRoundRect(r, gap, gap, ink)
            tile?.let {
                c.save(); c.translate(r.centerX(), r.centerY()); c.scale(size * .34f, size * .34f)
                BotanicalSymbols.draw(c, ink, it.symbol); c.restore()
            }
        }
        val timeLeft = pulseUntil - android.os.SystemClock.uptimeMillis()
        if (timeLeft > 0) {
            ghosts.forEachIndexed { i, tile ->
                c.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (timeLeft.coerceAtMost(180) * 180 / 180).toInt())
                c.translate(width / 2f + (i - .5f) * size, height / 2f)
                c.scale(size * .28f, size * .28f)
                BotanicalSymbols.draw(c, ink, tile.symbol); c.restore()
            }
            ink.style = Paint.Style.STROKE; ink.color = Color.argb(90, 230, 209, 150); ink.strokeWidth = 2f
            c.drawRoundRect(2f, 2f, width - 2f, height - 2f, gap, gap, ink)
            postInvalidateOnAnimation()
        }
    }
}
