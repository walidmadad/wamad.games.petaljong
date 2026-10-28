package com.tilebloom.game.presentation

import android.app.Activity
import android.app.AlertDialog
import com.tilebloom.game.ads.RewardedAdManager
import com.tilebloom.game.ads.AdvertisingService
import com.tilebloom.game.ads.AdExposure
import com.tilebloom.game.ads.InterstitialService
import android.os.SystemClock
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
    private lateinit var homeActions: FrameLayout
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
    private lateinit var hint: GardenIconButton
    private var rewardDialog: AlertDialog? = null
    private var hintPending = false
    private var hintRequest = 0
    private lateinit var undo: GardenIconButton
    private lateinit var restart: GardenIconButton
    private lateinit var controls: LinearLayout
    private lateinit var defeatActions: LinearLayout
    private var pendingPlay = false
    val isPuzzleReady get() = ::navigation.isInitialized
    private var previewLevel = 1L
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

    private var resumed = false
    private var focused = false
    private var pendingVictory: Long? = null
    private val exposureChanged: () -> Unit = { if (!isDestroyed) { accountGameplay(); if (::navigation.isInitialized) update() } }
    private var timingEligible = false
    private fun accountGameplay() {
        val ready = ::navigation.isInitialized
        val eligible = ActiveGameplayConditions(resumed, focused,
            ready && navigation.state.screen == GardenScreen.GAME,
            ready && game.status == Status.PLAYING, online, GameStore.transitioning || !ready,
            AdExposure.busy || AdvertisingService.provider.state == RewardedAdManager.State.SHOWING,
            RewardedAdManager.consentInProgress, rewardDialog?.isShowing == true).eligible
        AdExposure.policy.active(SystemClock.elapsedRealtime(), eligible)
        if (eligible != timingEligible) { timingEligible = eligible; AdExposure.save() }
    }
    private fun completedByPlayer() {
        update() // Persists actual victory and stops active time before considering an ad.
        if (game.status != Status.WON || !GameStore.persistVictory(applicationContext)) return
        val level = session.level.id
        val eligible = AdExposure.policy.victory(level, true, SystemClock.elapsedRealtime())
        val saved = AdExposure.save() // Consume before asynchronous presentation.
        if (!saved || !eligible || !resumed || !focused || !online || !InterstitialService.provider.ready) return
        pendingVictory = level
        next.isEnabled = false
        // Let the final match (180ms) and victory confirmation finish; never wait for inventory.
        root.postDelayed({
            if (pendingVictory != level) return@postDelayed
            pendingVictory = null
            if (!isDestroyed && resumed && focused && online && session.level.id == level &&
                navigation.state.screen == GardenScreen.GAME && game.status == Status.WON &&
                !GameStore.transitioning && rewardDialog?.isShowing != true && !AdExposure.busy) {
                InterstitialService.provider.show(this)
            }
            if (!isDestroyed) update()
        }, 350L)
    }
    private var online = false
    private var showConnection = false
    private lateinit var connectivity: NetworkObserver
    private lateinit var offlineHome: LinearLayout
    private lateinit var offlineScroll: ScrollView
    private lateinit var onlinePrivacyParent: LinearLayout
    private var lastRetry = -1000L
    private lateinit var connection: LinearLayout
    private lateinit var connectionShade: FrameLayout
    private lateinit var privacy: Button
    private fun connectivityChanged(available: Boolean, panel: Boolean) {
        online = available; showConnection = panel
        if (!available) {
            pendingVictory = null
            hintRequest++; hintPending = false
            rewardDialog?.dismiss()
            if (::board.isInitialized) { board.clearFeedback(); tray.clearFeedback() }
        }
        update()
    }
    private var backCallback: android.window.OnBackInvokedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdExposure.initialize(applicationContext)
        AdExposure.changed = exposureChanged
        // Decode only the bounded save envelope; generation/restoration stays off the UI thread.
        pendingPlay = savedInstanceState?.getBoolean("pending_play") ?: false
        val metadata = ProgressRepository.load(applicationContext)
        previewLevel = metadata.highestUnlocked
        createContent()
        connectivity = ConnectivityService.factory(this, ::connectivityChanged)
        AdvertisingService.provider.attach(this)
        Thread {
            val loaded = runCatching { GameStore.enter(applicationContext, savedInstanceState?.getString("garden_screen") != GardenScreen.GAME.name, ConnectivityMonitor.validated(applicationContext)) }
            runOnUiThread {
                if (!isDestroyed) loaded.onSuccess {
                    progress = it.first
                    if (progress.recovered) android.util.Log.w("TileBloomSave", "Board snapshot rejected; completion retained")
                    val restored = savedInstanceState?.takeUnless { _ -> it.second }?.let { saved ->
                        val screen = runCatching { GardenScreen.valueOf(saved.getString("garden_screen").orEmpty()) }.getOrNull()
                        screen?.let { NavigationState(saved.getLong("garden_level"), it) }
                    }
                    navigation = LevelNavigation(session.level.id, restored)
                    if (pendingPlay && online) navigation.play(session.level.id)
                    update()
                }.onFailure {
                    pendingPlay = false
                    play.isEnabled = online
                    startHint.setText(R.string.load_retry)
                    play.setOnClickListener { recreate() }
                }
            }
        }.start()
    }

    private fun createContent() {
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
        createConnectionPanel()
        setContentView(root)
        root.requestApplyInsets()
        play.isEnabled = online && !pendingPlay
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
        add(GardenWelcomeArt(this).apply { id = R.id.home_art }, 10, dp(if (compact) 176 else 220))
        val levelCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = GardenStyle.surface(this@MainActivity, GardenStyle.panel)
            setPadding(dp(14), dp(16), dp(14), dp(16))
        }
        startLevel = textLabel(26f, GardenStyle.gold).apply {
            id = R.id.start_level; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 2
            setAutoSizeTextTypeUniformWithConfiguration(12, 26, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        levelCard.addView(startLevel, LinearLayout.LayoutParams(-1, -2))
        startHint = textLabel(14f, GardenStyle.secondary).apply { setPadding(0, dp(8), 0, 0) }
        levelCard.addView(startHint, LinearLayout.LayoutParams(-1, -2))
        add(levelCard, 8)
        play = CenteredPlayButton(this).apply {
            id = R.id.play; GardenStyle.button(this, true)
            contentDescription = getString(R.string.play_description, previewLevel)
            minimumHeight = dp(64); letterSpacing = .08f
            setOnClickListener {
                if (!online) return@setOnClickListener
                if (!::navigation.isInitialized) {
                    pendingPlay = true
                    play.isEnabled = false
                    startHint.setText(R.string.preparing_garden)
                } else if (!GameStore.transitioning && navigation.state.screen == GardenScreen.START) {
                    navigation.play(session.level.id)
                    update()
                }
            }
        }
        homeActions = FrameLayout(this).apply { id = R.id.home_actions }
        homeActions.addView(play, FrameLayout.LayoutParams(-1, dp(64), Gravity.CENTER))
        add(homeActions, 16)
        privacy = Button(this).apply {
            id = R.id.privacy_options
            setText(R.string.privacy_short); GardenStyle.button(this, false)
            textSize = 13f; minimumHeight = dp(48); elevation = 0f
            contentDescription = getString(R.string.privacy_options)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_privacy, 0, 0, 0)
            compoundDrawablePadding = dp(8)
            setOnClickListener {
                if (AdvertisingService.provider.privacyRequired) AdvertisingService.provider.privacyOptions(this@MainActivity)
                else Toast.makeText(this@MainActivity, R.string.privacy_not_required, Toast.LENGTH_SHORT).show()
            }
        }
        onlinePrivacyParent = content
        content.addView(privacy, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(12) })
        // Cap the welcoming composition on wider devices; scroll protects large-font navigation.
        val frame = FrameLayout(this)
        frame.addView(content, FrameLayout.LayoutParams(if (resources.configuration.screenWidthDp > 480) dp(420) else -1, -2, Gravity.CENTER))
        startScreen.addView(frame, FrameLayout.LayoutParams(-1, -2))
        root.addView(startScreen, FrameLayout.LayoutParams(-1, -1))
    }

    private fun createConnectionPanel() {
        connectionShade = FrameLayout(this).apply {
            id = R.id.connection_overlay
            setBackgroundColor(android.graphics.Color.argb(165, 9, 28, 22))
            isClickable = true; isFocusable = true; visibility = View.GONE
        }
        root.addView(connectionShade, FrameLayout.LayoutParams(-1, -1))
        connection = LinearLayout(this).apply {
            id = R.id.connection_panel
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(18), dp(18), dp(18), dp(14))
            background = GardenStyle.surface(this@MainActivity, GardenStyle.panel, border = false)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        connection.background = null
        connection.addView(ImageView(this).apply {
            id = R.id.offline_icon
            setImageResource(R.drawable.ic_network_off)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(96), dp(96)))
        connection.addView(textLabel(28f).apply {
            setText(R.string.offline_heading); setPadding(0, dp(24), 0, dp(10))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        connection.addView(textLabel(16f, GardenStyle.secondary).apply { setText(R.string.connect_continue) })
        connection.addView(Button(this).apply {
            id = R.id.connection_retry; GardenStyle.button(this, true); setText(R.string.connection_retry)
            contentDescription = getString(R.string.connection_retry)
            GardenStyle.primaryIcon(this, R.drawable.ic_restart)
            setOnClickListener {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastRetry < 1000) return@setOnClickListener
                lastRetry = now
                connectivity.retry()
                announceForAccessibility(getString(R.string.checking_connection))
                isEnabled = false
                postDelayed({ isEnabled = true }, 1000)
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(30) })
        connection.addView(Button(this).apply {
            id = R.id.network_settings
            GardenStyle.button(this); textSize = 14f; setText(R.string.network_settings_short)
            background = GardenStyle.surface(this@MainActivity, android.graphics.Color.TRANSPARENT, 18)
            elevation = 0f
            GardenStyle.primaryIcon(this, R.drawable.ic_network_settings, GardenStyle.gold)
            contentDescription = getString(R.string.network_settings)
            setOnClickListener {
                val actions = if (Build.VERSION.SDK_INT >= 29) listOf(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY,
                    android.provider.Settings.ACTION_WIRELESS_SETTINGS, android.provider.Settings.ACTION_SETTINGS)
                    else listOf(android.provider.Settings.ACTION_WIRELESS_SETTINGS, android.provider.Settings.ACTION_SETTINGS)
                val opened = actions.any { action -> runCatching {
                    startActivity(android.content.Intent(action)); true
                }.getOrDefault(false) }
                if (!opened) Toast.makeText(this@MainActivity, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
            }
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
        offlineHome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(16))
        }
        offlineHome.addView(textLabel(38f).apply {
            setText(R.string.app_name); typeface = Typeface.create("serif", Typeface.NORMAL)
        }, LinearLayout.LayoutParams(-1, -2))
        offlineHome.addView(Space(this), LinearLayout.LayoutParams(1, dp(24), 1f))
        offlineHome.addView(connection, LinearLayout.LayoutParams(-1, -2))
        offlineHome.addView(Space(this), LinearLayout.LayoutParams(1, dp(24), 1f))
        offlineScroll = ScrollView(this).apply {
            id = R.id.offline_home; isFillViewport = true
            addView(offlineHome, android.view.ViewGroup.LayoutParams(-1, -2))
        }
        root.addView(offlineScroll, FrameLayout.LayoutParams(
            if (resources.configuration.screenWidthDp > 480) dp(420) else -1, -1, Gravity.CENTER))
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
            setPadding(dp(12), dp(7), dp(12), dp(7))
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
        column.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(10) })
        tray = TileTrayView(this) { game.tray }.apply { id = R.id.tray }
        column.addView(tray, LinearLayout.LayoutParams(-1, dp(if (compact) 76 else 84)))
        board = MahjongBoardView(this, game) { completedByPlayer() }.apply { id = R.id.board }
        val area = FrameLayout(this)
        column.addView(area, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(4) })
        area.addView(board, FrameLayout.LayoutParams(-1, -1))
        createOutcome(area)
        feedback = textLabel(12f, GardenStyle.secondary).apply {
            id = R.id.feedback; setPadding(0, dp(4), 0, dp(6))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        column.addView(feedback, LinearLayout.LayoutParams(-1, -2))
        controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
            background = GardenStyle.surface(this@MainActivity, GardenStyle.panel, 28, false)
            clipChildren = false; clipToPadding = false
        }
        home = GardenIconButton(this, R.drawable.ic_home).apply {
            id = R.id.home; contentDescription = getString(R.string.home_description); tooltipText = contentDescription
            setOnClickListener {
                if (online && game.status != Status.WON && !GameStore.transitioning) {
                    hintRequest++; hintPending = false; board.clearFeedback(); tray.clearFeedback()
                    navigation.home(session.level.id); update()
                }
            }
        }
        hint = GardenIconButton(this, R.drawable.ic_hint).apply {
            id = R.id.hint
            setOnClickListener {
                if (!online) return@setOnClickListener
                if (progress.hintCredits == 0) { requestReward(RewardPurpose.HINT); return@setOnClickListener }
                if (!hintPending && board.hintedTileId == null && !GameStore.transitioning && game.status == Status.PLAYING && progress.hintCredits > 0) {
                    hintPending = true
                    val request = ++hintRequest
                    val expected = game.snapshot()
                    val identity = session.level.id
                    val tiles = session.level.tiles
                    this@MainActivity.hint.isEnabled = false
                    Thread {
                        val result = runCatching { HintPolicy.suggest(tiles, expected) }.getOrNull()
                        runOnUiThread {
                            if (!isDestroyed && request == hintRequest) {
                                hintPending = false
                                if (online && navigation.state.screen == GardenScreen.GAME && session.level.id == identity && game.snapshot() == expected) {
                                    val id = result?.id
                                    if (id != null && progress.acceptHint(id, expected)) {
                                        board.showHint(id)
                                        update()
                                    } else {
                                        update()
                                        Toast.makeText(this@MainActivity, R.string.no_safe_hint, Toast.LENGTH_SHORT).show()
                                    }
                                } else update()
                            }
                        }
                    }.start()
                }
            }
        }
        undo = GardenIconButton(this, R.drawable.ic_undo).apply {
            id = R.id.undo
            setOnClickListener {
                if (!online) return@setOnClickListener
                if (progress.wallet.balance == 0) { requestReward(RewardPurpose.UNDO); return@setOnClickListener }
                if (!GameStore.transitioning && progress.undo()) {
                    hintRequest++; hintPending = false; board.clearFeedback(); tray.clearFeedback(); update()
                }
            }
        }
        restart = GardenIconButton(this, R.drawable.ic_restart).apply {
            id = R.id.restart; contentDescription = getString(R.string.restart_description); tooltipText = contentDescription
            setOnClickListener {
                if (online && !GameStore.transitioning && session.restart()) {
                    hintRequest++; hintPending = false; board.clearFeedback(); tray.clearFeedback(); update()
                }
            }
        }
        listOf(home, hint, undo, restart).forEach { button ->
            controls.addView(button, LinearLayout.LayoutParams(dp(64), dp(76)).apply {
                marginStart = dp(2); marginEnd = dp(2)
            })
        }
        column.addView(controls, LinearLayout.LayoutParams(-2, -2))
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
        defeatActions = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        outcome.addView(defeatActions, LinearLayout.LayoutParams(-1, -2))
        if (Build.VERSION.SDK_INT >= 28) outcomeTitle.isAccessibilityHeading = true
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(4), dp(12), dp(4), dp(12))
        }
        center.addView(outcome, LinearLayout.LayoutParams(-1, -2))
        outcomeScroll = ScrollView(this).apply { isFillViewport = true; visibility = View.GONE; isClickable = true }
        outcomeScroll.addView(center, FrameLayout.LayoutParams(-1, -2))
        area.addView(outcomeScroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun advance() {
        if (!online || AdExposure.busy || pendingVictory != null || GameStore.transitioning || !session.canAdvance) return
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

    fun refreshPrivacyOptions() {
        privacy.visibility = if (AdvertisingService.provider.privacyRequired) View.VISIBLE else View.GONE
        privacy.isEnabled = AdvertisingService.provider.privacyRequired
    }

    private fun update() {
        accountGameplay()
        if (online && resumed) {
            if (AdvertisingService.provider === RewardedAdManager) RewardedAdManager.load()
            InterstitialService.provider.prepare(this)
        }
        play.isEnabled = online && !GameStore.transitioning
        privacy.visibility = if (AdvertisingService.provider.privacyRequired) View.VISIBLE else View.GONE
        privacy.isEnabled = AdvertisingService.provider.privacyRequired
        play.visibility = if (showConnection) View.GONE else View.VISIBLE
        play.contentDescription = getString(R.string.play_description, if (::navigation.isInitialized) session.level.id else previewLevel)
        val showingPuzzle = ::navigation.isInitialized && navigation.state.screen == GardenScreen.GAME
        connectionShade.visibility = if (showConnection && showingPuzzle) View.VISIBLE else View.GONE
        // A home route never exposes level/art/Play until validated connectivity arrives.
        val offlineAtHome = !online && !showingPuzzle
        offlineScroll.visibility = if (offlineAtHome) View.VISIBLE else View.GONE
        startScreen.visibility = if (online && !showingPuzzle) View.VISIBLE else View.GONE
        connection.visibility = if (offlineAtHome || showConnection && showingPuzzle) View.VISIBLE else View.GONE
        val connectionParent = if (showingPuzzle) connectionShade else offlineHome
        if ((if (showingPuzzle) (connection.parent as? View)?.parent else connection.parent) !== connectionParent) {
            (connection.parent as? android.view.ViewGroup)?.removeView(connection)
            if (showingPuzzle) {
                connection.background = GardenStyle.surface(this, GardenStyle.panel, border = false)
                val scroll = ScrollView(this).apply { isFillViewport = false }
                // Overlay content scrolls on short screens and with enlarged text.
                connectionShade.removeAllViews()
                scroll.addView(connection)
                connectionShade.addView(scroll, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply {
                    setMargins(dp(24), dp(16), dp(24), dp(16))
                })
            } else {
                connection.background = null
                offlineHome.addView(connection, 2, LinearLayout.LayoutParams(-1, -2))
            }
        }
        val privacyParent = if (offlineAtHome) offlineHome else onlinePrivacyParent
        if (privacy.parent !== privacyParent) {
            (privacy.parent as? android.view.ViewGroup)?.removeView(privacy)
            privacyParent.addView(privacy, LinearLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(12)
            })
        }
        if (offlineAtHome) {
            privacy.background = null
            privacy.setTextColor(GardenStyle.gold)
        } else {
            GardenStyle.button(privacy); privacy.textSize = 13f; privacy.minimumHeight = dp(48); privacy.elevation = 0f
        }
        connectionShade.bringToFront()

        if (!::navigation.isInitialized) {
            startLevel.text = getString(R.string.start_level, previewLevel)
            startHint.setText(if (pendingPlay) R.string.preparing_garden else R.string.ready_garden)
            return
        }
        navigation.synchronize(session.level.id)
        GameStore.persist(this)
        val showingGame = navigation.state.screen == GardenScreen.GAME
        updateBackCallback(showingGame)
        startLevel.text = getString(R.string.start_level, session.level.id)
        startHint.setText(when {
            game.status == Status.WON -> R.string.pending_victory
            game.status == Status.LOST -> R.string.pending_defeat
            game.snapshot().selections.orEmpty().isNotEmpty() -> R.string.resume_garden
            else -> R.string.ready_garden
        })
        play.isEnabled = online && !GameStore.transitioning
        startScreen.visibility = if (showingGame || !online) View.GONE else View.VISIBLE
        if (showingGame) createGameplay()
        gameplay?.visibility = if (showingGame) View.VISIBLE else View.GONE
        gameplay?.importantForAccessibility = if (showConnection) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        if (!showingGame) return
        levelLabel.text = getString(R.string.level_number, session.level.id)
        counter.text = getString(R.string.pairs, game.remainingPairs)
        val presentation = ScreenPresentation.forStatus(game.status, session.canAdvance)
        val terminal = presentation.victory || game.status == Status.LOST
        board.importantForAccessibility = if (terminal) View.IMPORTANT_FOR_ACCESSIBILITY_NO else View.IMPORTANT_FOR_ACCESSIBILITY_YES
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
        next.isEnabled = online && !GameStore.transitioning && pendingVictory == null && !AdExposure.busy
        next.setText(if (GameStore.transitioning) R.string.preparing_garden else R.string.next_level)
        GardenStyle.primaryIcon(next, R.drawable.ic_next)
        feedback.visibility = if (terminal) View.GONE else View.VISIBLE
        feedback.setText(if (game.status == Status.LOST) R.string.defeat_hint else R.string.instruction)
        val lost = game.status == Status.LOST
        // Move the same controls: one action, one listener, one wallet source.
        val destination = if (lost) defeatActions else controls
        listOf(undo, restart).forEach { button ->
            if (button.parent !== destination) {
                (button.parent as? android.view.ViewGroup)?.removeView(button)
                val params = if (destination === controls) LinearLayout.LayoutParams(dp(64), dp(76)).apply {
                    marginStart = dp(2); marginEnd = dp(2)
                } else LinearLayout.LayoutParams(dp(64), dp(56)).apply {
                    marginStart = dp(8); marginEnd = dp(8)
                }
                destination.addView(button, params)
            }
        }
        defeatActions.visibility = if (lost) View.VISIBLE else View.GONE
        controls.visibility = if (presentation.footer) View.VISIBLE else View.GONE
        home.visibility = View.VISIBLE
        restart.visibility = View.VISIBLE
        hint.badge = progress.hintCredits
        hint.contentDescription = if (progress.hintCredits == 0) getString(R.string.hint_zero_description) else getString(R.string.hint_description, progress.hintCredits)
        hint.tooltipText = hint.contentDescription
        hint.isEnabled = online && !hintPending && !GameStore.transitioning && game.status == Status.PLAYING
        home.isEnabled = online && !GameStore.transitioning
        restart.isEnabled = online && !GameStore.transitioning && game.status != Status.WON
        undo.isEnabled = online && !GameStore.transitioning && (progress.wallet.balance == 0 || game.canUndo)
        undo.badge = progress.wallet.balance
        undo.contentDescription = if (progress.wallet.balance == 0) getString(R.string.undo_zero_description) else getString(R.string.undo_description, progress.wallet.balance)
        undo.tooltipText = undo.contentDescription
        board.isEnabled = online && !GameStore.transitioning
        board.refresh(); tray.refresh()
        connectionShade.bringToFront()
    }

    private fun requestReward(purpose: RewardPurpose) {
        if (!online || GameStore.transitioning || rewardDialog?.isShowing == true || AdvertisingService.provider.state == RewardedAdManager.State.SHOWING) return
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(20), dp(8), dp(20), dp(20))
        }
        val close = GardenIconButton(this, R.drawable.ic_close).apply {
            id = android.R.id.button2
            contentDescription = getString(R.string.close_reward)
        }
        panel.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)).apply { gravity = Gravity.END })
        val rewardIcon = android.widget.ImageView(this).apply {
            setImageResource(if (purpose == RewardPurpose.HINT) R.drawable.ic_hint else R.drawable.ic_undo)
            imageTintList = android.content.res.ColorStateList.valueOf(GardenStyle.gold)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        panel.addView(rewardIcon, LinearLayout.LayoutParams(dp(48), dp(48)))
        panel.addView(textLabel(24f, GardenStyle.ivory).apply {
            setText(if (purpose == RewardPurpose.HINT) R.string.reward_hint else R.string.reward_undo)
            setPadding(0, dp(12), 0, dp(8))
        })
        val loading = android.widget.ProgressBar(this).apply {
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(GardenStyle.gold)
        }
        val statusRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(28)
        }
        statusRow.addView(loading, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(8) })
        val availability = textLabel(13f, GardenStyle.secondary).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        statusRow.addView(availability)
        panel.addView(statusRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val watch = Button(this).apply {
            id = android.R.id.button1
            GardenStyle.button(this, true)
            minHeight = dp(52)
        }
        panel.addView(watch, LinearLayout.LayoutParams(-1, -2))
        rewardDialog = AlertDialog.Builder(this)
            .setView(panel)
            .create().also { dialog ->
                dialog.setOnDismissListener { accountGameplay(); AdExposure.save() }
                dialog.show()
                accountGameplay(); AdExposure.save()
                dialog.window?.setBackgroundDrawable(GardenStyle.surface(this, GardenStyle.panel))
                dialog.window?.setLayout(
                    minOf(resources.displayMetrics.widthPixels - dp(40), dp(360)), -2)
                close.setOnClickListener { dialog.dismiss() }
                watch.setOnClickListener {
                    if (!online) return@setOnClickListener
                    if (AdvertisingService.provider.state == RewardedAdManager.State.READY) {
                        if (AdvertisingService.provider.show(this, purpose)) dialog.dismiss()
                    } else AdvertisingService.provider.retry()
                }
                // Observe presentation state only: becoming ready never starts an ad.
                val refresh = object : Runnable {
                    private var renderedState: RewardedAdManager.State? = null
                    override fun run() {
                        if (!dialog.isShowing || isDestroyed) return
                        val state = AdvertisingService.provider.state
                        if (state == renderedState) {
                            panel.postDelayed(this, 250)
                            return
                        }
                        renderedState = state
                        val ready = state == RewardedAdManager.State.READY
                        val busy = state == RewardedAdManager.State.LOADING
                        loading.visibility = if (busy) View.VISIBLE else View.GONE
                        availability.setText(if (busy) R.string.ad_loading_short else R.string.ad_unavailable_short)
                        statusRow.visibility = if (ready) View.INVISIBLE else View.VISIBLE
                        watch.isEnabled = !busy
                        watch.setText(if (ready || busy) R.string.watch_ad else R.string.ad_retry)
                        watch.contentDescription = if (ready || busy) getString(
                            if (purpose == RewardPurpose.HINT) R.string.watch_hint else R.string.watch_undo
                        ) else watch.text
                        GardenStyle.primaryIcon(watch, if (ready || busy) R.drawable.ic_play else R.drawable.ic_restart)
                        panel.postDelayed(this, 250)
                    }
                }
                refresh.run()
            }
    }

    private fun returnHomeFromBack() {
        if (!online) return
        navigation.back(session.level.id, game.status == Status.WON, GameStore.transitioning)
        if (::board.isInitialized) { hintRequest++; hintPending = false; board.clearFeedback(); tray.clearFeedback() }
        update()
    }

    private fun updateBackCallback(showingGame: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (showingGame && backCallback == null) {
                backCallback = android.window.OnBackInvokedCallback { returnHomeFromBack() }.also {
                    onBackInvokedDispatcher.registerOnBackInvokedCallback(
                        android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, it)
                }
            } else if (!showingGame) {
                backCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
                backCallback = null
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::navigation.isInitialized && navigation.state.screen == GardenScreen.GAME) returnHomeFromBack()
        else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("pending_play", pendingPlay && !::navigation.isInitialized)
        if (::navigation.isInitialized) {
            outState.putString("garden_screen", navigation.state.screen.name)
            outState.putLong("garden_level", navigation.state.levelId)
        }
    }
    override fun onResume() {
        super.onResume(); resumed = true; update()
    }
    override fun onPause() {
        resumed = false; pendingVictory = null
        accountGameplay(); AdExposure.save()
        super.onPause()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        focused = hasFocus
        if (!hasFocus) pendingVictory = null
        accountGameplay(); AdExposure.save()
    }
    override fun onStart() {
        super.onStart()
        connectivity.start()
    }
    override fun onStop() {
        online = false
        if (::board.isInitialized) board.isEnabled = false
        connectivity.stop()
        super.onStop()
        ProgressRepository.awaitWrites()
    }
    override fun onDestroy() {
        if (AdExposure.changed === exposureChanged) AdExposure.changed = null
        connectivity.destroy()
        rewardDialog?.setOnDismissListener(null)
        rewardDialog?.dismiss()
        AdvertisingService.provider.detach(this)
        hintRequest++
        if (::board.isInitialized) board.clearFeedback()
        updateBackCallback(false)
        if (GameStore.onChanged === refreshListener) GameStore.onChanged = null
        super.onDestroy()
    }
    private fun dp(value: Int) = GardenStyle.dp(this, value)
}
