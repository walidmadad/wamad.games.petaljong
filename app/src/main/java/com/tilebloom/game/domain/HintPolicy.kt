package com.tilebloom.game.domain

/** Individual tray actions, a shared node budget, and explicit uncertainty. No witness assumptions. */
object HintPolicy {
    data class Suggestion(val id: Int?, val outcome: Solvability, val nodes: Int, val kind: ActionKind = ActionKind.COLLECT)
    fun suggest(tiles: List<Tile>, snapshot: GameSnapshot, budget: Int = 300): Suggestion {
        require(budget >= 0)
        val start = Game(tiles)
        require(start.restore(snapshot))
        if (start.status != Status.PLAYING) return Suggestion(null, Solvability.UNSOLVABLE, 0)
        start.remaining.firstOrNull { it.concealed && start.isAvailable(it.id) }?.let {
            return Suggestion(it.id, Solvability.UNKNOWN_BUDGET_EXCEEDED, 0, ActionKind.REVEAL)
        }
        var nodes = 0
        val memo = mutableMapOf<Pair<Set<Int>, List<Int>>, Solvability>()
        fun after(g: Game, id: Int) = Game(tiles).also { check(it.restore(g.snapshot())); check(it.select(id)) }
        fun ordered(g: Game): List<Tile> {
            g.remaining.filter { it.concealed && g.isAvailable(it.id) }.forEach { check(g.reveal(it.id)) }
            return g.remaining.filter { g.isAvailable(it.id) }
            .sortedWith(compareByDescending<Tile> { t -> g.tray.any { it.symbol == t.symbol } }
                .thenByDescending { it.layer }.thenBy { it.id })
        }
        fun visit(g: Game): Solvability {
            if (g.status == Status.WON) return Solvability.SOLVABLE
            if (g.status == Status.LOST) return Solvability.UNSOLVABLE
            val key = g.remaining.map { it.id }.toSet() to g.tray.map { it.symbol.stableId }.sorted()
            memo[key]?.let { return it }
            if (nodes >= budget) return Solvability.UNKNOWN_BUDGET_EXCEEDED
            nodes++
            var unknown = false
            for (t in ordered(g)) when (visit(after(g, t.id))) {
                Solvability.SOLVABLE -> return Solvability.SOLVABLE.also { memo[key] = it }
                Solvability.UNKNOWN_BUDGET_EXCEEDED -> unknown = true
                else -> Unit
            }
            return (if (unknown) Solvability.UNKNOWN_BUDGET_EXCEEDED else Solvability.UNSOLVABLE).also { memo[key] = it }
        }
        val candidates = ordered(start).map { it to after(start, it.id) }.filter { it.second.status != Status.LOST }
        val results = candidates.map { (tile, next) -> tile to visit(next) }
        // Immediate tray matches take precedence; within each priority prefer proof over uncertainty.
        val chosen = results.sortedWith(compareByDescending<Pair<Tile, Solvability>> { (t, _) -> start.tray.any { it.symbol == t.symbol } }
            .thenBy { if (it.second == Solvability.SOLVABLE) 0 else if (it.second == Solvability.UNKNOWN_BUDGET_EXCEEDED) 1 else 2 }).firstOrNull()
        return Suggestion(chosen?.first?.id, chosen?.second ?: Solvability.UNSOLVABLE, nodes)
    }
}
