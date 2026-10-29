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
            if (nodes and 255 == 0) PreparationMetrics.checkBudget()
            nodes++
            // Same stable move order as Game.matchingPairs; avoid rebuilding and
            // validating an immutable board at every search node.
            val free = remaining.filter { Rules.available(it, remaining) }
            val moves = mutableListOf<PairMove>()
            for (i in free.indices) for (j in i + 1 until free.size) {
                if (free[i].symbol == free[j].symbol) moves.add(PairMove(free[i].id, free[j].id))
            }
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
    val searchNodes: Int, val layout: String, val seed: Long,
    val strategic: StrategicAnalysis? = null
) {
    val initialBlocked get() = tileCount - initialFree
    val pairRemovals get() = tileCount / 2
    val complete get() = unknownAlternatives == 0
}
data class MoveImpact(val move: PairMove, val newlyAvailable: Set<Int>, val result: SearchResult)
object PuzzleAnalyzer {
    fun strategic(level: LevelDefinition, budget: Int = 1800) = StrategicAnalyzer.analyze(level, budget)
    fun analyzeTray(tiles: List<Tile>, snapshot: GameSnapshot, budget: Int = 2000) =
        TrayAnalyzer.analyze(tiles, snapshot, budget)
    /** Every legal association, with a shared budget. Empty unlock sets can be equivalent
     * immediately without being equivalent for eventual solvability. */
    fun alternatives(tiles: List<Tile>, budget: Int = 10000): List<MoveImpact> {
        require(budget >= 0)
        val game = Game(tiles.filterNot { it.removed })
        val before = game.remaining.filter { game.isAvailable(it.id) }.map { it.id }.toSet()
        var nodes = 0
        return game.matchingPairs().map { move ->
            val remaining = game.remaining.filter { it.id != move.first && it.id != move.second }
            val result = PuzzleSolver.solve(remaining, budget - nodes)
            nodes += result.nodes
            MoveImpact(move, remaining.filter { Rules.available(it, remaining) && it.id !in before }
                .map { it.id }.toSet(), result)
        }
    }

    /** Full-state alternatives sampled along the individual-action witness. Shared hard node cap.
     * Unexamined states are not called safe or losing. Pair-era metrics remain historical only. */
    private fun analyzeSelections(level: LevelDefinition, budget: Int): PuzzleAnalysis {
        val game = Game(level.tiles)
        val free = game.remaining.count { game.isAvailable(it.id) }
        val pairs = game.matchingPairs()
        val branching = mutableListOf<Int>()
        var nodes = 0; var safe = 0; var dead = 0; var unknown = 0
        var consequences = 0; var risky = 0; var unlocks = 0
        level.winningSelections.forEachIndexed { index, id ->
            val legal = game.remaining.filter { game.isAvailable(it.id) }
            branching.add(legal.size)
            if (index % 6 == 0) {
                val a = TrayAnalyzer.analyze(level.tiles, game.snapshot(), budget - nodes)
                nodes += a.result.nodes; safe += a.safeContinuations
                dead += a.losingContinuations; unknown += a.unknownContinuations
                if (a.safeContinuations > 0 && a.losingContinuations > 0) risky++
                val signatures = legal.map { tile ->
                    val next = game.fork(); check(next.select(tile.id))
                    next.remaining.filter { next.isAvailable(it.id) && legal.none { before -> before.id == it.id } }
                        .map { it.id }.toSet() to next.tray.map { it.symbol }.toSet()
                }.distinct()
                if (signatures.size > 1) consequences++
            }
            check(game.select(id))
            if (game.remaining.any { game.isAvailable(it.id) && legal.none { old -> old.id == it.id } }) unlocks++
        }
        return PuzzleAnalysis(level.tiles.size, level.tiles.map { it.layer }.distinct().size,
            level.tiles.map { it.symbol }.distinct().size, free, pairs.size,
            pairs.map { p -> level.tiles.first { it.id == p.first }.symbol }.distinct().size,
            branching, branching.count { it == 1 }, branching.count { it > 1 }, unlocks,
            consequences, risky, safe, dead, unknown, nodes, level.layout.id, level.seed, level.strategicAnalysis)
    }

