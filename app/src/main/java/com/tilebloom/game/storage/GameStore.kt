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
    fun persist(context: Context) {
        if (transitioning) return
        ProgressRepository.save(context.applicationContext, owner, get(context).capture())
    }
}
