package com.tilebloom.game

import com.tilebloom.game.domain.Game

/** Existing collection regressions now perform the required production reveal first. */
internal fun Game.collectRevealed(id: Int): Boolean {
    if (remaining.any { it.id == id && it.concealed } && !reveal(id)) return false
    return select(id)
}
