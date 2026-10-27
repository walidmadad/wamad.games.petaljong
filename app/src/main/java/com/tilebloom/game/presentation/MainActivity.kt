package com.tilebloom.game.presentation

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.view.WindowInsets
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.View
import android.view.Gravity
import android.widget.*
import com.tilebloom.game.R
import com.tilebloom.game.domain.*

class MainActivity : Activity() {
    private val session = GardenSession()
    private val game get() = session.game
    private lateinit var levelLabel: TextView
    private lateinit var victoryScroll: ScrollView
    private lateinit var victory: LinearLayout
    private lateinit var victoryTitle: TextView
    private lateinit var victoryDetail: TextView
    private lateinit var next: Button
    private lateinit var board: MahjongBoardView
    private lateinit var counter: TextView
    private lateinit var feedback: TextView
    private lateinit var restart: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getIntArray("moves")?.let { moves ->
            if (moves.size % 2 == 0) {
                session.restore(SessionSnapshot(savedInstanceState.getInt("level_id", 1),
                    savedInstanceState.getLong("level_seed", 0), savedInstanceState.getInt("level_version", 0),
                    GameSnapshot(moves.toList().chunked(2).map { PairMove(it[0], it[1]) },
                    savedInstanceState.getInt("selected", -1).takeIf { it >= 0 })))
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
        val compact = resources.configuration.screenHeightDp < 640 || resources.configuration.fontScale > 1.3f
        val ivory = Color.rgb(245, 237, 214)
        val gold = Color.rgb(184, 163, 107)
        fun surface(color: Int, radius: Int, border: Boolean = false) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (border) setStroke(dp(1), gold)
        }
        fun label(text: String, size: Float) = TextView(this).apply {
            this.text = text; textSize = size; gravity = Gravity.CENTER
            setTextColor(Color.rgb(245, 237, 214))
            root.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        label(getString(R.string.app_name), if (compact) 30f else 36f).apply {
            typeface = Typeface.create("serif", Typeface.NORMAL)
            letterSpacing = .035f
            setPadding(0, 0, 0, dp(2))
        }
        label(getString(R.string.subtitle), 13f).apply {
            setTextColor(Color.rgb(191, 207, 187))
            letterSpacing = .06f
        }
        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = surface(Color.rgb(27, 65, 51), 16)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(if (compact) 10 else 22)
            bottomMargin = dp(if (compact) 4 else 10)
        })
        levelLabel = TextView(this).apply {
            text = getString(R.string.level)
            textSize = 14f
            setTextColor(gold)
            setPadding(0, 0, dp(8), 0)
        }
        status.addView(levelLabel, LinearLayout.LayoutParams(0, -2, 1f))
        counter = TextView(this).apply {
            id = R.id.counter
            textSize = 15f
            gravity = Gravity.END
            setTextColor(ivory)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        status.addView(counter, LinearLayout.LayoutParams(0, -2, 2f))
        board = MahjongBoardView(this, game) { update() }
        board.id = R.id.board
        val playArea = FrameLayout(this)
        root.addView(playArea, LinearLayout.LayoutParams(-1, 0, 1f))
        playArea.addView(board, FrameLayout.LayoutParams(-1, -1))
        victory = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = surface(Color.rgb(27, 65, 51), 24, true)
            visibility = View.GONE
        }
        victoryScroll = ScrollView(this).apply { isFillViewport = true; visibility = View.GONE }
        val victoryCenter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        }
        victoryCenter.addView(victory, LinearLayout.LayoutParams(-1, -2))
        victoryScroll.addView(victoryCenter, FrameLayout.LayoutParams(-1, -2))
        playArea.addView(victoryScroll, FrameLayout.LayoutParams(-1, -1))
        // Original leaf motif, drawn with the same botanical artwork as the tiles.
        victory.addView(object : View(this) {
            private val ink = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: android.graphics.Canvas) {
                canvas.save()
                canvas.translate(width / 2f, height / 2f)
                canvas.scale(dp(20).toFloat(), dp(20).toFloat())
                BotanicalSymbols.draw(canvas, ink, Symbol.LEAF)
                canvas.restore()
            }
        }, LinearLayout.LayoutParams(-1, dp(52)))
        victoryTitle = TextView(this).apply {
            textSize = if (compact) 24f else 28f; gravity = Gravity.CENTER
            setTextColor(ivory); typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        victory.addView(victoryTitle)
        victoryDetail = TextView(this).apply {
            textSize = 15f; gravity = Gravity.CENTER; setTextColor(ivory)
            setPadding(0, dp(8), 0, dp(12))
        }
        victory.addView(victoryDetail)
        next = Button(this).apply {
            id = R.id.next_level; text = getString(R.string.next_level); isAllCaps = false
            minimumHeight = dp(48)
            setOnClickListener {
                try {
                    if (session.advance()) { board.load(game); update() }
                } catch (_: IllegalStateException) {
                    Toast.makeText(this@MainActivity, R.string.generation_error, Toast.LENGTH_LONG).show()
                }
            }
        }
        victory.addView(next)
        victory.addView(Button(this).apply {
            text = getString(R.string.replay_level); isAllCaps = false; minimumHeight = dp(48)
            setOnClickListener { session.restart(); board.clearFeedback(); update() }
        })
        feedback = label("", 16f).apply {
            id = R.id.feedback
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setTextColor(gold)
            minLines = 2
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(10))
        }
        restart = Button(this).apply {
            id = R.id.restart
            minimumHeight = dp(48)
            isAllCaps = false
            textSize = 16f
            setTextColor(ivory)
            backgroundTintList = null
            background = RippleDrawable(ColorStateList.valueOf(Color.argb(45, 235, 218, 164)),
                surface(Color.rgb(35, 77, 58), 24, true),
                surface(Color.WHITE, 24))
            setPadding(dp(28), dp(10), dp(28), dp(10))
            setOnClickListener { game.restart(); board.clearFeedback(); update() }
        }
        root.addView(restart, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(4) })
        setContentView(root)
        root.requestApplyInsets()
        update()
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun update() {
        levelLabel.text = getString(R.string.level_number, session.level.id)
        val won = game.status == Status.WON
        victory.animate().cancel()
        victory.alpha = 1f
        val enteringVictory = won && victory.visibility != View.VISIBLE
        victory.visibility = if (won) View.VISIBLE else View.GONE
        victoryScroll.visibility = victory.visibility
        if (enteringVictory && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            victory.alpha = 0f
            victory.animate().alpha(1f).setDuration(200L).start()
        }
        victoryTitle.text = getString(if (session.level.id == Levels.COUNT) R.string.journey_complete else R.string.won)
        victoryDetail.text = getString(if (session.level.id == Levels.COUNT) R.string.journey_detail else R.string.won_detail)
        next.visibility = if (session.canAdvance) View.VISIBLE else View.GONE
        counter.text = getString(R.string.pairs, game.remainingPairs)
        feedback.text = when (game.status) {
            Status.PLAYING -> getString(R.string.instruction)
            Status.WON -> getString(R.string.won) + "\n" + getString(R.string.won_detail)
            Status.NO_MOVES -> getString(R.string.no_moves) + "\n" + getString(R.string.no_moves_detail)
        }
        feedback.setTextColor(if (game.status == Status.PLAYING) Color.rgb(191, 207, 187) else Color.rgb(184, 163, 107))
        restart.setText(if (game.status == Status.WON) R.string.play_again else R.string.restart)
        board.refresh()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("level_id", session.level.id)
        outState.putLong("level_seed", session.level.seed)
        outState.putInt("level_version", session.level.version)
        outState.putIntArray("moves", game.snapshot().moves.flatMap { listOf(it.first, it.second) }.toIntArray())
        outState.putInt("selected", game.selected ?: -1)
    }
}
