package com.wamad.petaljong.presentation

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Settings row plus a locale-aware title placed in the measured gap above the tile artwork. */
class GameplayHeader(context: Context, settings: View) : LinearLayout(context) {
    val levelLabel = object : TextView(context) {
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            paint.shader = android.graphics.LinearGradient(0f, h * .2f, 0f, h * .85f,
                intArrayOf(0xfff7e3a5.toInt(), 0xffe8c66d.toInt()),
                null, android.graphics.Shader.TileMode.CLAMP)
        }
    }.apply {
        background = null
        setPadding(GardenStyle.dp(context, 4), 0, GardenStyle.dp(context, 4), 0)
        includeFontPadding = false
        textSize = 38f
        typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        setShadowLayer(GardenStyle.dp(context, 2).toFloat(), 0f, GardenStyle.dp(context, 2).toFloat(), 0xff193b31.toInt())
        setAutoSizeTextTypeUniformWithConfiguration(16, 38, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        setTextColor(0xffe8c66d.toInt())
        gravity = Gravity.CENTER
        maxLines = 1
        isAllCaps = true
        // Real localized text retains native shaping and RTL; shrink only on narrow screens.
    }
    init {
        gravity = Gravity.CENTER_VERTICAL
        val height = GardenStyle.dp(context, 48)
        addView(settings, LayoutParams(-2, height))
    }
}
