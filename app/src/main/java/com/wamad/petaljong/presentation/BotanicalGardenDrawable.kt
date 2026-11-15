package com.wamad.petaljong.presentation

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import com.wamad.petaljong.R
import com.wamad.petaljong.domain.GardenTheme

/** One bounded decode per Activity; static artwork never schedules animation or receives touches. */
class BotanicalGardenDrawable(resources: Resources) : Drawable() {
    private val image = BitmapFactory.decodeResource(resources, R.drawable.gameplay_botanical_garden)
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val atmosphere = Paint()
    private val quietCenter = Paint()
    private val destination = RectF()

    private var appliedTheme: GardenTheme? = null

    fun setTheme(theme: GardenTheme) {
        if (appliedTheme == theme) return
        appliedTheme = theme
        // Gentle grading preserves pink blossoms while differentiating every garden palette.
        val channels = when (theme.id) {
            "twilight" -> floatArrayOf(1.03f, .88f, 1.16f)
            "autumn", "sunrise" -> floatArrayOf(1.16f, 1.02f, .78f)
            "lotus" -> floatArrayOf(.8f, 1.08f, 1.12f)
            else -> floatArrayOf(.88f, 1.08f, .9f)
        }
        imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            setScale(channels[0], channels[1], channels[2], 1f)
        })
        atmosphere.color = (theme.background and 0x00ffffff) or (72 shl 24)
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        destination.set(bounds)
        // Stretch the environmental composition to keep its flowers on the outer edges
        // on both short and tall portrait displays, rather than cropping them away.
        quietCenter.shader = RadialGradient(destination.centerX(), destination.centerY(),
            (destination.height() * .58f).coerceAtLeast(1f),
            intArrayOf(0x65101c1a, 0x35101c1a, Color.TRANSPARENT),
            floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(image, null, destination, imagePaint)
        canvas.drawRect(destination, atmosphere)
        canvas.drawRect(destination, quietCenter)
    }
    override fun setAlpha(alpha: Int) { imagePaint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { imagePaint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