    /** Entire analysis shares a strict node budget, including all witness alternatives. */
    fun analyze(level: LevelDefinition, budget: Int = 10000): PuzzleAnalysis {
        require(budget >= 0)
        if (level.tiles.isEmpty()) {
            require(level.layout.slots.isEmpty() && level.witness.isEmpty())
            return PuzzleAnalysis(0,0,0,0,0,0,emptyList(),0,0,0,0,0,0,0,0,0,level.layout.id,level.seed)
        }
        LevelValidator.validate(level)
        if (level.selectionWitness != null) return analyzeSelections(level, budget)
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
            for (impact in alternatives(game.remaining, budget - nodes)) {
                signatures.add(impact.newlyAvailable)
                nodes += impact.result.nodes
                when (impact.result.outcome) {
                    Solvability.SOLVABLE -> { safe++; stateSafe = true }
                    Solvability.UNSOLVABLE -> { dead++; stateDead = true }
                    else -> unknown++
                }
            }
            if (signatures.size > 1) consequences++
            if (stateSafe && stateDead) risky++
            val removedLayer = game.remaining.filter { it.id == step.first || it.id == step.second }.maxOf { it.layer }
            check(game.match(step.first, step.second))
            if (removedLayer > 0 && game.remaining.any { it.layer < removedLayer && it.id !in before && game.isAvailable(it.id) }) unlocks++
        }
        return PuzzleAnalysis(level.tiles.size, level.tiles.map { it.layer }.distinct().size,
            level.tiles.map { it.symbol }.distinct().size, initialFree, initialPairs.size,
            initialPairs.map { p -> level.tiles.first { it.id == p.first }.symbol }.distinct().size,
            profile, profile.count { it == 1 }, profile.count { it > 1 }, unlocks, consequences, risky,
            safe, dead, unknown, nodes, level.layout.id, level.seed)
    }
}

/** Bounded individual-action analysis. UNKNOWN never terminates a live attempt. */
object TrayAnalyzer {
    data class Analysis(val result: SearchResult, val available: Int, val unmatched: Int,
        val immediateLosses: Int, val matchingReveals: Int,
        val safeContinuations: Int, val losingContinuations: Int, val unknownContinuations: Int)
    fun analyze(tiles: List<Tile>, snapshot: GameSnapshot, budget: Int = 2000): Analysis {
        require(budget >= 0)
        val start = Game(tiles); require(start.restore(snapshot))
        var nodes = 0
        val memo = mutableMapOf<Pair<Set<Int>, List<Int>>, Solvability>()
        fun visit(g: Game): Solvability {
            if (g.status == Status.WON) return Solvability.SOLVABLE
            if (g.status == Status.LOST) return Solvability.UNSOLVABLE
            val key = g.remaining.map { it.id }.toSet() to g.tray.map { it.symbol.stableId }.sorted()
            memo[key]?.let { return it }
            if (nodes >= budget) return Solvability.UNKNOWN_BUDGET_EXCEEDED
            if (nodes and 255 == 0) PreparationMetrics.checkBudget()
            nodes++
            var unknown = false
            for (tile in g.remaining.filter { g.isAvailable(it.id) }) {
                val next = g.fork(); check(next.select(tile.id))
                when (visit(next)) {
                    Solvability.SOLVABLE -> { memo[key] = Solvability.SOLVABLE; return Solvability.SOLVABLE }
                    Solvability.UNKNOWN_BUDGET_EXCEEDED -> unknown = true
                    else -> Unit
                }
            }
            val result = if (unknown) Solvability.UNKNOWN_BUDGET_EXCEEDED else Solvability.UNSOLVABLE
            if (!unknown) memo[key] = result
            return result
        }
        if (start.status != Status.PLAYING) return Analysis(
            SearchResult(if (start.status == Status.WON) Solvability.SOLVABLE else Solvability.UNSOLVABLE, 0),
            0, start.tray.size, 0, 0, 0, 0, 0)
        val free = start.remaining.filter { start.isAvailable(it.id) }
        val losses = free.count { tile -> start.tray.size == 3 && start.tray.none { it.symbol == tile.symbol } }
        val reveals = free.count { tile ->
            val next = Game(tiles); check(next.restore(snapshot)); next.select(tile.id)
            next.remaining.any { next.isAvailable(it.id) && !start.isAvailable(it.id) && next.tray.any { held -> held.symbol == it.symbol } }
        }
        var safe = 0; var losing = 0; var unknown = 0
        for (tile in free) {
            val next = Game(tiles); check(next.restore(snapshot)); check(next.select(tile.id))
            when (visit(next)) {
                Solvability.SOLVABLE -> safe++
                Solvability.UNSOLVABLE -> losing++
                Solvability.UNKNOWN_BUDGET_EXCEEDED -> unknown++
            }
        }
        val result = when {
            start.status == Status.WON || safe > 0 -> Solvability.SOLVABLE
            unknown > 0 -> Solvability.UNKNOWN_BUDGET_EXCEEDED
            else -> Solvability.UNSOLVABLE
        }
        return Analysis(SearchResult(result, nodes), free.size, start.tray.size, losses, reveals, safe, losing, unknown)
    }
}
