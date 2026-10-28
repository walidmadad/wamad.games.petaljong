package com.tilebloom.game.presentation.board

import com.tilebloom.game.domain.Game
import com.tilebloom.game.domain.Status
import kotlin.math.cos
import kotlin.math.PI

/** Ephemeral, single-target feedback. Never saved and never owns wallet or selection state. */
class HintPresentation {
    companion object { const val DURATION_MS = 4000L }
    var targetId: Int? = null; private set
    private var started = 0L
    fun show(id: Int, game: Game, now: Long): Boolean {
        if (targetId != null || game.status != Status.PLAYING || !game.isAvailable(id)) return false
        targetId = id
        started = now
        return true
    }
    fun clear() { targetId = null }
    fun validate(game: Game, now: Long) {
        val id = targetId ?: return
        if (now - started >= DURATION_MS || game.status != Status.PLAYING || !game.isAvailable(id)) clear()
    }
    fun brightness(now: Long, animations: Boolean): Float =
        if (!animations) 1f else (.5 - .5 * cos((now - started) * 2 * PI / 1400.0)).toFloat()
    fun face(game: Game, layout: BoardGeometry): BoardRect? = targetId?.let { id ->
        game.remaining.find { it.id == id && game.isAvailable(id) }?.let(layout::face)
    }
    fun indicator(face: BoardRect, width: Float, height: Float, radius: Float,
                  game: Game? = null, layout: BoardGeometry? = null): BoardPoint {
        var best = BoardPoint(radius, radius)
        var bestScore = Int.MAX_VALUE
        // Prefer above a corner; try other corners when a neighboring symbol is nearby.
        repeat(4) { corner ->
            val x = if (corner % 2 == 0) face.right else face.left
            val y = if (corner < 2) face.top - radius * 1.25f else face.bottom + radius * 1.25f
            val candidate = BoardPoint(x.coerceIn(radius, (width - radius).coerceAtLeast(radius)),
                y.coerceIn(radius, (height - radius).coerceAtLeast(radius)))
            var score = 0
            if (game != null && layout != null) for (tile in game.remaining) {
                val center = layout.footprint(tile).center
                // Reserve the full botanical drawing envelope, including antialiasing.
                if (kotlin.math.abs(center.x - candidate.x) < layout.scale * .42f + radius &&
                    kotlin.math.abs(center.y - candidate.y) < layout.scale * .42f + radius) score++
            }
            if (score < bestScore) { best = candidate; bestScore = score }
        }
        return best
    }
}
