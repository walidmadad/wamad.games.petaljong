package com.tilebloom.game.domain

/** Per-worker instrumentation; no global mutable collector or Android dependency. */
object PreparationMetrics {
    private val deadline = ThreadLocal<Long?>()
    fun checkBudget() { deadline.get()?.let { check(System.nanoTime() < it) { "Preparation time budget exceeded" } } }
    fun <T> bounded(work: () -> T): T {
        val previous = deadline.get()
        deadline.set(System.nanoTime() + 30_000_000_000L)
        try { return work() } finally { deadline.set(previous) }
    }
    private val collector = ThreadLocal<MutableMap<String, Long>?>()
    fun <T> phase(name: String, work: () -> T): T {
        checkBudget()
        val start = System.nanoTime()
        try { return work() } finally {
            collector.get()?.let { it[name] = (it[name] ?: 0L) + System.nanoTime() - start }
        }
    }
    fun <T> measure(work: () -> T): Pair<T, Map<String, Long>> {
        val previous = collector.get()
        val phases = linkedMapOf<String, Long>()
        collector.set(phases)
        try { return phase("total", work) to phases.toMap() } finally { collector.set(previous) }
    }
}
