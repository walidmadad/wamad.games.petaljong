package com.tilebloom.game.presentation

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.view.WindowInsets
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import com.tilebloom.game.R
import com.tilebloom.game.domain.*

class MainActivity : Activity() {
    private val game = Game(LevelOne.tiles())
    private lateinit var board: MahjongBoardView
    private lateinit var counter: TextView
    private lateinit var feedback: TextView
    private lateinit var restart: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getIntArray("moves")?.let { moves ->
            if (moves.size % 2 == 0) {
                game.restore(GameSnapshot(moves.toList().chunked(2).map { PairMove(it[0], it[1]) },
                    savedInstanceState.getInt("selected", -1).takeIf { it >= 0 }))
            }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.rgb(16, 47, 39))
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(dp(18) + safe.left, dp(12) + safe.top, dp(18) + safe.right, dp(12) + safe.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(dp(18) + insets.systemWindowInsetLeft, dp(12) + insets.systemWindowInsetTop,
                    dp(18) + insets.systemWindowInsetRight, dp(12) + insets.systemWindowInsetBottom)
            }
            insets
        }
        fun label(text: String, size: Float) = TextView(this).apply {
            this.text = text; textSize = size; gravity = Gravity.CENTER
            setTextColor(Color.rgb(245, 237, 214))
            root.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        label(getString(R.string.app_name), 30f)
        label(getString(R.string.subtitle), 15f)
        label(getString(R.string.level), 16f)
        counter = label("", 18f).apply {
            id = R.id.counter
            accessibilityLiveRegion = android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        board = MahjongBoardView(this, game) { update() }
        board.id = R.id.board
        root.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        feedback = label("", 18f).apply {
            id = R.id.feedback
            accessibilityLiveRegion = android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        label(getString(R.string.instruction), 16f)
        restart = Button(this).apply {
            id = R.id.restart
            minimumHeight = dp(48)
            setOnClickListener { game.restart(); board.clearFeedback(); update() }
        }
        root.addView(restart, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        root.requestApplyInsets()
        update()
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun update() {
        counter.text = getString(R.string.pairs, game.remainingPairs)
        feedback.text = when (game.status) {
            Status.PLAYING -> ""
            Status.WON -> getString(R.string.won) + "\n" + getString(R.string.won_detail)
            Status.NO_MOVES -> getString(R.string.no_moves) + "\n" + getString(R.string.no_moves_detail)
        }
        restart.setText(if (game.status == Status.WON) R.string.play_again else R.string.restart)
        board.refresh()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("moves", game.snapshot().moves.flatMap { listOf(it.first, it.second) }.toIntArray())
        outState.putInt("selected", game.selected ?: -1)
    }
}
