package com.wamad.petaljong.presentation

import android.graphics.*
import android.graphics.drawable.Drawable

/** Fine botanical engraving below the live label, inside the existing header height. */
internal class LevelFlourishDrawable : Drawable() {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xffd9b96f.toInt() }
    private val leaves = Path()
    override fun onBoundsChange(bounds: Rect) {
        leaves.reset()
        val cx = bounds.exactCenterX()
        val y = bounds.bottom - bounds.height() * .08f
        val unit = bounds.height() * .075f
        for (side in listOf(-1f, 1f)) {
            val start = cx + side * unit * 2
            val end = cx + side * unit * 10
            leaves.moveTo(start, y)
            leaves.quadTo(cx + side * unit * 6, y + unit * .4f, end, y - unit * .3f)
            leaves.lineTo(start, y + unit * .15f)
            for (i in 4..8 step 2) {
                val x = cx + side * unit * i
                leaves.moveTo(x, y)
                leaves.quadTo(x - side * unit * 2, y - unit * 2, x - side * unit * 2.5f, y - unit * 1.5f)
                leaves.quadTo(x - side * unit * 2.5f, y - unit * .2f, x, y)
            }
        }
    }
    override fun draw(canvas: Canvas) { canvas.drawPath(leaves, ink) }
    override fun setAlpha(alpha: Int) { ink.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { ink.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
