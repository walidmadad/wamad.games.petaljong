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
    private lateinit var progress: ProgressController
    private val session get() = progress.session
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
    private lateinit var undo: Button
    private lateinit var restart: Button
    private var loading = false
    private val refreshListener: () -> Unit = {
        if (::board.isInitialized && !isDestroyed) { board.load(game); update() }
    }
    override fun onDestroy() {
        if (com.tilebloom.game.storage.GameStore.onChanged === refreshListener)
            com.tilebloom.game.storage.GameStore.onChanged = null
        super.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Preparing garden…"; gravity = Gravity.CENTER
            setTextColor(Color.rgb(245, 237, 214)); setBackgroundColor(Color.rgb(16, 47, 39))
        })
        Thread {
            val loaded = runCatching { com.tilebloom.game.storage.GameStore.get(applicationContext) }
            runOnUiThread {
                if (!isDestroyed) loaded.onSuccess { progress = it; createContent(savedInstanceState) }
                    .onFailure { setContentView(Button(this).apply {
                        text = "Garden could not load. Tap to retry."
                        setOnClickListener { recreate() }
                    }) }
            }
        }.start()
    }
    private fun createContent(savedInstanceState: Bundle?) {
        if (progress.recovered) android.util.Log.w("TileBloomSave", "Board snapshot rejected; completion retained")
        // Durable moves always win. Bundle may restore only a selection on that exact board.
        if (savedInstanceState != null) {
            val snapshot = session.snapshot()
            val moves = snapshot.game.moves.flatMap { listOf(it.first, it.second) }.toIntArray()
            if (savedInstanceState.getLong("level_id") == snapshot.id &&
                savedInstanceState.getLong("level_seed") == snapshot.seed &&
                savedInstanceState.getInt("level_version") == snapshot.version &&
                savedInstanceState.getIntArray("moves")?.contentEquals(moves) == true) {
                val selected = savedInstanceState.getInt("selected", -1)
                if (selected >= 0 && game.isAvailable(selected)) game.select(selected)
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
        levelLabel.textSize = 16f
        levelLabel.minHeight = dp(48)
        levelLabel.isClickable = false
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
        fun styleVictory(button: Button, primary: Boolean) {
            button.setTextColor(ivory)
            button.backgroundTintList = null
            button.background = RippleDrawable(ColorStateList.valueOf(Color.argb(40, 235, 218, 164)),
                surface(if (primary) Color.rgb(43, 91, 65) else Color.rgb(22, 55, 43), 24, true),
                surface(Color.WHITE, 24))
            button.setPadding(dp(24), dp(10), dp(24), dp(10))
        }
        next = Button(this).apply {
            id = R.id.next_level; text = getString(R.string.next_level); isAllCaps = false
            minimumHeight = dp(48)
            setOnClickListener {
                if (com.tilebloom.game.storage.GameStore.transitioning || !session.canAdvance) return@setOnClickListener
                loading = true; com.tilebloom.game.storage.GameStore.transitioning = true; next.isEnabled = false; next.text = "Preparing garden…"
                val expected = session.level.id
                Thread {
                    val result = runCatching {
                        val started = android.os.SystemClock.elapsedRealtime()
                        val definition = progress.prepareNext() ?: error("No pending victory")
                        android.util.Log.i("TileBloomGeneration", "Level ${definition.id}: ${android.os.SystemClock.elapsedRealtime() - started} ms on Android worker")
                        val state = progress.nextProgress(definition) ?: error("Stale transition")
                        check(com.tilebloom.game.storage.GameStore.persistTransition(this@MainActivity, state))
                        definition
                    }
                    runOnUiThread {
                        loading = false
                        result.onSuccess {
                            if (session.level.id == expected && progress.commitNext(it)) {
                                board.load(game)
                                com.tilebloom.game.domain.DifficultyPolicy.transitionMessage(session.level.id)?.let { message ->
                                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.onFailure {
                            Toast.makeText(this@MainActivity, R.string.generation_error, Toast.LENGTH_LONG).show()
                        }
                        com.tilebloom.game.storage.GameStore.transitioning = false
                        com.tilebloom.game.storage.GameStore.onChanged?.invoke()
                    }
                }.start()
            }
        }
        styleVictory(next, true)
        victory.addView(next, LinearLayout.LayoutParams(-1, -2))
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
            setOnClickListener { session.restart(); board.clearFeedback(); update() }
        }
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        undo = Button(this).apply {
            id = R.id.undo; isAllCaps = false; textSize = 16f; minimumHeight = dp(48); maxLines = 1
            styleVictory(this, true)
            setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(Color.rgb(191, 207, 187), ivory)))
            setOnClickListener {
                if (game.canUndo && progress.wallet.balance > 0) {
                    if (progress.undo()) { board.clearFeedback(); update() }
                    else android.util.Log.e("TileBloomUndo", "Validated history reconstruction failed; board retained")
                }
            }
        }
        actions.addView(undo, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
        actions.addView(restart, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
        setContentView(root)
        root.requestApplyInsets()
        com.tilebloom.game.storage.GameStore.onChanged = refreshListener
        update()
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun update() {
        com.tilebloom.game.storage.GameStore.persist(this)
        levelLabel.text = getString(R.string.level_number, session.level.id)
        val presentation = com.tilebloom.game.presentation.board.ScreenPresentation.forStatus(game.status, session.canAdvance)
        val won = presentation.victory
        victory.animate().cancel()
        victory.alpha = 1f
        val enteringVictory = won && victory.visibility != View.VISIBLE
        victory.visibility = if (won) View.VISIBLE else View.GONE
        victoryScroll.visibility = victory.visibility
        if (enteringVictory && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            victory.alpha = 0f
            victory.animate().alpha(1f).setDuration(200L).start()
        }
        victoryTitle.text = getString(R.string.won)
        victoryDetail.text = "Beautifully done.\nLevel ${session.level.id} Complete" +
            if (session.level.id == Long.MAX_VALUE) "\nLevel number limit reached. Progress saved." else ""
        loading = com.tilebloom.game.storage.GameStore.transitioning
        next.isEnabled = !loading
        next.text = if (loading) "Preparing garden…" else getString(R.string.next_level)
        next.visibility = if (presentation.advance) View.VISIBLE else View.GONE
        counter.text = getString(R.string.pairs, game.remainingPairs)
        feedback.visibility = if (presentation.footer) View.VISIBLE else View.GONE
        restart.visibility = if (presentation.restart) View.VISIBLE else View.GONE
        feedback.text = when (game.status) {
            Status.PLAYING -> getString(R.string.instruction)
            Status.WON -> ""
            Status.NO_MOVES -> getString(R.string.no_moves) + "\n" + getString(R.string.no_moves_detail)
        }
        feedback.setTextColor(if (game.status == Status.PLAYING) Color.rgb(191, 207, 187) else Color.rgb(184, 163, 107))
        // The wallet is global, including before the first move and after victory.
        undo.visibility = View.VISIBLE
        undo.isEnabled = game.canUndo && progress.wallet.balance > 0
        undo.text = resources.getStringArray(R.array.undo_balances).getOrNull(progress.wallet.balance)
            ?: getString(R.string.undo_credits, progress.wallet.balance)
        restart.setText(R.string.restart)
        board.refresh()
    }
    override fun onStop() {
        super.onStop()
        // Drain already-submitted small writes on ordinary departure; never resubmit stale state.
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!::progress.isInitialized) return
        outState.putLong("level_id", session.level.id)
        outState.putLong("level_seed", session.level.seed)
        outState.putInt("level_version", session.level.version)
        outState.putIntArray("moves", game.snapshot().moves.flatMap { listOf(it.first, it.second) }.toIntArray())
        outState.putInt("selected", game.selected ?: -1)
    }
}
