package com.tilebloom.game.storage

import android.content.Context
import com.tilebloom.game.domain.ProgressController

/** Process-owned state: the wallet and board survive activity replacement together. */
object GameStore {
    private var controller: ProgressController? = null
    private var owner = 0L
    internal fun resetForTestStorage() { controller = null }
    fun get(context: Context): ProgressController {
        return controller ?: ProgressController(ProgressRepository.load(context)).also {
            owner = ProgressRepository.acquire(); controller = it
        }
    }
    fun persist(context: Context) {
        ProgressRepository.save(context.applicationContext, owner, get(context).capture())
    }
}
