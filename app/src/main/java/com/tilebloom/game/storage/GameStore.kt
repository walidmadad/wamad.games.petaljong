package com.tilebloom.game.storage

import android.content.Context
import com.tilebloom.game.domain.ProgressController

/** Process-owned state: the wallet and board survive activity replacement together. */
object GameStore {
    private val generation = java.util.concurrent.Executors.newSingleThreadExecutor()
    val preparation = com.tilebloom.game.domain.LevelPreparation({ work -> generation.execute(work) })
    var transitionError: String? = null
    var transitionStartedAt = 0L; private set
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    fun prepareVictory() {
        val state = controller ?: return
        if (state.session.canAdvance) preparation.request(state.session.level.id + 1)
    }
    fun advance(context: Context, retry: Boolean = false) {
        val state = controller ?: return
        if (transitioning || !state.session.canAdvance) return
        val expected = state.session.level.id
        val target = expected + 1
        prepareVictory()
        preparation.request(target, retry)
        transitionStartedAt = System.nanoTime() / 1_000_000
        transitioning = true; transitionError = null
        val app = context.applicationContext
        val poll = object : Runnable {
            override fun run() {
                if (controller !== state || state.session.level.id != expected) {
                    transitioning = false; onChanged?.invoke(); return
                }
                val result = preparation.state?.takeIf { it.target == target }?.result
                if (result == null) { main.postDelayed(this, 50); return }
                android.util.Log.i("TileBloomPreparation", "level=$target phasesNs=${preparation.state?.timings}")
                val level = result.getOrNull()
                if (level == null) { finish(false); return }
                val candidate = state.nextProgress(level)
                if (candidate == null) { finish(false); return }
                generation.execute {
                    val saveStart = System.nanoTime()
                    val saved = runCatching { persistTransition(app, candidate) }.getOrDefault(false)
                    android.util.Log.i("TileBloomPreparation", "level=$target persistNs=${System.nanoTime()-saveStart} saved=$saved")
                    main.post {
                        val committed = saved && controller === state && state.session.level.id == expected && state.commitNext(level)
                        finish(committed)
                    }
                }
            }
            fun finish(success: Boolean) {
                transitioning = false
                transitionError = if (success) null else "Garden not ready."
                onChanged?.invoke()
            }
        }
        main.post(poll)
    }
    private var controller: ProgressController? = null
    @Volatile var transitioning = false
    var onChanged: (() -> Unit)? = null
    fun persistTransition(context: Context, state: com.tilebloom.game.domain.PlayerProgress): Boolean =
        ProgressRepository.saveDurably(context.applicationContext, owner, state)
    private var owner = 0L
    internal fun resetForTestStorage() { controller = null; transitioning = false; transitionError = null }
    @Synchronized fun get(context: Context): ProgressController {
        return controller ?: ProgressController(ProgressRepository.load(context)).also {
            owner = ProgressRepository.acquire(); controller = it
        }
    }
    /** Returns the controller and whether saved Activity navigation must be discarded.
     * A live process plus saved GAME is recreation; a cold process is application entry.
     */
    @Synchronized fun enter(context: Context, freshEntry: Boolean, allowAdvance: Boolean = true): Pair<ProgressController, Boolean> {
        val coldEntry = controller == null
        val state = get(context)
        val previous = state.session.level.id
        if (!transitioning && allowAdvance && (freshEntry || coldEntry) &&
            preparation.state?.target != state.session.level.id + 1) check(state.finalizeCompletedEntry { persistTransition(context, it) }) {
            "Completed level transition was not saved"
        }
        return state to (coldEntry || state.session.level.id != previous)
    }
    fun reward(context: Context, purpose: com.tilebloom.game.domain.RewardPurpose): Boolean {
        check(android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
        if (transitioning) return false
        val saved = get(context).grantReward(purpose) { persistTransition(context, it) }
        if (saved) onChanged?.invoke()
        return saved
    }
    fun persistVictory(context: Context): Boolean {
        val state = get(context)
        return !transitioning && state.session.game.status == com.tilebloom.game.domain.Status.WON &&
            ProgressRepository.saveDurably(context.applicationContext, owner, state.capture())
    }
    fun persist(context: Context) {
        if (transitioning) return
        val state = get(context).capture()
        if (get(context).session.game.status == com.tilebloom.game.domain.Status.WON) {
            ProgressRepository.saveDurably(context.applicationContext, owner, state)
        } else ProgressRepository.save(context.applicationContext, owner, state)
    }
}
