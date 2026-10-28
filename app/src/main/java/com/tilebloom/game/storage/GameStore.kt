package com.tilebloom.game.storage

import android.content.Context
import com.tilebloom.game.domain.ProgressController

/** Process-owned state: the wallet and board survive activity replacement together. */
object GameStore {
    private var controller: ProgressController? = null
    @Volatile var transitioning = false
    var onChanged: (() -> Unit)? = null
    fun persistTransition(context: Context, state: com.tilebloom.game.domain.PlayerProgress): Boolean =
        ProgressRepository.saveDurably(context.applicationContext, owner, state)
    private var owner = 0L
    internal fun resetForTestStorage() { controller = null }
    @Synchronized fun get(context: Context): ProgressController {
        return controller ?: ProgressController(ProgressRepository.load(context)).also {
            owner = ProgressRepository.acquire(); controller = it
        }
    }
    /** Returns the controller and whether saved Activity navigation must be discarded.
     * A live process plus saved GAME is recreation; a cold process is application entry.
     */
    @Synchronized fun enter(context: Context, freshEntry: Boolean): Pair<ProgressController, Boolean> {
        val coldEntry = controller == null
        val state = get(context)
        val previous = state.session.level.id
        if (freshEntry || coldEntry) check(state.finalizeCompletedEntry { persistTransition(context, it) }) {
            "Completed level transition was not saved"
        }
        return state to (coldEntry || state.session.level.id != previous)
    }
    fun persist(context: Context) {
        if (transitioning) return
        val state = get(context).capture()
        if (get(context).session.game.status == com.tilebloom.game.domain.Status.WON) {
            ProgressRepository.saveDurably(context.applicationContext, owner, state)
        } else ProgressRepository.save(context.applicationContext, owner, state)
    }
}
