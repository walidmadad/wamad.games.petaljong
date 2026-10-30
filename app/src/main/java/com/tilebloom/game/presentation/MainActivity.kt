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
    private val practice get() = intent.getBooleanExtra("tutorial_practice", false)
    private var tutorialOverlay: TutorialOverlay? = null
    private var completionVisible = false
    private var practiceInFront = false
    private fun persistGame() { if (!practice) GameStore.persist(this) }
    private fun saveExposure(): Boolean = !practice && AdExposure.save()
    private lateinit var audio: com.tilebloom.game.audio.GameAudioManager
    private lateinit var homeSettings: View
    private var settingsDialog: AlertDialog? = null
    private lateinit var progress: ProgressController
    private val session get() = progress.session
    private val game get() = session.game
    private lateinit var navigation: LevelNavigation
    private lateinit var root: FrameLayout
    private lateinit var startScreen: ScrollView
    private lateinit var startLevel: TextView
    private lateinit var startHint: TextView
    private lateinit var levelCard: ContinueLevelCard
    private var gameplay: LinearLayout? = null
    private lateinit var levelLabel: TextView
    private lateinit var board: MahjongBoardView
    private lateinit var tray: TileTrayView
    private lateinit var outcome: LinearLayout
    private lateinit var outcomeScroll: ScrollView
    private lateinit var outcomeTitle: TextView
    private lateinit var outcomeDetail: TextView
    private lateinit var next: ContinueLevelCard
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
            val boardStart = System.nanoTime()
            navigation.synchronize(session.level.id)
            if (::board.isInitialized) { board.load(game); tray.clearFeedback() }
            update()
            android.util.Log.i("TileBloomPreparation", "level=${session.level.id} boardAndPresentationNs=${System.nanoTime()-boardStart}")
        }
    }

    private var resumed = false
    private var focused = false
    private var pendingVictory: Long? = null
    private val exposureChanged: () -> Unit = { if (!isDestroyed) { audio.advertisement(AdExposure.busy); accountGameplay(); if (::navigation.isInitialized) update() } }
    private var timingEligible = false
    private fun accountGameplay() {
        if (practice) return
        val ready = ::navigation.isInitialized
        val eligible = ActiveGameplayConditions(resumed, focused,
            ready && navigation.state.screen == GardenScreen.GAME,
            ready && game.status == Status.PLAYING, online, GameStore.transitioning || !ready,
            AdExposure.busy || AdvertisingService.provider.state == RewardedAdManager.State.SHOWING,
            RewardedAdManager.consentInProgress, rewardDialog?.isShowing == true || settingsDialog?.isShowing == true).eligible
        AdExposure.policy.active(SystemClock.elapsedRealtime(), eligible)
        if (eligible != timingEligible) { timingEligible = eligible; saveExposure() }
    }
    private fun completedByPlayer() {
        val previous = progress.tutorial.step
        progress.tutorial.collected()
        if (previous == TutorialStep.COMPLETE_PAIR && progress.tutorial.step == TutorialStep.FINISHED) completionVisible = true
        update() // Persists actual victory and stops active time before considering an ad.
        if (practice || game.status != Status.WON || !GameStore.persistVictory(applicationContext)) return
        GameStore.prepareVictory()
        val level = session.level.id
        val eligible = AdExposure.policy.victory(level, true, SystemClock.elapsedRealtime())
        val saved = saveExposure() // Consume before asynchronous presentation.
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
    private var connectionStatus = com.tilebloom.game.domain.ConnectivityStatus.CHECKING
    private var online = false
    private var showConnection = false
    private lateinit var connectivity: NetworkObserver
    private lateinit var offlineHome: LinearLayout
    private lateinit var offlineScroll: ScrollView
    private var lastRetry = -1000L
    private lateinit var connection: LinearLayout
    private lateinit var connectionShade: FrameLayout
    private lateinit var privacy: Button
    private fun connectivityChanged(status: com.tilebloom.game.domain.ConnectivityStatus) {
        connectionStatus = status
        val available = status == com.tilebloom.game.domain.ConnectivityStatus.ONLINE
        online = available; showConnection = status == com.tilebloom.game.domain.ConnectivityStatus.OFFLINE
        if (!available) {
            pendingVictory = null
            hintRequest++; hintPending = false
            rewardDialog?.dismiss()
            if (::board.isInitialized) { board.clearFeedback(); tray.clearFeedback() }
        }
        update()
    }
    private var backCallback: android.window.OnBackInvokedCallback? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.attach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        audio = com.tilebloom.game.audio.GameAudioManager(applicationContext)
        if (!practice) AdExposure.initialize(applicationContext)
        if (!practice) AdExposure.changed = exposureChanged
        // Decode only the bounded save envelope; generation/restoration stays off the UI thread.
        pendingPlay = savedInstanceState?.getBoolean("pending_play") ?: false
        practiceInFront = savedInstanceState?.getBoolean("practice_in_front") ?: false
        val metadata = if (practice) PlayerProgress() else ProgressRepository.load(applicationContext)
        previewLevel = metadata.highestUnlocked
        createContent()
        connectivity = ConnectivityService.factory(this, ::connectivityChanged)
        if (!practice) AdvertisingService.provider.attach(this)
        Thread {
            val loaded = runCatching {
                if (practice) ProgressController(savedInstanceState?.getString("practice_state")?.let { ProgressCodec.decode(it) } ?: PlayerProgress()) to false
                else GameStore.enter(applicationContext, savedInstanceState == null, ConnectivityMonitor.validated(applicationContext))
            }
            runOnUiThread {
                if (!isDestroyed) loaded.onSuccess {
                    progress = it.first
                    completionVisible = (savedInstanceState?.getBoolean("tutorial_completion") ?: false) || (practice && progress.tutorial.step == TutorialStep.FINISHED)
                    if (progress.recovered) android.util.Log.w("TileBloomSave", "Board snapshot rejected; completion retained")
                    val restored = savedInstanceState?.takeUnless { _ -> it.second }?.let { saved ->
                        val screen = runCatching { GardenScreen.valueOf(saved.getString("garden_screen").orEmpty()) }.getOrNull()
                        screen?.let { NavigationState(saved.getLong("garden_level"), it) }
                    }
                    navigation = LevelNavigation(session.level.id, restored)
                    if (practice || pendingPlay && online) navigation.play(session.level.id)
                    pendingPlay = false
                    update()
                }.onFailure {
                    pendingPlay = false
                    levelCard.isEnabled = online
                    startHint.setText(R.string.load_retry)
                    levelCard.setOnClickListener {
                        if (online && levelCard.isEnabled) {
                            levelCard.isEnabled = false
                            recreate()
                        }
                    }
                }
            }
        }.start()
    }

    private fun createContent() {
        root = FrameLayout(this).apply {
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(HomeTheme.palette.backgroundTop, HomeTheme.palette.backgroundBottom))
        }
        applyBackground(HomeTheme.palette)
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
        levelCard.isEnabled = online && !pendingPlay
        if (!practice) GameStore.onChanged = refreshListener
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
            setPadding(dp(24), dp(72), dp(24), dp(28))
        }
        fun add(view: View, top: Int = 0, height: Int = -2) {
            content.addView(view, LinearLayout.LayoutParams(-1, height).apply { topMargin = dp(top) })
        }
        add(textLabel(42f).apply {
            setText(R.string.app_name); typeface = Typeface.create("serif", Typeface.NORMAL)
            letterSpacing = .03f; maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(24, 42, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        })
        add(textLabel(16f, HomeTheme.palette.textSecondary).apply { setText(R.string.subtitle); letterSpacing = .05f }, 6)
        add(GardenWelcomeArt(this).apply { id = R.id.home_art }, 10, dp(if (compact) 176 else 220))
        levelCard = ContinueLevelCard(this).apply { id = R.id.continue_level }
        startLevel = levelCard.levelLabel.apply { id = R.id.start_level }
        startHint = levelCard.continuationLabel
        add(levelCard, 8)
        levelCard.apply {
            setOnClickListener {
                if (!online || pendingPlay || GameStore.transitioning) return@setOnClickListener
                audio.play(AudioCue.BUTTON)
                if (!::navigation.isInitialized) {
                    pendingPlay = true
                    levelCard.isEnabled = false
                    startHint.setText(R.string.preparing_garden)
                } else if (!GameStore.transitioning && navigation.state.screen == GardenScreen.START) {
                    navigation.play(session.level.id)
                    update()
                }
            }
        }
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
        // Cap the welcoming composition on wider devices; scroll protects large-font navigation.
        val frame = FrameLayout(this)
        frame.addView(content, FrameLayout.LayoutParams(if (resources.configuration.screenWidthDp > 480) dp(420) else -1, -2, Gravity.CENTER))
        startScreen.addView(frame, FrameLayout.LayoutParams(-1, -2))
        root.addView(startScreen, FrameLayout.LayoutParams(-1, -1))
        homeSettings = settingsControls()
        root.addView(homeSettings, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.END).apply { topMargin = dp(12); marginEnd = dp(16) })
    }

    private fun createConnectionPanel() {
        connectionShade = FrameLayout(this).apply {
            id = R.id.connection_overlay
            setBackgroundColor((GardenStyle.theme.palette.tileShadow and 0x00ffffff) or (165 shl 24))
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
        connection.addView(settingsControls(), LinearLayout.LayoutParams(-1, dp(48)))
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
        val header = GameplayHeader(this, settingsControls())
        levelLabel = header.levelLabel
        column.addView(header, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })
        tray = TileTrayView(this) { game.tray }.apply { id = R.id.tray }
        column.addView(tray, LinearLayout.LayoutParams(-1, dp(if (compact) 76 else 84)))
        board = MahjongBoardView(this, game) { completedByPlayer() }.apply { id = R.id.board; audioCue = { audio.play(it) }; allowTile = { progress.tutorial.allowsTile(it) } }
        val area = FrameLayout(this)
        column.addView(area, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(4) })
        area.addView(board, FrameLayout.LayoutParams(-1, -1))
        createOutcome(area)
        feedback = textLabel(14f, GardenStyle.secondary).apply {
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
                if (practice) { finish(); return@setOnClickListener }
                if (progress.tutorial.step.active) return@setOnClickListener
                if (online && game.status != Status.WON && !GameStore.transitioning) {
                    hintRequest++; hintPending = false; board.clearFeedback(); tray.clearFeedback()
                    hideMysteryWithSound(); audio.play(AudioCue.BUTTON); navigation.home(session.level.id); update()
                }
            }
        }
        hint = GardenIconButton(this, R.drawable.ic_hint).apply {
            id = R.id.hint
            setOnClickListener {
                if (!online) return@setOnClickListener
                if (progress.hintCredits == 0 && !progress.tutorial.step.active) { requestReward(RewardPurpose.HINT); return@setOnClickListener }
                if (!hintPending && board.hintedTileId == null && !GameStore.transitioning && game.status == Status.PLAYING && (progress.hintCredits > 0 || progress.tutorial.step == TutorialStep.DEMONSTRATE_HINT)) {
                    hintPending = true
                    val request = ++hintRequest
                    val expected = game.snapshot()
                    val guidedHint = progress.tutorial.step == TutorialStep.DEMONSTRATE_HINT
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
                                    if (id != null && (!guidedHint || progress.tutorial.step == TutorialStep.DEMONSTRATE_HINT) && progress.acceptHint(id, expected)) {
                                        audio.play(AudioCue.HINT)
                                        board.showHint(id)
                                        update()
                                        Toast.makeText(this@MainActivity, if (result.kind == ActionKind.REVEAL) R.string.hint_reveal else R.string.hint_collect, Toast.LENGTH_SHORT).show()
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
                if (progress.wallet.balance == 0 && !progress.tutorial.step.active) { requestReward(RewardPurpose.UNDO); return@setOnClickListener }
                if (!GameStore.transitioning && audio.perform(AudioCue.UNDO) { progress.undo() }) {
                    hintRequest++; hintPending = false; board.clearFeedback(); tray.clearFeedback(); update()
                }
            }
        }
        restart = GardenIconButton(this, R.drawable.ic_restart).apply {
            id = R.id.restart; contentDescription = getString(R.string.restart_description); tooltipText = contentDescription
            setOnClickListener {
                if (!progress.tutorial.step.active && online && !GameStore.transitioning && audio.perform(AudioCue.RESTART) { session.restart() }) {
                    hintRequest++; hintPending = false; board.clearFeedback(); tray.clearFeedback(); update()
                }
            }
        }
        listOf(home, hint, undo, restart).forEach { button ->
            controls.addView(button, LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = dp(1); marginEnd = dp(1)
            })
        }
        column.addView(controls, LinearLayout.LayoutParams(
            if (resources.configuration.screenWidthDp > 400) dp(368) else -1, -2))
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
        next = ContinueLevelCard(this).apply {
            id = R.id.next_level
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
        if (practice) { finish(); return }
        if (!online || AdExposure.busy || pendingVictory != null || GameStore.transitioning || !session.canAdvance) return
        audio.play(AudioCue.BUTTON)
        GameStore.advance(applicationContext)
        update()
        root.postDelayed(loadingRefresh, 250)
    }

    private var loadingScreen: LevelLoadingScreen? = null
    private val loadingRefresh = object : Runnable {
        override fun run() {
            if (!isDestroyed && resumed) {
                updateLoading()
                if (GameStore.transitioning) root.postDelayed(this, 100)
            }
        }
    }
    private fun updateLoading() {
        if (practice) return
        if (!::navigation.isInitialized) return
        val target = session.level.id + if (session.canAdvance) 1 else 0
        val error = GameStore.transitionError != null
        val visible = online && !showConnection && navigation.state.screen == GardenScreen.GAME && !AdExposure.busy &&
            (error || LevelLoadingPolicy.visible(GameStore.transitionStartedAt, System.nanoTime()/1_000_000, GameStore.transitioning))
        if (visible) {
            val screen = loadingScreen ?: LevelLoadingScreen(this) {
                if (online && !AdExposure.busy) {
                    GameStore.advance(applicationContext, retry = true); update(); root.postDelayed(loadingRefresh, 250)
                }
            }.also { loadingScreen = it; root.addView(it, FrameLayout.LayoutParams(-1,-1)) }
            if (screen.visibility != View.VISIBLE || error) screen.present(target, error, resumed)
            screen.visibility = View.VISIBLE; screen.bringToFront()
        } else { loadingScreen?.visibility = View.GONE; loadingScreen?.stopAnimation() }
    }

    fun refreshPrivacyOptions() {
        privacy.visibility = if (showConnection && AdvertisingService.provider.privacyRequired) View.VISIBLE else View.GONE
        privacy.isEnabled = AdvertisingService.provider.privacyRequired
    }

    private fun update() {
        updateLoading()
        accountGameplay()
        if (!practice && online && resumed) {
            if (AdvertisingService.provider === RewardedAdManager) RewardedAdManager.load()
            InterstitialService.provider.prepare(this)
        }
        levelCard.isEnabled = online && !pendingPlay && !GameStore.transitioning
        privacy.visibility = if (showConnection && AdvertisingService.provider.privacyRequired) View.VISIBLE else View.GONE
        privacy.isEnabled = AdvertisingService.provider.privacyRequired
        levelCard.visibility = if (showConnection) View.GONE else View.VISIBLE
        levelCard.contentDescription = getString(R.string.continue_level_description, if (::navigation.isInitialized) session.level.id else previewLevel)
        val showingPuzzle = ::navigation.isInitialized && navigation.state.screen == GardenScreen.GAME
        connectionShade.visibility = if (showConnection && showingPuzzle) View.VISIBLE else View.GONE
        // A home route never exposes level/art/card until validated connectivity arrives.
        val offlineAtHome = !online && !showingPuzzle
        offlineScroll.visibility = if (offlineAtHome) View.VISIBLE else View.GONE
        startScreen.visibility = if (online && !showingPuzzle) View.VISIBLE else View.GONE
        homeSettings.visibility = startScreen.visibility
        connection.visibility = if (showConnection) View.VISIBLE else View.GONE
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
        if (privacy.parent == null) {
            offlineHome.addView(privacy, LinearLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(12)
            })
        }
        privacy.background = null
        privacy.setTextColor(GardenStyle.gold)
        connectionShade.bringToFront()

        if (!::navigation.isInitialized) {
            startLevel.text = getString(R.string.start_level, previewLevel)
            startHint.setText(if (pendingPlay) R.string.preparing_garden else R.string.continue_garden)
            return
        }
        navigation.synchronize(session.level.id)
        persistGame()
        val showingGame = navigation.state.screen == GardenScreen.GAME
        updateBackCallback(showingGame)
        startLevel.text = getString(R.string.start_level, session.level.id)
        startHint.setText(R.string.continue_garden)
        levelCard.isEnabled = online && !pendingPlay && !GameStore.transitioning
        startScreen.visibility = if (showingGame || !online) View.GONE else View.VISIBLE
        if (showingGame) createGameplay()
        gameplay?.visibility = if (showingGame && !showConnection) View.VISIBLE else View.GONE
        gameplay?.importantForAccessibility = if (showConnection) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        if (!showingGame) {
            tutorialOverlay?.let { root.removeView(it) }; tutorialOverlay = null
            applyBackground(HomeTheme.palette)
            return
        }
        applyGameplayTheme(GardenTheme.forLevel(session.level.id))
        board.theme = GardenTheme.forLevel(session.level.id)
        tray.theme = board.theme
        levelLabel.text = getString(R.string.level_number, session.level.id)
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
        next.present(progress.highestUnlocked)
        feedback.visibility = if (terminal) View.GONE else View.VISIBLE
        feedback.setText(if (game.status == Status.LOST) R.string.defeat_hint else R.string.instruction)
        val lost = game.status == Status.LOST
        // Move the same controls: one action, one listener, one wallet source.
        val destination = if (lost) defeatActions else controls
        listOf(undo, restart).forEach { button ->
            if (button.parent !== destination) {
                (button.parent as? android.view.ViewGroup)?.removeView(button)
                val params = if (destination === controls) LinearLayout.LayoutParams(0, -2, 1f).apply {
                    marginStart = dp(1); marginEnd = dp(1)
                } else LinearLayout.LayoutParams(dp(72), -2).apply {
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
        progress.tutorial.begin(progress.capture().frontier)
        persistGame()
        presentTutorial()
        connectionShade.bringToFront()
    }

    private fun requestReward(purpose: RewardPurpose) {
        if (practice || progress.tutorial.step.active) return
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
        val availability = textLabel(15f, GardenStyle.secondary).apply {
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
                dialog.setOnDismissListener { accountGameplay(); saveExposure() }
                dialog.show()
                accountGameplay(); saveExposure()
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

    private fun hideMysteryWithSound() {
        val revealed = session.game.activeMysteryId != null
        session.game.hideMystery()
        if (revealed) audio.play(AudioCue.HIDE)
    }

    private fun returnHomeFromBack() {
        if (practice) { finish(); return }
        if (::progress.isInitialized && progress.tutorial.step.active) {
            hintRequest++; hintPending = false
            progress.tutorial.skip(); completionVisible = false; update(); return
        }
        hideMysteryWithSound()
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
        outState.putBoolean("tutorial_completion", completionVisible)
        outState.putBoolean("practice_in_front", practiceInFront)
        if (practice && ::progress.isInitialized) outState.putString("practice_state", ProgressCodec.encode(progress.capture()))
        outState.putBoolean("pending_play", pendingPlay && !::navigation.isInitialized)
        if (::navigation.isInitialized) {
            outState.putString("garden_screen", navigation.state.screen.name)
            outState.putLong("garden_level", navigation.state.levelId)
        }
    }
    override fun onResume() {
        super.onResume(); practiceInFront = false; resumed = true; audio.advertisement(AdExposure.busy); audio.foreground(focused); update(); loadingScreen?.let { it.visibility = View.GONE }; loadingRefresh.run()
    }
    override fun onPause() {
        resumed = false; audio.foreground(false); pendingVictory = null
        loadingScreen?.stopAnimation(); root.removeCallbacks(loadingRefresh)
        accountGameplay(); saveExposure()
        super.onPause()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        focused = hasFocus
        if (::audio.isInitialized) audio.foreground(resumed && hasFocus)
        if (!hasFocus) pendingVictory = null
        accountGameplay(); saveExposure()
    }
    override fun onStart() {
        super.onStart()
        connectivity.start()
    }
    override fun onStop() {
        online = false
        connectionStatus = com.tilebloom.game.domain.ConnectivityStatus.CHECKING
        showConnection = false
        if (::board.isInitialized) {
            board.isEnabled = false
            if (!isChangingConfigurations && !practiceInFront) game.hideMystery()
            persistGame()
        }
        connectivity.stop()
        super.onStop()
        ProgressRepository.awaitWrites()
    }
    override fun onDestroy() {
        settingsDialog?.setOnDismissListener(null)
        settingsDialog?.dismiss()
        audio.close()
        loadingScreen?.stopAnimation(); root.removeCallbacks(loadingRefresh)
        if (AdExposure.changed === exposureChanged) AdExposure.changed = null
        connectivity.destroy()
        rewardDialog?.setOnDismissListener(null)
        rewardDialog?.dismiss()
        if (!practice) AdvertisingService.provider.detach(this)
        hintRequest++
        if (::board.isInitialized) board.clearFeedback()
        updateBackCallback(false)
        if (GameStore.onChanged === refreshListener) GameStore.onChanged = null
        super.onDestroy()
    }
    private var appliedTheme: GardenTheme? = null
    private fun applyGameplayTheme(theme: GardenTheme) {
        if (appliedTheme == theme) { applyBackground(theme.palette); return }
        appliedTheme = theme
        GardenStyle.theme = theme
        val palette = theme.palette
        applyBackground(palette)
        connectionShade.setBackgroundColor((palette.tileShadow and 0x00ffffff) or (165 shl 24))
        fun tint(view: View) {
            if (view is ContinueLevelCard) return
            when (view) {
                is GardenIconButton -> view.invalidate()
                is Button -> GardenStyle.button(view)
                is TextView -> view.setTextColor(palette.textPrimary)
            }
            if (view is android.view.ViewGroup) {
                if (view.background != null) view.background = GardenStyle.surface(this,
                    if (view === controls) palette.buttonSurface else palette.headerSurface, border = false)
                for (i in 0 until view.childCount) tint(view.getChildAt(i))
            }
        }
        gameplay?.let { tint(it) }
        levelLabel.setTextColor(palette.primaryAccent)
        feedback.setTextColor(palette.textSecondary)
    }

    private fun applyBackground(palette: GardenPalette) {
        root.background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(palette.backgroundTop, palette.backgroundBottom))
        window.statusBarColor = palette.backgroundTop
        window.navigationBarColor = palette.backgroundBottom
    }

    private fun presentTutorial() {
        tutorialOverlay?.let { root.removeView(it) }; tutorialOverlay = null
        val step = progress.tutorial.step
        if (!step.active && !completionVisible) return
        val title: Int
        val detail: Int
        var action: Int? = null
        var target: View? = null
        var bounds: (() -> android.graphics.RectF)? = null
        when {
            completionVisible -> { title = R.string.tutorial_complete; detail = R.string.tutorial_ready; action = R.string.tutorial_continue }
            step == TutorialStep.INTRODUCTION -> { title = R.string.tutorial_welcome; detail = R.string.tutorial_intro; action = R.string.tutorial_start }
            step == TutorialStep.EXPLAIN_TRAY -> { title = R.string.tutorial_tray; detail = R.string.tutorial_tray_detail; target = tray; action = R.string.tutorial_continue }
            step == TutorialStep.DEMONSTRATE_UNDO -> { title = R.string.tutorial_undo; detail = R.string.tutorial_undo_detail; target = undo }
            step == TutorialStep.DEMONSTRATE_HINT -> { title = R.string.tutorial_hint; detail = R.string.tutorial_hint_detail; target = hint }
            else -> {
                title = if (step == TutorialStep.SELECT_TILE) R.string.tutorial_select else R.string.tutorial_pair
                detail = if (step == TutorialStep.SELECT_TILE) R.string.tutorial_select_detail else R.string.tutorial_pair_detail
                val id = progress.tutorial.target
                if (id == null) { progress.tutorial.skip(); persistGame(); return }
                target = board; bounds = { board.tutorialBounds(id) }
            }
        }
        tutorialOverlay = TutorialOverlay(this, target, bounds, title, detail, action, if (practice) R.string.tutorial_exit else R.string.tutorial_skip, {
            if (completionVisible) {
                completionVisible = false
                if (practice) { finish(); return@TutorialOverlay }
            } else progress.tutorial.continueGuide()
            update()
        }, {
            hintRequest++; hintPending = false
            progress.tutorial.skip(); completionVisible = false
            if (practice) finish() else update()
        }).also { root.addView(it, FrameLayout.LayoutParams(-1, -1)) }
    }

    private fun showSettings(anchor: View) {
        if (::progress.isInitialized && progress.tutorial.step.active) return
        if (isFinishing || settingsDialog?.isShowing == true || AdExposure.busy) return
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(8))
        }
        content.addView(Button(this).apply {
            id = R.id.how_to_play
            setText(R.string.how_to_play)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hint, 0, 0, 0)
            GardenStyle.button(this, false)
            minimumHeight = dp(56)
            setOnClickListener {
                settingsDialog?.dismiss()
                practiceInFront = true
                startActivity(android.content.Intent(this@MainActivity, MainActivity::class.java).putExtra("tutorial_practice", true))
            }
        }, LinearLayout.LayoutParams(-1, -2))
        val sound = android.widget.Switch(this).apply {
            id = R.id.settings_sound
            setText(R.string.sound_effects)
            setTextColor(GardenStyle.ivory)
            textSize = 17f
            minimumHeight = dp(56)
            switchPadding = dp(12)
            switchMinWidth = dp(52)
            thumbTintList = android.content.res.ColorStateList.valueOf(GardenStyle.ivory)
            trackTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(0xFF64895F.toInt(), 0xFF53645C.toInt()))
            showText = false
            textOn = getString(R.string.sound_on)
            textOff = getString(R.string.sound_off)
            isSoundEffectsEnabled = false
            isChecked = !audio.policy.preferences.muted
            if (Build.VERSION.SDK_INT >= 30) stateDescription = getString(if (isChecked) R.string.sound_on else R.string.sound_off)
            setOnCheckedChangeListener { _, enabled ->
                audio.preferences(audio.policy.preferences.copy(muted = !enabled))
                if (Build.VERSION.SDK_INT >= 30) stateDescription = getString(if (enabled) R.string.sound_on else R.string.sound_off)
            }
        }
        content.addView(sound, LinearLayout.LayoutParams(-1, -2))
        content.addView(languageRow(AppLanguage.selected(this), false).apply {
            id = R.id.settings_language
            contentDescription = getString(R.string.language_current, getString(R.string.language), contentDescription)
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33D9C58A),
                GardenStyle.surface(this@MainActivity, GardenStyle.panel, 12),
                GardenStyle.surface(this@MainActivity, android.graphics.Color.WHITE, 12, false))
            val labels = getChildAt(1) as LinearLayout
            labels.addView(TextView(this@MainActivity).apply {
                setText(R.string.language); textSize = 16f; setTextColor(GardenStyle.secondary)
            }, 0)
            setOnClickListener { dialogLanguage(anchor) }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        content.addView(textLabel(16f, GardenStyle.secondary).apply {
            setText(R.string.settings_privacy_heading)
            setPadding(0, dp(16), 0, dp(4))
        })
        val privacyAction = Button(this).apply {
            id = R.id.settings_privacy
            setText(R.string.settings_privacy)
            GardenStyle.button(this, false)
            minimumHeight = dp(48)
            setOnClickListener {
                if (AdvertisingService.provider.privacyRequired) {
                    AdvertisingService.provider.privacyOptions(this@MainActivity)
                } else Toast.makeText(this@MainActivity, R.string.privacy_not_required, Toast.LENGTH_SHORT).show()
            }
        }
        content.addView(privacyAction, LinearLayout.LayoutParams(-1, -2))
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton(R.string.settings_close, null)
            .create()
        settingsDialog = dialog
        dialog.setOnDismissListener {
            settingsDialog = null
            accountGameplay()
            if (anchor.isShown) anchor.requestFocus()
        }
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        styleSettingsWindow(dialog)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(GardenStyle.gold); textSize = 16f; minimumHeight = dp(48)
        }
        accountGameplay()
    }

    private fun dialogLanguage(anchor: View) {
        settingsDialog?.dismiss()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val current = AppLanguage.selected(this)
        val tags = listOf("") + AppLanguage.tags
        val dialog = AlertDialog.Builder(this).setTitle(R.string.language)
            .setView(ScrollView(this).apply { addView(panel) })
            .setPositiveButton(R.string.settings_close, null).create()
        tags.forEach { tag ->
            panel.addView(languageRow(tag, tag == current).apply {
                this.tag = "language:$tag"
                setOnClickListener {
                    if (tag != current) {
                        if (::navigation.isInitialized) persistGame()
                        AppLanguage.select(this@MainActivity, tag)
                        if (Build.VERSION.SDK_INT < 33) recreate()
                    }
                    dialog.dismiss()
                }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        settingsDialog = dialog
        dialog.setOnDismissListener { settingsDialog = null; accountGameplay(); if (anchor.isShown) anchor.requestFocus() }
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        styleSettingsWindow(dialog)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(GardenStyle.gold); textSize = 16f; minimumHeight = dp(48)
        }
        accountGameplay()
    }

    private fun styleSettingsWindow(dialog: AlertDialog) {
        dialog.window?.apply {
            // A floating, touch-modal window keeps the Activity's original rendering intact.
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(.28f)
            setBackgroundDrawable(GardenStyle.surface(this@MainActivity, GardenStyle.panel, 24))
            setLayout(minOf(resources.displayMetrics.widthPixels - dp(24), dp(400)), -2)
        }
    }

    private fun languageRow(tag: String, selected: Boolean): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(56)
        setPaddingRelative(dp(12), dp(8), dp(12), dp(8))
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x33D9C58A),
            GardenStyle.surface(this@MainActivity, GardenStyle.panel, 12, false),
            GardenStyle.surface(this@MainActivity, android.graphics.Color.WHITE, 12, false))
        isClickable = true; isFocusable = true
        val index = AppLanguage.tags.indexOf(tag)
        val name = if (index < 0) getString(R.string.language_automatic) else AppLanguage.nativeNames[index]
        addView(android.widget.ImageView(this@MainActivity).apply {
            setImageResource(LanguageFlags.drawable(tag))
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }, LinearLayout.LayoutParams(dp(28), dp(20)).apply { marginEnd = dp(12) })
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = name; textSize = 17f; setTextColor(GardenStyle.ivory)
                // Native names resolve their own bidi direction within the surrounding row.
                textDirection = View.TEXT_DIRECTION_FIRST_STRONG
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(-1, -2))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(this@MainActivity).apply {
            text = if (selected) "✓" else ""; textSize = 20f
            setTextColor(GardenStyle.gold); gravity = Gravity.CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(28), dp(32)))
        isSelected = selected
        contentDescription = name
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: android.view.accessibility.AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isCheckable = true; info.isChecked = selected
            }
        }
    }

    private fun settingsControls(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        val settings = ImageButton(this@MainActivity).apply {
            id = R.id.settings
            setImageResource(R.drawable.ic_settings)
            contentDescription = getString(R.string.settings_label)
            tooltipText = contentDescription
            imageTintList = android.content.res.ColorStateList.valueOf(GardenStyle.gold)
            background = null
            minimumWidth = dp(48); minimumHeight = dp(48)
            // A gear has no directional meaning; only its parent placement follows RTL.
            setPadding(dp(10), dp(10), dp(10), dp(10))
            imageTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf()),
                intArrayOf(GardenStyle.ivory, GardenStyle.gold))
            setOnClickListener { showSettings(this) }
        }
        addView(settings, LinearLayout.LayoutParams(dp(48), dp(48)))
    }
    private fun dp(value: Int) = GardenStyle.dp(this, value)
}
