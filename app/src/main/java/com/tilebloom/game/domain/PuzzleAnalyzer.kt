package com.tilebloom.game.domain

enum class Solvability { SOLVABLE, UNSOLVABLE, UNKNOWN_BUDGET_EXCEEDED }
data class SearchResult(val outcome: Solvability, val nodes: Int)
/** A per-call budget and immutable, arbitrary-size ID sets; no 64-tile assumption. */
object PuzzleSolver {
    fun solve(tiles: List<Tile>, budget: Int = 2000): SearchResult {
        require(budget >= 0)
        Game(tiles)
        var nodes = 0
        val memo = mutableMapOf<Set<Int>, Solvability>()
        fun visit(remaining: List<Tile>): Solvability {
            if (remaining.isEmpty()) return Solvability.SOLVABLE
            val key = remaining.map { it.id }.toSet()
            memo[key]?.let { return it }
            if (nodes >= budget) return Solvability.UNKNOWN_BUDGET_EXCEEDED
            nodes++
            val moves = Game(remaining).matchingPairs()
            var unknown = false
            for (p in moves) {
                when (visit(remaining.filter { it.id != p.first && it.id != p.second })) {
                    Solvability.SOLVABLE -> { memo[key] = Solvability.SOLVABLE; return Solvability.SOLVABLE }
                    Solvability.UNKNOWN_BUDGET_EXCEEDED -> unknown = true
                    else -> Unit
                }
            }
            val result = if (unknown) Solvability.UNKNOWN_BUDGET_EXCEEDED else Solvability.UNSOLVABLE
            if (!unknown) memo[key] = result
            return result
        }
        return SearchResult(visit(tiles.filterNot { it.removed }), nodes)
    }
}
data class PuzzleAnalysis(
    val tileCount: Int, val layers: Int, val symbols: Int, val initialFree: Int,
    val initialPairs: Int, val initialMatchingSymbols: Int, val branching: List<Int>,
    val forced: Int, val multiple: Int, val upperUnlockMoves: Int,
    val consequenceChoiceStates: Int, val riskyChoiceStates: Int,
    val solvableAlternatives: Int, val unsolvableAlternatives: Int, val unknownAlternatives: Int,
    val searchNodes: Int, val layout: String, val seed: Long
) {
    val initialBlocked get() = tileCount - initialFree
    val pairRemovals get() = tileCount / 2
    val complete get() = unknownAlternatives == 0
}
object PuzzleAnalyzer {
    /** Entire analysis shares a strict node budget, including all witness alternatives. */
    fun analyze(level: LevelDefinition, budget: Int = 10000): PuzzleAnalysis {
        require(budget >= 0)
        if (level.tiles.isEmpty()) {
            require(level.layout.slots.isEmpty() && level.witness.isEmpty())
            return PuzzleAnalysis(0,0,0,0,0,0,emptyList(),0,0,0,0,0,0,0,0,0,level.layout.id,level.seed)
        }
        LevelValidator.validate(level)
        val game = Game(level.tiles)
        val initialFree = game.remaining.count { game.isAvailable(it.id) }
        val initialPairs = game.matchingPairs()
        val profile = mutableListOf<Int>()
        var unlocks = 0; var consequences = 0; var risky = 0
        var safe = 0; var dead = 0; var unknown = 0; var nodes = 0
        for (step in level.witness) {
            val moves = game.matchingPairs()
            profile.add(moves.size)
            val before = game.remaining.filter { game.isAvailable(it.id) }.map { it.id }.toSet()
            val signatures = mutableSetOf<Set<Int>>()
            var stateSafe = false; var stateDead = false
            for (move in moves) {
                val remaining = game.remaining.filter { it.id != move.first && it.id != move.second }
                // Newly available IDs exclude the removed pair: order-independent moves share a signature.
                signatures.add(remaining.filter { Rules.available(it, remaining) && it.id !in before }.map { it.id }.toSet())
                val result = PuzzleSolver.solve(remaining, budget - nodes)
                nodes += result.nodes
                when (result.outcome) {
                    Solvability.SOLVABLE -> { safe++; stateSafe = true }
                    Solvability.UNSOLVABLE -> { dead++; stateDead = true }
                    else -> unknown++
                }
            }
            if (signatures.size > 1) consequences++
            if (stateSafe && stateDead) risky++
            val upper = game.remaining.any { (it.id == step.first || it.id == step.second) && it.layer > 0 }
            check(game.match(step.first, step.second))
            if (upper && game.remaining.any { it.layer == 0 && it.id !in before && game.isAvailable(it.id) }) unlocks++
        }
        return PuzzleAnalysis(level.tiles.size, level.tiles.map { it.layer }.distinct().size,
            level.tiles.map { it.symbol }.distinct().size, initialFree, initialPairs.size,
            initialPairs.map { p -> level.tiles.first { it.id == p.first }.symbol }.distinct().size,
            profile, profile.count { it == 1 }, profile.count { it > 1 }, unlocks, consequences, risky,
            safe, dead, unknown, nodes, level.layout.id, level.seed)
    }
}
