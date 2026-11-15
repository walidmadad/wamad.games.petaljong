package com.wamad.petaljong.presentation

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.ceil

/** Positions the title before drawing, using the tray boundary and actual tile envelope.
 * The board keeps all available space unless its artwork leaves too little room for text.
 */
class GameplayBoardArea(
    context: Context,
    private val board: MahjongBoardView,
    private val title: TextView
) : FrameLayout(context) {
    private var boardInset = 0
    private var gap = 0

    init {
        addView(board, LayoutParams(-1, -1))
        addView(title, LayoutParams(-1, -2))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = measuredWidth
        val h = measuredHeight
        val spacing = GardenStyle.dp(context, 8)
        // At the smallest supported type size, reserve the complete shaped font line.
        val metrics = android.text.TextPaint(title.paint).apply {
            textSize = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, 16f, resources.displayMetrics)
        }.fontMetrics
        val minimumGap = ceil(metrics.descent - metrics.ascent).toInt() + spacing * 2
        fun artworkTop(inset: Int) = inset + board.artworkTop(w, (h - inset).coerceAtLeast(0))
        boardInset = 0
        if (artworkTop(0) < minimumGap) {
            // Find the smallest reservation that clears the text, in this measure pass.
            // This monotonic search handles both width-limited and height-limited boards.
            var low = 0
            var high = h
            while (high - low > 1) {
                val middle = (low + high) / 2
                if (artworkTop(middle) < minimumGap) low = middle else high = middle
            }
            boardInset = high
        }
        board.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((h - boardInset).coerceAtLeast(0), MeasureSpec.EXACTLY))
        gap = (boardInset + board.artworkTop(w, board.measuredHeight)).toInt().coerceIn(0, h)
        title.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((gap - spacing * 2).coerceAtLeast(0), MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // Let FrameLayout place every child once, including the existing outcome overlay.
        // Laying out the board at two heights would reset its transient feedback twice.
        (board.layoutParams as LayoutParams).topMargin = boardInset
        (title.layoutParams as LayoutParams).topMargin = (gap - title.measuredHeight) / 2
        super.onLayout(changed, left, top, right, bottom)
    }
}
