package com.wamad.petaljong.domain

import com.wamad.petaljong.presentation.board.BoardGeometry
import com.wamad.petaljong.presentation.board.TileVisibility

/** Deterministic observed-information baselines, not estimates of human ability.
 * All transitions, availability and losses use the production controller. */
object SimulatedPlayer {
    enum class Strategy { GREEDY, SIMPLE, LOOKAHEAD }
    data class Result(val won: Boolean, val actions: List<Int>, val nodes: Int, val peakTray: Int,
        val pressureStates: Int, val exhausted: Boolean, val snapshot: GameSnapshot? = null)
    fun play(level: LevelDefinition, strategy: Strategy, budget: Int = 1800, depth: Int = 6, tieBreak: Int = 0, legacyMysteries: Boolean = false): Result {
        require(budget >= 0 && depth in 1..10)
        if (level.version >= 12 && !legacyMysteries) return PlanningPlayer.play(level, strategy, budget, depth, tieBreak)
        val game = Game(level.tiles, legacyMysteries)
        var nodes = 0; var peak = 0; var pressure = 0
        fun next(g: Game, id: Int): Game = g.fork().also { check(it.select(id)) }
        fun ordered(g: Game, known: Set<Int>? = null): List<Tile> {
            val free = g.remaining.filter { !it.concealed && (known == null || it.id in known) && g.isAvailable(it.id) }
            val counts = free.groupingBy { it.symbol }.eachCount()
            val held = g.tray.map { it.symbol }.toSet()
            return free.sortedWith(compareByDescending<Tile> { it.symbol in held }
                .thenByDescending { counts.getValue(it.symbol) > 1 }.thenBy { if (tieBreak == 0) it.id.toLong() else {
                    val mixed = (it.id.toLong() + tieBreak * 7919L) * -7046029254386353131L
                    mixed xor (mixed ushr 32)
                } }.thenBy { it.id })
        }
        fun value(g: Game): Int = when (g.status) {
            Status.WON -> 100000
            Status.LOST -> -100000
            else -> -g.remaining.size * 20 - g.tray.size * 30
        }
        val geometry = BoardGeometry(360f, 520f, tiles = level.tiles)
        while (game.status == Status.PLAYING) {
            game.remaining.filter { it.concealed && game.isAvailable(it.id) }.forEach { check(game.reveal(it.id)) }
            // Freeze observed identities for this decision. Search may uncover unknown artwork,
            // but cannot select it or inspect its identity until the next real observation.
            val observed = if (strategy != Strategy.LOOKAHEAD) emptySet() else game.remaining.filter { t ->
                val c = geometry.face(t).center
                !t.concealed && TileVisibility.faceVisibleAt(t, game.tiles, geometry, c.x, c.y)
            }.map { it.id }.toSet() + game.remaining.filter { game.isAvailable(it.id) }.map { it.id }
            val choices = ordered(game)
            val selected = when (strategy) {
                Strategy.GREEDY -> choices.first()
                Strategy.SIMPLE -> choices.maxBy { t ->
                    val n = next(game, t.id)
                    (if (game.tray.any { it.symbol == t.symbol }) 1000 else 0) +
                        (if (choices.any { it.id != t.id && it.symbol == t.symbol }) 100 else 0) +
                        n.remaining.count { n.isAvailable(it.id) && !game.isAvailable(it.id) }
                }
                Strategy.LOOKAHEAD -> {
                    // Share the remaining budget fairly across roots and turns. Never use the witness.
                    val perRoot = maxOf(1, (budget - nodes) / maxOf(1, game.remaining.size * choices.size))
                    choices.maxBy { t ->
                        val before = nodes
                        val n = next(game, t.id)
                        val ceiling = minOf(budget, before + perRoot)
                        fun bounded(g: Game, d: Int): Int {
                            if (d == 0 || g.status != Status.PLAYING || nodes >= ceiling) return value(g)
                            nodes++; PreparationMetrics.checkBudget()
                            val known = ordered(g, observed)
                            if (known.isEmpty()) return value(g)
                            return known.maxOf { bounded(next(g, it.id), d - 1) }
                        }
                        bounded(n, depth - 1)
                    }
                }
            }
            check(game.select(selected.id))
            peak = maxOf(peak, game.tray.size)
            if (game.tray.size in 2..3) pressure++
        }
        return Result(game.status == Status.WON, game.snapshot().selections.orEmpty(), nodes, peak, pressure, nodes >= budget)
    }
}

