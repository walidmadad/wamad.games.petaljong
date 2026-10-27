package com.tilebloom.game.presentation.board

import com.tilebloom.game.domain.Tile

/** Short-lived visual copies only. No engine references, delayed actions or saved state. */
class MatchEffects {
    data class Ghost(val tile: Tile, val startedAt: Long)
    private val ghosts = mutableListOf<Ghost>()
    val active: List<Ghost> get() = ghosts

    fun record(before: List<Tile>, after: List<Tile>, now: Long, enabled: Boolean) {
        if (!enabled) { clear(); return }
        val removed = after.filter { it.removed }.map { it.id }.toSet()
        before.filter { !it.removed && it.id in removed }.forEach {
            ghosts.add(Ghost(it, now))
        }
    }
    fun expire(now: Long) { ghosts.removeAll { progress(it, now) >= 1f } }
    fun progress(ghost: Ghost, now: Long) = ((now - ghost.startedAt).toFloat() / DURATION_MS).coerceIn(0f, 1f)
    fun clear() { ghosts.clear() }

    companion object { const val DURATION_MS = 150L }
}
