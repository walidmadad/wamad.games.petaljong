package com.wamad.petaljong.domain

/** Individual tray actions, a shared node budget, and explicit uncertainty. No witness assumptions. */
object HintPolicy {
    data class Suggestion(val id: Int?, val outcome: Solvability, val nodes: Int, val kind: ActionKind = ActionKind.COLLECT)
    fun suggest(tiles: List<Tile>, snapshot: GameSnapshot, budget: Int = 300): Suggestion {
        require(budget >= 0)
        val start = Game(tiles)
        require(start.restore(snapshot, preserveTemporary = true))
        if (start.status != Status.PLAYING) return Suggestion(null, Solvability.UNSOLVABLE, 0)
        start.remaining.firstOrNull { start.activeMysteryId == null && it.concealed && start.isAvailable(it.id) }?.let {
            return Suggestion(it.id, Solvability.UNKNOWN_BUDGET_EXCEEDED, 0, ActionKind.REVEAL)
        }
        var nodes = 0
        val memo = mutableMapOf<Triple<Set<Int>, List<Int>, Int?>, Solvability>()
        fun after(g: Game, id: Int) = Game(tiles).also { check(it.restore(g.snapshot(), preserveTemporary = true)); if (it.tiles.first { t -> t.id == id }.concealed) check(it.reveal(id)); check(it.select(id)) }
        fun ordered(g: Game): List<Tile> {
            return g.remaining.filter { g.isAvailable(it.id) }
            .sortedWith(compareByDescending<Tile> { t -> g.tray.any { it.symbol == t.symbol } }
                .thenByDescending { it.layer }.thenBy { it.id })
        }
        fun visit(g: Game): Solvability {
            if (g.status == Status.WON) return Solvability.SOLVABLE
            if (g.status == Status.LOST) return Solvability.UNSOLVABLE
            val key = Triple(g.remaining.map { it.id }.toSet(), g.tray.map { it.symbol.stableId }.sorted(), g.activeMysteryId)
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
        val candidates = ordered(start).filter { !it.concealed }.map { it to after(start, it.id) }.filter { it.second.status != Status.LOST }
        if (candidates.isEmpty()) start.remaining.firstOrNull { it.concealed && start.isAvailable(it.id) }?.let {
            return Suggestion(it.id, Solvability.UNKNOWN_BUDGET_EXCEEDED, nodes, ActionKind.REVEAL)
        }
        val results = candidates.map { (tile, next) -> tile to visit(next) }
        // Immediate tray matches take precedence; within each priority prefer proof over uncertainty.
        val chosen = results.sortedWith(compareByDescending<Pair<Tile, Solvability>> { (t, _) -> start.tray.any { it.symbol == t.symbol } }
            .thenBy { if (it.second == Solvability.SOLVABLE) 0 else if (it.second == Solvability.UNKNOWN_BUDGET_EXCEEDED) 1 else 2 }).firstOrNull()
        return Suggestion(chosen?.first?.id, chosen?.second ?: Solvability.UNSOLVABLE, nodes)
    }
}
