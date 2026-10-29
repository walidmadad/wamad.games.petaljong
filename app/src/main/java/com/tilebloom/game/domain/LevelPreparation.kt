package com.tilebloom.game.domain

/** One process-owned task and one reusable result. Executor and clock are injectable. */
class LevelPreparation(private val execute: (() -> Unit) -> Unit,
    private val generate: (Long) -> LevelDefinition = Levels::get,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 }) {
    data class State(val target: Long, val started: Long, val result: Result<LevelDefinition>? = null, val timings: Map<String, Long> = emptyMap()) {
        fun showLoading(now: Long) = LevelLoadingPolicy.visible(started, now, result == null)
    }
    @Volatile var state: State? = null; private set
    @Synchronized fun request(target: Long, retry: Boolean = false) {
        require(target > 1)
        val old = state
        if (old?.target == target && (!retry || old.result?.isFailure != true)) return
        val pending = State(target, clock())
        state = pending
        execute {
            var timings: Map<String, Long> = emptyMap()
            val result = runCatching {
                val measured = PreparationMetrics.measure { PreparationMetrics.bounded { generate(target).also { require(it.id == target); LevelValidator.validate(it) } } }
                timings = measured.second
                measured.first
            }
            synchronized(this) { if (state === pending) state = pending.copy(result = result, timings = timings) }
        }
    }
}

/** No minimum display duration: the caller hides immediately when pending becomes false. */
object LevelLoadingPolicy {
    fun visible(started: Long, now: Long, pending: Boolean) = pending && now - started >= 250
}
