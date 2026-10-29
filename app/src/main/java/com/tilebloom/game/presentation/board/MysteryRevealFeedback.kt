package com.tilebloom.game.presentation.board

/** Visual clock and deliberate-tap boundary only; Game owns the durable reveal. */
class MysteryRevealFeedback {
    companion object { const val DURATION_MS = 260L; const val TAP_GUARD_MS = 420L }
    private val starts = mutableMapOf<Int, Long>()
    fun revealed(id: Int, now: Long) { starts[id] = now }
    fun accepts(id: Int, downTime: Long) = starts[id]?.let { downTime - it >= TAP_GUARD_MS } ?: true
    fun progress(id: Int, now: Long, animations: Boolean): Float? = starts[id]?.let {
        if (!animations || now - it >= DURATION_MS) null else ((now - it).toFloat() / DURATION_MS).coerceIn(0f, 1f)
    }
    fun active(now: Long, animations: Boolean) = animations && starts.values.any { now - it < DURATION_MS }
    fun clear() { starts.clear() }
}
