package com.tilebloom.game.presentation

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** One compact, locale-aware row. Board coordinates remain independent of its direction. */
class GameplayHeader(context: Context, settings: View) : LinearLayout(context) {
    val levelLabel = TextView(context).apply {
        textSize = 14f
        setTextColor(GardenStyle.gold)
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        maxLines = 1
        setAutoSizeTextTypeUniformWithConfiguration(12, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
    }
    init {
        gravity = Gravity.CENTER_VERTICAL
        val height = GardenStyle.dp(context, 48)
        addView(settings, LayoutParams(-2, height))
        addView(levelLabel, LayoutParams(0, height, 1f).apply {
            marginStart = GardenStyle.dp(context, 12)
        })
    }
}
