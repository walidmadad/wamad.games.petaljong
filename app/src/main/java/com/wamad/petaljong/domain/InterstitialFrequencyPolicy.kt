package com.wamad.petaljong.domain

/** Monotonic interval accounting. Persist snapshots, never monotonic timestamps. */
class InterstitialFrequencyPolicy(duration: Long = 0, lastVictory: Long = 0) {
    companion object { const val MIN_ACTIVE_GAMEPLAY_MS = 180_000L }
    private var accumulated = duration.takeIf { it in 0..MIN_ACTIVE_GAMEPLAY_MS } ?: 0
    var lastVictory = lastVictory.coerceAtLeast(0); private set
    private var started: Long? = null
    fun active(now: Long, eligible: Boolean) {
        checkpoint(now)
        started = if (eligible) now else null
    }
    private fun checkpoint(now: Long) {
        started?.let { accumulated = (accumulated + (now - it).coerceIn(0, MIN_ACTIVE_GAMEPLAY_MS)).coerceAtMost(MIN_ACTIVE_GAMEPLAY_MS) }
        if (started != null) started = now
    }
    fun duration(now: Long): Long { checkpoint(now); return accumulated }
    fun victory(level: Long, won: Boolean, now: Long): Boolean {
        if (!won || level <= lastVictory) return false
        lastVictory = level
        return duration(now) >= MIN_ACTIVE_GAMEPLAY_MS
    }
    fun displayed() { accumulated = 0; started = null }
}

/** Inputs are sampled at lifecycle/navigation boundaries, never per rendering frame. */
data class ActiveGameplayConditions(
    val foreground: Boolean, val focused: Boolean, val gameScreen: Boolean,
    val playing: Boolean, val online: Boolean, val loading: Boolean,
    val fullScreen: Boolean, val consent: Boolean, val dialog: Boolean
) {
    val eligible get() = foreground && focused && gameScreen && playing && online &&
        !loading && !fullScreen && !consent && !dialog
}
