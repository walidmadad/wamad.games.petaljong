package com.tilebloom.game.presentation

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.view.WindowInsets
import android.graphics.Typeface
import android.view.View
import android.view.Gravity
import android.widget.*
import com.tilebloom.game.R
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.*
import com.tilebloom.game.storage.GameStore
import com.tilebloom.game.storage.ProgressRepository

class MainActivity : Activity() {
    private lateinit var progress: ProgressController
    private val session get() = progress.session
    private val game get() = session.game
    private lateinit var navigation: LevelNavigation
    private lateinit var root: FrameLayout
    private lateinit var startScreen: ScrollView
    private lateinit var startLevel: TextView
    private lateinit var startHint: TextView
    private lateinit var play: Button
    private var gameplay: LinearLayout? = null
    private lateinit var levelLabel: TextView
    private lateinit var counter: TextView
    private lateinit var board: MahjongBoardView
    private lateinit var tray: TileTrayView
    private lateinit var outcome: LinearLayout
    private lateinit var outcomeScroll: ScrollView
    private lateinit var outcomeTitle: TextView
    private lateinit var outcomeDetail: TextView
    private lateinit var next: Button
    private lateinit var undo: GardenIconButton
    private lateinit var restart: GardenIconButton
    private lateinit var home: GardenIconButton
    private lateinit var feedback: TextView
    private val compact get() = resources.configuration.screenHeightDp < 740 || resources.configuration.fontScale > 1.3f
    private val refreshListener: () -> Unit = {
        if (!isDestroyed && ::navigation.isInitialized) {
            navigation.synchronize(session.level.id)
            if (::board.isInitialized) { board.load(game); tray.clearFeedback() }
            update()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            setText(R.string.preparing_garden); gravity = Gravity.CENTER
            setTextColor(GardenStyle.ivory); setBackgroundColor(GardenStyle.forest)
        })
        Thread {
            val loaded = runCatching { GameStore.get(applicationContext) }
            runOnUiThread {
                if (!isDestroyed) loaded.onSuccess {
                    progress = it
                    val restored = savedInstanceState?.let { saved ->
                        val screen = runCatching { GardenScreen.valueOf(saved.getString("garden_screen").orEmpty()) }.getOrNull()
                        screen?.let { NavigationState(saved.getLong("garden_level"), it) }
                    }
                    navigation = LevelNavigation(session.level.id, restored)
                    createContent()
                }.onFailure {
                    setContentView(Button(this).apply {
                        setText(R.string.load_retry); setOnClickListener { recreate() }
                    })
                }
            }
        }.start()
    }

    private fun createContent() {
        if (progress.recovered) android.util.Log.w("TileBloomSave", "Board snapshot rejected; completion retained")
        root = FrameLayout(this).apply {
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(GardenStyle.forest, android.graphics.Color.rgb(20, 56, 43)))
        }
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        createStartScreen()
        setContentView(root)
        root.requestApplyInsets()
        GameStore.onChanged = refreshListener
        update()
    }

    private fun textLabel(size: Float, color: Int = GardenStyle.ivory) = TextView(this).apply {
        textSize = size; gravity = Gravity.CENTER; setTextColor(color)
    }

    private fun createStartScreen() {
        startScreen = ScrollView(this).apply {
            id = R.id.level_start; isFillViewport = true; clipToPadding = false
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(28))
        }
        fun add(view: View, top: Int = 0, height: Int = -2) {
            content.addView(view, LinearLayout.LayoutParams(-1, height).apply { topMargin = dp(top) })
        }
        add(textLabel(42f).apply {
            setText(R.string.app_name); typeface = Typeface.create("serif", Typeface.NORMAL)
            letterSpacing = .03f; maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(24, 42, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        })
        add(textLabel(14f, GardenStyle.secondary).apply { setText(R.string.subtitle); letterSpacing = .05f }, 6)
        add(GardenWelcomeArt(this), 14, dp(if (compact) 176 else 220))
        val levelCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = GardenStyle.surface(this@MainActivity, GardenStyle.panel)
            setPadding(dp(14), dp(16), dp(14), dp(16))
        }
        startLevel = textLabel(28f, GardenStyle.gold).apply {
            id = R.id.start_level; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 2
            setAutoSizeTextTypeUniformWithConfiguration(12, 28, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        levelCard.addView(startLevel, LinearLayout.LayoutParams(-1, -2))
        startHint = textLabel(14f, GardenStyle.secondary).apply { setPadding(0, dp(8), 0, 0) }
        levelCard.addView(startHint, LinearLayout.LayoutParams(-1, -2))
        add(levelCard, 12)
        play = Button(this).apply {
            id = R.id.play; GardenStyle.button(this, true)
            setText(R.string.play); contentDescription = getString(R.string.play_description)
            minimumHeight = dp(64); letterSpacing = .08f
            GardenStyle.primaryIcon(this, R.drawable.ic_play)
            setOnClickListener {
                if (!GameStore.transitioning && navigation.state.screen == GardenScreen.START) {
                    navigation.play(session.level.id)
                    update()
                }
            }
        }
        add(play, 20)
        // Cap the welcoming composition on wider devices; scroll protects large-font navigation.
        val frame = FrameLayout(this)
        frame.addView(content, FrameLayout.LayoutParams(if (resources.configuration.screenWidthDp > 480) dp(420) else -1, -2, Gravity.CENTER))
        startScreen.addView(frame, FrameLayout.LayoutParams(-1, -2))
        root.addView(startScreen, FrameLayout.LayoutParams(-1, -1))
    }

    private fun createGameplay() {
        if (gameplay != null) return
        val column = LinearLayout(this).apply {
            id = R.id.gameplay; orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(8), dp(16), dp(10))
        }
        gameplay = column
        column.addView(textLabel(if (compact) 25f else 29f).apply {
            setText(R.string.app_name); typeface = Typeface.create("serif", Typeface.NORMAL)
            letterSpacing = .03f
        }, LinearLayout.LayoutParams(-1, -2))
        val status = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GardenStyle.surface(this@MainActivity, GardenStyle.panel, 16, false)
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }
        levelLabel = textLabel(14f, GardenStyle.gold).apply {
            maxLines = 2
            setAutoSizeTextTypeUniformWithConfiguration(10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        }
        counter = textLabel(12f, GardenStyle.secondary).apply {
            id = R.id.counter; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        status.addView(levelLabel, LinearLayout.LayoutParams(-1, -2))
        status.addView(counter, LinearLayout.LayoutParams(-1, -2))
        column.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6); bottomMargin = dp(8) })
        tray = TileTrayView(this) { game.tray }.apply { id = R.id.tray }
        column.addView(tray, LinearLayout.LayoutParams(-1, dp(if (compact) 58 else 72)))
        board = MahjongBoardView(this, game) { update() }.apply { id = R.id.board }
        val area = FrameLayout(this)
        column.addView(area, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(4) })
        area.addView(board, FrameLayout.LayoutParams(-1, -1))
        createOutcome(area)
        feedback = textLabel(12f, GardenStyle.secondary).apply {
            id = R.id.feedback; setPadding(0, dp(4), 0, dp(6))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        column.addView(feedback, LinearLayout.LayoutParams(-1, -2))
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        home = GardenIconButton(this, R.drawable.ic_home).apply {
            id = R.id.home; contentDescription = getString(R.string.home_description); tooltipText = contentDescription
            setOnClickListener {
                if (game.status != Status.WON && !GameStore.transitioning) {
                    board.clearFeedback(); tray.clearFeedback()
                    navigation.home(session.level.id); update()
                }
            }
        }
        undo = GardenIconButton(this, R.drawable.ic_undo).apply {
            id = R.id.undo
            setOnClickListener {
                if (!GameStore.transitioning && progress.undo()) {
                    board.clearFeedback(); tray.clearFeedback(); update()
                }
            }
        }
        restart = GardenIconButton(this, R.drawable.ic_restart).apply {
            id = R.id.restart; contentDescription = getString(R.string.restart_description); tooltipText = contentDescription
            setOnClickListener {
                if (!GameStore.transitioning && session.restart()) {
                    board.clearFeedback(); tray.clearFeedback(); update()
                }
            }
        }
        listOf(home, undo, restart).forEach { button ->
            controls.addView(button, LinearLayout.LayoutParams(dp(64), dp(56)).apply {
                marginStart = dp(10); marginEnd = dp(10)
            })
        }
        column.addView(controls, LinearLayout.LayoutParams(-1, -2))
        root.addView(column, FrameLayout.LayoutParams(-1, -1))
    }

    private fun createOutcome(area: FrameLayout) {
        outcome = LinearLayout(this).apply {
            id = R.id.outcome; orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GardenStyle.surface(this@MainActivity, GardenStyle.panel, 24)
            visibility = View.GONE
        }
        outcomeTitle = textLabel(26f).apply { typeface = Typeface.create("serif", Typeface.NORMAL) }
        outcomeDetail = textLabel(15f, GardenStyle.secondary).apply { setPadding(0, dp(12), 0, dp(20)) }
        outcome.addView(outcomeTitle, LinearLayout.LayoutParams(-1, -2))
        outcome.addView(outcomeDetail, LinearLayout.LayoutParams(-1, -2))
        next = Button(this).apply {
            id = R.id.next_level; GardenStyle.button(this, true); setText(R.string.next_level)
            contentDescription = getString(R.string.next_description)
            GardenStyle.primaryIcon(this, R.drawable.ic_next)
            setOnClickListener { advance() }
        }
        outcome.addView(next, LinearLayout.LayoutParams(-1, -2))
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(4), dp(12), dp(4), dp(12))
        }
        center.addView(outcome, LinearLayout.LayoutParams(-1, -2))
        outcomeScroll = ScrollView(this).apply { isFillViewport = true; visibility = View.GONE }
        outcomeScroll.addView(center, FrameLayout.LayoutParams(-1, -2))
        area.addView(outcomeScroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun advance() {
        if (GameStore.transitioning || !session.canAdvance) return
        GameStore.transitioning = true
        update()
        val expected = session.level.id
        Thread {
            val result = runCatching {
                val definition = progress.prepareNext() ?: error("No pending victory")
                val state = progress.nextProgress(definition) ?: error("Stale transition")
                check(GameStore.persistTransition(applicationContext, state))
                definition
            }
            runOnUiThread {
                result.onSuccess {
                    if (session.level.id == expected && progress.commitNext(it)) {
                        DifficultyPolicy.transitionMessage(session.level.id)?.let { message ->
                            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }.onFailure { Toast.makeText(applicationContext, R.string.generation_error, Toast.LENGTH_LONG).show() }
                GameStore.transitioning = false
                // The current activity observes the committed identity even if its predecessor rotated.
                GameStore.onChanged?.invoke()
            }
        }.start()
    }

    private fun update() {
        navigation.synchronize(session.level.id)
        GameStore.persist(this)
        val showingGame = navigation.state.screen == GardenScreen.GAME
        startLevel.text = getString(R.string.start_level, session.level.id)
        startHint.setText(when {
            game.status == Status.WON -> R.string.pending_victory
            game.status == Status.LOST -> R.string.pending_defeat
            game.snapshot().selections.orEmpty().isNotEmpty() -> R.string.resume_garden
            else -> R.string.ready_garden
        })
        play.isEnabled = !GameStore.transitioning
        startScreen.visibility = if (showingGame) View.GONE else View.VISIBLE
        if (showingGame) createGameplay()
        gameplay?.visibility = if (showingGame) View.VISIBLE else View.GONE
        if (!showingGame) return
        levelLabel.text = getString(R.string.level_number, session.level.id)
        counter.text = getString(R.string.pairs, game.remainingPairs)
        val presentation = ScreenPresentation.forStatus(game.status, session.canAdvance)
        val terminal = presentation.victory || game.status == Status.LOST
        val enteringOutcome = terminal && outcome.visibility != View.VISIBLE
        outcome.animate().cancel(); outcome.alpha = 1f
        outcome.visibility = if (terminal) View.VISIBLE else View.GONE
        outcomeScroll.visibility = outcome.visibility
        if (enteringOutcome && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            outcome.alpha = 0f; outcome.animate().alpha(1f).setDuration(180L).start()
        }
        outcomeTitle.setText(if (game.status == Status.WON) R.string.won else if (game.tray.size == 4) R.string.tray_full else R.string.unrecoverable)
        outcomeDetail.text = if (game.status == Status.WON) {
            getString(R.string.level_complete, session.level.id) +
                if (session.level.id == Long.MAX_VALUE) "\n" + getString(R.string.level_limit) else ""
        } else getString(if (game.tray.size == 4) R.string.tray_full_detail else R.string.unrecoverable_detail)
        next.visibility = if (presentation.advance) View.VISIBLE else View.GONE
        next.isEnabled = !GameStore.transitioning
        next.setText(if (GameStore.transitioning) R.string.preparing_garden else R.string.next_level)
        feedback.visibility = if (game.status == Status.WON) View.GONE else View.VISIBLE
        feedback.setText(if (game.status == Status.LOST) R.string.defeat_hint else R.string.instruction)
        home.visibility = if (game.status == Status.WON) View.GONE else View.VISIBLE
        restart.visibility = home.visibility
        home.isEnabled = !GameStore.transitioning
        restart.isEnabled = !GameStore.transitioning && game.status != Status.WON
        undo.isEnabled = !GameStore.transitioning && game.canUndo && progress.wallet.balance > 0
        undo.badge = progress.wallet.balance
        undo.contentDescription = getString(R.string.undo_description, progress.wallet.balance)
        undo.tooltipText = undo.contentDescription
        board.refresh(); tray.refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::navigation.isInitialized) {
            outState.putString("garden_screen", navigation.state.screen.name)
            outState.putLong("garden_level", navigation.state.levelId)
        }
    }
    override fun onStop() {
        super.onStop()
        ProgressRepository.awaitWrites()
    }
    override fun onDestroy() {
        if (GameStore.onChanged === refreshListener) GameStore.onChanged = null
        super.onDestroy()
    }
    private fun dp(value: Int) = GardenStyle.dp(this, value)
}
