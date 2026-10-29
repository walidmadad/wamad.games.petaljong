package com.tilebloom.game.presentation

import android.content.Context
import android.graphics.*
import android.view.View
import com.tilebloom.game.domain.GardenTheme
import com.tilebloom.game.domain.Tile
import com.tilebloom.game.R

/** Presentation only; replayable engine state owns every tile. */
class TileTrayView(context: Context, private val tiles: () -> List<Tile>) : View(context) {
    init { layoutDirection = View.LAYOUT_DIRECTION_LTR }
    var theme = GardenTheme.forLevel(1)
        set(value) { field = value; invalidate() }
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
        contentDescription = context.getString(R.string.tray_description, ids.size, tiles().joinToString { BotanicalSymbols.label(context, it.symbol) })
        invalidate()
    }
    fun clearFeedback() { pulseUntil = 0; ghosts = emptyList(); lastTiles = tiles().toList(); previous = lastTiles.map { it.id }; invalidate() }
    override fun onDetachedFromWindow() { clearFeedback(); super.onDetachedFromWindow() }
    companion object { const val SLOT_ASPECT = .72f }
    /** Equal portrait holders, constrained by both available axes. */
    fun slotBounds(): List<RectF> {
        val gap = height * .1f
        val slotHeight = minOf(height - gap * 2, (width - gap * 5) / (4 * SLOT_ASPECT))
        val slotWidth = slotHeight * SLOT_ASPECT
        val left = (width - slotWidth * 4 - gap * 3) / 2
        val top = (height - slotHeight) / 2
        return List(4) { i -> RectF(left + i * (slotWidth + gap), top,
            left + i * (slotWidth + gap) + slotWidth, top + slotHeight) }
    }
    override fun onDraw(c: Canvas) {
        val gap = height * .1f
        val slots = slotBounds()
        val size = slots.first().width()
        ink.style = Paint.Style.FILL; ink.color = theme.palette.traySurface
        c.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), gap * 1.6f, gap * 1.6f, ink)
        ink.style = Paint.Style.STROKE; ink.strokeWidth = resources.displayMetrics.density
        ink.color = theme.palette.border
        c.drawRoundRect(1f, 1f, width - 1f, height - 1f, gap * 1.6f, gap * 1.6f, ink)
        repeat(4) { i ->
            val r = slots[i]
            val tile = tiles().getOrNull(i)
            ink.style = Paint.Style.FILL
            ink.color = if (tile == null) theme.palette.emptyTraySlot else theme.palette.availableTileFace
            c.drawRoundRect(r, gap, gap, ink)
            ink.style = Paint.Style.STROKE; ink.strokeWidth = resources.displayMetrics.density
            ink.color = if (tiles().size == 4) theme.palette.secondaryAccent else if (tile == null) theme.palette.border else theme.palette.primaryAccent
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
            ink.style = Paint.Style.STROKE; ink.color = (theme.palette.primaryAccent and 0x00ffffff) or (90 shl 24); ink.strokeWidth = 2f
            c.drawRoundRect(2f, 2f, width - 2f, height - 2f, gap, gap, ink)
            postInvalidateOnAnimation()
        }
    }
}