data class StrategicAnalysis(val initialLegal: Int, val immediatePairs: Int,
    val blocked: Int, val coverRelationships: Int, val witnessPressure: Int, val witnessPeak: Int,
    val delayedMatches: Int, val longestPartnerDelay: Int,
    val greedy: SimulatedPlayer.Result, val simple: SimulatedPlayer.Result, val lookahead: SimulatedPlayer.Result,
    val blockingDepth: Int = 0, val greedyProbeWins: Int = 0, val simpleProbeWins: Int = 0, val probes: Int = 3,
    val witnessOneHeld: Int = 0, val witnessTwoHeld: Int = 0, val witnessThreeHeld: Int = 0,
    val consequentialTrayChoices: Int = 0, val provenImmediateTraps: Int = 0,
    val pairOnlyOutcome: Solvability = Solvability.UNKNOWN_BUDGET_EXCEEDED,
    val shortLookahead: SimulatedPlayer.Result? = null) {
    fun meets(tier: Difficulty) = when (tier) {
        Difficulty.NORMAL -> true
        Difficulty.HARD -> !greedy.won && greedyProbeWins <= 1 && witnessPressure > 0
        Difficulty.VERY_HARD -> !greedy.won && !simple.won && greedyProbeWins <= 2 && simpleProbeWins <= 2 &&
            lookahead.won && witnessPeak == 3 && delayedMatches >= 3 && blocked >= 30 && blockingDepth >= 3
        Difficulty.EXPERT -> !greedy.won && !simple.won && greedyProbeWins <= 1 && simpleProbeWins <= 1 &&
            lookahead.won && witnessPeak == 3 && delayedMatches >= 6 && blocked >= 34 && blockingDepth >= 4
    }
    fun score(tier: Difficulty): Int = (if (meets(tier)) 100000 else 0) +
        (if (!greedy.won) 500 else 0) + (if (!simple.won) 500 else 0) +
        (if (lookahead.won) 2000 else 0) - simpleProbeWins * 300 - greedyProbeWins * 150 + minOf(30, witnessPressure) * 5 +
        minOf(20, delayedMatches) * 8 + minOf(10, blockingDepth) * 3 +
        minOf(120, coverRelationships) / 10 - immediatePairs * 15 - lookahead.nodes / 200 +
        minOf(20, consequentialTrayChoices) * 12 + minOf(80, provenImmediateTraps) * 2 -
        (if (pairOnlyOutcome == Solvability.SOLVABLE) 500 else 0) +
        (if (shortLookahead?.won == false && lookahead.won) 12000 else 0)
}
object StrategicAnalyzer {
    fun analyze(level: LevelDefinition, budget: Int = 1800, adaptive: Boolean = false, legacyMysteries: Boolean = false): StrategicAnalysis {
        val game = Game(level.tiles, legacyMysteries)
        val initialLegal = game.remaining.count { game.isAvailable(it.id) }
        // Safe, free information must not earn a difficulty score by hiding immediate matches.
        game.remaining.filter { it.concealed && game.isAvailable(it.id) }.forEach { check(game.reveal(it.id)) }
        val pairs = if (level.version >= 12) game.remaining.filter { game.isAvailable(it.id) }
            .groupingBy { it.symbol }.eachCount().values.sumOf { it * (it - 1) / 2 }
            else game.matchingPairs().size
        if (!legacyMysteries) game.hideMystery()
        var pressure = 0; var peak = 0; var delayed = 0; var longest = 0
        var consequential = 0; var traps = 0
        val opened = mutableMapOf<Symbol, Int>()
        val heldCounts = IntArray(4)
        level.winningSelections.forEachIndexed { index, id ->
            if (level.version >= 12 && game.tray.size == 3) {
                check(game.activeMysteryId == null)
                val held = game.tray.map { it.symbol }.toSet()
                val losingTiles = game.remaining.filter { game.isAvailable(it.id) && it.symbol !in held }
                // Exact four-distinct rule, not a search-loss estimate. Check a production
                // transition per state instead of rebuilding every symbol-equivalent loss.
                losingTiles.firstOrNull()?.let { tile ->
                    val branch=game.fork()
                    if(tile.concealed) check(branch.reveal(tile.id))
                    check(branch.select(tile.id)); check(branch.status==Status.LOST)
                }
                val losing=losingTiles.size
                if (losing > 0) { consequential++; traps += losing }
            }
            val symbol = game.remaining.first { it.id == id }.symbol
            opened.remove(symbol)?.let { delay ->
                val gap = index - delay
                if (gap > 1) delayed++
                longest = maxOf(longest, gap)
            } ?: run { opened[symbol] = index }
            if (game.remaining.first { it.id == id }.concealed) check(game.reveal(id))
            check(game.select(id))
            check(game.status != Status.LOST)
            peak = maxOf(peak, game.tray.size)
            heldCounts[game.tray.size]++
            if (game.tray.size in 2..3) pressure++
        }
        check(game.status == Status.WON)
        var remaining = level.tiles
        var blockingDepth = -1
        while (remaining.isNotEmpty()) {
            val free = remaining.filter { Rules.available(it, remaining) }.map { it.id }.toSet()
            check(free.isNotEmpty())
            remaining = remaining.filter { it.id !in free }
            blockingDepth++
        }
        val greedy = SimulatedPlayer.play(level, SimulatedPlayer.Strategy.GREEDY, legacyMysteries = legacyMysteries)
        val simple = SimulatedPlayer.play(level, SimulatedPlayer.Strategy.SIMPLE, legacyMysteries = legacyMysteries)
        val greedyWins = (if (greedy.won) 1 else 0) + (1..2).count {
            SimulatedPlayer.play(level, SimulatedPlayer.Strategy.GREEDY, tieBreak = it, legacyMysteries = legacyMysteries).won }
        val simpleWins = (if (simple.won) 1 else 0) + (1..2).count {
            SimulatedPlayer.play(level, SimulatedPlayer.Strategy.SIMPLE, tieBreak = it, legacyMysteries = legacyMysteries).won }
        // Spend full lookahead work on candidates that can meet the shallow-player target.
        // Rejected candidates still get a real, explicitly bounded 100-node simulation.
        val tier = DifficultyPolicy.difficulty(level.id)
        val shallowTarget = when (tier) {
            Difficulty.NORMAL -> true
            Difficulty.HARD -> !greedy.won && greedyWins <= 1
            Difficulty.VERY_HARD -> !greedy.won && !simple.won && greedyWins <= 2 && simpleWins <= 2
            Difficulty.EXPERT -> !greedy.won && !simple.won && greedyWins <= 1 && simpleWins <= 1
        }
        val strongBudget = if (level.version >= 12) maxOf(0, budget - minOf(600, budget)) else budget
        val simulationBudget = if (adaptive && !shallowTarget) minOf(100, strongBudget) else strongBudget
        val shortBudget = if (adaptive && !shallowTarget) minOf(100, budget) else minOf(600, budget)
        return StrategicAnalysis(initialLegal, pairs, level.tiles.size - initialLegal,
            GeometryMetrics.measure(level.tiles).overlapRelationships, pressure, peak, delayed, longest,
            greedy, simple,
            SimulatedPlayer.play(level, SimulatedPlayer.Strategy.LOOKAHEAD, simulationBudget, legacyMysteries = legacyMysteries), blockingDepth, greedyWins, simpleWins, 3, heldCounts[1], heldCounts[2], heldCounts[3], consequential, traps,
            if (level.version >= 12) PuzzleSolver.solve(level.tiles.map { it.copy(mystery = false) }, 64).outcome else Solvability.UNKNOWN_BUDGET_EXCEEDED,
            if (level.version >= 12) PlanningPlayer.play(level, SimulatedPlayer.Strategy.LOOKAHEAD, shortBudget, 3) else null)
    }
}
