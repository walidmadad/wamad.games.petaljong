package com.tilebloom.game.presentation

import android.content.Context
import android.graphics.*
import android.view.View
import com.tilebloom.game.domain.Symbol

/** Static original botanical composition. Shared bundled botanical artwork; no perpetual redraw. */
class GardenWelcomeArt(context: Context) : View(context) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ivory = LinearGradient(-25f, -47f, 25f, 39f,
        0xfffffdf5.toInt(), 0xffe9ddc3.toInt(), Shader.TileMode.CLAMP)
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO; BotanicalSymbols.prepare(context) }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val unit = minOf(width / 280f, height / 210f)
        c.save(); c.translate(width / 2f, height / 2f); c.scale(unit, unit)
        ink.style = Paint.Style.FILL; ink.color = Color.rgb(28, 65, 48)
        c.drawCircle(0f, 0f, 90f, ink)
        ink.style = Paint.Style.STROKE; ink.strokeWidth = .8f; ink.color = Color.rgb(100, 122, 78)
        c.drawCircle(0f, 0f, 91f, ink); c.drawArc(-104f, -104f, 104f, 104f, 30f, 120f, false, ink)
        c.drawArc(-104f, -104f, 104f, 104f, 210f, 120f, false, ink)
        fun tile(x: Float, y: Float, rotation: Float, symbol: Symbol) {
            c.save(); c.translate(x, y); c.rotate(rotation)
            ink.style = Paint.Style.FILL; ink.color = Color.argb(80, 0, 17, 11)
            c.drawRoundRect(-32f, -41f, 34f, 47f, 8f, 8f, ink)
            ink.color = Color.rgb(181, 167, 126)
            c.drawRoundRect(-33f, -45f, 33f, 43f, 8f, 8f, ink)
            ink.shader = ivory
            c.drawRoundRect(-33f, -47f, 33f, 39f, 8f, 8f, ink)
            ink.shader = null; ink.color = GardenStyle.gold
            ink.style = Paint.Style.STROKE; ink.strokeWidth = .7f
            c.drawRoundRect(-32.5f, -46.5f, 32.5f, 38.5f, 8f, 8f, ink)
            c.scale(23f, 23f); BotanicalSymbols.draw(c, ink, symbol, context); c.restore()
        }
        tile(-49f, 9f, -22f, Symbol.FERN)
        tile(48f, 9f, 22f, Symbol.FLOWER)
        tile(0f, -6f, -5f, Symbol.LEAF)
        ink.style = Paint.Style.FILL; ink.color = GardenStyle.gold
        c.drawCircle(0f, -100f, 2.5f, ink); c.drawCircle(0f, 100f, 2.5f, ink)
        c.restore()
    }
}
