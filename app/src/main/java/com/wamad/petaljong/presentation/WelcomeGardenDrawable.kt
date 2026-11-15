package com.wamad.petaljong.presentation

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import com.wamad.petaljong.R

/** Soft, permanently green entrance garden; deliberately quieter than the playing field. */
class WelcomeGardenDrawable(resources: Resources) : Drawable() {
    private val image = BitmapFactory.decodeResource(resources, R.drawable.gameplay_botanical_garden,
        BitmapFactory.Options().apply { inSampleSize = 8 })
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val veil = Paint()
    private val edgeShade = Paint()
    private val area = RectF()
    override fun onBoundsChange(bounds: Rect) {
        area.set(bounds)
        veil.shader = RadialGradient(area.centerX(), area.centerY(),
            (area.height() * .7f).coerceAtLeast(1f),
            intArrayOf(0xf2173f35.toInt(), 0xc0173f35.toInt(), 0x80102f27.toInt()),
            floatArrayOf(0f, .52f, 1f), Shader.TileMode.CLAMP)
        // Quiet both system-bar edges without interrupting the illustration.
        edgeShade.shader = LinearGradient(0f, area.top, 0f, area.bottom,
            intArrayOf(0xb0102f27.toInt(), Color.TRANSPARENT, Color.TRANSPARENT, 0xb0102f27.toInt()),
            floatArrayOf(0f, .13f, .87f, 1f), Shader.TileMode.CLAMP)
    }
    override fun draw(canvas: Canvas) {
        canvas.drawColor(0xff173f35.toInt())
        canvas.drawBitmap(image, null, area, paint)
        canvas.drawRect(area, veil)
        canvas.drawRect(area, edgeShade)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity() = PixelFormat.OPAQUE
}
