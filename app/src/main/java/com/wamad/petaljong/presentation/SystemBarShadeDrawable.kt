package com.wamad.petaljong.presentation

import android.graphics.*
import android.graphics.drawable.Drawable

/** Insets define the protection area; never inserts a layout spacer or consumes touches. */
class SystemBarShadeDrawable : Drawable() {
    private val paint = Paint().apply { color = 0x99102a21.toInt() }
    var top = 0
    var bottom = 0
    override fun draw(canvas: Canvas) {
        canvas.drawRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(),
            (bounds.top + top).toFloat(), paint)
        canvas.drawRect(bounds.left.toFloat(), (bounds.bottom - bottom).toFloat(),
            bounds.right.toFloat(), bounds.bottom.toFloat(), paint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
